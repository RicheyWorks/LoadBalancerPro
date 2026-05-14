# LoadBalancerPro Observability SLO Templates

These SLOs are templates for the Enterprise Lab and future Production Gateway Candidate tracks. Note: current metrics are lab-grade/process-local unless a later production rollout explicitly adds durable telemetry, trusted scrape paths, alert routing, and operator ownership. No production SLO certification is claimed.

## Enterprise Lab Workflow SLO Templates

| Objective | Example target | Evidence source | Notes |
| --- | --- | --- | --- |
| Lab API availability | 99% successful local lab requests during a review session | `/api/lab/metrics`, smoke output | Local/demo or authenticated prod-like review only. |
| Evidence generation success | 100% successful observability evidence script runs for a release review | `target/enterprise-lab-observability/observability-summary.md` | Evidence remains ignored and local. |
| Performance baseline generation success | 100% successful local performance evidence script runs for a comparable review machine | `target/performance-baseline/performance-summary.md` and `performance-dashboard.json` | Local/lab-grade only; not a production performance SLO. |
| Deterministic run consistency | Same scenario catalog count and stable scorecard fields across repeated package runs | Enterprise Lab workflow smoke and observability pack smoke | Does not imply production traffic performance. |
| Explanation coverage | 100% scenario explanations in deterministic lab runs | `loadbalancerpro_lab_explanations_total` and run scorecards | Review output for stale/conflicting/all-unhealthy cases. |

## Controlled Adaptive-Routing SLO Templates

| Objective | Example target | Evidence source | Notes |
| --- | --- | --- | --- |
| Default behavior preservation | 100% off/shadow/recommend runs keep baseline final decisions | controlled adaptive-routing evidence | Active changes require explicit active-experiment mode. |
| Policy decision explainability | 100% decisions include guardrail or pass reason plus rollback reason | `/api/lab/audit-events` and `/api/lab/metrics` | Process-local audit only today. |
| Rollback/fail-closed success | 100% failed/stale/conflicting cases retain baseline final decision | controlled policy smoke | Future production rollout needs operator rollback automation. |
| Guardrail coverage | Every deterministic unsafe class has at least one counted guardrail block | Prometheus-style sample and JSON metrics | Lab-grade coverage, not production incident proof. |

## Future Production Gateway Candidate SLO Templates

| Objective | Candidate target | Required production evidence before claiming |
| --- | --- | --- |
| Request latency | p95 below an operator-defined threshold | Durable metrics, representative traffic, ingress/TLS/IAM proof, rollback playbooks |
| Error rate | Error budget set by service class | Real deployment telemetry, alert routing, incident ownership |
| Availability | Availability target by environment | Multi-instance deployment design and health-check behavior |
| Adaptive decision safety | Active changes below guardrail violation threshold | Central audit store, production rollout gates, canary/shadow controls |
| Rollback time | Operator-defined rollback time objective | Tested rollback automation and production runbook evidence |

## Production Rollout Requirements

Before using these as real production SLOs, operators need durable telemetry storage, trusted Prometheus or OpenTelemetry collection, alert ownership, dashboard access control, real traffic baselines, production TLS/IAM/ingress controls, and a rollback/disable path that is exercised outside demo mode.
