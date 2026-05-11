# Operator API Contract Trace

The operator API contract trace is part of the unified browser cockpit:

```text
http://localhost:8080/load-balancing-cockpit.html
```

It maps visible cockpit panels back to the endpoint paths, request payload sources, raw response sources, displayed raw fields, client-side derived labels, unavailable fields, and safety notes used by the browser.

## How To Generate It

1. Start the local app.
2. Open the cockpit URL.
3. Load or run a scenario from `Scenario Gallery`.
4. Run `Operator Comparison Matrix`, `Operator Replay Mode`, `Explanation Drill-Down`, or `Operator Review Packet` as needed.
5. Click `Generate trace`.

If a panel has not been run yet, the trace marks the source as `not generated yet` or unavailable instead of inventing fields.

## What It Shows

- panel name
- endpoint path
- request payload source
- raw response source
- displayed raw fields
- derived fields and labels
- unavailable or missing fields
- mutation behavior and safety notes

The trace includes the scenario gallery, explanation drill-down, comparison matrix, replay mode, review packet, and the trace panel itself.

## Raw Fields Vs Derived Labels

Raw API fields are values returned by the existing endpoints, such as routing strategy results, selected server IDs, allocation unallocated load, load-shedding action, and advisory remediation-plan fields.

Derived labels are browser-side summaries built from visible request/response data, such as matrix rationale summaries, replay changed-field labels, and drill-down explanatory text. Exact internal scores and hidden thresholds are not exposed by the current API and are not invented.

## Review Packet Relationship

The `Operator Review Packet` includes an `API Contract Trace Summary` section when a trace has been generated. If no trace exists yet, the packet labels that section as not generated yet.

## Safety Boundaries

- Client-side only.
- Same-origin read/evaluation calls only.
- No backend writes.
- No cloud mutation.
- No browser storage.
- No generated runtime reports.
- No fake scoring.
- Not certification.
- Not benchmark proof.
- Not legal compliance proof.
- Not identity proof.
- No external scripts, CDNs, services, or dependencies.
