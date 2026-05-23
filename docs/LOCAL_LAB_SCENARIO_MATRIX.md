# Local Lab Scenario Matrix

This matrix is docs/test-only planning for the future Local Lab Kit and simulated datacenter test harness described by [`adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md`](adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md).

It is planning-only. It does not implement Docker Compose files, scripts, fake backend nodes, k6 scenario files, Bruno collections, Toxiproxy configuration, Prometheus/Grafana dashboards, telemetry ingestion, replay execution, report generation, storage/persistence, filesystem-writing behavior, export/upload/download/PDF/ZIP behavior, routing/scoring/strategy/proxy/API behavior changes, or production validation.

Local simulation is useful evidence. Local simulation is not production certification. LAN/server/Acer AI hardware experiments remain controlled lab work unless separately validated.

## Test-Scope Scenario Model Foundation

PR #250 adds only a test-scope scenario model/catalog. It does not implement fake backend servers. It does not implement Docker Compose, k6, Bruno, Toxiproxy, Prometheus/Grafana, scripts, networking, or runtime behavior. It is a stepping stone toward future local lab tooling.

This PR adds test-scope response fixtures only. The fixtures describe future fake backend response expectations. It is not fake backend server implementation. It does not implement fake backend servers. It does not start listeners, open ports, call localhost, generate traffic, run Docker, k6, Bruno, Toxiproxy, Prometheus/Grafana, scripts, networking, or runtime behavior. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling, and this is not production proof.

This PR adds test-scope passive transcript fixtures only. Transcripts describe future request/response evidence expectations. Transcripts do not execute HTTP requests. Transcripts do not implement fake backend servers. Transcripts do not start listeners, open ports, call localhost, generate traffic, run replay, write reports, persist storage, or run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. This is still not production proof.

This PR adds a test-scope passive transcript summary renderer only. The renderer summarizes existing passive transcript fixtures in memory. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It does not implement fake backend servers. It does not start listeners, open ports, call localhost, generate traffic, or run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. This is still not production proof.

This PR adds a test-scope passive reviewer checklist mapper only. The mapper turns existing passive transcript summaries into in-memory reviewer checklist entries. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It does not implement fake backend servers. It does not start listeners, open ports, call localhost, generate traffic, or run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. This is still not production proof.

This PR adds a test-scope implementation readiness gate only. The readiness gate evaluates passive planning/test artifacts in memory. It does not implement fake backend servers. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It does not start listeners, open ports, call localhost, generate traffic, or run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. Fake backend execution remains future tooling only. Passing the readiness gate only means ready for a separately scoped implementation PR, not production proof.

This PR adds a test-scope fake backend handler only. The handler maps simulated request labels to existing response fixtures in memory. It does not implement fake backend servers. It does not start listeners. It does not open ports. It does not call localhost. It does not generate traffic. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It does not run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. Passing handler tests is not production proof.

## Scenario Matrix

