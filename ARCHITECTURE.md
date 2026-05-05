# Incident Management System — Architecture & Design Documentation

> **Version:** 1.0.0 | **Stack:** Spring Boot 3.2, PostgreSQL, MongoDB, React + TypeScript

---

## 1. System Overview

The Incident Management System (IMS) is a production-grade platform designed to ingest, triage, investigate, and close infrastructure incidents at scale. It is built around two core design principles:

1. **High-throughput async signal ingestion** — capable of handling 10,000 signals/second via an in-memory buffered pipeline.
2. **Strict, auditable incident lifecycle** — enforced by the GoF State pattern, preventing invalid workflow transitions and requiring an RCA before closure.

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND (React + TS)                    │
│     Dashboard  │  Incident List  │  Incident Detail  │  RCA     │
└──────────────────────────────┬──────────────────────────────────┘
                               │  REST + WebSocket (STOMP)
┌──────────────────────────────▼──────────────────────────────────┐
│                   BACKEND (Spring Boot 3.2 / Java 17)           │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                    REST Controllers                         │  │
│  │  SignalController │ IncidentController │ RCA │ Dashboard   │  │
│  └────────────────┬──────────────────────────────────────────┘  │
│                   │                                              │
│  ┌────────────────▼──────────────────────────────────────────┐  │
│  │                    Service Layer                            │  │
│  │  SignalService │ IncidentService │ RCAService │ Dashboard  │  │
│  └────────────────┬──────────────────────────────────────────┘  │
│          ┌────────┴───────────────────────┐                      │
│          │                                │                      │
│  ┌───────▼──────┐              ┌─────────▼───────────────────┐  │
│  │ Ingestion    │              │ State Machine + Alert Svc    │  │
│  │ Pipeline     │              │ (State + Strategy Patterns)  │  │
│  │ (Buffer+     │              └─────────────────────────────┘  │
│  │  Scheduler)  │                                               │
│  └───────┬──────┘                                               │
│          │                                                       │
│  ┌───────▼──────────────┐  ┌───────────────────────────────┐   │
│  │   MongoDB            │  │   PostgreSQL (Supabase)        │   │
│  │   (Signal Audit Log) │  │   (WorkItems, RCA Records)     │   │
│  └──────────────────────┘  └───────────────────────────────┘   │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │   Caffeine Cache (in-process)                              │  │
│  │   - signalComponentWorkItemDebouncer (10s TTL, 50k cap)   │  │
│  │   - ConcurrentMapCacheManager (dashboard summary)          │  │
│  └────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Package Structure

```
com.ims
├── ImsApplication.java            # Spring Boot entry point
│
├── config/
│   ├── CorsConfig.java            # Cross-origin request policy
│   ├── RedisConfig.java           # Caffeine cache + CacheManager beans
│   └── WebSocketConfig.java       # STOMP WebSocket broker configuration
│
├── controller/
│   ├── SignalController.java       # POST /api/v1/signals/ingest[/batch]
│   ├── IncidentController.java     # CRUD + lifecycle transitions
│   ├── RCAController.java          # RCA submit / fetch / update
│   └── DashboardController.java    # Summary, metrics, heatmap
│
├── service/
│   ├── SignalService.java          # Core ingestion + debounce logic
│   ├── IncidentService.java        # Lifecycle orchestration + WebSocket push
│   ├── RCAService.java             # RCA CRUD + MTTR calculation trigger
│   ├── DashboardService.java       # Aggregation, caching
│   ├── AlertService.java           # Strategy pattern context
│   └── alert/
│       ├── AlertStrategy.java      # Strategy interface
│       ├── P0AlertStrategy.java    # Critical alert
│       ├── P1AlertStrategy.java    # High alert
│       ├── P2AlertStrategy.java    # Medium alert
│       └── DefaultAlertStrategy.java
│
├── ingestion/
│   └── SignalIngestionPipeline.java  # LinkedBlockingQueue + @Scheduled drain
│
├── statemachine/
│   ├── IncidentStateMachine.java   # State machine orchestrator
│   └── state/
│       ├── IncidentState.java      # State interface (default throws)
│       ├── OpenState.java          # OPEN → INVESTIGATING only
│       ├── InvestigatingState.java # INVESTIGATING → RESOLVED only
│       ├── ResolvedState.java      # RESOLVED → CLOSED only
│       └── ClosedState.java        # Terminal state (no transitions)
│
├── model/
│   ├── WorkItem.java               # JPA entity — PostgreSQL
│   ├── Signal.java                 # MongoDB document
│   └── RCA.java                    # JPA entity — PostgreSQL
│
├── repository/
│   ├── WorkItemRepository.java     # Spring Data JPA
│   ├── SignalRepository.java       # Spring Data MongoDB
│   └── RCARepository.java          # Spring Data JPA
│
├── dto/
│   ├── SignalRequest.java          # Inbound signal payload
│   ├── StatusUpdateRequest.java    # Lifecycle transition payload
│   ├── RCARequest.java             # RCA submission payload
│   └── ApiResponse.java            # Unified API envelope
│
└── exception/
    ├── GlobalExceptionHandler.java
    ├── IncidentNotFoundException.java
    ├── InvalidStateTransitionException.java
    ├── RCAIncompleteException.java
    └── RateLimitExceededException.java
```

