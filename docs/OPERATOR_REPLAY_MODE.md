# Operator Replay Mode

Operator replay mode is part of the unified browser cockpit:

```text
http://localhost:8080/load-balancing-cockpit.html
```

It lets a reviewer choose one packaged scenario as the baseline, choose another packaged scenario as the comparison, and replay both through the existing local API calls in deterministic order.

## What It Runs

Click `Replay selected pair` to run the baseline first and the comparison second:

```text
POST /api/routing/compare
POST /api/allocate/capacity-aware
POST /api/allocate/evaluate
```

Replay mode does not add a new API contract. It reuses the same packaged scenario payloads as the scenario gallery and comparison matrix.

## What It Shows

The replay panel shows side-by-side before/after values for:

- routing strategy and selected route summary
- selected server or outcome label
- allocation pressure summary
- load-shedding or overload signal
- remediation hint summary
- explanation or rationale summary
- scenario delta
- error state

Changed rows are highlighted in the browser. Missing fields and unavailable sections are shown as unavailable instead of being invented.

## Copy Controls

- `Copy reviewer note`: deterministic Markdown note for reviewer handoff.
- `Copy replay curls`: baseline and comparison curl commands for the existing endpoints.
- `Copy replay payloads`: baseline and comparison packaged scenario request bodies.

The copy output is generated client-side only. It is not written to disk, sent to a server-side report endpoint, or stored in browser storage.

## Review Packet Follow-Up

After replaying a pair, click `Generate review packet` in the cockpit to assemble the selected scenario summary, comparison matrix summary, replay delta summary, explanation notes, endpoint references, raw JSON reference notes, and safety notes into one deterministic copyable/printable handoff.

## Safety Boundaries

- Local/operator demo only.
- Not certification.
- Not benchmark proof.
- Not legal compliance proof.
- Not identity proof.
- No fake scoring.
- No cloud mutation.
- No browser `localStorage` or `sessionStorage`.
- No backend writes.
- No generated runtime reports.
- No external scripts, CDNs, services, or dependencies.
