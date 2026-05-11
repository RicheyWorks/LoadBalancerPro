# Operator Scenario Gallery

The operator scenario gallery extends the unified browser cockpit at:

```text
http://localhost:8080/load-balancing-cockpit.html
```

It lets a reviewer load safe packaged scenarios, run the same real endpoints for each scenario, and compare what changed across allocation, routing, load-shedding, and remediation-hint output. It also feeds the cockpit's operator comparison matrix, which can run every packaged scenario in sequence and summarize the outputs in one table. It adds no backend endpoint and does not replace the existing CLI, routing demo, or evidence training cockpit.

## Start

Start the local API:

```bash
mvn spring-boot:run
```

Or run the packaged JAR on the Postman default port:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=local
```

Then verify:

```bash
curl -fsS http://127.0.0.1:8080/api/health
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
```

## Scenarios

The gallery includes these packaged local scenarios:

- `Normal Load`: healthy nodes with spare capacity and low pressure.
- `Overload Pressure`: constrained healthy capacity, high queue depth, high latency, and elevated error pressure.
- `All-Unhealthy Degradation`: every candidate is unhealthy, so the cockpit should show no fabricated routing or allocation result.
- `Recovery / Capacity Restored`: healthy capacity returns and pressure drops.

Each scenario shows expected outcome hints before execution:

- Routing behavior hint.
- Allocation behavior hint.
- Load-shedding or overload hint.
- Remediation-plan hint.

The actual result still comes from the existing API response after the operator runs the scenario.

## Browser Flow

1. Open `http://localhost:8080/load-balancing-cockpit.html`.
2. Pick a scenario in `Scenario Gallery`.
3. Click `Load scenario` to copy its deterministic payload into the cockpit editor.
4. Click `Run selected scenario` to call health, readiness, allocation, routing, and evaluation flows.
5. Review raw JSON for `POST /api/allocate/capacity-aware`, `POST /api/routing/compare`, and `POST /api/allocate/evaluate`.
6. Run a second scenario, then click `Compare with previous scenario`.
7. Review `Explanation Drill-Down` for routing, allocation, overload, remediation, and scenario-delta rationale.
8. Click `Run all scenarios` in `Operator Comparison Matrix` when you want the four packaged scenarios summarized side by side.
9. Copy the selected payload, curl snippets, scenario summary, drill-down summary, matrix summary, matrix curls, matrix payloads, explanation curl snippets, or operator rationale.

The what-changed summary compares scenario name, unallocated load, rejected load, load-shedding action, remediation status, and selected server per routing strategy. It is client-side copyable text only; it is not written to disk or sent to a report endpoint.

The explanation drill-down uses the same real endpoint outputs and visible scenario inputs. Exact internal scores and every internal threshold are not exposed by the current API, so supporting math is labeled as derived from visible request/response fields and raw JSON remains the source of truth.

The comparison matrix uses stable scenario ordering and reports unavailable fields honestly. It is not a benchmark, not a scorecard, and not a production-capacity claim.

## Real Endpoints Used

```text
GET  /api/health
GET  /actuator/health/readiness
POST /api/allocate/capacity-aware
POST /api/allocate/evaluate
POST /api/routing/compare
```

Supported routing strategies are the real registered strategy IDs:

```text
TAIL_LATENCY_POWER_OF_TWO
WEIGHTED_LEAST_LOAD
WEIGHTED_LEAST_CONNECTIONS
WEIGHTED_ROUND_ROBIN
ROUND_ROBIN
```

If a section cannot run, the cockpit must show `Not available in current API` rather than inventing output.

## Postman Parity

Import:

```text
postman/LoadBalancerPro.postman_collection.json
```

Set:

```text
baseUrl = http://localhost:8080
```

Run the `Operator Scenario Gallery` folder. It includes health, readiness, and per-scenario routing comparison, capacity-aware allocation, and read-only evaluation requests for the normal load, overload pressure, all-unhealthy degradation, and recovery flows.

Run the `Operator Explanation Drill-Down` folder when a reviewer wants the matching request set for the cockpit explanation panels. It uses the same endpoints and scenario bodies; the browser-side drill-down labels any derived explanation as derived from visible request/response fields.

## Fixtures

Deterministic scenario fixtures live under:

```text
src/test/resources/load-balancing-cockpit/scenarios/
```

They contain synthetic server names, deterministic capacity/connection/latency values, no credentials, no cloud IDs, no external service URLs, and no generated runtime output.

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