---

## 4. Design Patterns

### 4.1 State Pattern — Incident Lifecycle

**Problem:** Incident workflow transitions must be strictly enforced. An incident that is CLOSED must not be re-opened; an OPEN incident cannot skip directly to RESOLVED.

**Solution:** Each lifecycle status is modelled as a concrete `IncidentState` implementation. The `IncidentState` interface provides default implementations of every transition method that throw `InvalidStateTransitionException`. Concrete states only override the transitions they permit.

```
                  ┌──────┐
                  │ OPEN │
                  └──┬───┘
    toInvestigating() │  (only valid transition from OPEN)
                  ┌───▼────────────┐
                  │ INVESTIGATING  │
                  └──┬─────────────┘
        toResolved() │  (only valid from INVESTIGATING)
                  ┌───▼──────┐
                  │ RESOLVED │
                  └──┬───────┘
          toClosed() │  (only valid from RESOLVED + requires RCA)
                  ┌───▼──────┐
                  │ CLOSED   │  ← Terminal state
                  └──────────┘
```

**Spring Integration:** Each concrete state is a `@Component` with a name matching the pattern `{STATUS}State` (e.g. `@Component("OPENState")`). Spring auto-collects them into a `Map<String, IncidentState>` injected into `IncidentStateMachine`. The machine resolves the handler by looking up `currentStatus.name() + "State"`.

**Key benefit:** Adding a new state (e.g. `ESCALATED`) requires only a new `@Component` class — the machine and service layer are unmodified (Open/Closed Principle).

---

### 4.2 Strategy Pattern — Priority-Based Alerting

**Problem:** Different incident priorities (P0/P1/P2) require different alerting behaviours (paging on-call, sending email, logging only). The selection logic should be open to extension.

**Solution:** `AlertStrategy` interface with concrete implementations registered as Spring beans under the naming convention `{PRIORITY}AlertStrategy`.

```
AlertService (Context)
       │
       ├── P0AlertStrategy  → page on-call  (log.error)
       ├── P1AlertStrategy  → team alert    (log.warn)
       ├── P2AlertStrategy  → ticket only   (log.info)
       └── DefaultAlertStrategy → fallback  (log.info)
```

`AlertService.sendAlert()` resolves the strategy bean by name from the injected `Map<String, AlertStrategy>`. Unknown priorities fall back to `DefaultAlertStrategy`.

---

### 4.3 Async Ingestion Pipeline — Producer/Consumer

**Problem:** Signal ingestion must not be blocked by database write latency. The API must return in sub-millisecond time.

**Solution:** A `LinkedBlockingQueue<SignalRequest>` (capacity 50,000) decouples the HTTP accept path from the persistence path.

```
HTTP POST /ingest
    │
    ▼
Bucket4j rate limiter (5,000 req/sec)
    │
    ▼
pipeline.enqueue(signal)  →  LinkedBlockingQueue (non-blocking offer)
    │                              ↑
    │                     Returns HTTP 202 immediately
    │
    ▼ (background thread, every 100ms)
processBatch() — drains up to 500 signals
    │
    ▼
SignalService.ingestBatch()
    │
    ├── Caffeine debouncer lookup (10s TTL)
    │       Hit  → increment signalCount on existing WorkItem
    │       Miss → create new WorkItem + sendAlert
    │
    └── Signal persisted to MongoDB
```

