# Incident Fixture Catalog

LoadBalancerPro keeps curated scenario replay fixtures under `src/test/resources/scenarios/replay/`.
They are deterministic regression examples for operators and CI, not production load tests or live traffic generators.

The catalog index is `src/test/resources/scenarios/replay/catalog.json`. Each entry names the fixture, expected descriptor, expected status, and operator purpose.

## Fixture Catalog

| Fixture | Event Sequence | Operator Interpretation |
| --- | --- | --- |
| `normal-baseline.json` | `ALLOCATE`, `EVALUATE`, `ROUTE` | Healthy servers accept requested load, emit read-only metrics previews, and route deterministically. |
| `overload-scale-recommendation.json` | `OVERLOAD` | Available capacity is accepted, excess load is rejected/unallocated, and a scale-up recommendation is returned. |
| `single-server-failure-recovery.json` | `MARK_UNHEALTHY`, `ROUTE`, `ALLOCATE`, `MARK_HEALTHY`, `ROUTE` | A failed server is excluded from routing/allocation, then re-enters decisions after recovery. |
| `all-unhealthy-degradation.json` | `EVALUATE`, `ROUTE` | No healthy capacity is available, so allocation degrades with unallocated load and routing returns no selected server. |
| `mixed-incident-replay.json` | `ROUTE`, `OVERLOAD`, `MARK_UNHEALTHY`, `EVALUATE`, `MARK_HEALTHY`, `ALLOCATE` | A multi-step incident stays ordered and deterministic across overload, degradation, recovery, and allocation. |
| `invalid-scenario.json` | unsupported event | The API returns a controlled `400` error for invalid scenario contracts. |

## Expected Descriptors

Stable expected-output descriptors live under `src/test/resources/scenarios/replay/expected/`.
They are deliberately smaller than full replay responses and include only contract fields that should remain stable:

- fixture name and request path;
- expected HTTP status;
- `readOnly=true` and `cloudMutation=false`;
- ordered replay event sequence;
- accepted, rejected, and unallocated load where deterministic;
- scaling recommendation count where deterministic;
- load-shedding priority/action where deterministic;
- selected server IDs for deterministic routing steps;
- health-state expectations after failure/recovery steps;
- no-negative-allocation expectation.

Descriptors must not include volatile or incidental fields such as timestamps, random IDs, request IDs, raw hosts, raw paths outside the API contract, generated output, or unordered whole-response snapshots.

## Regression Diff Expectations

`ScenarioReplayFixtureCatalogTest` posts each valid fixture to `POST /api/scenarios/replay` and asserts stable fields instead of brittle full-response snapshots:

- `readOnly=true` and `cloudMutation=false`;
- ordered replay step types;
- accepted, rejected, and unallocated load where stable;
- scale recommendation and load-shedding decisions where stable;
- no negative allocation values;
- deterministic replay output for the mixed incident;
- no `CloudManager` construction for valid fixtures;
- controlled bad-request shape for the invalid fixture.

`ScenarioReplayRegressionDiffTest` loads each expected descriptor, posts the matching request fixture, and compares the descriptor to the current replay response. Failures are reported as focused diffs:

```text
Scenario replay regression diff mismatch for fixture 'overload-scale-recommendation' at /steps/0/unallocatedLoad: expected <50.0> but was <45.0>
```

The tests intentionally avoid timestamps, random IDs, raw request identifiers, or incidental JSON ordering. Ordered replay steps are asserted because sequence order is part of the API contract.

## Running The Fixture Tests

Run the focused scenario replay catalog coverage:

```bash
mvn -B -Dtest=*ScenarioReplayRegressionDiffTest test
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
2. Add a matching descriptor under `src/test/resources/scenarios/replay/expected/`.
3. Add an entry to `catalog.json` with `path`, `expectedPath`, expected status, and a clear operator purpose.
4. Add stable-field assertions to `ScenarioReplayFixtureCatalogTest` only when the new fixture introduces a new behavior class.
5. Prefer semantic descriptor assertions over full JSON snapshots.
6. Avoid dynamic timestamps, random identifiers, request IDs, raw hosts, external services, real cloud calls, and generated artifacts.

Fixtures should remain small enough to read during incident review and deterministic enough to catch contract regressions in CI.
