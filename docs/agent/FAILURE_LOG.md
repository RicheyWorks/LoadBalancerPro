# Failure Log

## Unresolved blockers

None.

## Reusable technical lessons

- Treat required checks as evidence for one exact head; stale, cancelled, skipped, or different-head results are not substitutes.
- Repair dependency or image findings in an isolated branch from green `main`; do not use allowlists, suppressions, or gate weakening.
- Derive the executable JAR from Maven's effective `project.build.finalName` in every consumer instead of hard-coding an artifact name.
- Cross-process durable state needs OS-level exclusion, atomic replacement, integrity verification, and fail-closed startup reconciliation.
- Timing-sensitive tests should coordinate on observable state and bounded convergence rather than narrow wall-clock assumptions.
- Keep external targets, credentials, and live mutation outside default tests; use loopback fixtures, mocked clients, and explicit operator gates.
- PowerShell `HttpListener` jobs blocked in `GetContext()` can make both stop and forced removal wait indefinitely; signal a loopback shutdown route, use a bounded wait, then remove the completed job.
