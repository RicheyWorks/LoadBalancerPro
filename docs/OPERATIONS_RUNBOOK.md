# Operations Runbook

This runbook is for degraded allocation, load-shedding, validation-failure, and observability incidents in LoadBalancerPro. It assumes the service is running in a trusted environment and that release and cloud-safety guardrails remain enabled.

## First Checks

1. Confirm the running version:

```bash
curl -fsS http://127.0.0.1:8080/api/health
```

2. Confirm actuator health where exposed:

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
```

3. Confirm the latest deployed version matches the expected GitHub Release or deployment artifact. The current release automation publishes the JAR, CycloneDX SBOMs, and SHA256SUMS for future semantic version tags.

4. Confirm metrics exposure is intentional for the active profile. Local/demo exposes Prometheus. Production-like profiles should expose metrics only behind trusted network and authentication controls.

## Degraded Allocation Or Load Shedding

Symptoms:

- `allocation.rejected.load` or `allocation.unallocated.load` is sustained above zero.
- `allocation.scaling.recommended.servers` is repeatedly non-zero.
- Allocation responses include positive `unallocatedLoad`.
- Evaluation responses show `loadShedding.action` as a throttling or rejection decision.

Triage:

1. Inspect the latest allocation response:

```bash
curl -fsS -X POST http://127.0.0.1:8080/api/allocate/capacity-aware \
  -H "Content-Type: application/json" \
  -d @examples/capacity-aware-request.json
```

2. Inspect the read-only evaluation response for the same server set and load pressure. Evaluation is safe for operator review because it does not mutate allocation state, construct `CloudManager`, call AWS, or emit normal allocation metrics.

3. Compare:

- `acceptedLoad`
- `rejectedLoad`
- `unallocatedLoad`
- `recommendedAdditionalServers`
- `loadShedding.action`
- `loadShedding.reason`
- `metricsPreview.emitted`
- `remediationPlan.recommendations`

4. Use `remediationPlan.recommendations` as ranked advisory guidance:

- `SCALE_UP`: review capacity and cloud guardrails before any live scaling action.
- `SHED_LOAD`: defer, queue, or shed lower-priority work until capacity pressure clears.
- `INVESTIGATE_UNHEALTHY`: inspect health checks, server telemetry, and deployment drift.
- `RESTORE_CAPACITY` / `RETRY_WHEN_HEALTHY`: restore at least one healthy server before retrying traffic.
- `NO_ACTION`: keep current state and continue monitoring.

5. Export a deterministic Markdown or JSON remediation report when the incident needs to be attached to a ticket:

```bash
curl -fsS -X POST http://127.0.0.1:8080/api/remediation/report \
  -H "Content-Type: application/json" \
  -d '{
    "format": "MARKDOWN",
    "reportId": "incident-123",
    "title": "Allocation Overload Review",
    "evaluation": <evaluation-response-json>
  }'
```

The report exporter formats an existing evaluation or scenario replay response. It is advisory and read-only, does not execute remediation, does not construct `CloudManager`, and does not generate timestamps or random ids unless the caller supplies a report id.

For offline handoff or post-incident analysis, save the evaluation or replay JSON and run the remediation report CLI without starting the API server:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --remediation-report \
  --input saved-evaluation.json \
  --format markdown \
  --output incident-report.md \
  --manifest incident-report.manifest.json \
  --report-id incident-123
```

Attach both `incident-report.md` and `incident-report.manifest.json` to the incident ticket when integrity evidence is needed. Re-verify the bundle offline with:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --verify-manifest incident-report.manifest.json
```

The manifest is SHA-256 checksum evidence only. It detects missing or changed files, but it is not a private-key signature and does not prove operator identity.

For a portable ticket attachment, export a ZIP bundle instead:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --input saved-evaluation.json \
  --format markdown \
  --bundle incident-bundle.zip \
  --report-id incident-123 \
  --redact internal-host-01 \
  --redact-file incident-redactions.txt \
  --audit-log incident-audit.jsonl \
  --audit-action-id incident-123 \
  --audit-actor operator-a
```

