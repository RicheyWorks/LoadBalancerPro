# Failure Log

## Unresolved blockers

None.

## Resolved campaign failures

- P-1.2: the first full-suite wrapper expired after 304 seconds while the existing 10,000-command ledger soak was still progressing. A clean-package rerun with a larger bound exited zero after 2,467 tests with zero failures/errors/skips.
- P-1.2: PowerShell split the first unquoted dotted CycloneDX property, so Maven rejected the invocation. Quoting each `-D` argument generated and validated fresh JSON/XML SBOMs.
- P-1.2: `pwsh` was unavailable and the default profile-smoke port 18182 was occupied. The same checked-in scripts passed under Windows PowerShell on a prechecked alternate loopback port range.
- P-1.2/P-1.3: local Docker image/runtime proof was unavailable because the installed client could not reach the absent Docker Desktop Linux engine. Remote Docker build and blocking Trivy image scan remain required.
- P-1.2: the written campaign lifecycle includes `LOCAL_GREEN`, but the manifest guard accepts only `OPEN`, `IN_PROGRESS`, and `MAIN_GREEN`. The slot remains `IN_PROGRESS` through remote gating; this product PR does not widen campaign-infrastructure scope.

## Reusable technical lessons

- Treat required checks as evidence for one exact head; stale, cancelled, skipped, or different-head results are not substitutes.
- Repair dependency or image findings in an isolated branch from green `main`; do not use allowlists, suppressions, or gate weakening.
- Derive the executable JAR from Maven's effective `project.build.finalName` in every consumer instead of hard-coding an artifact name.
- Cross-process durable state needs OS-level exclusion, atomic replacement, integrity verification, and fail-closed startup reconciliation.
- Timing-sensitive tests should coordinate on observable state and bounded convergence rather than narrow wall-clock assumptions.
- Keep external targets, credentials, and live mutation outside default tests; use loopback fixtures, mocked clients, and explicit operator gates.
- PowerShell `HttpListener` jobs blocked in `GetContext()` can make both stop and forced removal wait indefinitely; signal a loopback shutdown route, use a bounded wait, then remove the completed job.
