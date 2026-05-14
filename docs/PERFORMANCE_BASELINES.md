# Performance Regression Baselines

LoadBalancerPro uses CI-safe performance smoke tests to catch major regressions in the calculation-heavy API paths. These tests are not microbenchmarks, production capacity claims, or service-level objectives.

For reviewer-facing measured local loopback evidence, use [`../evidence/PERFORMANCE_BASELINE.md`](../evidence/PERFORMANCE_BASELINE.md) and `scripts/smoke/performance-baseline.ps1 -Package`. That lane writes latency/error-rate summaries, warning-only threshold results, and dashboard-ready JSON under ignored `target/performance-baseline/`.

## Covered Paths

The performance baseline suite covers:

- core routing comparison decisions through `RoutingComparisonService`
- `POST /api/allocate/capacity-aware` with an overloaded allocation request
- `POST /api/allocate/evaluate` with read-only load-shedding metadata
- `POST /api/routing/compare` for both healthy and all-unhealthy candidate sets

The fixtures are deterministic and do not use cloud credentials, external networks, Docker, Redis, databases, or live AWS resources.

## Budget Philosophy

Thresholds are intentionally broad. They are designed to catch seconds-level regressions such as accidental blocking calls, external I/O, repeated Spring context work inside a hot path, or unexpectedly expensive routing/allocation loops.

They are not intended to fail on normal CI jitter, host variance, garbage collection timing, or small algorithmic changes. If a performance smoke test fails, inspect whether the code path started doing unnecessary work before tightening any threshold.

## Running The Baselines

Run the focused suite with:

```bash
mvn -B -Dtest=*Performance* test
```

Run the full validation path with:

```bash
mvn -B test
mvn -B -DskipTests package
```

GitHub CI is the authoritative validation lane while local workstation Maven dependency resolution is affected by the known Maven Central PKIX trust-chain issue.

## Future Work

If the project needs deeper measurement later, add a separate opt-in benchmark profile using a purpose-built tool such as JMH or an external load-test harness. Keep that separate from normal pull-request CI unless the budgets remain broad, deterministic, and fast.