Verify the bundle later without starting the API server:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --verify-bundle incident-bundle.zip
```

The bundle contains the saved input JSON, generated report, checksum manifest, verification summary, and README. Bundle verification checks manifest-listed SHA-256 hashes and rejects unsafe ZIP entry paths. It is tamper-evident evidence, not a cryptographic signature or identity proof.

Use `--redact` or `--redact-file` before sharing incident evidence outside the immediate response team. Redaction is deterministic literal string replacement for known sensitive values such as hostnames, server IDs, internal IDs, operator notes, or ticket-specific strings. Redacted bundles include `redaction-summary.json` with token SHA-256 digests and replacement counts, not the original sensitive strings. Review the exported report before attachment because redaction is not legal anonymization and cannot infer every sensitive value automatically.

Use `--audit-log` when the offline CLI action should be chained into a local operator evidence trail. Audit entries are JSON Lines with SHA-256 `entryHash` and `previousEntryHash` fields. Verify the chain later with:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --verify-audit-log incident-audit.jsonl
```

The local audit log detects changed entries, malformed entries, sequence gaps, deleted middle entries, and reordered entries. It is checksum chaining only: it is not a cryptographic signature, does not prove identity, does not provide non-repudiation, and is not centralized append-only storage. Save the latest entry hash in the incident ticket if tail-truncation detection is required later.

Inventory the local evidence directory before handoff:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --inventory incident-evidence \
  --inventory-format markdown \
  --verify-inventory \
  --include-hashes \
  --fail-on-invalid \
  --inventory-output evidence-catalog.md
```

The inventory detects bundles, manifests, audit logs, redaction summaries, reports, saved inputs, and verification summaries. It reuses offline bundle, manifest, and audit-log verification and reports the latest audit anchor hash/count when available. Treat it as a local checksum catalog only: it is not identity proof, cryptographic signing, centralized append-only storage, or legal chain-of-custody.

Compare sender and receiver inventories during handoff:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory sender-catalog.json receiver-catalog.json \
  --diff-format markdown \
  --fail-on-drift \
  --diff-output handoff-delta.md
```

The diff reports added, removed, checksum-changed, verification-status-drifted, and audit-anchor-drifted evidence. Audit anchor drift means the latest audit `entryHash` or entry count differs between catalogs. Treat drift as a review signal, not proof of malicious activity or legal chain-of-custody.

Evaluate the handoff delta against a local policy when some drift is expected:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory sender-catalog.json receiver-catalog.json \
  --policy-template regulated-handoff \
  --policy-report-format markdown \
  --fail-on-policy-fail \
  --policy-output handoff-policy-report.md
```

Use `strict-zero-drift` for final equality checks, `receiver-redaction` when receiver-side redaction is expected, `audit-append` when verification appends local audit entries, `regulated-handoff` for the strictest packaged review profile, and `investigation-working-copy` during active investigation. Use `--list-policy-templates` to see packaged templates, `--export-policy-template <name> --policy-output <path>` to copy one for local edits, and `--validate-policy <path>` before using custom policy files.

Use `STRICT` policies for zero-drift handoffs and `ALLOWLIST` policies when expected changes are known, such as a receiver-side redaction summary or expected audit-log append. Policy decisions are `PASS`, `WARN`, or `FAIL`; `--fail-on-policy-fail` exits non-zero only for `FAIL`. Attach the policy report to the incident ticket alongside both inventories.

For operator training or a dry-run before a real handoff, list and export packaged examples:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --list-policy-examples

java -jar target/LoadBalancerPro-2.4.2.jar \
  --walkthrough-policy-example receiver-redaction-warn \
  --example-output-dir walkthrough/receiver-redaction \
  --policy-report-format markdown
```

The walkthrough exports synthetic `before.json`, `after.json`, and `expected-decision.json` files, runs the same local diff and policy evaluator, and emits a deterministic tutorial summary. These examples are for training and regression checks only; they are not identity proof or legal chain-of-custody.

To practice the entire packaged policy matrix in one offline command:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --run-policy-training-lab \
  --training-lab-format markdown \
  --training-lab-output evidence-policy-training-lab.md
