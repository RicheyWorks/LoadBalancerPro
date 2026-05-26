# Core LoadBalancer Feature Contract

## Campaign

- Campaign name: Core LoadBalancer Reliability Contract Campaign.
- Classification: WARN / core-feature-audit.
- Slot: Core-LB-G01 - Core load-balancer feature contract and invariant audit.
- Branch: `codex/core-loadbalancer-feature-contract`.
- Scope: documentation/test-only.

This contract is an audit and reviewer-navigation artifact. It does not add production code, Maven config, CI/workflow behavior, Dockerfile behavior, Compose behavior, scripts, runtime resources, endpoints, secrets, external targets, deployment behavior, runner services, automation, or production-looking defaults.

## Purpose

The campaign exists to make the core load-balancer behavior easier to review before future implementation hardening. It maps the legacy allocation facade, shared load-distribution planner, read-only evaluator, server registry, routing strategy registry, and request-level routing strategies into explicit invariants.

The first slot does not change runtime behavior. It records what the current code and tests already support, where reviewers should look, and which gaps should become later PR-sized goals.

## Audited Core Surface

- `LoadBalancer`: public allocation facade for server registration, health filtering, round-robin, least-loaded, weighted distribution, consistent hashing, capacity-aware allocation, predictive allocation, accumulated-load rebalance, failover, import/export, cloud manager shims, and shutdown lifecycle.
- `LoadDistributionPlanner`: deterministic planner for round-robin, least-loaded, weighted, capacity-aware, and predictive allocation math.
- `LoadDistributionEngine`: stateful allocation accumulator and domain-metric recording wrapper around the planner.
- `LoadDistributionEvaluator`: read-only preview path for capacity-aware and predictive allocation without mutating `LoadBalancer` accumulated distribution state.
- `Server`: validated server identity, health, weight, capacity, load score, metric history, snapshots, rollback, serialization, and property-change behavior.
- `ServerRegistry`: registry snapshot, healthy snapshot, type filtering, add/remove, duplicate replacement, and map snapshot behavior.
- `RoutingStrategyRegistry`: request-level strategy lookup and default registration order.
- Request-level routing strategies: `TailLatencyPowerOfTwoStrategy`, `WeightedLeastLoadStrategy`, `WeightedLeastConnectionsRoutingStrategy`, `WeightedRoundRobinRoutingStrategy`, and `RoundRobinRoutingStrategy`.

## Strategy Invariants

| Strategy or path | Current invariant to preserve | Current evidence | Follow-up hardening need |
| --- | --- | --- | --- |
| Round robin allocation facade | Splits requested batch load evenly across healthy registered servers and returns an empty allocation when no healthy server is available. | `LoadBalancerTest`, `CoreRoutingDecisionIntegrationTest` | Add a compact cross-strategy invariant test for zero load, empty server set, and all-unhealthy behavior. |
| Least-loaded allocation facade | Current contract sorts by load score but still allocates equal shares across healthy servers for positive-load tested cases. Zero-load least-loaded allocation exposes the lowest-load candidate before the current loop stops. | `LoadBalancerTest` characterization tests, `CoreLoadBalancerLeastLoadedSemanticsTest` | Preserve as an explicit contract unless a later behavior PR proposes and verifies different semantics. |
| Weighted distribution facade | Uses server weights proportionally, gives zero-weight servers zero allocation when positive weights exist, and falls back to equal allocation when all weights are zero. | `LoadBalancerTest` | Add table-style invariant tests for mixed zero/positive weights and all-zero weights. |
| Consistent hashing facade | Routes keys through the hash ring to healthy registered servers and rejects invalid key counts. | `LoadBalancerTest` | Add tighter deterministic-removal and all-unhealthy hash-ring checks if missing after inventory. |
| Capacity-aware allocation | Allocates only within available capacity, reports unallocated load when requested load exceeds available capacity, and preserves deterministic degraded/recovery behavior. | `LoadBalancerTest`, `CoreRoutingDecisionIntegrationTest` | Harden overload and unallocated-load assertions across edge cases. |
| Predictive allocation | Uses predicted load as a capacity input, allocates only within predicted available capacity, and reports capped excess load through result variants. | `LoadBalancerTest` | Mirror the capacity-aware invariant matrix for predictive overload cases. |
| Routing strategy registry | Default registry exposes tail-latency power-of-two, weighted least-load, weighted least-connections, weighted round-robin, and round-robin strategies in a stable reviewer-visible order. Requested comparison output preserves requested order, reports absent strategies safely, and keeps decision explanation fields visible. | `RoutingComparisonEngineTest`, `CoreRoutingRegistryComparisonContractTest`, strategy-specific tests | Preserve the registry/comparison contract unless a later behavior PR proposes and verifies different semantics. |
| Request-level routing strategies | Exclude unhealthy candidates, return safe no-candidate decisions when needed, keep deterministic tie handling where implemented, and expose bounded explanations. | `CoreRoutingDecisionIntegrationTest`, `ServerTelemetryRoutingTest`, `RoundRobinRoutingStrategyTest`, `WeightedLeastLoadStrategyTest`, `WeightedLeastConnectionsRoutingStrategyTest`, `WeightedRoundRobinRoutingStrategyTest`, `TailLatencyPowerOfTwoHysteresisTest` | Add a cross-strategy request-level invariant bundle for no healthy candidates, duplicate equivalent candidates, and explanation field presence. |

