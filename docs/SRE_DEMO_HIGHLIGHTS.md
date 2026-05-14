# SRE Demo Highlights

Use this page as the concise reviewer or portfolio walkthrough for LoadBalancerPro Enterprise Lab after the verified `v2.5.0` JAR/docs-first release and the adaptive-routing hardening sprint. It is a demo guide, not production certification.

## Fast Story

LoadBalancerPro Enterprise Lab is a Java/Spring lab for adaptive-routing scenarios, deterministic replay, LASE shadow/influence comparison, policy gates, scorecards, evidence export, SRE walkthroughs, and a bounded Production Gateway Candidate track. Review [`ENTERPRISE_LAB_PRODUCT_CHARTER.md`](ENTERPRISE_LAB_PRODUCT_CHARTER.md) and [`ENTERPRISE_LAB_ROADMAP.md`](ENTERPRISE_LAB_ROADMAP.md) when the question is where the product goes next.

The current implementation has guarded cloud boundaries, deterministic adaptive-routing evidence, optional local proxy forwarding, and release evidence that a reviewer can inspect without trusting hidden infrastructure.

The strongest SRE/product-value thread is:

1. A request arrives at a calculation-only allocation, evaluation, routing, replay, remediation, or proxy endpoint.
2. Local/demo mode stays convenient, while container/default deployment uses the protected `prod` profile.
3. Prod/cloud-sandbox API-key mode is deny-by-default for non-`OPTIONS` `/api/**`, with `GET /api/health` as the explicit public API exception.
4. OAuth2 mode accepts app roles only from dedicated role claims, not ordinary `scope` or `scp` values.
5. Required DTO fields reject omitted JSON instead of silently defaulting to `0`, `0.0`, or `false`.
6. Optional process-local rate limiting can protect allocation, routing, replay, remediation, proxy, and LASE shadow surfaces in single-instance demos; distributed edge rate limiting remains required for shared or public deployments.
7. LASE shadow mode can explain the adaptive-routing signals considered for `POST /api/allocate/evaluate` without altering live allocation.
8. The Enterprise Lab workflow turns those signals into a reviewer-facing scenario catalog, run API, scorecard, evidence export, and browser lab page while remaining process-local and bounded.
9. Cloud mutation remains behind explicit dry-run, intent, prefix, ownership, account/region, and capacity guardrails.

## Release Proof

The `v2.5.0` release is verified as a JAR/docs-first GitHub Release:

- exact tag: `v2.5.0`
- exact release commit: `4cc03750be5479d9f8f88f8ef8014e05a8dc587a`
- expected release assets verified: `LoadBalancerPro-2.5.0.jar`, `LoadBalancerPro-2.5.0-bom.json`, `LoadBalancerPro-2.5.0-bom.xml`, and `LoadBalancerPro-2.5.0-SHA256SUMS.txt`
- checksum verification passed
- SBOM JSON/XML assets are present
- GitHub artifact attestation status is verified
- container publication and container signing are deferred

Evidence links: [`V2_5_0_POST_RELEASE_VERIFICATION.md`](V2_5_0_POST_RELEASE_VERIFICATION.md), [`RELEASE_NOTES_v2.5.0.md`](RELEASE_NOTES_v2.5.0.md), and [`PRODUCTION_READINESS_SUMMARY.md`](PRODUCTION_READINESS_SUMMARY.md).

## Guardrail Depth

The most reviewer-visible safety boundaries are implemented and tested:

- `Dockerfile` container startup defaults to the `prod` profile and requires an operator-provided `LOADBALANCERPRO_API_KEY`.
- Prod/cloud-sandbox API-key mode protects `/api/**`, `/proxy/**`, `/api/proxy/status`, `/v3/api-docs`, and Swagger UI, except for documented public health and preflight routes.
- OAuth2 application roles come from dedicated role claims such as `roles`, `role`, `authorities`, or `realm_access.roles`.
- Scope-only OAuth2 tokens do not silently become `ROLE_operator` or `ROLE_admin`.
- Allocation DTOs use explicit required values so omitted JSON fails validation.
- Local/default mode is intentionally permissive and must stay loopback/private.
- The optional process-local rate limiter is disabled by default, returns `429 rate_limited` with `Retry-After` when enabled, and is documented as a single-instance guardrail rather than a distributed quota system.

Evidence links: [`API_SECURITY.md`](API_SECURITY.md), [`IDP_CLAIM_MAPPING_EXAMPLES.md`](IDP_CLAIM_MAPPING_EXAMPLES.md), [`API_CONTRACTS.md`](API_CONTRACTS.md), and [`CONTAINER_DEPLOYMENT.md`](CONTAINER_DEPLOYMENT.md).

## Cloud Safety

