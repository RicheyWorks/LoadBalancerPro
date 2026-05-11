# Operator Navigation Readiness

The operator navigation and readiness layer is part of the unified browser cockpit:

```text
http://localhost:8080/load-balancing-cockpit.html
```

It helps reviewers move through the large cockpit without adding API behavior. The panel provides a section index, current-panel orientation text, in-memory readiness badges, and a deterministic copyable readiness summary.

## How To Use It

1. Start the local app.
2. Open the cockpit URL.
3. Use `Cockpit Navigation & Readiness` near the top of the page.
4. Click section index buttons to jump to major panels such as `Scenario Gallery`, `Explanation Drill-Down`, `Operator Comparison Matrix`, `Operator Replay Mode`, `Operator Review Packet`, `API Contract Trace`, and `Operator Guided Walkthrough`.
5. Click `Refresh readiness` after generating scenario, matrix, replay, trace, packet, or walkthrough output.
6. Click `Copy readiness summary` when a reviewer needs a deterministic status note.

## Readiness Badges

Readiness is computed from current page state and existing output containers only:

- `Scenario selected` means a packaged scenario is loaded in the scenario payload panel.
- `Scenario run` means allocation, routing, and evaluation endpoint outputs are present.
- `Raw JSON available` means allocation, routing, evaluation, and remediation raw JSON blocks have generated output.
- `Explanation generated` means the explanation drill-down summary has been generated.
- `Matrix generated` means comparison matrix rows exist.
- `Replay generated` means replay baseline/comparison output exists.
- `Trace generated` means API Contract Trace output exists.
- `Review packet generated` means Operator Review Packet output exists.
- `Walkthrough started` means the guided walkthrough is active or has completed steps.
- `Walkthrough completed or partially completed` shows completed walkthrough progress.

If evidence has not been produced in the current page session, the cockpit says `Not ready yet` or `not generated yet`.

## Safety Boundaries

- Client-side only.
- Readiness state is in memory only.
- No backend writes.
- No generated runtime reports.
- No cloud mutation.
- No browser storage.
- No external scripts, CDNs, fonts, images, services, or dependencies.
- No benchmark, certification, legal compliance, identity, or production guarantee claims.

## Limitations

The navigation panel does not prove that a reviewer inspected every raw JSON block. It is an orientation and readiness aid for the current browser session. It does not add endpoints, change routing or allocation behavior, or replace API tests, Postman checks, API Contract Trace review, or the Operator Review Packet.
