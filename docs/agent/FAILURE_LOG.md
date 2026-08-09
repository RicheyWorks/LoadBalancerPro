# Failure Log

## Unresolved blockers

None.

## Resolved campaign failures

- P-1.2: the first full-suite wrapper expired after 304 seconds while the existing 10,000-command ledger soak was still progressing. A clean-package rerun with a larger bound exited zero after 2,467 tests with zero failures/errors/skips.
- P-1.2: PowerShell split the first unquoted dotted CycloneDX property, so Maven rejected the invocation. Quoting each `-D` argument generated and validated fresh JSON/XML SBOMs.
- P-1.2: `pwsh` was unavailable and the default profile-smoke port 18182 was occupied. The same checked-in scripts passed under Windows PowerShell on a prechecked alternate loopback port range.
- P-1.2/P-1.3/P-1.4/P-1.5: local Docker image/runtime proof was unavailable because the installed client could not reach the absent Docker Desktop Linux engine. Remote Docker build and blocking Trivy image scan remain required.
- P-1.2: the written campaign lifecycle includes `LOCAL_GREEN`, but the manifest guard accepts only `OPEN`, `IN_PROGRESS`, and `MAIN_GREEN`. The slot remains `IN_PROGRESS` through remote gating; this product PR does not widen campaign-infrastructure scope.
- P-1.4: the first focused compatibility run failed because the legacy retry/cooldown test assumed a status read performed the recovery probe; the background prober could instead complete a successful probe before the fixture changed health. The test now coordinates on the initial snapshot and bounded background transitions.
- P-2.2: aggregate CodeQL blocked the first PR head because local proxy JSON errors reflected request-derived path and exception details. Client messages are now deterministic while detailed diagnostics remain server-side, with a regression assertion that unmatched paths are not echoed.
- P-2.5: the first remote Compose gate proved every deployment assertion but failed during teardown because the deliberately read-only TLS directory prevented the runner from deleting its ephemeral files. Teardown now restores owner write permission only after the containers are down, then removes the bounded temporary directory.
- P-3.1: the first full suite exposed that minimal proxy configuration-test contexts do not install a `MeterRegistry`; optional provider injection now uses the managed registry when present and an internal simple-registry fallback otherwise, preserving validation startup behavior and non-fatal instrumentation.
- P-3.2: a zero-I/O mocked timing gate produced scheduler-dominated overhead samples and blocked the first full suite; the gate now uses a saturated literal-loopback HTTP upstream, and the access-log handoff shares request timing and uses a bounded single-consumer ring.
- P-3.2: pre-PR concurrency review found that stop could exit between an access-log producer entering enqueue and publishing its record; in-flight producer accounting now keeps the writer draining, with concurrent stop/exact-once regressions.
- P-3.2: merge-main CI and the first repair head failed only the `<5%` overhead gate while repeated local runs passed; runner samples exposed continued warm-up plus avoidable monitors on request-owned observations, so the repair adds steady-state warm-up and an atomic exactly-once publication boundary without relaxing the workload, zero-drop assertion, or threshold.
- P-3.3: both first-head Linux CI runs failed the inherited `<5%` access-log overhead gate while request completion still issued an unconditional writer wake for every accepted record; the bounded writer now coalesces wakeups only while waiting, preserving the workload, threshold, zero-drop assertion, queue bound, and exact-once stop boundary.
- P-3.4: the first one-hour remote soak completed all 72,000 sustained-phase requests with zero 5xx/transport errors and local p99 observations below budget, but invalid heap extraction and a non-portable awk loop variable blocked the gate. Prometheus label values contain spaces, so the parser now sums `$NF`, rejects non-positive samples before and after the hour, and avoids awk's built-in `index` name; the failed run is not accepted as heap-trend evidence.
- P-4.1: the first full-suite run caught an IPv6 host-validation implementation that used a resolver-capable `InetAddress` API inside the shared route planner. The existing offline classifier guard correctly blocked the change; host validation now uses URI syntax only, with valid/invalid bracketed-IPv6 regressions, so route compilation remains free of DNS resolution and network probes.

## Reusable technical lessons

- Treat required checks as evidence for one exact head; stale, cancelled, skipped, or different-head results are not substitutes.
- Repair dependency or image findings in an isolated branch from green `main`; do not use allowlists, suppressions, or gate weakening.
- Derive the executable JAR from Maven's effective `project.build.finalName` in every consumer instead of hard-coding an artifact name.
- Cross-process durable state needs OS-level exclusion, atomic replacement, integrity verification, and fail-closed startup reconciliation.
- Timing-sensitive tests should coordinate on observable state and bounded convergence rather than narrow wall-clock assumptions.
- Keep external targets, credentials, and live mutation outside default tests; use loopback fixtures, mocked clients, and explicit operator gates.
- PowerShell `HttpListener` jobs blocked in `GetContext()` can make both stop and forced removal wait indefinitely; signal a loopback shutdown route, use a bounded wait, then remove the completed job.
- Servlet-local error responses must use fixed client-safe messages; retain request-derived paths and exception details only in bounded server-side diagnostics.
