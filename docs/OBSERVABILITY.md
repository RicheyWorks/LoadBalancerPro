# Operator Observability

LoadBalancerPro emits domain metrics for allocation, overload, validation, parsing, and guarded cloud decisions through Micrometer. The local/demo profile exposes `/actuator/prometheus`; production-like profiles keep metrics exposure disabled unless operators intentionally enable it behind trusted network and authentication controls.

This document describes the stable operator signals. It does not require Prometheus, Grafana, OpenTelemetry, Docker, cloud credentials, or any external service to run tests.

## Enterprise Lab Observability Pack

The Enterprise Lab adds lab-grade, process-local observability for controlled adaptive-routing review. These signals are separate from production Prometheus exposure and remain protected by the existing `/api/**` boundary in prod/cloud-sandbox API-key mode:

- `GET /api/lab/metrics` returns JSON counters for lab runs, scenarios executed, policy decisions by mode, recommendations, active-experiment changes, guardrail blocks by reason, rollback/fail-closed events, audit retention/drops, explanation coverage, and rate-limit interactions.
- `GET /api/lab/metrics/prometheus` returns the same process-local counters as deterministic Prometheus-style sample text for local evidence and dashboard review.
- `scripts/smoke/enterprise-lab-observability-pack.ps1 -Package` writes metrics JSON, Prometheus-style text, Markdown summary, and a manifest under ignored `target/enterprise-lab-observability/`.
- `docs/observability/grafana-enterprise-lab-dashboard.json` is a source-visible dashboard template with placeholder datasource configuration and no secrets.
- `docs/observability/enterprise-lab-alerts.yml` contains alert-rule examples only.
- `docs/observability/SLO_TEMPLATES.md` contains lab and future production-gateway SLO templates; it does not claim production SLO certification.

The lab metrics are bounded, process-local, and intended for reviewer evidence. They are not centralized monitoring, durable audit storage, production capacity evidence, or production SLO proof.

## Metric Catalog

| Metric | Type | Labels | Meaning |
| --- | --- | --- | --- |
| `allocation.requests.count` | Counter | `strategy` | Allocation requests processed by the capacity-aware, predictive, or core routing strategy path. |
| `allocation.accepted.load` | Distribution summary | `strategy` | Load assigned to eligible healthy servers. |
| `allocation.rejected.load` | Distribution summary | `strategy` | Load that could not be assigned and was effectively shed or rejected. |
| `allocation.unallocated.load` | Distribution summary | `strategy` | Compatibility signal for unassigned load; intentionally mirrors rejected load. |
| `allocation.server.count` | Distribution summary | `strategy` | Healthy server count used by the allocation decision. |
| `allocation.scaling.recommended.servers` | Distribution summary | `strategy` | Simulated additional server count recommended for unallocated load. |
| `allocation.validation.failures.count` | Counter | `path`, `reason` | Malformed or invalid allocation API requests. |
| `cloud.scale.allowed.count` | Counter | `source`, `reason` | Guarded cloud scale decisions that passed safety checks. |
| `cloud.scale.denied.count` | Counter | `source`, `reason` | Guarded cloud scale decisions denied by safety checks. |
| `cloud.scale.dryrun.count` | Counter | `source`, `reason` | Guarded cloud scale decisions intentionally left in dry-run mode. |
| `cloud.scale.source` | Counter | `source`, `decision`, `reason` | Aggregate source/decision view of guarded cloud scaling. |
| `csv.parse.failures` | Counter | none | CSV parser failures. |
| `json.parse.failures` | Counter | none | JSON parser failures. |

The read-only evaluation endpoint, `POST /api/allocate/evaluate`, returns `metricsPreview.emitted=false` and previews the metric names and values without incrementing normal allocation metrics.

## Label Cardinality Policy

Metric labels must stay bounded and operator-safe.

Allowed domain label keys:

- `strategy`
- `path`, limited to known allocation API routes
- `reason`, limited to stable error or guardrail reasons
- `source`
- `decision`

Do not add labels that include request-specific, tenant-specific, or infrastructure-specific values:

- request IDs
- user IDs
- raw server IDs
- raw hostnames
- raw URL paths
- cloud resource IDs
- exception messages
- unbounded client-provided strings

If a new metric needs more detail, prefer a bounded enum-like reason, strategy, source, or decision label and put raw detail in logs or response payloads only when it is safe to expose.

## Prometheus Query Examples

Prometheus converts dots to underscores and counter series commonly include a `_total` suffix. Adjust labels and windows for the deployment scrape interval.

Allocation request rate by strategy:

```promql
sum by (strategy) (rate(allocation_requests_count_total[5m]))
```

Validation failure rate by endpoint and reason:

```promql
sum by (path, reason) (rate(allocation_validation_failures_count_total[5m]))
```

Rejected or shed load trend:

```promql
sum by (strategy) (rate(allocation_rejected_load_sum[5m]))
```

Unallocated load compatibility trend:

```promql
sum by (strategy) (rate(allocation_unallocated_load_sum[5m]))
```

Scaling recommendation pressure:

```promql
sum by (strategy) (rate(allocation_scaling_recommended_servers_sum[5m]))
```

Healthy server count used by allocation decisions:

```promql
sum by (strategy) (rate(allocation_server_count_sum[5m]))
/
clamp_min(sum by (strategy) (rate(allocation_server_count_count[5m])), 1)
```

Cloud scale denials by source and reason:

```promql
sum by (source, reason) (rate(cloud_scale_denied_count_total[5m]))
```

Evaluation usage note: `POST /api/allocate/evaluate` is read-only and does not emit normal allocation metrics. Track evaluation traffic through gateway or HTTP server metrics if the deployment exposes them, and use the response `metricsPreview` for decision review.

## Dashboard Panel Spec

| Panel | Query | Operator Question |
| --- | --- | --- |
| Allocation rate by strategy | `sum by (strategy) (rate(allocation_requests_count_total[5m]))` | Is traffic flowing through the expected allocation modes? |
| Validation failures | `sum by (path, reason) (rate(allocation_validation_failures_count_total[5m]))` | Are clients sending bad requests or malformed payloads? |
| Rejected load | `sum by (strategy) (rate(allocation_rejected_load_sum[5m]))` | Is the system shedding or leaving load unassigned? |
| Scaling pressure | `sum by (strategy) (rate(allocation_scaling_recommended_servers_sum[5m]))` | Is simulated scale-up being recommended repeatedly? |
| Healthy server count | `sum by (strategy) (rate(allocation_server_count_sum[5m])) / clamp_min(sum by (strategy) (rate(allocation_server_count_count[5m])), 1)` | Are allocations being made with too few healthy servers? |
| Cloud guardrail denials | `sum by (source, reason) (rate(cloud_scale_denied_count_total[5m]))` | Are cloud mutation guardrails blocking unsafe changes? |

Keep dashboard variables bounded. Strategy, path, source, decision, and reason are acceptable. Do not template dashboards by server ID, request ID, hostname, raw client IP, user ID, or cloud resource ID.

## Alert Ideas

- Validation failure rate remains elevated for 10 minutes.
- Rejected or unallocated load remains non-zero for multiple windows.
- Scaling recommendation pressure remains elevated while accepted load is flat.
- Average healthy server count trends toward zero while requests continue.
- Cloud scale denials spike after a deployment or configuration change.

Alerts should route to the owning operator with enough context to inspect the API response and current release version. Do not alert on single low-volume samples unless the deployment has a strict policy for any rejected load.