```

Use `--training-lab-export-dir walkthrough/all-examples` when the operator also needs the example `before.json`, `after.json`, and `expected-decision.json` files. The lab summarizes expected `PASS`, `WARN`, and `FAIL` examples, exits non-zero if any expected decision no longer matches, and remains a local checksum-policy training aid rather than proof of identity or legal custody.

To grade an operator's decision, reason, action, and optional remediation note against packaged answer keys:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --list-training-scorecards

java -jar target/LoadBalancerPro-2.4.2.jar \
  --print-training-scorecard regulated-handoff-fail

java -jar target/LoadBalancerPro-2.4.2.jar \
  --grade-training-scorecard scorecard-answers.json \
  --scorecard-format markdown \
  --scorecard-output scorecard-report.md \
  --fail-on-score-below 80
```

The answers file contains an optional `operator` label plus `answers` entries with `exerciseName`, `decision`, `reason`, `action`, and optional `notes`. Scorecards are intended for onboarding and refresher drills. They are local training aids only, not certification, legal compliance proof, identity proof, or legal chain-of-custody.

For GUI-facing or Postman-driven onboarding during a local API demo, use the read-only training routes:

```text
GET  /api/evidence-training/onboarding
GET  /api/evidence-training/templates
GET  /api/evidence-training/examples
GET  /api/evidence-training/scorecards
GET  /api/evidence-training/scorecards/{name}
GET  /api/evidence-training/scorecards/{name}/answer-template
POST /api/evidence-training/scorecards/grade
```

Open `http://localhost:8080/evidence-training-demo.html` for the first-party browser cockpit, or import `postman/LoadBalancerPro.postman_collection.json`, set `baseUrl` to the local API server such as `http://localhost:8080`, and run the `Evidence Training Demo Walkthrough` folder for the guided health, readiness, onboarding, scorecard-template, and grading path. The browser cockpit shows checklist step status, summary counters, a `Run demo sequence` control, stop/reset controls, copyable curl snippets, copyable response blocks, copy/export sample answer payloads, a copyable client-side summary preview, Postman parity notes, and PASS/WARN/FAIL grading summaries. Use the `Evidence Training Onboarding` folder when only the core onboarding routes are needed. These API routes are read-only discovery plus deterministic in-memory grading. They do not construct `CloudManager`, do not write runtime reports, and do not mutate cloud state. The browser page has no external scripts, styles, fonts, images, CDNs, services, dependencies, secrets, auth fields, browser storage, admin controls, release controls, ruleset controls, or cloud mutation controls. The API server is required for browser/API/Postman demos but optional for CLI workflows.

Open `http://localhost:8080/routing-demo.html` for the first-party routing decision demo, or run the `Routing Decision Demo` folder in the same Postman collection. This path uses the existing `POST /api/routing/compare` endpoint with deterministic synthetic server telemetry. It compares `TAIL_LATENCY_POWER_OF_TWO`, `WEIGHTED_LEAST_LOAD`, `WEIGHTED_LEAST_CONNECTIONS`, `WEIGHTED_ROUND_ROBIN`, and `ROUND_ROBIN`, then displays selected servers, strategy reasons, candidate lists, scores, raw JSON, copyable curl, copyable payload, and a normalized copyable summary. It is read-only recommendation output, not certification, not benchmark proof, and not a production-routing benchmark.

