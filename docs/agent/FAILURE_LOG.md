# Material Failure Log

## Unresolved Defects

None.

## Reusable Engineering Lessons

- Required checks are evidence for one exact commit; stale, skipped, cancelled, or different-head results are not
  substitutes.
- Durable cross-process state needs OS-level exclusion, atomic replacement, integrity verification, and fail-closed
  startup reconciliation.
- Timing-sensitive tests should coordinate on observable state and bounded convergence. Hosted microbenchmarks belong
  in an explicit diagnostic lane with raw samples, not a deterministic correctness suite.
- External targets, credentials, and live mutation stay outside default tests; use loopback fixtures, mocks, and
  explicit operator gates.
- Client error responses use fixed safe messages; request-derived paths and exception details remain in bounded
  server-side diagnostics.
- Cleanup for deliberately read-only temporary material must restore owner permissions only after the consuming process
  has stopped.
