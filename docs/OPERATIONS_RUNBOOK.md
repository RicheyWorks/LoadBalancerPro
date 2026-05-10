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

See [`REMEDIATION_REPORT_CLI.md`](REMEDIATION_REPORT_CLI.md) for CLI inputs, bundle export, manifest verification, safety guarantees, and JSON output.

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