Open `http://localhost:8080/load-balancing-cockpit.html` for the unified load-balancing cockpit, or run the `Unified Load-Balancing Cockpit` folder in the same Postman collection. This page reuses existing calculation-only routes for allocation, routing comparison, overload/load-shedding evaluation, and advisory remediation-plan hints. The `Cockpit Navigation & Readiness` section provides a section index, current-panel orientation, in-memory readiness badges, and a deterministic copyable readiness summary. The `Operator Guided Walkthrough` section steps reviewers through scenario selection, endpoint execution, raw JSON review, explanation drill-down, comparison matrix generation, replay comparison, API contract trace verification, and review packet handoff with in-memory checklist state and panel highlights. The `Scenario Gallery` section adds normal load, overload pressure, all-unhealthy degradation, and recovery/capacity-restored payloads so an operator can compare real endpoint output across safe synthetic scenarios. The `Operator Comparison Matrix` section runs all packaged scenarios in deterministic order and summarizes routing, selected outcomes, allocation pressure, load-shedding, remediation hints, explanation rationale, and deltas in one copyable table. The `Operator Replay Mode` section replays a selected baseline/comparison scenario pair, highlights changed routing, allocation, overload, remediation, rationale, and error-state fields, and produces a copyable deterministic reviewer note without backend writes or browser storage. The `Operator Review Packet` section assembles current scenario, matrix, replay, drill-down, API contract trace, endpoint, payload, raw JSON reference, and safety notes into one copyable and printable browser handoff. The `API Contract Trace` section maps visible cockpit claims to endpoint paths, request payload sources, raw response sources, displayed raw fields, derived client labels, unavailable fields, and mutation/safety notes. The `Explanation Drill-Down` section adds routing strategy explanation, allocation capacity math, overload reason breakdowns, remediation rationale, and scenario delta explanation using real response fields plus clearly labeled derivations from visible request/response fields. Run the `Operator Scenario Gallery` and `Operator Explanation Drill-Down` Postman folders for matching request-by-request parity. It shows unsupported sections as `Not available in current API` instead of fabricating results. It is local/operator review only, not certification, not benchmark proof, not legal compliance proof, and not identity proof.

For real local HTTP forwarding tests, enable the optional `/proxy/**` mode with `loadbalancerpro.proxy.enabled=true` and configured loopback upstreams. The proxy removes the `/proxy` prefix, forwards method/path/query/body/safe headers, reuses the request-level routing strategy registry, skips upstreams configured with `healthy=false`, and returns `X-LoadBalancerPro-Upstream` plus `X-LoadBalancerPro-Strategy` response headers. Optional active health checks can be enabled with `loadbalancerpro.proxy.health-check.enabled=true`; probe responses with 2xx/3xx are treated as healthy, failing probes mark the upstream unavailable for selection, and `GET /api/proxy/status` reports read-only local counters and effective health. Optional bounded retries and process-local cooldown can be enabled with `loadbalancerpro.proxy.retry.*` and `loadbalancerpro.proxy.cooldown.*`; retries are off by default, non-idempotent retries require explicit opt-in, and cooldown state is memory-only. Open `http://localhost:8080/proxy-status.html` for the read-only browser status view of those fields, counters, retry/cooldown state, raw JSON, and local demo curl commands. Use [`PROXY_STRATEGY_DEMO_LAB.md`](PROXY_STRATEGY_DEMO_LAB.md) for deterministic `ROUND_ROBIN`, `WEIGHTED_ROUND_ROBIN`, and health-aware failover recipes that verify selected-upstream behavior through real forwarded HTTP headers. See [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md), [`REVERSE_PROXY_HEALTH_AND_METRICS.md`](REVERSE_PROXY_HEALTH_AND_METRICS.md), [`REVERSE_PROXY_RESILIENCE.md`](REVERSE_PROXY_RESILIENCE.md), and [`PROXY_OPERATOR_STATUS_UI.md`](PROXY_OPERATOR_STATUS_UI.md). Keep this mode loopback-bound or behind trusted private controls unless deployment-specific auth, TLS, ingress, and rate limits are in place. It is not a production-grade gateway or benchmark.

