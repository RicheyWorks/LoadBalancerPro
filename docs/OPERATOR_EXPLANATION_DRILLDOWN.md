# Operator Explanation Drill-Down

The operator explanation drill-down lives inside the unified cockpit:

```text
http://localhost:8080/load-balancing-cockpit.html
```

It helps reviewers inspect why a packaged scenario produced its routing, allocation, overload, and advisory remediation output. The drill-down is browser-side only; it reuses existing endpoint responses and visible scenario input fields.

## What It Explains

- `Routing Strategy Explanation`: selected server, strategy status, API reason, visible health, weight, capacity, connection, latency, and queue inputs.
- `Allocation Math / Capacity Explanation`: requested load, returned allocations, unallocated load, recommended simulated capacity, and client-derived remaining capacity from visible request/response fields.
- `Load-Shedding / Overload Reason Breakdown`: evaluation decision reason, priority, action, queue depth, utilization, latency, error rate, healthy-server count, and read-only metrics state.
- `Remediation Rationale`: advisory-only `remediationPlan` status and recommendation reasons returned by the evaluate endpoint.
- `Scenario Delta Explanation`: changed scenario name, healthy allocation server count, total visible capacity, average routing pressure, unallocated load, and load-shedding action versus the prior run.

Exact internal scores and every internal threshold are not exposed by the current API. When the browser derives supporting math, the page labels it as derived from visible request/response fields and preserves the raw JSON as the source of truth.

The operator comparison matrix reuses the same explanation boundaries. It summarizes routing, allocation pressure, overload, remediation, and delta fields across scenarios without inventing scores or benchmark claims.

Operator replay mode also reuses these boundaries. Its before/after rationale deltas are derived from visible request/response fields and do not expose or invent unavailable internal scores.

The operator review packet includes the drill-down summary and operator rationale when they have been generated, or marks those sections as not generated yet.

The API contract trace maps each drill-down claim to endpoint paths, request payload sources, raw response sources, displayed raw fields, derived labels, unavailable fields, and safety notes. Use it when a reviewer wants to audit which text came from raw API responses and which text is client-side explanation derived from visible request/response fields.

The operator guided walkthrough places explanation review before matrix, replay, trace verification, and review packet generation so reviewers see raw-vs-derived rationale before copying a handoff.

The cockpit navigation/readiness layer can jump directly to `Explanation Drill-Down` and shows whether the explanation summary has been generated in the current page session.

## Browser Flow

1. Start the local app.
2. Open `http://localhost:8080/load-balancing-cockpit.html`.
3. Optionally click `Start walkthrough` in `Operator Guided Walkthrough`.
4. Select `Normal Load`, `Overload Pressure`, `All-Unhealthy Degradation`, or `Recovery / Capacity Restored`.
5. Click `Run selected scenario`.
6. Review raw JSON for `POST /api/routing/compare`, `POST /api/allocate/capacity-aware`, and `POST /api/allocate/evaluate`.
7. Review `Explanation Drill-Down`.
8. Run a second scenario, then click `Compare with previous scenario`.
9. Run `Operator Comparison Matrix` when you want all packaged scenario explanations summarized side by side.
10. Use `Operator Replay Mode` when you want one baseline/comparison pair highlighted side by side.
11. Generate `API Contract Trace` when you want raw-vs-derived contract mapping.
12. Generate `Operator Review Packet` when you want one copyable/printable handoff.
13. Copy the drill-down summary, matrix summary, replay reviewer note, API contract trace, review packet, explanation curl snippets, or operator rationale.

## Postman Parity

Import:

```text
postman/LoadBalancerPro.postman_collection.json
```

Set:

```text
baseUrl = http://localhost:8080
```

Run the `Operator Explanation Drill-Down` folder. It includes health, readiness, and per-scenario routing, allocation, overload, and remediation explanation requests using the same synthetic scenario bodies as the browser cockpit.

## Existing Endpoints Used

```text
GET  /api/health
GET  /actuator/health/readiness
POST /api/routing/compare
POST /api/allocate/capacity-aware
POST /api/allocate/evaluate
```

No new API contract is added for the drill-down.

## Safety Boundaries

- Local/operator demo only.
- Not certification.
- Not benchmark proof.
- Not legal compliance proof.
- Not identity proof.
- No cloud mutation.
- No `CloudManager` required.
- No external scripts, CDNs, services, or dependencies.
- No generated runtime reports.
- No server-side transcript writing.
- No fabricated allocation, routing, load-shedding, remediation, score, or threshold output.
