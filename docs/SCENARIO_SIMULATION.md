# Scenario Replay Simulation

LoadBalancerPro provides a read-only scenario replay endpoint for operator review and API-level regression coverage:

```text
POST /api/scenarios/replay
```

The endpoint models degraded allocation and routing incidents without sending real traffic, calling cloud providers, mutating persistent allocation state, or constructing `CloudManager`.

## Purpose

Use scenario replay to model:

- normal allocation pressure;
- overload and unallocated load;
- all servers unhealthy;
- one server failing and later recovering;
- repeated routing decisions under degraded conditions;
- read-only evaluation and load-shedding metadata;
- deterministic replay output for tests and operator review.

This is an app-native simulator. It is not a production traffic generator, queue, database, cloud sandbox, or external load-test harness.

## Request Shape

At the top level, provide:

- `scenarioId`, optional, for operator context;
- `servers`, the initial server list using the same fields as allocation requests;
- `steps`, an ordered list of replay events.

Supported step types:

| Type | Required Fields | Behavior |
| --- | --- | --- |
| `ALLOCATE` | `requestedLoad` | Previews allocation using the evaluation engine and does not emit allocation metrics. |
| `EVALUATE` | `requestedLoad` | Returns read-only allocation, scaling, load-shedding, and metrics-preview metadata. |
| `OVERLOAD` | `requestedLoad` | Same read-only evaluation path, intended for overload fixtures. |
| `ROUTE` | none | Runs routing comparison against the current per-replay server state. |
| `MARK_UNHEALTHY` | `serverId` | Marks one server unhealthy inside this replay only. |
| `MARK_HEALTHY` | `serverId` | Marks one server healthy inside this replay only. |

Optional fields on allocation/evaluation/overload steps:

- `strategy`, currently `CAPACITY_AWARE` or `PREDICTIVE`;
- `priority`, such as `CRITICAL`, `USER`, `BACKGROUND`, or `PREFETCH`;
- `currentInFlightRequestCount`;
- `concurrencyLimit`;
- `queueDepth`;
- `observedP95LatencyMillis`;
- `observedErrorRate`.

Optional fields on route steps:

- `routingStrategies`, such as `ROUND_ROBIN`, `TAIL_LATENCY_POWER_OF_TWO`, `WEIGHTED_LEAST_LOAD`, `WEIGHTED_LEAST_CONNECTIONS`, or `WEIGHTED_ROUND_ROBIN`;
- `queueDepth`, used when deriving routing candidate telemetry from the replay server state.

## Response Shape

The response includes:

- `scenarioId`;
- `readOnly=true`;
- `cloudMutation=false`;
- ordered `steps`.

Each step includes the current `serverStates` snapshot. Allocation-like steps include:

- `allocations`;
- `acceptedLoad`;
- `rejectedLoad`;
- `unallocatedLoad`;
- `recommendedAdditionalServers`;
- `scalingSimulation`;
- `loadShedding`;
- `metricsPreview`, with `emitted=false`.

Routing steps include:

- `selectedServerId`;
- `routingResults`;
- current `serverStates`.

## Normal Scenario Example

```json
{
  "scenarioId": "normal-load",
  "servers": [
    {
      "id": "green",
      "cpuUsage": 20.0,
      "memoryUsage": 20.0,
      "diskUsage": 20.0,
      "capacity": 100.0,
      "weight": 1.0,
      "healthy": true
    }
  ],
  "steps": [
    {
      "stepId": "allocate-normal",
      "type": "ALLOCATE",
      "requestedLoad": 40.0,
      "strategy": "CAPACITY_AWARE"
    }
  ]
}
```

## Overload Scenario Example

```json
{
  "scenarioId": "overload-review",
  "servers": [
    {
      "id": "primary",
      "cpuUsage": 30.0,
      "memoryUsage": 30.0,
      "diskUsage": 30.0,
      "capacity": 100.0,
      "weight": 1.0,
      "healthy": true
    },
    {
      "id": "fallback",
      "cpuUsage": 70.0,
      "memoryUsage": 70.0,
      "diskUsage": 70.0,
      "capacity": 100.0,
      "weight": 1.0,
      "healthy": true
    }
  ],
  "steps": [
    {
      "stepId": "overload-check",
      "type": "OVERLOAD",
      "requestedLoad": 150.0,
      "strategy": "CAPACITY_AWARE",
      "priority": "BACKGROUND",
      "currentInFlightRequestCount": 95,
      "concurrencyLimit": 100,
      "queueDepth": 25,
      "observedP95LatencyMillis": 300.0,
      "observedErrorRate": 0.20
    }
  ]
}
```

## Failure And Recovery Example

```json
{
  "scenarioId": "failure-recovery",
  "servers": [
    {
      "id": "green",
      "cpuUsage": 5.0,
      "memoryUsage": 10.0,
      "diskUsage": 10.0,
      "capacity": 100.0,
      "weight": 1.0,
      "healthy": true
    },
    {
      "id": "blue",
      "cpuUsage": 15.0,
      "memoryUsage": 10.0,
      "diskUsage": 10.0,
      "capacity": 100.0,
      "weight": 1.0,
      "healthy": true
    }
  ],
  "steps": [
    {
      "stepId": "fail-green",
      "type": "MARK_UNHEALTHY",
      "serverId": "green"
    },
    {
      "stepId": "route-around",
      "type": "ROUTE",
      "routingStrategies": ["ROUND_ROBIN"]
    },
    {
      "stepId": "recover-green",
      "type": "MARK_HEALTHY",
      "serverId": "green"
    },
    {
      "stepId": "route-after-recovery",
      "type": "ROUTE",
      "routingStrategies": ["ROUND_ROBIN"]
    }
  ]
}
```

## Cloud-Safety Guarantee

Scenario replay is read-only:

- no `CloudManager` construction;
- no AWS calls;
- no live scale-up or scale-down;
- no release, tag, asset, or workflow interaction;
- no persistent allocation state mutation;
- no normal allocation metrics emitted by allocation-like replay steps.

The response deliberately sets `cloudMutation=false` and includes `metricsPreview.emitted=false` for allocation-like steps.

## Limitations

- Route steps derive request-level routing telemetry from the current replay server state. Use `POST /api/routing/compare` directly for full custom routing telemetry.
- Replay state exists only for the request. It is not stored, scheduled, or replayed asynchronously.
- This endpoint is for deterministic operator review and tests, not production traffic generation or capacity benchmarking.
