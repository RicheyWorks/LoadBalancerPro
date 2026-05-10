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

4. If unallocated load is expected because all servers are unhealthy or exhausted, remediate the server health/capacity input before changing cloud settings.

5. If simulated scale-up is recommended, review the cloud guardrails separately. Do not enable live cloud mutation until operator intent, account, region, ownership, and capacity limits are verified.

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
4. Confirm no live cloud mutation is expected from allocation or evaluation APIs.
5. Escalate to infrastructure owners only after validating input health and capacity data.

## Cloud-Safety Expectations

Allocation, routing comparison, and read-only evaluation APIs are calculation and recommendation paths. They must not:

- create or mutate Auto Scaling Groups;
- register or deregister cloud resources;
- delete cloud resources;
- construct `CloudManager` for recommendation-only requests;
- require AWS credentials for local/demo use.

Live cloud mutation remains isolated behind `CloudManager` and explicit guardrails. Treat any unexpected cloud mutation from allocation/evaluation paths as a high-severity bug.

## Rollback And Release Evidence

If a deployment must be rolled back:

1. Identify the previous known-good GitHub Release.
2. Verify release assets and checksums before deploying artifacts.
3. Confirm the SBOM matches the artifact being deployed.
4. Re-run health and a small allocation/evaluation smoke after rollback.
5. Record the incident, release version, metrics observed, and operator decision.

Do not manually replace release assets unless hashes and provenance have been verified.

## Follow-Up After Stabilization

- Add a focused regression test for the exact degraded path.
- Update dashboards only with bounded labels.
- Review `docs/OBSERVABILITY.md` for alert and dashboard query improvements.
- Review `docs/API_SECURITY.md` if abuse or unauthenticated exposure contributed to the incident.
- Review `docs/LOAD_SHEDDING.md` if semantics were misunderstood by operators or clients.
