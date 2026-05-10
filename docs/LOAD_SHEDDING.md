# Load Shedding And Overload Semantics

LoadBalancerPro allocation endpoints are calculation-only. They model how a requested batch load would be assigned to caller-provided server capacity, but they do not enqueue traffic, call cloud providers, or mutate AWS resources.

## Public Allocation API Contract

The public allocation endpoints currently use aggregate load-shedding semantics:

- `POST /api/allocate/capacity-aware`
- `POST /api/allocate/predictive`

When requested load is greater than available healthy capacity, the API returns HTTP 200 with a partial allocation response instead of rejecting the request with HTTP 429. The response makes the overload explicit:

- `allocations` contains only load assigned to eligible healthy servers.
- `unallocatedLoad` is the rejected or shed portion of the requested load.
- `recommendedAdditionalServers` is a simulated scale-up count based on average healthy capacity.
- `scalingSimulation.simulatedOnly` remains `true`.
- `scalingSimulation.reason` explains whether simulated scale-up is recommended or unavailable.

Invalid input still returns a controlled 4xx response. Examples include malformed JSON, negative requested load, invalid server capacity, empty server lists, unsupported media types, and oversized payloads.

## Priority Classes

The core LASE package includes internal `RequestPriority` and `LoadSheddingPolicy` types for priority-aware overload modeling. Those types are covered by core tests, but they are not part of the public allocation request contract yet.

Until a future API version explicitly exposes priority classes, public allocation requests are treated as aggregate batch-load simulations. The service does not prioritize one caller-provided unit of work over another inside a single allocation request.

## Read-Only Evaluation API

Operators can preview allocation and load-shedding behavior without performing an allocation by calling:

- `POST /api/allocate/evaluate`

The endpoint is read-only and recommendation-only. It uses the caller-provided server list and load pressure to return:

- `allocations`: the load that would be assigned to eligible healthy servers.
- `acceptedLoad`: the total load that would be assigned.
- `rejectedLoad` and `unallocatedLoad`: the load that would remain unassigned under current healthy capacity.
- `recommendedAdditionalServers` and `scalingSimulation`: the simulated scale-up recommendation.
- `loadShedding`: the internal priority/load-shedding decision for the supplied or default pressure signal.
- `metricsPreview`: the allocation metric names and values that correspond to the evaluation result.
- `remediationPlan`: ranked, advisory operator recommendations such as `SCALE_UP`, `SHED_LOAD`, `INVESTIGATE_UNHEALTHY`, `RESTORE_CAPACITY`, `RETRY_WHEN_HEALTHY`, or `NO_ACTION`.

Supported evaluation strategies are `CAPACITY_AWARE` and `PREDICTIVE`; omitted strategy defaults to `CAPACITY_AWARE`. Supported priority values are `CRITICAL`, `USER`, `BACKGROUND`, and `PREFETCH`; omitted priority defaults to `USER`. Optional pressure fields such as `currentInFlightRequestCount`, `concurrencyLimit`, `queueDepth`, `observedP95LatencyMillis`, and `observedErrorRate` let operators preview soft or hard overload decisions. If those fields are omitted, the service derives deterministic defaults from requested load, healthy capacity, and unallocated load.

Evaluation does not construct `CloudManager`, does not call AWS, does not enqueue traffic, does not mutate `LoadBalancer` allocation state, and does not increment the normal allocation metrics. The `metricsPreview.emitted` field is `false` to make that observability boundary explicit.

The remediation plan is not an execution path. `advisoryOnly=true`, `readOnly=true`, `cloudMutation=false`, and each recommendation has `executable=false`.

## Determinism

Capacity-aware and predictive allocation are deterministic for the same input:

- unhealthy servers are excluded;
- exhausted servers do not receive negative allocations;
- available healthy capacity is assigned in a stable order;
- repeated identical requests produce the same allocation, unallocated load, and scale recommendation.

## Metrics

Allocation overload paths emit domain metrics through Micrometer:

- `allocation.requests.count`: allocation requests by strategy.
- `allocation.accepted.load`: load assigned to eligible servers.
- `allocation.rejected.load`: load rejected or shed because it could not be assigned.
- `allocation.unallocated.load`: unallocated load retained for compatibility with existing dashboards.
- `allocation.server.count`: healthy server count used by the allocation decision.
- `allocation.scaling.recommended.servers`: simulated additional server count.
- `allocation.validation.failures.count`: validation or malformed-request failures by API path and reason.

`allocation.rejected.load` and `allocation.unallocated.load` intentionally track the same overload quantity. The rejected-load name is the clearer operational signal; the unallocated-load name preserves the existing API vocabulary.

## Cloud Safety

Overload, rejection, and scaling recommendation paths are simulation-only. They must not construct `CloudManager`, call AWS, create Auto Scaling Groups, scale cloud capacity, or mutate infrastructure.

Live cloud mutation remains behind the separate guarded `CloudManager` boundary and requires explicit live-mode guardrails outside the allocation API.
