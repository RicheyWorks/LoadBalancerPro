# Postman Load-Balancing Cockpit

This guide covers the `Unified Load-Balancing Cockpit`, `Operator Scenario Gallery`, and `Operator Explanation Drill-Down` folders in `postman/LoadBalancerPro.postman_collection.json`.
It mirrors the browser page at:

```text
http://localhost:8080/load-balancing-cockpit.html
```

The cockpit is local/operator review only. It is not certification, not benchmark proof, not legal compliance proof, and not identity proof. It uses synthetic inputs, calls existing calculation/recommendation routes, does not mutate cloud state, does not construct `CloudManager`, does not call external services, and does not write runtime reports.

## Import

Import:

```text
postman/LoadBalancerPro.postman_collection.json
```

Set the collection variable:

```text
baseUrl = http://localhost:8080
```

## Request Order

Run the `Unified Load-Balancing Cockpit` folder from top to bottom for the single default cockpit path:

1. `GET Unified Cockpit Health Check`
2. `GET Unified Cockpit Readiness Check`
3. `POST Cockpit Routing Comparison`
4. `POST Cockpit Capacity-Aware Allocation`
5. `POST Cockpit Predictive Allocation`
6. `POST Cockpit Load-Shedding Evaluation`

The browser cockpit also exposes copyable curl snippets, a copyable scenario payload, raw JSON response blocks, a deterministic side-by-side summary, and an operator comparison matrix summary.

Run the `Operator Scenario Gallery` folder when a reviewer wants to compare multiple packaged scenarios:

1. `GET Scenario Gallery Health Check`
2. `GET Scenario Gallery Readiness Check`
3. `POST Normal Load Routing Comparison`
4. `POST Normal Load Allocation Preview`
5. `POST Normal Load Overload Evaluation Preview`
6. `POST Overload Pressure Routing Comparison`
7. `POST Overload Pressure Allocation Preview`
8. `POST Overload Pressure Overload Evaluation Preview`
9. `POST All-Unhealthy Degradation Routing Comparison`
10. `POST All-Unhealthy Degradation Allocation Preview`
11. `POST All-Unhealthy Degradation Overload Evaluation Preview`
12. `POST Recovery Capacity Restored Routing Comparison`
13. `POST Recovery Capacity Restored Allocation Preview`
14. `POST Recovery Capacity Restored Overload Evaluation Preview`

Run the `Operator Explanation Drill-Down` folder when a reviewer wants request-by-request parity for the cockpit's explanation panels:

1. `GET Explanation Drill-Down Health Check`
2. `GET Explanation Drill-Down Readiness Check`
3. Per-scenario `Routing Explanation` requests against `POST /api/routing/compare`
4. Per-scenario `Allocation Explanation` requests against `POST /api/allocate/capacity-aware`
5. Per-scenario `Overload And Remediation Explanation` requests against `POST /api/allocate/evaluate`

The explanation folder does not call a new endpoint. It exposes the same visible scenario inputs and real response fields that the browser uses for routing, allocation, overload, remediation, and scenario-delta rationale.

## Supported Existing Endpoints

The sprint does not add a new cockpit API endpoint. The browser and Postman flow reuse:

```text
GET  /api/health
GET  /actuator/health/readiness
POST /api/allocate/capacity-aware
POST /api/allocate/predictive
POST /api/allocate/evaluate
POST /api/routing/compare
```

`POST /api/routing/compare` supports the registered routing strategy IDs:

```text
TAIL_LATENCY_POWER_OF_TWO
WEIGHTED_LEAST_LOAD
WEIGHTED_LEAST_CONNECTIONS
WEIGHTED_ROUND_ROBIN
ROUND_ROBIN
```

## Sample Request Shape

The cockpit fixture stores allocation, evaluation, and routing requests side by side:

