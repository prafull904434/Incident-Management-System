
from __future__ import annotations

import argparse
import sys
import time
import uuid
from typing import Any

try:
    import requests
    from requests.exceptions import ConnectionError as ReqConnectionError
    from requests.exceptions import ReadTimeout, Timeout
except ImportError:
    print("Install requests:  pip install requests", file=sys.stderr)
    sys.exit(1)


def ok(name: str, cond: bool, detail: str = "") -> bool:
    status = "PASS" if cond else "FAIL"
    extra = f"  ({detail})" if detail else ""
    print(f"[{status}] {name}{extra}")
    return cond


def pick_incident_id(
    rq: Any,
    base: str,
    *,
    component_contains: str | None = None,
    timeout_s: float = 90.0,
) -> int | None:
    """Poll incidents until one appears (optional filter on componentId substring)."""
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        r = rq("GET", f"{base}/api/v1/incidents", params={"page": 0, "size": 50})
        if r.status_code != 200:
            time.sleep(0.5)
            continue
        data = r.json()
        payload = data.get("data") or {}
        content = payload.get("content") if isinstance(payload, dict) else None
        if not content:
            time.sleep(0.5)
            continue
        for row in content:
            cid = row.get("componentId") or ""
            if component_contains is None or component_contains in cid:
                wid = row.get("id")
                if wid is not None:
                    return int(wid)
        time.sleep(0.5)
    return None


def valid_signal(component_suffix: str | None = None) -> dict[str, Any]:
    suf = component_suffix or uuid.uuid4().hex[:8]
    return {
        "componentId": f"EDGE_TEST_{suf}",
        "componentType": "API",
        "errorCode": "CONNECTION_TIMEOUT",
        "severity": "P2",
        "message": "edge-case script probe",
        "metadata": {"runId": suf},
    }


def valid_rca() -> dict[str, Any]:
    return {
        "incidentStart": "2026-01-01T10:00:00",
        "incidentEnd": "2026-01-01T11:30:00",
        "rootCauseCategory": "NETWORK",
        "rootCauseDescription": "Script-generated RCA",
        "fixApplied": "Restarted service",
        "preventionSteps": "Add monitoring",
    }


