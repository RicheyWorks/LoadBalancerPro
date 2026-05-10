# Postman Routing Decision Demo

This guide covers the `Routing Decision Demo` folder in `postman/LoadBalancerPro.postman_collection.json`.
It mirrors the browser page at:

```text
http://localhost:8080/routing-demo.html
```

The demo is local/operator review only. It is not certification, not benchmark proof, not legal compliance proof, and not identity proof. It uses synthetic caller-provided routing telemetry, does not mutate cloud state, does not construct `CloudManager`, does not call external services, and does not write runtime reports.

## Import

Import the collection:

```text
postman/LoadBalancerPro.postman_collection.json
```

Set the collection variable:

```text
baseUrl = http://localhost:8080
```

## Request Order

Run the `Routing Decision Demo` folder from top to bottom:

1. `GET Routing Demo Health Check`
2. `GET Routing Demo Readiness Check`
3. `POST Compare All Supported Strategies`
4. `POST Weighted Strategy Sample`
5. `POST Least Connections Sample`
6. `POST Tail Latency Sample`

The compare request uses the existing read-only endpoint:

```text
POST {{baseUrl}}/api/routing/compare
```

Supported request-level strategy IDs are:

- `TAIL_LATENCY_POWER_OF_TWO`
- `WEIGHTED_LEAST_LOAD`
- `WEIGHTED_LEAST_CONNECTIONS`
- `WEIGHTED_ROUND_ROBIN`
- `ROUND_ROBIN`

These are the strategies currently registered by the API. The demo does not invent a separate response-time strategy or merge legacy batch allocation behavior into request-level routing.

## Sample Body

The all-strategy sample uses synthetic server names and deterministic telemetry:

```json
{
  "strategies": [
    "TAIL_LATENCY_POWER_OF_TWO",
    "WEIGHTED_LEAST_LOAD",
    "WEIGHTED_LEAST_CONNECTIONS",
    "WEIGHTED_ROUND_ROBIN",
    "ROUND_ROBIN"
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
    },
    {
      "serverId": "edge-beta",
      "healthy": true,
      "inFlightRequestCount": 28,
      "configuredCapacity": 100.0,
      "estimatedConcurrencyLimit": 100.0,
      "weight": 4.0,
      "averageLatencyMillis": 24.0,
      "p95LatencyMillis": 52.0,
      "p99LatencyMillis": 96.0,
      "recentErrorRate": 0.02,
      "queueDepth": 3
    }
  ]
}
```

Test fixtures are kept under `src/test/resources/routing-demo/` for deterministic API and Postman coverage.

For the broader side-by-side load-balancing review, use the `Unified Load-Balancing Cockpit` folder documented in [`POSTMAN_LOAD_BALANCING_COCKPIT.md`](POSTMAN_LOAD_BALANCING_COCKPIT.md). That folder keeps this routing comparison flow but adds existing allocation and read-only evaluation requests for overload/load-shedding and advisory remediation-plan hints.

## Response Shape

The response includes the requested strategies, candidate count, and one result per strategy. Each result exposes the selected server, candidate set, score map when the strategy reports scores, and the strategy's explanation.

```json
{
  "requestedStrategies": [
    "WEIGHTED_LEAST_CONNECTIONS"
  ],
  "candidateCount": 2,
  "results": [
    {
      "strategyId": "WEIGHTED_LEAST_CONNECTIONS",
      "status": "SUCCESS",
      "chosenServerId": "edge-weighted",
      "reason": "Chose edge-weighted because its weighted least-connections score 3.000 was the lowest across 2 healthy candidates.",
      "candidateServersConsidered": [
        "edge-standard",
        "edge-weighted"
      ],
      "scores": {
        "edge-standard": 5.0,
        "edge-weighted": 3.0
      }
    }
  ]
}
```

The API response also includes server-generated metadata fields. The browser demo's copyable summary omits variable fields so reviewers can compare the selected server, status, reason, and scores without treating runtime metadata as evidence.

## Why This Server?

The browser page and Postman responses use the real `reason`, `chosenServerId`, `candidateServersConsidered`, and `scores` fields returned by `/api/routing/compare`.

Explanation limits:

- `ROUND_ROBIN` explains request-order position.
- `WEIGHTED_ROUND_ROBIN` explains effective routing weight.
- `WEIGHTED_LEAST_CONNECTIONS` explains weighted connection score.
- `WEIGHTED_LEAST_LOAD` explains weighted load score.
- `TAIL_LATENCY_POWER_OF_TWO` explains the lower score among sampled healthy candidates.

The demo is a local explanation aid, not production benchmarking or proof of a deployment decision.

## Safety

- Local/operator demo only.
- Not certification.
- Not benchmark proof.
- Not legal compliance proof.
- Not identity proof.
- No cloud mutation.
- No `CloudManager` required.
- No external scripts, CDNs, services, or dependencies.
- No generated runtime reports.
- No release, tag, ruleset, admin, or cloud-control requests in the Postman folder.