```json
{
  "scenarioName": "safe-local-load-balancing-cockpit",
  "allocationRequest": {
    "requestedLoad": 120.0,
    "servers": [
      {
        "id": "edge-alpha",
        "cpuUsage": 45.0,
        "memoryUsage": 40.0,
        "diskUsage": 35.0,
        "capacity": 100.0,
        "weight": 2.0,
        "healthy": true
      }
    ]
  },
  "evaluationRequest": {
    "requestedLoad": 210.0,
    "strategy": "CAPACITY_AWARE",
    "priority": "BACKGROUND",
    "currentInFlightRequestCount": 120,
    "concurrencyLimit": 100,
    "queueDepth": 55,
    "observedP95LatencyMillis": 320.0,
    "observedErrorRate": 0.18,
    "servers": [
      {
        "id": "edge-alpha",
        "cpuUsage": 45.0,
        "memoryUsage": 40.0,
        "diskUsage": 35.0,
        "capacity": 100.0,
        "weight": 2.0,
        "healthy": true
      }
    ]
  },
  "routingRequest": {
    "strategies": [
      "TAIL_LATENCY_POWER_OF_TWO",
      "WEIGHTED_LEAST_LOAD"
    ],
    "servers": [
      {
        "serverId": "edge-alpha",
        "healthy": true,
        "inFlightRequestCount": 5,
        "configuredCapacity": 100.0,
        "estimatedConcurrencyLimit": 100.0,
        "weight": 2.0,
        "averageLatencyMillis": 20.0,
        "p95LatencyMillis": 40.0,
        "p99LatencyMillis": 80.0,
        "recentErrorRate": 0.01,
        "queueDepth": 1
      }
    ]
  }
}
```

The folder sends each sub-request to the matching existing endpoint. Test fixtures are kept under `src/test/resources/load-balancing-cockpit/` and `src/test/resources/load-balancing-cockpit/scenarios/` for deterministic API and Postman coverage.

## Operator Scenario Gallery

The browser cockpit includes a `Scenario Gallery` section with four safe packaged scenarios:

- `Normal Load`
- `Overload Pressure`
- `All-Unhealthy Degradation`
- `Recovery / Capacity Restored`

Each scenario provides expected routing, allocation, load-shedding, and remediation hints before execution. The actual output still comes from the real endpoints after the scenario is run. The page can compare the latest run with the previous run and summarize what changed in unallocated load, rejected load, load-shedding action, remediation status, and selected routing servers.

## Operator Comparison Matrix

The browser cockpit includes an `Operator Comparison Matrix` section. Click `Run all scenarios` to execute the packaged scenarios in this deterministic order:

1. `Normal Load`
2. `Overload Pressure`
3. `All-Unhealthy Degradation`
4. `Recovery / Capacity Restored`

The matrix summarizes routing strategy output, selected server labels, allocation pressure, load-shedding action, remediation hints, explanation rationale, and delta versus the prior scenario. It uses existing endpoint responses and visible scenario inputs only. It is not a benchmark, does not invent score values, and marks missing or unavailable fields as unavailable.

Copy controls provide deterministic matrix Markdown, scenario-by-scenario curl commands, and packaged scenario payloads.

## Explanation Drill-Down

The cockpit includes an `Explanation Drill-Down` area for operator review. It shows routing strategy explanations, allocation capacity math, overload reason breakdowns, remediation rationale, and scenario delta explanations.

Raw JSON remains the source of truth. Exact internal scores and every internal threshold are not exposed by the current API, so client-side supporting text is labeled as derived from visible request/response fields. The page provides copy controls for drill-down summary, explanation curl snippets, and operator rationale.

## Response Shape

Allocation responses include:

- `allocations`
- `unallocatedLoad`
- `recommendedAdditionalServers`
- `scalingSimulation`

Read-only evaluation responses include:

- `acceptedLoad`
- `rejectedLoad`
- `unallocatedLoad`
- `loadShedding`
- `remediationPlan`
- `readOnly=true`
- `remediationPlan.advisoryOnly=true`
- `remediationPlan.cloudMutation=false`

Routing comparison responses include one result per requested strategy with selected server, strategy status, candidate list, score map when available, and explanation reason.

## Available Vs Unavailable

Allocation, routing, load-shedding/overload signals, and remediation-plan hints are available through existing endpoints. If a future build removes or disables one of those routes, the browser cockpit must show `Not available in current API` instead of inventing output.

## Safety Boundaries

- Local/operator demo only.
- Not certification.
- Not benchmark proof.
- Not legal compliance proof.
- Not identity proof.
- No cloud mutation.
- No `CloudManager` required.
- No external scripts, CDNs, services, or dependencies.
- No generated runtime reports.
- No server-side transcript writing.
- No release, tag, ruleset, admin, or cloud-control requests in the Postman folder.