## Required Contract Topics

### Empty server set behavior

Current `LoadBalancer` allocation methods return empty allocations when no servers exist. Result-returning capacity-aware and predictive paths preserve the requested load as unallocated. Request-level routing comparison returns safe empty decisions for strategies when no healthy candidate exists.

Evidence:

- `LoadBalancerTest`
- `CoreRoutingDecisionIntegrationTest`
- `RoutingComparisonEngineTest`
- `ServerTelemetryRoutingTest`

### All-unhealthy behavior

Current allocation facades skip unhealthy servers. Map-returning batch methods return empty allocations when all servers are unhealthy. Result-returning capacity-aware and predictive methods report the requested load as unallocated. Request-level strategies return no healthy eligible server decisions.

Evidence:

- `LoadBalancerTest`
- `CoreRoutingDecisionIntegrationTest`
- strategy-specific routing tests

### Unhealthy server exclusion

The public facade filters through healthy server snapshots before distribution. Request-level strategies filter unhealthy candidates before choosing or scoring. Tests cover exclusion in facade and comparison paths.

Evidence:

- `LoadBalancerTest`
- `CoreRoutingDecisionIntegrationTest`
- `ServerTelemetryRoutingTest`
- `RoutingComparisonEngineTest`

### Recovered server re-entry

Existing integration tests cover a server returning to healthy status and re-entering allocation after degraded behavior. The current evidence is local deterministic test evidence only.

Evidence:

- `CoreRoutingDecisionIntegrationTest`

### Duplicate server replacement

Duplicate server IDs replace the existing registry entry, update hash-ring participation, and clear accumulated load tied to the replaced server identity.

Evidence:

- `LoadBalancerTest`

### Server removal and accumulated-load reconciliation

Removing a server updates public snapshots, hash-ring participation, typed server views, and accumulated allocation state before later rebalance.

Evidence:

- `LoadBalancerTest`

### Zero load and negative load handling

Negative batch allocation input is rejected by the current facade methods that validate total data. Zero-load behavior is covered indirectly in several planner and strategy paths, but it should be made explicit in a later cross-strategy invariant bundle.

Evidence:

- `LoadBalancerTest`
- strategy-specific tests

Gap:

- Core-LB-G02 should add a compact zero-load invariant table across facade methods.

### Zero weight and all-zero weight behavior

Weighted batch distribution preserves the current proportional semantics, gives zero-weight servers zero allocation when total weight is positive, and falls back to equal allocation when all weights are zero. Request-level weighted strategies default missing or zero routing weight safely according to their own strategy contracts.

Evidence:

- `LoadBalancerTest`
- `WeightedLeastLoadStrategyTest`
- `WeightedLeastConnectionsRoutingStrategyTest`
- `WeightedRoundRobinRoutingStrategyTest`

### Capacity exhaustion and unallocated load

Capacity-aware and predictive result variants expose unallocated load when available or predicted capacity is insufficient. Map-only methods intentionally expose allocations only, so reviewers should use result variants when overload accounting matters.

Evidence:

- `LoadBalancerTest`
- `CoreRoutingDecisionIntegrationTest`

Gap:

- Core-LB-G03 should harden capacity-aware and predictive result invariants together.

### Predictive overload behavior

Predictive allocation uses load score multiplied by the predictive load factor to calculate predicted available capacity. The result variant reports unallocated load when the predicted capacity cannot absorb the request.

Evidence:

- `LoadBalancerTest`

Gap:

- Core-LB-G03 should add tighter paired examples showing capacity-aware versus predictive overload differences without implying benchmark evidence.

### Deterministic ordering and tie handling

Planner paths use sorted or insertion-preserving maps where the current code needs deterministic reviewer behavior. Request-level routing strategies have strategy-specific tie behavior. Tail-latency power-of-two now includes bounded anti-flapping behavior and stable server-id tie selection within its anti-flapping band.

Evidence:

- `LoadBalancerTest`
- `RoutingComparisonEngineTest`
- `TailLatencyPowerOfTwoHysteresisTest`
- strategy-specific tests

Gap:

- Core-LB-G04 should add a focused deterministic tie/order test bundle for planner and routing strategies.

### Explanation boundaries

Request-level routing decisions expose candidate lists, chosen server IDs where available, score maps where the strategy scores candidates, and reason text. Batch allocation facade methods return allocation maps or result objects rather than full decision explanations.

Evidence:

- `ServerTelemetryRoutingTest`
- `RoutingComparisonEngineTest`
- strategy-specific tests

Gap:

- Core-LB-G06 should tie strategy registry, comparison output, and explanation field presence into one reviewer-facing contract.

## Current Tested Guarantees