**Backpressure:** If the buffer is full, `enqueue()` returns `false` (non-blocking `offer()`), and the API returns `503 Service Unavailable`. Failed batch persistence re-queues signals for retry.

---

### 4.4 Caffeine-Based Debouncing

**Problem:** Multiple signals arriving for the same component within a short window should be correlated to one WorkItem, not spawn N new incidents.

**Solution:** A Caffeine cache (`signalComponentWorkItemDebouncer`) maps `componentId → workItemId` with a 10-second TTL and maximum 50,000 entries.

```
Signal arrives for "CACHE_CLUSTER_01"
    │
    ▼
Cache.getIfPresent("CACHE_CLUSTER_01") → Long workItemId?
    │
    ├── HIT  → load WorkItem, increment signalCount, persist Signal (debounced=true)
    │
    └── MISS → DB lookup for open WorkItem
                  │
                  ├── Found  → update cache, increment count
                  └── Not found → create new WorkItem, alert, cache it
```

---

## 5. Data Model

### 5.1 WorkItem (PostgreSQL — `work_items` table)

| Column         | Type           | Notes                                    |
|----------------|----------------|------------------------------------------|
| `id`           | BIGSERIAL PK   | Auto-generated                           |
| `component_id` | VARCHAR(255)   | e.g. `CACHE_CLUSTER_01`                  |
| `component_type`| VARCHAR(255)  | e.g. `DISTRIBUTED_CACHE`, `RDBMS`        |
| `status`       | VARCHAR(20)    | Enum: OPEN, INVESTIGATING, RESOLVED, CLOSED |
| `priority`     | VARCHAR(5)     | P0, P1, P2                               |
| `assigned_to`  | VARCHAR(255)   | Engineer handling the incident           |
| `notes`        | TEXT           | Investigation notes                      |
| `signal_count` | INT            | Number of correlated signals             |
| `created_at`   | TIMESTAMP      | Set on first persist (`@PrePersist`)     |
| `updated_at`   | TIMESTAMP      | Refreshed on every update (`@PreUpdate`) |
| `closed_at`    | TIMESTAMP      | Set when status → CLOSED                 |

### 5.2 RCA (PostgreSQL — `rca_records` table)

| Column                  | Type      | Notes                                    |
|-------------------------|-----------|------------------------------------------|
| `id`                    | BIGSERIAL | Auto-generated                           |
| `work_item_id`          | BIGINT UNIQUE | FK to work_items                     |
| `incident_start`        | TIMESTAMP | When the incident began                  |
| `incident_end`          | TIMESTAMP | When the incident was resolved           |
| `root_cause_category`   | VARCHAR   | INFRASTRUCTURE, CODE_BUG, etc.           |
| `root_cause_description`| TEXT      | Detailed analysis                        |
| `fix_applied`           | TEXT      | What was done to resolve                 |
| `prevention_steps`      | TEXT      | Action items to prevent recurrence       |
| `mttr_seconds`          | BIGINT    | Auto-calculated: `end - start` in seconds|
| `mttr_formatted`        | VARCHAR   | Human-readable, e.g. "2 hours 30 minutes"|
| `submitted_at`          | TIMESTAMP | When RCA was filed                       |

> **MTTR** is computed automatically in JPA lifecycle hooks (`@PrePersist`, `@PreUpdate`) — no manual calculation required.

### 5.3 Signal (MongoDB — `signals` collection)

| Field           | Type           | Notes                              |
|-----------------|----------------|------------------------------------|
| `_id`           | ObjectId       | Auto-generated by MongoDB          |
| `work_item_id`  | Long           | Reference to PostgreSQL WorkItem   |
| `component_id`  | String         | Component that emitted the signal  |
| `component_type`| String         | Component category                 |
| `error_code`    | String         | e.g. `CONNECTION_TIMEOUT`          |
| `severity`      | String         | Normalised to uppercase (P0/P1/P2) |
| `message`       | String         | Human-readable description         |
| `timestamp`     | LocalDateTime  | Signal generation time             |
| `metadata`      | Map<String,String> | Extra context (region, version) |
| `debounced`     | boolean        | `true` if linked to existing WorkItem |

---

## 6. API Reference