CloudManager remains the only AWS mutation boundary. Demo and CI paths do not require AWS credentials. Live sandbox mutation remains denied unless the operator deliberately configures:

- live mode instead of dry-run default
- operator intent flag
- sandbox resource-name prefix
- ownership confirmation
- account and region allow-lists
- capacity limits
- deletion-specific confirmation before resource deletion

The recommended SRE demo line is: "The adaptive-routing and remediation paths can recommend or explain actions, but they do not quietly mutate cloud resources."

Evidence links: [`OPERATIONS_RUNBOOK.md`](OPERATIONS_RUNBOOK.md), [`DEPLOYMENT_HARDENING_GUIDE.md`](DEPLOYMENT_HARDENING_GUIDE.md), and [`../evidence/SAFETY_INVARIANTS.md`](../evidence/SAFETY_INVARIANTS.md).

## Adaptive-Routing Story

The adaptive-routing foundation has moved from a demo foundation toward observable product value:

- deterministic LASE/replay tests cover empty replay input, malformed replay entries, repeated event ordering, stale/out-of-order metrics, conflicting signals, all-unhealthy candidates, overload pressure, and recovery-style transitions
- `POST /api/allocate/evaluate` now includes a `laseShadow` block when `loadbalancerpro.lase.shadow.enabled=true`
- `laseShadow` is shadow-only, lists signals considered, records observation status, reports recommended server/action when available, and states that it does not alter live allocation
- `GET /api/lase/shadow` exposes bounded process-local observability for recent shadow events
- the adaptive-routing experiment harness compares baseline vs shadow vs opt-in influence across deterministic fixtures, keeps default behavior unchanged, and writes ignored review evidence under `target/adaptive-routing-experiments/`
- the Enterprise Lab workflow exposes `GET /api/lab/scenarios`, `POST /api/lab/runs`, bounded in-memory run retrieval, scorecards, `/enterprise-lab.html`, and ignored evidence export under `target/enterprise-lab-runs/`
- active LASE influence over live allocation remains future work and is intentionally not enabled by default

Evidence links: [`API_CONTRACTS.md`](API_CONTRACTS.md), [`SCENARIO_SIMULATION.md`](SCENARIO_SIMULATION.md), and `LaseAllocationShadowIntegrationTest`.

Run the local experiment harness after packaging with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\adaptive-routing-experiment.ps1 -Package
```

The script runs `--adaptive-routing-experiment=all`, records shadow-only and opt-in influence comparison output, and performs no live cloud mutation, API server startup, release action, container publication, or external network call. The opt-in influence path is a feature flag style experiment mode in the CLI output only; it is not a production routing control.

Run the Enterprise Lab workflow evidence export after packaging with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\enterprise-lab-workflow.ps1 -Package
```

The script runs `--enterprise-lab-workflow=all`, writes scenario catalog JSON, run JSON, Markdown scorecard summary, and metadata under `target/enterprise-lab-runs/`, and performs no CloudManager, live cloud, external network, release, tag, asset, container, registry, or `release-downloads/` operation.

## Walkthrough

For a short interviewer walkthrough:

1. Open [`DEMO_WALKTHROUGH.md`](DEMO_WALKTHROUGH.md).
2. Run the local cockpit and show allocation pressure, routing comparison, remediation hints, and raw JSON.
3. Show the protected-prod/container posture in [`API_SECURITY.md`](API_SECURITY.md) or [`CONTAINER_DEPLOYMENT.md`](CONTAINER_DEPLOYMENT.md).
4. Run or describe `POST /api/allocate/evaluate` with `loadbalancerpro.lase.shadow.enabled=true` and point to the `laseShadow` explanation.
5. Open `/enterprise-lab.html` or run `scripts/smoke/enterprise-lab-workflow.ps1 -Package`, then show the scenario catalog, lab run scorecard, guardrail-blocked influence counts, and ignored `target/enterprise-lab-runs/` evidence.
6. Run or describe the adaptive-routing experiment harness and show the baseline vs shadow vs opt-in influence matrix under `target/adaptive-routing-experiments/`.
7. Open [`V2_5_0_POST_RELEASE_VERIFICATION.md`](V2_5_0_POST_RELEASE_VERIFICATION.md) to show release evidence, SBOM, checksums, and attestation posture.
8. Close with the current limits below so the review stays honest.

## Honest Remaining Risks

- This is not production deployment certification.
- No real enterprise IdP tenant proof is included.
- Production TLS, IAM, ingress, monitoring, log retention, WAF, distributed rate limiting, backup, and incident-response operations are deployment-owner responsibilities.
- Container registry publication and container signing are deferred.
- Active LASE influence over live allocation is future-gated; current production integration is shadow-only and experiment influence is local/opt-in only.
- Live cloud sandbox validation is outside the default Maven/CI evidence.