See [`OPERATOR_DEMO_WALKTHROUGH.md`](OPERATOR_DEMO_WALKTHROUGH.md), [`OPERATOR_GUIDED_WALKTHROUGH.md`](OPERATOR_GUIDED_WALKTHROUGH.md), [`OPERATOR_NAVIGATION_READINESS.md`](OPERATOR_NAVIGATION_READINESS.md), [`OPERATOR_SCENARIO_GALLERY.md`](OPERATOR_SCENARIO_GALLERY.md), [`OPERATOR_COMPARISON_MATRIX.md`](OPERATOR_COMPARISON_MATRIX.md), [`OPERATOR_REPLAY_MODE.md`](OPERATOR_REPLAY_MODE.md), [`OPERATOR_REVIEW_PACKET.md`](OPERATOR_REVIEW_PACKET.md), [`OPERATOR_API_CONTRACT_TRACE.md`](OPERATOR_API_CONTRACT_TRACE.md), [`OPERATOR_EXPLANATION_DRILLDOWN.md`](OPERATOR_EXPLANATION_DRILLDOWN.md), [`POSTMAN_EVIDENCE_TRAINING.md`](POSTMAN_EVIDENCE_TRAINING.md), [`POSTMAN_ROUTING_DEMO.md`](POSTMAN_ROUTING_DEMO.md), [`POSTMAN_LOAD_BALANCING_COCKPIT.md`](POSTMAN_LOAD_BALANCING_COCKPIT.md), [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md), [`REVERSE_PROXY_HEALTH_AND_METRICS.md`](REVERSE_PROXY_HEALTH_AND_METRICS.md), [`REVERSE_PROXY_RESILIENCE.md`](REVERSE_PROXY_RESILIENCE.md), [`PROXY_OPERATOR_STATUS_UI.md`](PROXY_OPERATOR_STATUS_UI.md), [`PROXY_STRATEGY_DEMO_LAB.md`](PROXY_STRATEGY_DEMO_LAB.md), [`REMEDIATION_REPORT_CLI.md`](REMEDIATION_REPORT_CLI.md), [`EVIDENCE_POLICY_TEMPLATES.md`](EVIDENCE_POLICY_TEMPLATES.md), and [`EVIDENCE_POLICY_EXAMPLES.md`](EVIDENCE_POLICY_EXAMPLES.md) for demo steps, guided review order, navigation/readiness review, Postman import, scenario gallery review, comparison matrix review, replay mode review, review packet handoff, API contract trace review, explanation drill-down review, routing comparison, load-balancing cockpit review, lightweight proxy forwarding, proxy health/metrics demos, proxy retry/cooldown behavior, proxy status UI review, strategy-specific proxy selected-upstream demos, CLI inputs, bundle export, manifest verification, evidence inventory, evidence catalog diffing, evidence handoff policies, packaged policy templates, example sender/receiver catalog pairs, safety guarantees, and JSON output.

6. If unallocated load is expected because all servers are unhealthy or exhausted, remediate the server health/capacity input before changing cloud settings.

7. If simulated scale-up is recommended, review the cloud guardrails separately. Do not enable live cloud mutation until operator intent, account, region, ownership, and capacity limits are verified.

## Validation Failure Spike

Symptoms:

- `allocation.validation.failures.count` increases sharply.
- API clients receive structured 400, 413, 415, or 405 responses.
- Gateway or HTTP server metrics show a 4xx spike.

Triage:

1. Group failures by bounded labels:

```promql
sum by (path, reason) (rate(allocation_validation_failures_count_total[5m]))
```

2. Check whether failures are malformed JSON, validation failures, unsupported media type, wrong method, or payload-size enforcement.

3. Confirm responses do not include stack traces, exception class names, credentials, request bodies, or cloud details.

4. If failures come from a known client deployment, validate that client payloads still match the OpenAPI contract and examples.

5. If failures appear abusive, rate limit at the gateway or reverse proxy. Keep app-level labels bounded and do not add request-specific metric labels.

## No Healthy Servers Or All-Unhealthy Input

Symptoms:

- Allocation responses have an empty `allocations` map.
- `unallocatedLoad` equals the requested load.
- `recommendedAdditionalServers` may be zero when target capacity is unavailable.
- `allocation.server.count` trends toward zero.

Triage:

1. Verify the caller-provided server list marks the intended servers as healthy.
2. Check CPU, memory, disk, capacity, and weight fields for validation errors or exhausted capacity.
3. Use `POST /api/allocate/evaluate` to preview the decision with the same server list.
4. Check `remediationPlan.status`; `NO_HEALTHY_CAPACITY` means restore health/capacity before retrying allocation or routing.
5. Confirm no live cloud mutation is expected from allocation or evaluation APIs.
6. Escalate to infrastructure owners only after validating input health and capacity data.

## Cloud-Safety Expectations

Allocation, routing comparison, and read-only evaluation APIs are calculation and recommendation paths. They must not:

- create or mutate Auto Scaling Groups;
- register or deregister cloud resources;
- delete cloud resources;
- construct `CloudManager` for recommendation-only requests;
- require AWS credentials for local/demo use.

