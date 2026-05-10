# Incident Fixture Catalog

LoadBalancerPro keeps curated scenario replay fixtures under `src/test/resources/scenarios/replay/`.
They are deterministic regression examples for operators and CI, not production load tests or live traffic generators.

The catalog index is `src/test/resources/scenarios/replay/catalog.json`. Each entry names the fixture, expected status, and operator purpose.

## Fixture Catalog

| Fixture | Event Sequence | Operator Interpretation |
| --- | --- | --- |
| `normal-baseline.json` | `ALLOCATE`, `EVALUATE`, `ROUTE` | Healthy servers accept requested load, emit read-only metrics previews, and route deterministically. |
| `overload-scale-recommendation.json` | `OVERLOAD` | Available capacity is accepted, excess load is rejected/unallocated, and a scale-up recommendation is returned. |
| `single-server-failure-recovery.json` | `MARK_UNHEALTHY`, `ROUTE`, `ALLOCATE`, `MARK_HEALTHY`, `ROUTE` | A failed server is excluded from routing/allocation, then re-enters decisions after recovery. |
| `all-unhealthy-degradation.json` | `EVALUATE`, `ROUTE` | No healthy capacity is available, so allocation degrades with unallocated load and routing returns no selected server. |
| `mixed-incident-replay.json` | `ROUTE`, `OVERLOAD`, `MARK_UNHEALTHY`, `EVALUATE`, `MARK_HEALTHY`, `ALLOCATE` | A multi-step incident stays ordered and deterministic across overload, degradation, recovery, and allocation. |
| `invalid-scenario.json` | unsupported event | The API returns a controlled `400` error for invalid scenario contracts. |

## Regression Expectations

`ScenarioReplayFixtureCatalogTest` posts each valid fixture to `POST /api/scenarios/replay` and asserts stable fields instead of brittle full-response snapshots:

- `readOnly=true` and `cloudMutation=false`;
- ordered replay step types;
- accepted, rejected, and unallocated load where stable;
- scale recommendation and load-shedding decisions where stable;
- no negative allocation values;
- deterministic replay output for the mixed incident;
- no `CloudManager` construction for valid fixtures;
- controlled bad-request shape for the invalid fixture.

The tests intentionally avoid timestamps, random IDs, raw request identifiers, or incidental JSON ordering. Ordered replay steps are asserted because sequence order is part of the API contract.

## Running The Fixture Tests

Run the focused scenario replay catalog coverage:

```bash
mvn -B -Dtest=*ScenarioReplay* test
mvn -B -Dtest=*Fixture* test
```

Run the broader API contract coverage when changing replay schemas:

```bash
mvn -B -Dtest=*ApiContractTest test
```

GitHub CI is the source of truth for this repository when a local workstation cannot resolve Maven Central because of trust-chain or certificate issues.

## Adding A New Fixture

1. Add a minimal request JSON file under `src/test/resources/scenarios/replay/`.
2. Add an entry to `catalog.json` with a clear operator purpose.
3. Add stable-field assertions to `ScenarioReplayFixtureCatalogTest`.
4. Prefer semantic assertions over full JSON snapshots.
5. Avoid dynamic timestamps, random identifiers, request IDs, raw hosts, external services, real cloud calls, and generated artifacts.

Fixtures should remain small enough to read during incident review and deterministic enough to catch contract regressions in CI.
