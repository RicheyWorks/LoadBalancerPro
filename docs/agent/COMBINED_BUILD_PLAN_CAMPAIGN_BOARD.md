# Combined Build-Plan Goal Campaign Board

This board is the human-readable view of the 49-slot combined campaign defined by
[`COMBINED_BUILD_PLAN_CAMPAIGN_CONTRACT.md`](COMBINED_BUILD_PLAN_CAMPAIGN_CONTRACT.md). The authoritative machine-
checked inventory is [`COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json`](COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json).

## Campaign State

- Contract prerequisite: `CONTRACT-00` merged in [PR #496](https://github.com/RicheyWorks/LoadBalancerPro/pull/496);
  head `248203dc3046769f5b5e689c0c528a4b229fa322`, merge
  `7479482835e76938d11aaae00d9c99a35d0c0d6a`, exact-main CI and CodeQL green.
- Campaign-start main: `0f1e97b9ce4acceaad02877bf1fc2185997aba9d`.
- Source layout: `74b1f6758304bc5a3a85ff4888039e7309324ddf`, based on `e800ba06875d0897f8459ad14a5d5cf60dc34568`.
- Source item count: 50.
- Unique implementation slots: 49.
- `MAIN_GREEN` implementation slots: 4 / 49.
- Latest completed slot: `P-0.3` in merged [PR #501](https://github.com/RicheyWorks/LoadBalancerPro/pull/501);
  final head `f6b592937d05ae1853d7e00b834714a6f33721f6`, merge
  `8c491133041422b998d8e19eee5a18c827472ac8`, exact-main local package, CI, and CodeQL green.
- Active implementation slot: `P-0.4`; weight-zero drain and interrupted-retry classification are in
  [PR #502](https://github.com/RicheyWorks/LoadBalancerPro/pull/502), with exact-head remote gates pending.
- Next implementation slot after the active gate: `P-0.6`.

All `OPEN` rows are planned work, not evidence that the imported defect description remains exact or that the target
behavior exists. The active slot must reconcile its row with current main and the full source-plan acceptance contract.

## Board

| # | Canonical ID | Source ID(s) | Scope | Technical dependencies | Status |
| ---: | --- | --- | --- | --- | --- |
| 1 | SEC-DEFAULT-DENY | P-0.5, L-0.1 | Fail-closed auth, role matrix, actuator lockdown | — | MAIN_GREEN |
| 2 | P-0.1 | P-0.1 | Shutdown-hook lifecycle | — | MAIN_GREEN |
| 3 | P-0.2 | P-0.2 | Health drain, thresholds, re-admission | — | MAIN_GREEN |
| 4 | P-0.3 | P-0.3 | Route-scoped strategy instances | — | MAIN_GREEN |
| 5 | P-0.4 | P-0.4 | Weight-zero drain and retry classification | — | PR_OPEN |
| 6 | P-0.6 | P-0.6 | Simulation-core correctness batch | — | OPEN |
| 7 | L-0.2 | L-0.2 | Explorer size caps | SEC-DEFAULT-DENY | OPEN |
| 8 | L-0.3 | L-0.3 | Non-mutating CLI abort | SEC-DEFAULT-DENY | OPEN |
| 9 | L-0.4 | L-0.4 | Cockpit HTML-injection removal | SEC-DEFAULT-DENY | OPEN |
| 10 | L-0.5 | L-0.5 | Stable mocked-cloud ASG ownership | SEC-DEFAULT-DENY | OPEN |
| 11 | L-0.6 | L-0.6 | Ledger torn-read correctness | SEC-DEFAULT-DENY | OPEN |
| 12 | L-1.1 | L-1.1 | Delete metadata-about-metadata services | L0 complete | OPEN |
| 13 | L-1.2 | L-1.2 | One explainability module | L-1.1 | OPEN |
| 14 | L-1.3 | L-1.3 | Explainability correctness/determinism | L-1.2, P-0.3 | OPEN |
| 15 | L-1.4 | L-1.4 | One comparison/experiment path | L-1.3 | OPEN |
| 16 | L-2.1 | L-2.1 | One chained JSONL engine | L-0.6 | OPEN |
| 17 | L-2.2 | L-2.2 | Rotation and recovery | L-2.1 | OPEN |
| 18 | L-2.3 | L-2.3 | OS-lock-aware takeover | L-2.2 | OPEN |
| 19 | L-2.4 | L-2.4 | Proof tools/JavaFX out of production artifact | L-2.3 | OPEN |
| 20 | L-2.5 | L-2.5 | Honest durability and logging | L-2.4 | OPEN |
| 21 | L-4.1 | L-4.1 | Fix or retire CLI | L0 complete | OPEN |
| 22 | L-4.2 | L-4.2 | Delete or repair JavaFX | L0 complete | OPEN |
| 23 | L-4.3 | L-4.3 | Unify artifact selection | L0 complete | OPEN |
| 24 | L-4.4 | L-4.4 | Consolidate viewers and archive ceremony | L-1.1, L-2.4 | OPEN |
| 25 | P-1.1 | P-1.1 | Timeout correctness | Proxy M0 complete | OPEN |
| 26 | P-1.2 | P-1.2 | Upstream runtime stats | P-1.1 | OPEN |
| 27 | P-1.3 | P-1.3 | Live telemetry into routing | P-1.2 | OPEN |
| 28 | P-1.4 | P-1.4 | Background health checking | Proxy M0 complete | OPEN |
| 29 | P-1.5 | P-1.5 | Forwarding-header trust policy | SEC-DEFAULT-DENY | OPEN |
| 30 | P-1.6 | P-1.6 | Live shedding and concurrency limits | P-1.2, P-1.3 | OPEN |
| 31 | P-1.7 | P-1.7 | Consistent hash and cookie affinity | P-0.3, P-0.6 | OPEN |
| 32 | P-1.8 | P-1.8 | Retry budget, backoff, slow start | P-1.2, P-1.4 | OPEN |
| 33 | L-3.1 | L-3.1 | Capture real local proxy decisions | P-1.2, P-1.3 | OPEN |
| 34 | L-3.2 | L-3.2 | Real telemetry into async shadow | L-3.1, P-1.2, P-1.3 | OPEN |
| 35 | L-3.3 | L-3.3 | Explain a captured local decision | L-3.1, L-1.2, L-1.3 | OPEN |
| 36 | L-3.4 | L-3.4 | Gated local/mock actuation | L-3.2, L-0.5, L-2.5 | OPEN |
| 37 | P-2.1 | P-2.1 | Streaming request path | P-1.1 | OPEN |
| 38 | P-2.2 | P-2.2 | Streaming response path | P-2.1 | OPEN |
| 39 | P-2.3 | P-2.3 | Local TLS/SNI/reload/backend TLS | P-2.2 | OPEN |
| 40 | P-2.4 | P-2.4 | Graceful shutdown and draining reload | P-1.4, P-1.8 | OPEN |
| 41 | P-2.5 | P-2.5 | Bounded local deployment packaging | P-2.3, P-2.4, SEC-DEFAULT-DENY | OPEN |
| 42 | P-3.1 | P-3.1 | Micrometer proxy metrics | P-1.2, P-2.5 | OPEN |
| 43 | P-3.2 | P-3.2 | Structured async access log | P-2.5 | OPEN |
| 44 | P-3.3 | P-3.3 | Authenticated admin API v1 | P-2.4, SEC-DEFAULT-DENY | OPEN |
| 45 | P-3.4 | P-3.4 | Bounded local benchmark/soak harness | P-2.5, P-3.1, P-3.2, P-3.3 | OPEN |
| 46 | P-4.1 | P-4.1 | Host/header routing and percentage splits | P-2.5 | OPEN |
| 47 | P-4.2 | P-4.2 | DNS service discovery | P-2.5 | OPEN |
| 48 | P-4.3 | P-4.3 | HTTP/2 and WebSocket stack decision | P-2.2 | OPEN |
| 49 | P-4.4 | P-4.4 | Quarantine simulation/lab artifact content | L-2.4, P-2.5 | OPEN |

## Checkpoint Rule

A row changes only in the PR whose exact head contains the supporting checkpoint. `MERGED` does not count as
completion; only `MAIN_GREEN` does. Record branch, PR, head, changed files, focused/full verification, remote checks,
merge commit, scope/safety audit, and remaining not-proven boundaries in `SESSION_MANAGER.md`.

This board authorizes no live cloud, tenant, public, or production target and proves no production readiness,
production certification, throughput/p95/p99, load/stress/soak result, distributed durability, or broader automation.
