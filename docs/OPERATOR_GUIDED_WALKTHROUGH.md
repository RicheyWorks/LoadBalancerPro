# Operator Guided Walkthrough

The operator guided walkthrough is part of the unified browser cockpit:

```text
http://localhost:8080/load-balancing-cockpit.html
```

It gives reviewers a deterministic checklist for moving through scenario selection, endpoint execution, raw JSON review, explanation drill-down, comparison matrix generation, replay comparison, API contract trace verification, and review packet handoff.

## How To Use It

1. Start the local API.
2. Open the cockpit URL.
3. Click `Start walkthrough`.
4. Use `Next step`, `Previous step`, and `Mark step complete` to move through the checklist.
5. Use each `Jump to panel` control to move to the cockpit panel for the current step.
6. Run the existing cockpit controls when a step calls for evidence.
7. Click `Copy walkthrough summary` when the checklist should be attached to a reviewer note.

`Clear walkthrough` resets the checklist state in the browser only.

## Deterministic Step Order

1. Select packaged scenario.
2. Run scenario / endpoint evaluation.
3. Review raw JSON output.
4. Review explanation drill-down.
5. Generate comparison matrix.
6. Replay selected pair.
7. Generate API Contract Trace.
8. Verify raw-vs-derived fields.
9. Generate Operator Review Packet.
10. Copy or print final handoff.

## Expected Evidence

Each step shows the action to take next and the evidence expected from the existing cockpit panels. If evidence has not been generated, the walkthrough says `not generated yet`. Trace verification remains a manual review step because the browser can list raw API fields, derived client labels, and unavailable fields, but the operator still decides whether those notes are sufficient for the review.

## Safety Boundaries

- Client-side only.
- Walkthrough state is in memory only.
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

## Limitations

The walkthrough does not add API behavior. It guides existing cockpit controls and panels. It does not prove that a reviewer actually inspected every raw JSON field, and it does not replace API tests, Postman checks, or offline evidence workflows.
