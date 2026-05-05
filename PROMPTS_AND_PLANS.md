# PROMPTS, SPECIFICATIONS, AND PLANS

This document contains the collected prompts, specifications, and implementation plans used during the development of the Incident Management System.

## 1. Specifications (Spec)
The core design and specifications for this system are documented in the `docs/DESIGN_AND_TESTING.md` file. It includes:
* System Architecture (High-throughput Signal Ingestion)
* Strategy Pattern for Alerting (P0, P1, P2 strategies)
* State Pattern for Incident Lifecycle (Open, Investigating, RCA, Resolved)
* Asynchronous Processing queues and In-Memory Debouncing.

## 2. Implementation Plans

### Phase 1: High-Throughput Ingestion & Debouncing
* **Goal**: Implement an async ingestion pipeline capable of handling high volumes of signals without blocking the HTTP threads.
* **Plan**:
  1. Use `Bucket4j` for API rate-limiting at the controller level.
  2. Implement an in-memory `SignalIngestionPipeline` using standard Java concurrent queues (`ConcurrentLinkedQueue`).
  3. Introduce `Caffeine` cache for signal debouncing to prevent duplicate alerts from overwhelming the system within short time windows.

### Phase 2: Design Patterns
* **Goal**: Refactor the codebase to utilize robust Object-Oriented design patterns for flexibility and maintainability.
* **Plan**:
  1. **State Pattern**: Create an `IncidentState` interface with implementations for `OpenState`, `InvestigatingState`, `RcaState`, and `ResolvedState`. Apply this to the `Incident` entity to control workflow transitions strictly.
  2. **Strategy Pattern**: Create an `AlertStrategy` interface with `P0AlertStrategy` (immediate, critical notification), `P1AlertStrategy`, and `P2AlertStrategy`. Apply this to determine routing logic based on signal severity.

### Phase 3: Frontend Modernization
* **Goal**: Redesign the React frontend to be aesthetically pleasing and highly responsive.
* **Plan**:
  1. Transition to a modern UI using Tailwind CSS.
  2. Implement a glassmorphic aesthetic for dashboards and detailed incident views.
  3. Create an RCA (Root Cause Analysis) form submission interface that enforces strict validation before an incident can be closed.

---

## 3. AI Prompts Used

Below is a summarized list of prompts used with the AI assistant (Google Gemini / Antigravity) to help build and debug this system:

**Prompt 1: Initial Backend Setup & Pattern Design**
> "I am building an Incident Management System in Spring Boot. I need to implement a high-throughput signal ingestion API. Can you help me set up an async pipeline using Java concurrency and Bucket4j for rate limiting? Also, I want to use the Strategy Pattern for routing P0 vs P1 alerts."

**Prompt 2: State Pattern Implementation**
> "Help me implement the State Design Pattern for an 'Incident' entity. The incident should move from OPEN -> INVESTIGATING -> RCA_REQUIRED -> RESOLVED. An incident cannot move to RESOLVED unless a valid RCA document has been provided."

**Prompt 3: Debugging OutOfMemoryErrors**
> "My Spring Boot backend is failing to start and crashing with an OutOfMemoryError during Maven compilation and JVM startup. How can I isolate the memory constraints, fix the compilation errors in SignalController related to ApiResponse method signatures, and ensure embedded MongoDB is configured correctly?"

**Prompt 4: Test Suite Development**
> "Write comprehensive unit tests for the AlertStrategy implementations and the IncidentState transitions. Then, write an integration test for the asynchronous signal ingestion pipeline to ensure debouncing works."

**Prompt 5: UI Redesign**
> "Help me redesign the frontend Dashboard, IncidentDetail, and RCAForm components using Tailwind CSS. It should have a modern, responsive, and glassmorphic design. Avoid generic colors and use smooth gradients and modern typography."

**Prompt 6: Cascading Failure Simulation**
> "Provide a script or JSON file to mock a failure event across the stack, simulating an RDBMS outage followed by an MCP failure."