| Future scenario | Purpose | Expected signals | Expected evidence | Not-proven boundary | Future tool candidate |
| --- | --- | --- | --- | --- | --- |
| healthy baseline | Establish a stable local reference before introducing damage. | low latency, low error rate, low load, healthy backend state | selected backend, rejected backends, signals used, policy gate status, safety mode, what was proven, what was not proven | not production throughput, not SLA/SLO proof, not live-cloud validation | Docker Compose, lightweight fake services, Windows PowerShell |
| slow backend / tail latency | Show how p95/p99 tail latency changes reviewer/operator interpretation. | p95/p99 latency, latency jitter, slow response count, timeout rate | selected/rejected backend explanation, latency observations, tail-latency warning, local simulation boundary | not production benchmark, not capacity planning, not routing behavior change | k6, Toxiproxy, fake backend delay controls |
| partial degradation | Model degraded-but-not-down behavior before simple health checks hide nuance. | intermittent slowness, mixed status, degraded health label, recovery marker | degradation notes, candidate comparison, policy gate status, safety mode | not real datacenter fault proof, not production resilience proof | fake backend controls, Toxiproxy |
| intermittent 500 errors | Exercise controlled error-prone backend evidence. | 500 rate, recent error burst, successful recovery responses | error observations, rejected backend rationale, safety-mode boundary | not production incident proof, not compliance proof | WireMock or lightweight fake services, k6 |
| overload / queue pressure | Model overload and queue-depth pressure as separate review signals. | queue depth, active requests, load, saturation, retry/timeout trend | overload observations, rejected options, what changed during pressure | not autoscaling proof, not production capacity proof | fake backend controls, k6 |
| all-unhealthy or no-good-choice scenario | Make the no-good-choice boundary explicit. | all candidates unhealthy/degraded, high error/latency, unavailable healthy option | selected fallback if any, rejected options, policy gate status, safety mode, not-proven notes | not correctness proof, not production failover certification | fake services, Toxiproxy |
| recovery scenario | Show whether evidence captures transition back toward healthy behavior. | recovery marker, error-rate drop, latency improvement, queue reduction | what changed during recovery, selected/rejected candidate changes, operator-review notes | not proof of production recovery objectives | k6, fake backend controls |
| strategy comparison scenario | Compare existing or future strategies under the same local conditions. | selected backend by strategy, candidate scores where exposed, signal deltas | strategy-by-scenario comparison, selected/rejected explanation, warning boundaries | not exact production scoring, not strategy certification | k6, local APIs |
| evidence completeness scenario | Check whether future scenario output is reviewer-ready. | selected backend, rejected backends, signals used, policy gate status, safety mode, not-proven fields | completeness checklist, missing-field warnings, what was proven, what was not proven | not EvidencePacket implementation, not report generation | Bruno, local APIs |
| safety-mode boundary scenario | Exercise observe-only, recommendation, shadow, and active-experiment boundaries before autonomy. | mode, policy gate status, guardrail reason, changed/not-changed flag | safety-mode explanation, guardrail decision, rejected mutation authority | not autonomous production traffic shifting, not runtime enforcement implementation | local APIs, Bruno |
| local hardware expansion scenario | Plan staged movement from one Windows PC to LAN hosts. | host role, backend role, network scope, local-only boundary | hardware expansion note, lab-only warning, production-not-proven warning | not live-cloud validation, not real-tenant validation, not production infrastructure | Windows PowerShell, future LAN server, future Acer AI mini |
| operator-review scenario | Give operators a repeatable checklist before future experiments expand. | scenario name, expected signals, observed signals, evidence completeness, safety mode | operator review notes, not-proven boundary, next recommended action | not approval to run production traffic | Bruno, README/trust-map docs |

## Evidence Expectations

Future scenario output should eventually show:

- selected backend;
- rejected backends;
- signals used;
- latency/error/load observations;
- p95/p99 tail latency observations when modeled;
- overload or queue pressure observations when modeled;
- policy gate status;
- safety mode;
- what changed during the scenario;
- what was proven;
- what was not proven;
- why local simulation is not production proof;
- why evidence completeness matters before autonomy.

## Hardware Boundary

The planned hardware expansion path remains staged:

1. Current Windows PC runs LoadBalancerPro and test tools.
2. Same PC simulates backends in containers.
3. Future LAN server can host backend nodes.
4. Future Acer AI mini machine can support coding, research, local inference, and telemetry experiments.
5. Additional machines can become degraded or overloaded nodes.
6. Real hardware tests still do not equal production certification.
7. Live-cloud and real-tenant validation remain separate future gates.

The Acer AI mini machine is future local lab/coding/research/local inference/telemetry support, not production infrastructure.

## Non-Goals

This matrix does not add:

- Docker Compose lab implementation;
- scripts implementation;
- fake backend node implementation;
- k6 scenario files;
- Bruno collections;
- Prometheus/Grafana dashboards;
- Toxiproxy configuration;
- runtime LASE enforcement;
- package-boundary enforcement;
- ArchUnit dependency/enforcement;
- source-name guard implementation;
- routing/scoring/strategy/proxy/API behavior changes;
- reviewer portal/dashboard/API behavior;
- EvidencePacket implementation;
- EvidenceAssembler implementation;
- replay execution;
- evidence/report generation;
- storage/persistence;
- filesystem-writing/export behavior;
- workload generation;
- trace import;
- external signal ingestion;
- autonomous production traffic shifting;
- carbon-aware routing;
- GPU orchestration;
- power/grid control;
- facility automation;
- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation.
