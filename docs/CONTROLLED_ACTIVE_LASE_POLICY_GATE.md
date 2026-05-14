# Controlled Active LASE Policy Gate

This page documents the controlled adaptive-routing policy gate for LoadBalancerPro Enterprise Lab. It is a real lab and evaluation capability: it makes LASE recommendations explicit, gated, auditable, and reversible before any future production gateway promotion.

It is not production deployment certification, not distributed traffic control, not a live-cloud validation path, and not approval to steer unmanaged production traffic.

## Modes

| Mode | Final allocation behavior | Intended use |
| --- | --- | --- |
| `off` | Baseline allocation stays final and LASE influence is disabled. | Default-safe production and local behavior. |
| `shadow` | LASE evaluates and explains, but baseline allocation stays final. | Observation and evidence without decision changes. |
| `recommend` | LASE records the recommendation, guardrail status, rollback reason, and audit event, but baseline allocation stays final unless a lab/operator path explicitly accepts it. | Reviewer/operator decision support. |
| `active-experiment` | LASE may change the experiment final decision only when every policy gate passes. | Explicitly opted-in, bounded Enterprise Lab experiments. |

`active-experiment` is disabled by default. Configuration resolves to `off` unless both the mode and the explicit active-experiment enable flag are set.

```properties
loadbalancerpro.lase.policy.mode=off
loadbalancerpro.lase.policy.active-experiment-enabled=false
loadbalancerpro.lase.policy.max-audit-events=100
```

Invalid mode values fail closed to `off`. Prod/cloud-sandbox startup does not silently enable active-experiment.

## Policy Gates

The policy engine only allows influence when all of the following are true:

- the mode permits influence;
- active-experiment is explicitly enabled for the bounded lab or evaluation context;
- the recommended backend exists, is eligible, and is healthy;
- capacity constraints pass;
- the signal is fresh enough;
- the signal is not conflicting;
- the candidate set is not all-unhealthy;
- the recommendation is different from the baseline decision;
- rollback or disable state is not active.

If any gate fails, the result degrades to the baseline decision and records a guardrail reason and rollback reason.

## Audit Events

Policy decisions emit bounded process-local audit events. Events include:

- timestamp;
- mode;
- context id or scenario id;
- baseline decision;
- recommendation;
- final decision;
- changed yes/no;
- guardrail reasons;
- rollback reason;
- explanation summary.

Audit events are bounded in memory, do not persist secrets, and are exposed through the Enterprise Lab status path for reviewer/operator inspection.

## Operator Status

The protected Enterprise Lab status endpoints are:

- `GET /api/lab/policy`;
- `GET /api/lab/audit-events`;
- `GET /api/lab/metrics`;
- `GET /api/lab/metrics/prometheus`.

Prod/cloud-sandbox API-key mode protects these endpoints through the existing `/api/**` deny-by-default boundary. OAuth2 mode uses the existing allocation-role boundary. Local/default mode remains convenient for loopback lab review.

Status output reports the configured mode, effective mode, active-experiment flag, allowed modes, retained audit event count, and the latest guardrail reason when present. Metrics output reports process-local counters for lab runs, scenarios, policy decisions, recommendations, active-experiment changes, guardrail blocks, rollback/fail-closed events, audit retention/drops, explanation coverage, and rate-limit interactions. It also warns that active-experiment and lab metrics are lab evidence only and not production certification.

## Evidence

Run the source-visible smoke script after packaging:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\controlled-adaptive-routing-policy.ps1 -Package
```

The script writes JSON and Markdown evidence under ignored `target/controlled-adaptive-routing/` output. It exercises `off`, `shadow`, `recommend`, `active-experiment`, guardrail-blocked scenarios, and rollback/fail-closed behavior. It does not call live cloud, private-network targets, release commands, container publication, registries, or `release-downloads/`.

Run the observability pack after packaging when reviewing metrics, dashboards, alerts, and SLO templates:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\enterprise-lab-observability-pack.ps1 -Package
```

The observability script writes metrics JSON, Prometheus-style sample text, a Markdown summary, and a manifest under ignored `target/enterprise-lab-observability/`. The related reviewer assets are `docs/observability/grafana-enterprise-lab-dashboard.json`, `docs/observability/enterprise-lab-alerts.yml`, and `docs/observability/SLO_TEMPLATES.md`.

## Limitations

- Audit events are process-local and bounded, not centralized append-only storage.
- Lab metrics are process-local and bounded, not centralized Prometheus or long-term observability storage.
- Active-experiment is lab/evaluation behavior, not a distributed production control plane.
- No production TLS, IAM, ingress, monitoring, incident-response, or rollback automation is provided by this policy gate alone.
- No live cloud/private-network validation is part of this evidence path.
- Future production gateway promotion still requires deployment architecture, operator approval flows, distributed observability, rollback controls, and release/container distribution gates.
