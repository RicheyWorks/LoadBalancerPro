# LoadBalancerPro Performance Baseline

Date: generated locally by script

Status: measured-lane ready. Committed docs do not include fixed measured numbers, but the source-visible local runner generates measured loopback evidence under ignored `target/performance-baseline/`.

## Purpose And Scope

This document defines repeatable local benchmark evidence for LoadBalancerPro.

It is local evidence only. It is not a production SLO, production SLA, production capacity-planning result, universal performance claim, high-availability proof, production performance certification, or proof of cloud or live AWS performance.

No measured numbers are committed in this document because local latency varies by workstation, Java version, OS, background load, and JIT state. Generate fresh evidence for the commit under review.

Measured work uses stable request fixtures, a source-visible script, ignored output under `target/performance-baseline/`, local loopback-only execution, and no live/private-network dependency.

The Enterprise Lab observability pack is complementary evidence, not a replacement for this baseline. Its process-local lab counters, dashboard JSON, alert examples, and SLO templates help reviewers inspect adaptive-routing behavior, but they do not provide production latency, throughput, capacity, availability, or production SLO measurements.

## Environment Metadata

| Field | Value |
| --- | --- |
| Date | captured in `target/performance-baseline/performance-report.json` |
| Commit/tag | captured in `target/performance-baseline/performance-report.json` |
| Machine/CPU/RAM | OS and Java captured; CPU/RAM remain operator notes |
| OS | captured in `target/performance-baseline/performance-report.json` |
| Java version | captured in `target/performance-baseline/performance-report.json` |
| Maven version | not captured by the current script |
| Docker version | not used |
| Spring profile | `local` |
| Packaged JAR or Docker | packaged JAR local loopback |
| Background load notes | operator note; not inferred by the script |

Record environment details before comparing runs. Local background load, OS, JDK, Maven, CPU, memory, and loopback behavior can materially affect results.

## Build And Startup Commands

Dry-run the plan:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\performance-baseline.ps1 -DryRun
```

Generate the local packaged-JAR baseline:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\performance-baseline.ps1 -Package
```

Use a different loopback port if needed:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\performance-baseline.ps1 -Package -Port 19681
```

The script starts the app on `127.0.0.1` with the `local` profile. The local baseline does not require AWS credentials, live cloud resources, Docker Compose, Kubernetes, Terraform, Prometheus, Grafana, a real IdP, or external services.

## Deterministic Fixture Catalog

The fixture catalog lives at `docs/performance/performance-fixtures.json` and covers:

- `GET /api/health`
- `POST /api/allocate/evaluate`
- `POST /api/routing/compare`
- `GET /api/lab/scenarios`
- `POST /api/lab/runs`
- `GET /api/lab/policy`
- `GET /api/lab/metrics`
- `GET /enterprise-lab.html`

Fixtures use deterministic local payloads. They do not call live cloud resources, external services, private networks, public upstreams, registry endpoints, release endpoints, or real IdP tenants.

## Measurement Method

The checked-in runner uses PowerShell `Invoke-WebRequest` and `System.Diagnostics.Stopwatch` to avoid requiring external benchmark binaries. It records:

- request count
- success count
- error count
- error rate
- min latency
- max latency
- average latency
- p50 latency
- p95 latency
- p99 latency
- status counts
- warning-only threshold evaluation
- environment metadata

This is intentionally modest. It is a local product evidence lane, not a replacement for production load testing.

## Evidence Files

The runner writes:

- `target/performance-baseline/performance-report.json`
- `target/performance-baseline/performance-dashboard.json`
- `target/performance-baseline/performance-threshold-results.json`
- `target/performance-baseline/performance-summary.md`
- `target/performance-baseline/performance-evidence-manifest.json`

`performance-dashboard.json` is the compact dashboard-ready output. It uses stable field names such as `fixtureId`, `category`, `requestCount`, `successCount`, `errorCount`, `errorRatePercent`, `averageLatencyMillis`, `p50LatencyMillis`, `p95LatencyMillis`, `p99LatencyMillis`, and `thresholdStatus`.

## Regression Thresholds

`docs/performance/performance-thresholds.example.json` defines warning-only thresholds. Threshold warnings are intentionally not CI performance gates because CI latency can be noisy. Operators can compare `performance-threshold-results.json` across local runs and decide whether a warning requires investigation.

Thresholds must not be treated as production SLOs, customer SLAs, capacity commitments, or deployment certification.

## Result Tables

### Health Endpoint

| Fixture | Requests | p50 | p95 | p99 | Error rate | Notes |
| --- | ---: | --- | --- | --- | --- | --- |
| generated locally | see `target/performance-baseline/performance-report.json` | generated locally | generated locally | generated locally | generated locally | local/lab-grade only |

### Allocation Evaluation

| Fixture | Requests | p50 | p95 | p99 | Error rate | Notes |
| --- | ---: | --- | --- | --- | --- | --- |
| generated locally | see `target/performance-baseline/performance-report.json` | generated locally | generated locally | generated locally | generated locally | local/lab-grade only |

### Routing Comparison

| Fixture | Requests | p50 | p95 | p99 | Error rate | Notes |
| --- | ---: | --- | --- | --- | --- | --- |
| generated locally | see `target/performance-baseline/performance-report.json` | generated locally | generated locally | generated locally | generated locally | local/lab-grade only |

### Enterprise Lab And Observability

| Fixture | Requests | p50 | p95 | p99 | Error rate | Notes |
| --- | ---: | --- | --- | --- | --- | --- |
| generated locally | see `target/performance-baseline/performance-report.json` | generated locally | generated locally | generated locally | generated locally | local/lab-grade only |

## Interpretation Guidance

Compare only similar machines, JDK versions, OS versions, Spring profiles, fixture versions, and background-load conditions.

Percentiles matter because averages hide tail behavior. Local loopback results do not represent networked or cloud deployments.

Results can change after dependency updates, JVM updates, OS updates, configuration changes, endpoint changes, or background-load changes.

Production SLOs require real deployment requirements, real user traffic, reliability objectives, and infrastructure constraints.

## What Not To Claim

Do not claim:

- production SLO
- production SLA
- cloud capacity
- high-availability behavior
- live AWS performance
- replacement for managed load balancers
- universal benchmark
- production readiness
- production performance certification
- real user traffic capacity

## Future Work

Possible later additions:

- add CI artifact upload for optional benchmark evidence without turning it into a gate;
- add richer dashboard import docs after a real monitoring target is selected;
- add production benchmark design after deployment topology, traffic model, SLOs, ingress, TLS, IAM, and monitoring ownership are decided;
- consider `k6` or JMeter later only if needed and only through source-visible scripts.

Keep future performance work conservative until benchmark tooling, request payloads, and review expectations are stable.