Live cloud mutation remains isolated behind `CloudManager` and explicit guardrails. Treat any unexpected cloud mutation from allocation/evaluation paths as a high-severity bug.

The remediation planner is advisory only. It ranks operator actions but does not execute them, does not construct `CloudManager`, and does not call AWS.

The remediation report exporter is also advisory only. It converts supplied evaluation or replay results into deterministic Markdown/JSON evidence for humans and automation. It does not run allocation, replay, cloud, or remediation actions by itself.

Incident bundle export wraps that same offline report output with the saved input JSON, checksum manifest, and verification summary. It remains local-only, does not start the API server, and does not add signing keys or key management.

Incident evidence redaction runs before checksum manifests are written. This means manifest and bundle verification prove the redacted files stayed unchanged after export, but they do not prove the original evidence was complete, safe to disclose, or fully scrubbed.

Offline CLI audit logging appends local checksum-chained entries after successful report, manifest, bundle, verification, and redacted-output actions. It remains local-only and does not start the API server, construct `CloudManager`, add signing keys, or contact external services.

Offline evidence inventory scans a local directory and summarizes bundles, manifests, audit logs, redaction summaries, reports, inputs, and verification summaries. With verification enabled, it detects tampered or missing bundle/manifest evidence and invalid audit chains while keeping the catalog deterministic and local-only.

Offline evidence catalog diffing compares two saved JSON inventory catalogs and summarizes handoff drift without starting the API server. It is useful for sender/receiver reviews and ticket revisions, but it remains checksum/inventory comparison only and does not prove identity or legal chain-of-custody.

Offline evidence handoff policies classify catalog drift with local pass/warn/fail rules. They support strict zero-drift checks and allowlists for expected file, checksum, verification-status, or audit-anchor changes, but they remain local policy evaluation only and do not prove identity, intent, or legal custody.

Packaged evidence policy templates provide reusable local profiles for common handoffs: zero drift, receiver redaction, audit append, regulated review, and active investigation working copies. They are convenience rules for deterministic drift classification, not compliance certification, identity proof, or legal chain-of-custody.

The evidence policy training lab batch-runs the packaged walkthrough examples and checks actual decisions against expected outcomes. It helps operators rehearse the sender/receiver handoff flow offline, but it remains synthetic training material and is not a substitute for real incident review.

Evidence training scorecards grade packaged-example operator answers locally. They produce deterministic Markdown or JSON reports, can fail a script with `--fail-on-score-below`, do not start the API server, and do not construct or mutate `CloudManager`.

## Test And Coverage Evidence

For reviewer-facing test evidence, use [`TESTING_COVERAGE.md`](TESTING_COVERAGE.md). GitHub CI is the source of truth when a local workstation cannot resolve Maven dependencies because of Java trust-store or PKIX issues.

The `Build, Test, Package, Smoke` workflow runs the default Maven tests, verifies the Surefire skipped-test count, generates JaCoCo HTML/XML/CSV output under `target/site/jacoco`, prints instruction/branch/line percentages from `jacoco.csv`, and uploads the report as the `jacoco-coverage-report` artifact. Treat those reports as quantified regression evidence for the simulator, API, CLI, safety, and local cockpit flows. They are not production reverse-proxy throughput proof, benchmark proof, certification, legal compliance proof, or live-cloud validation.

## Rollback And Release Evidence

If a deployment must be rolled back:

1. Identify the previous known-good GitHub Release.
2. Verify release assets and checksums before deploying artifacts.
3. Confirm the SBOM matches the artifact being deployed.
4. Re-run health and a small allocation/evaluation smoke after rollback.
5. Record the incident, release version, metrics observed, and operator decision.
6. Attach the Markdown or JSON remediation report if one was exported during triage.

Do not manually replace release assets unless hashes and provenance have been verified.

## Follow-Up After Stabilization

- Add a focused regression test for the exact degraded path.
- Update dashboards only with bounded labels.
- Review `docs/OBSERVABILITY.md` for alert and dashboard query improvements.
- Review `docs/API_SECURITY.md` if abuse or unauthenticated exposure contributed to the incident.
- Review `docs/LOAD_SHEDDING.md` if semantics were misunderstood by operators or clients.
