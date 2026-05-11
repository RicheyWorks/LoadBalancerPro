# Operator Comparison Matrix

The operator comparison matrix is part of the unified browser cockpit:

```text
http://localhost:8080/load-balancing-cockpit.html
```

It runs the packaged cockpit scenarios in a stable order and summarizes the real endpoint outputs in one reviewer-friendly table.

Use operator replay mode when a reviewer wants to focus on one baseline/comparison pair after scanning the full matrix.
Use the `Operator Guided Walkthrough` when a reviewer wants the matrix step placed in order between raw JSON/explanation review and replay comparison.
Use `Cockpit Navigation & Readiness` to jump to the matrix and confirm the `Matrix generated` badge after running all scenarios.

## What It Runs

Click `Run all scenarios` to execute:

1. `Normal Load`
2. `Overload Pressure`
3. `All-Unhealthy Degradation`
4. `Recovery / Capacity Restored`

For each scenario, the browser uses the same existing same-origin routes as the scenario gallery:

```text
POST /api/routing/compare
POST /api/allocate/capacity-aware
POST /api/allocate/evaluate
```

No new API contract is added.

## Matrix Columns

- `Scenario`: packaged scenario label.
- `Expected pressure / incident type`: scenario description and operator intent.
- `Routing strategy summary`: selected server or strategy status per strategy.
- `Selected server / outcome label`: unique selected servers, or a no-selection label when no route is returned.
- `Allocation pressure summary`: returned unallocated load and simulated additional-server count.
- `Load-shedding / overload signal`: returned load-shedding action, rejected load, and reason.
- `Remediation hint summary`: returned advisory remediation status and recommendation actions.
- `Explanation / rationale summary`: client-side summary derived from visible request/response fields.
- `Delta vs prior scenario`: unallocated load, rejected load, load-shedding action, and first routing selection compared to the previous scenario.
- `Raw status / error state`: `PASSED`, warning, or failure text for that row.

Missing fields or unavailable endpoints are shown as unavailable instead of being invented.

## Copy Controls

- `Copy matrix summary`: deterministic Markdown summary for reviewer notes.
- `Copy matrix curls`: scenario-by-scenario curl commands for the existing endpoints.
- `Copy matrix payloads`: packaged deterministic scenario request bodies.

The copy output is client-side text only. It is not written to disk or sent to a server-side report endpoint.

## Replay Follow-Up

After reviewing the matrix, select two scenario rows in the browser's `Operator Replay Mode` selectors and click `Replay selected pair`. Replay mode reruns the two packaged scenarios in deterministic order, highlights before/after routing, allocation, overload, remediation, rationale, and error-state differences, and produces a deterministic reviewer note.

When a reviewer needs one handoff artifact, click `Generate review packet` after the matrix or replay steps. The packet includes matrix output when available and labels it as not generated yet when it has not been run.

Run `API Contract Trace` after the matrix when a reviewer needs to audit matrix columns back to endpoint paths, packaged scenario payloads, raw response fields, derived labels, and unavailable-field handling.

## Safety Boundaries

- Local/operator demo only.
- Not certification.
- Not benchmark proof.
- Not legal compliance proof.
- Not identity proof.
- No fake scoring.
- No cloud mutation.
- No external scripts, CDNs, services, or dependencies.
- No browser `localStorage` or `sessionStorage`.
- No generated runtime reports.
- No server-side transcript writing.