### Signal Ingestion

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/signals/ingest` | Ingest a single signal (async) |
| POST | `/api/v1/signals/ingest/batch` | Ingest a batch of signals (async) |
| GET | `/api/v1/signals/{workItemId}` | Get all signals for a WorkItem |

### Incidents

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/incidents` | List all incidents (paginated) |
| GET | `/api/v1/incidents/{id}` | Get incident by ID |
| GET | `/api/v1/incidents/active` | List non-CLOSED incidents |
| GET | `/api/v1/incidents/severity/{priority}` | Filter by priority |
| POST | `/api/v1/incidents/{id}/investigate` | OPEN → INVESTIGATING |
| POST | `/api/v1/incidents/{id}/resolve` | INVESTIGATING → RESOLVED |
| POST | `/api/v1/incidents/{id}/close` | RESOLVED → CLOSED (requires RCA) |
| PATCH | `/api/v1/incidents/{id}/status` | Generic status update |
| DELETE | `/api/v1/incidents/{id}` | Delete incident |

### RCA

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/rca/{incidentId}` | Submit RCA for incident |
| GET | `/api/v1/rca/{incidentId}` | Fetch RCA |
| PUT | `/api/v1/rca/{incidentId}` | Update RCA |
| GET | `/api/v1/rca/{incidentId}/mttr` | Get MTTR in seconds |

### Dashboard

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/dashboard/summary` | Status + priority counts |
| GET | `/api/v1/dashboard/realtime` | Cache-first dashboard state |
| GET | `/api/v1/dashboard/metrics` | Signal volume by severity |
| GET | `/api/v1/dashboard/heatmap` | Failures per component |

### WebSocket

| Destination | Direction | Payload |
|-------------|-----------|---------|
| `/topic/incidents` | Server → Client | `WorkItem` JSON on every lifecycle change |

---

## 7. Rate Limiting

`SignalController` uses **Bucket4j** (token bucket algorithm) to enforce an API-level rate limit:

- **Limit:** 5,000 tokens/second (greedy refill)
- **Single signal:** consumes 1 token
- **Batch:** consumes `batch.size()` tokens atomically
- **Exceeded:** returns `HTTP 429 Too Many Requests`

The bucket is local to the controller instance (no distributed coordination needed at this scale).

---

## 8. Error Handling

All exceptions are centralised in `GlobalExceptionHandler` (`@RestControllerAdvice`):

| Exception | HTTP Status | Trigger |
|-----------|-------------|---------|
| `IncidentNotFoundException` | 404 | ID not found |
| `InvalidStateTransitionException` | 400 | Illegal state change |
| `RCAIncompleteException` | 422 | Closing incident without RCA |
| `RateLimitExceededException` | 429 | Bucket4j threshold hit |
| `MethodArgumentNotValidException` | 400 | Bean Validation failures |
| `Exception` (fallback) | 500 | Unexpected errors |

All responses follow the `ApiResponse<T>` envelope:
```json
{
  "success": true/false,
  "message": "...",
  "data": { ... },
  "error": "...",
  "timestamp": "2026-05-03T12:00:00"
}
```

---

## 9. Caching Strategy

| Cache | Implementation | Key | TTL |
|-------|---------------|-----|-----|
| Signal debouncer | Caffeine `Cache<String, Long>` | `componentId` | 10 seconds |
| Dashboard summary | `ConcurrentMapCacheManager` | `"dashboard:summary"` | Evicted on incident close |
| Dashboard realtime | `ConcurrentMapCacheManager` | `"dashboard:realtime"` | No explicit TTL |

The dashboard cache is **manually invalidated** in `IncidentService.moveToClosed()` via `DashboardService.refreshCache()`.

---

## 10. Technology Stack

| Layer | Technology | Rationale |
|-------|-----------|-----------|
| Backend framework | Spring Boot 3.2 / Java 17 | Production-grade, ecosystem maturity |
| Relational DB | PostgreSQL (Supabase) | ACID guarantees for WorkItems + RCA |
| NoSQL audit log | MongoDB | Schema-flexible, high write-throughput for signals |
| ORM | Spring Data JPA / Hibernate | Reduces boilerplate for relational model |
| Cache | Caffeine + ConcurrentMapCacheManager | Zero-dependency in-process caching |
| Rate limiter | Bucket4j | Token-bucket, minimal overhead |
| Real-time push | Spring WebSocket (STOMP) | Live incident updates to frontend |
| Frontend | React 18 + TypeScript + Tailwind CSS | Type-safe, component-based UI |
| Build tool | Maven | Standard Spring Boot convention |
| Testing | JUnit 5 + Mockito + MockMvc | Industry standard |