def run_tests(
    base: str,
    skip_load: bool,
    skip_lifecycle: bool,
    read_timeout: float,
    probe_actuator: bool,
) -> int:
    session = requests.Session()
    headers_json = {"Content-Type": "application/json"}
    failures = 0
    connect_t = min(5.0, read_timeout)
    timeout_pair = (connect_t, read_timeout)

    def rq(method: str, url: str, **kwargs: Any) -> requests.Response:
        kwargs.setdefault("timeout", timeout_pair)
        return session.request(method, url, **kwargs)

    print(f"\n=== IMS edge tests → {base} (HTTP timeouts connect={connect_t}s read={read_timeout}s)\n")

    # --- Reachability: incidents list uses PostgreSQL only (does not hang on Mongo health) ---
    try:
        r_ping = rq("GET", f"{base}/api/v1/incidents", params={"page": 0, "size": 1})
    except (ReqConnectionError, Timeout, ReadTimeout) as e:
        print(f"[FAIL] Backend not reachable at {base}\n        ({type(e).__name__}: {e})\n")
        print(
            "Hints: Start the Spring Boot app (mvn spring-boot:run). "
            "If connection refused, nothing listens on that port. "
            "If this keeps timing out, check PostgreSQL and firewall.\n"
        )
        return 1

    failures += not ok(
        "GET /incidents?page=0&size=1 (backend up; Postgres)",
        r_ping.status_code == 200,
        str(r_ping.status_code),
    )

    if probe_actuator:
        try:
            r_health = rq("GET", f"{base}/actuator/health")
            failures += not ok(
                "GET /actuator/health → 200",
                r_health.status_code == 200,
                str(r_health.status_code),
            )
        except ReadTimeout:
            print(
                "[WARN] /actuator/health timed out — MongoDB is often the cause "
                "(health aggregates mongo). Start Mongo or skip --probe-actuator.\n"
            )
            failures += 1

    r_summary = rq("GET", f"{base}/api/v1/dashboard/summary")
    failures += not ok("GET /dashboard/summary", r_summary.status_code == 200, str(r_summary.status_code))

    r_rt = rq("GET", f"{base}/api/v1/dashboard/realtime")
    failures += not ok("GET /dashboard/realtime", r_rt.status_code == 200, str(r_rt.status_code))

    # --- Signals ---
    r_ing = rq(
        "POST",
        f"{base}/api/v1/signals/ingest",
        json=valid_signal(),
        headers=headers_json,
    )
    failures += not ok(
        "POST /signals/ingest valid → 202",
        r_ing.status_code == 202,
        f"code={r_ing.status_code} body={r_ing.text[:200]}",
    )

    r_bad = rq(
        "POST",
        f"{base}/api/v1/signals/ingest",
        json={},
        headers=headers_json,
    )
    ok(
        "POST /signals/ingest empty JSON {{}} — HTTP response (often 202; worker may NPE — no @Valid)",
        r_bad.status_code in (202, 400, 422, 500),
        f"code={r_bad.status_code}",
    )

    r_batch_empty = rq(
        "POST",
        f"{base}/api/v1/signals/ingest/batch",
        json=[],
        headers=headers_json,
    )
    failures += not ok(
        "POST /signals/ingest/batch [] → 202",
        r_batch_empty.status_code == 202,
        str(r_batch_empty.status_code),
    )

    r_sig_nan = rq("GET", f"{base}/api/v1/signals/not-a-number")
    ok(
        "GET /signals/not-a-number (often 500 — parseLong)",
        r_sig_nan.status_code in (400, 404, 500),
        f"code={r_sig_nan.status_code}",
    )

    # --- Incidents ---
    r_nf = rq("GET", f"{base}/api/v1/incidents/999999999")
    failures += not ok(
        "GET /incidents/999999999 → 404",
        r_nf.status_code == 404,
        str(r_nf.status_code),
    )

    r_page = rq("GET", f"{base}/api/v1/incidents", params={"page": -1, "size": 10})
    ok(
        "GET /incidents?page=-1 (Spring may error or clamp)",
        r_page.status_code in (200, 400, 500),
        f"code={r_page.status_code}",
    )

    if not skip_load:
        print("\n--- Load-ish checks (rate limit / batch tokens) ---")
        codes: list[int] = []
        for _ in range(120):
            codes.append(
                rq(
                    "POST",
                    f"{base}/api/v1/signals/ingest",
                    json=valid_signal(),
                    headers=headers_json,
                    timeout=(3.0, 10.0),
                ).status_code
            )
        ok(
            "Burst POST ingest (120 fast) — watch for 429",
            202 in codes or 429 in codes,
            f"sample codes={sorted(set(codes))}",
        )

        big_batch = [valid_signal(str(i)) for i in range(6000)]
        r_big = rq(
            "POST",
            f"{base}/api/v1/signals/ingest/batch",
            json=big_batch,
            headers=headers_json,
            timeout=(10.0, 120.0),
        )
        ok(
            "POST batch size 6000 (may 429 whole batch)",
            r_big.status_code in (202, 429),
            str(r_big.status_code),
        )

    #Lifecycle
    if not skip_lifecycle:
        print("\n--- Incident lifecycle (async ingest + state machine) ---")

        uniq = uuid.uuid4().hex[:8]
        marker = f"LIFE_{uniq}"
        rq(
            "POST",
            f"{base}/api/v1/signals/ingest",
            json=valid_signal(marker),
            headers=headers_json,
        )

        wid = pick_incident_id(rq, base, component_contains=marker)
        if wid is None:
            print("[SKIP] No incident appeared in time — check Mongo + Postgres + worker logs")
            failures += 1
        else:
            ok("Found incident for ingested signal", True, f"id={wid} component contains {marker}")

            # OPEN
            fresh_marker = f"SKIPINV_{uuid.uuid4().hex[:8]}"
            rq(
                "POST",
                f"{base}/api/v1/signals/ingest",
                json=valid_signal(fresh_marker),
                headers=headers_json,
            )
            wid_open = pick_incident_id(rq, base, component_contains=fresh_marker, timeout_s=90.0)
            if wid_open is not None:
                r_skip = rq(
                    "POST",
                    f"{base}/api/v1/incidents/{wid_open}/resolve",
                    json={"status": "RESOLVED", "notes": "skip investigate"},
                    headers=headers_json,
                )
                failures += not ok(
                    "OPEN → resolve without investigate → 400",
                    r_skip.status_code == 400,
                    str(r_skip.status_code),
                )
            else:
                print("[WARN] Second incident not ready — skipped OPEN→resolve guard")

            st = {"status": "INVESTIGATING", "assignedTo": "tester", "notes": "edge script"}
            r_inv = rq(
                "POST",
                f"{base}/api/v1/incidents/{wid}/investigate",
                json=st,
                headers=headers_json,
            )
            failures += not ok(
                "POST /incidents/{id}/investigate",
                r_inv.status_code == 200,
                f"{r_inv.status_code} {r_inv.text[:300]}",
            )

            r_bad_resolve = rq(
                "POST",
                f"{base}/api/v1/incidents/{wid}/resolve",
                json={"status": "RESOLVED", "notes": "skip investigate test"},
                headers=headers_json,
            )
            ok(
                "POST resolve after investigate → 200",
                r_bad_resolve.status_code == 200,
                str(r_bad_resolve.status_code),
            )

            r_close_no_rca = rq(
                "POST",
                f"{base}/api/v1/incidents/{wid}/close",
                json={"status": "CLOSED", "notes": "should fail without RCA"},
                headers=headers_json,
            )
            failures += not ok(
                "POST close without RCA → 422",
                r_close_no_rca.status_code == 422,
                str(r_close_no_rca.status_code),
            )

            r_rca = rq(
                "POST",
                f"{base}/api/v1/incidents/{wid}/rca",
                json=valid_rca(),
                headers=headers_json,
            )
            failures += not ok("POST RCA", r_rca.status_code == 200, str(r_rca.status_code))

            r_rca_dup = rq(
                "POST",
                f"{base}/api/v1/incidents/{wid}/rca",
                json=valid_rca(),
                headers=headers_json,
            )
            ok(
                "POST duplicate RCA → 500 (current handler)",
                r_rca_dup.status_code == 500,
                str(r_rca_dup.status_code),
            )

            r_close_ok = rq(
                "POST",
                f"{base}/api/v1/incidents/{wid}/close",
                json={"status": "CLOSED", "notes": "closed with RCA"},
                headers=headers_json,
            )
            failures += not ok(
                "POST close with RCA → 200",
                r_close_ok.status_code == 200,
                str(r_close_ok.status_code),
            )

            r_inv_closed = rq(
                "POST",
                f"{base}/api/v1/incidents/{wid}/investigate",
                json=st,
                headers=headers_json,
            )
            ok(
                "POST investigate when CLOSED → 400",
                r_inv_closed.status_code == 400,
                str(r_inv_closed.status_code),
            )

            patch_bad = rq(
                "PATCH",
                f"{base}/api/v1/incidents/{wid}/status",
                json={"status": "NOT_A_STATUS"},
                headers=headers_json,
            )
            ok(
                "PATCH invalid status enum → 500",
                patch_bad.status_code == 500,
                str(patch_bad.status_code),
            )

    print(f"\n=== Done. Failures: {failures} ===\n")
    return 0 if failures == 0 else 1


def main() -> None:
    p = argparse.ArgumentParser(description="IMS API edge-case tests")
    p.add_argument("--base", default="http://localhost:8080", help="API origin (no trailing slash)")
    p.add_argument(
        "--timeout",
        type=float,
        default=90.0,
        metavar="SEC",
        help="Per-request read timeout in seconds (connect timeout is min(5, SEC); default 90)",
    )
    p.add_argument(
        "--probe-actuator",
        action="store_true",
        help="Also call GET /actuator/health (slow if MongoDB is down)",
    )
    p.add_argument(
        "--skip-load",
        action="store_true",
        help="Skip burst / 6000-batch tests",
    )
    p.add_argument(
        "--skip-lifecycle",
        action="store_true",
        help="Skip incident state-machine + RCA sequence",
    )
    args = p.parse_args()
    base = args.base.rstrip("/")
    sys.exit(
        run_tests(
            base,
            skip_load=args.skip_load,
            skip_lifecycle=args.skip_lifecycle,
            read_timeout=args.timeout,
            probe_actuator=args.probe_actuator,
        )
    )


if __name__ == "__main__":
    main()