- Public server list snapshots are detached from registry state.
- Server map snapshots are read-only.
- Duplicate server replacement does not leave stale registry or hash-ring entries.
- Removed servers do not remain in typed snapshots or hash-ring routing.
- Round-robin, weighted, capacity-aware, and predictive allocations accumulate enough state for rebalance tests.
- No-server and all-unhealthy paths fail closed into empty allocations or explicit unallocated load.
- Health recovery can re-admit a server in deterministic local integration tests.
- Request-level strategies skip unhealthy candidates and return safe no-candidate decisions.
- Weighted request-level strategies handle missing or zero routing weight safely and reject invalid negative weights.
- Tail-latency routing explanations name material factors and anti-flapping decisions when relevant.

## Current Gaps And Follow-Up Goals

### Core-LB-G02 - Cross-strategy edge invariant tests

- Scope: facade-level empty server set, all-unhealthy, negative load, and zero-load behavior across all allocation methods.
- Exit criteria: one deterministic test class protects shared edge semantics without changing production behavior.

### Core-LB-G03 - Capacity-aware and predictive overload hardening

- Scope: result variants for overload, available capacity, predicted capacity, unallocated load, and no negative allocations.
- Exit criteria: paired tests show exact local behavior for capacity-aware and predictive overload.

### Core-LB-G04 - Deterministic ordering and tie behavior

- Scope: planner ordering, stable server-id tie behavior where applicable, routing strategy tie evidence, and repeated-call determinism.
- Exit criteria: tests fail if equivalent inputs produce unstable reviewer-visible output.

### Core-LB-G05 - Least-loaded semantics decision

- Scope: preserve the current least-loaded facade semantics with explicit contract tests.
- Decision: current positive-load least-loaded behavior remains an equal-share allocation across healthy servers after sorting by load score; zero-load behavior exposes the lowest-load candidate before the current loop stops.
- Exit criteria: focused tests fail if least-loaded positive-load allocation stops matching the documented equal-share contract or if zero-load lowest-load selection changes without a later reviewed behavior PR.

### Core-LB-G06 - Routing registry and comparison contract

- Scope: default registry order, strategy ID support, requested strategy filtering, duplicate strategy rejection, safe absent-strategy reporting, and explanation field presence.
- Decision: the default routing strategy registry order remains tail-latency power-of-two, weighted least-load, weighted least-connections, weighted round-robin, then round-robin. `RoutingComparisonEngine` preserves requested strategy order, preserves duplicate requested strategy IDs as repeated results, reports registered strategy failures or absent strategies without crashing, and returns safe no-healthy-candidate decisions.
- Exit criteria: one reviewer-facing test bundle covers registry and comparison shape without changing production behavior.

### Core-LB-G07 - Server lifecycle invariants

- Scope: add/remove/replace/rebalance, health transitions, snapshots, and accumulated-load reconciliation.
- Decision: public facade lifecycle behavior keeps server-list snapshots detached, server-map snapshots read-only, duplicate ID replacement scoped to the current server identity, removal reconciled with accumulated load, and health transitions reflected in allocation eligibility.
- Exit criteria: `CoreLoadBalancerServerLifecycleInvariantTest` covers lifecycle behavior outside one large mixed-purpose test class without changing production behavior.

### Core-LB-G08 - Overload and recovery scenario tests

- Scope: local deterministic facade scenarios for capacity exhaustion, all-unhealthy degradation, recovery, and restored capacity.
- Exit criteria: scenarios show behavior without claiming production load proof.

### Core-LB-G09 - Reviewer evidence map update

- Scope: connect core load-balancer guarantees to README and Reviewer Trust Map surfaces.
- Exit criteria: reviewers can find core guarantees and boundaries without reading source first.

### Core-LB-G10 - Core load-balancer evidence consolidation

- Scope: summarize completed Core-LB goals, remote/main status, tested guarantees, and remaining boundaries.
- Exit criteria: consolidation names only completed evidence and preserves open not-proven boundaries.

## Not-Proven Boundaries

This contract and its guard do not prove:

- no production readiness;
- no production certification;
- no live-cloud validation;
- no real-tenant validation;
- no runtime enforcement beyond implemented and verified code paths;
- no load/stress/benchmarking evidence;
- no throughput/p95/p99 production evidence;
- no real-world latency improvement;
- no replay/evidence/report/storage/export proof;
- no broader automation;
- no branch-protection, release, registry publication, or container signing behavior.

## Verification Expectations For Core-LB Slots

Each future Core-LB PR should report exact verification. For implementation or test-hardening slots, expected verification is:

- focused tests for the changed contract area;
- relevant core selector bundle;
- `mvn -q test`;
- `mvn -q "-DskipTests" package`;
- `mvn -B package`;
- `git diff --check`;
- `git diff --cached --check`;
- enterprise lab package smoke when available and normally required;
- current-head remote checks before merge;
- post-merge main CI/CodeQL before marking a goal complete.

Failed, cancelled, stale, pending, missing, or duplicate-only required checks are not acceptable as green.
