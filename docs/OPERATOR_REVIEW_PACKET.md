# Operator Review Packet

The operator review packet is part of the unified browser cockpit:

```text
http://localhost:8080/load-balancing-cockpit.html
```

It assembles the current in-memory cockpit state into one deterministic Markdown-style handoff that can be copied or printed from the browser.

## How To Generate It

1. Start the local app.
2. Open the cockpit URL.
3. Start `Operator Guided Walkthrough` when you want a deterministic review order.
4. Load or run a scenario from `Scenario Gallery`.
5. Run `Operator Comparison Matrix` if matrix rows are needed.
6. Run `Operator Replay Mode` if before/after deltas are needed.
7. Review `Explanation Drill-Down` if rationale notes are needed.
8. Click `Generate trace` in `API Contract Trace` if endpoint/raw-field mapping is needed.
9. Use `Cockpit Navigation & Readiness` to refresh current-session readiness before final handoff if useful.
10. Click `Generate review packet`.

If a section has not been generated yet, the packet includes explicit `not generated yet` text instead of inventing data.

## What It Includes

- review packet summary
- guided walkthrough summary when started
- selected scenario summary
- comparison matrix summary
- replay delta summary
- explanation and operator rationale summary
- API contract trace summary when generated
- endpoint references
- payload and curl references
- raw JSON reference notes
- safety notes
- limitations and unavailable-field notes

## Copy And Print

- `Copy review packet` copies the deterministic Markdown-style handoff text.
- `Print review packet` uses the browser print dialog with print-focused styling for the packet panel.

The packet is not written to disk, sent to a backend report endpoint, or stored in browser storage.

The navigation/readiness summary is a separate copyable browser note. It can be copied alongside the packet when a reviewer wants an at-a-glance view of which cockpit panels generated evidence in the current page session.

## API Contract Trace Relationship

The packet includes an `API Contract Trace Summary` section. If the trace has been generated, that section maps cockpit output to endpoint paths, request payload sources, raw response sources, displayed raw fields, derived labels, unavailable fields, and safety notes. If the trace has not been generated, the packet labels the section as `not generated yet`.

## Safety Boundaries

- Client-side only.
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
