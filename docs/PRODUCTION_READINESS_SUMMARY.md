# Production Readiness Summary

This summary is the reviewer-facing snapshot for LoadBalancerPro after the enterprise-production-candidate hardening line. It says what is ready for controlled production-like review, what remains outside scope, and which evidence proves each claim.

Current label: production-candidate for controlled enterprise demo/reviewer usage, with a verified `v2.5.0` JAR/docs-first GitHub Release. This is not production certification and not approval for unmanaged public traffic.

## Product Identity

LoadBalancerPro's next identity is **LoadBalancerPro Enterprise Lab**: Enterprise Adaptive Routing Lab first, Production Gateway Candidate second. The lab track owns controlled scenario running, deterministic replay, LASE shadow/influence comparison, policy gates, scorecards, evidence export, and SRE walkthroughs. The gateway-candidate track owns optional future proxy/runtime hardening, config reload, metrics, rate limiting, canary/shadow mode, rollback, signed container distribution later, and deployment guides later. See [`ENTERPRISE_LAB_PRODUCT_CHARTER.md`](ENTERPRISE_LAB_PRODUCT_CHARTER.md), [`ENTERPRISE_LAB_ROADMAP.md`](ENTERPRISE_LAB_ROADMAP.md), and [`NEXT_GOAL_PROMPTS.md`](NEXT_GOAL_PROMPTS.md).

## Production-Candidate Status

| Area | Current status | Evidence |
| --- | --- | --- |
| Auth boundary | Local/default mode is intentionally permissive for developer demos. Container/default deployment uses the `prod` profile. Prod/cloud-sandbox API-key mode is deny-by-default for non-`OPTIONS` `/api/**`, with `GET /api/health` as the explicit public API exception. `/proxy/**`, `/api/proxy/status`, `/v3/api-docs`, and Swagger UI are protected in prod/cloud-sandbox API-key mode. | [`API_SECURITY.md`](API_SECURITY.md), [`CONTAINER_DEPLOYMENT.md`](CONTAINER_DEPLOYMENT.md), [`../evidence/SECURITY_POSTURE.md`](../evidence/SECURITY_POSTURE.md) |
| OAuth2 role claims | OAuth2 mode validates JWTs through Spring Security. Application roles come from dedicated `roles`, `role`, `authorities`, or `realm_access.roles` claims. Standard `scope` and `scp` claims do not grant `ROLE_operator` or `ROLE_admin`; missing or ambiguous role claims fail closed for role-required routes. | [`IDP_CLAIM_MAPPING_EXAMPLES.md`](IDP_CLAIM_MAPPING_EXAMPLES.md), [`API_SECURITY.md`](API_SECURITY.md), `OAuth2AuthorizationTest` |
| DTO validation | Enterprise-required allocation and evaluation fields reject omitted JSON values instead of silently defaulting to `0`, `0.0`, or `false`. | [`API_CONTRACTS.md`](API_CONTRACTS.md), [`OPERATIONS_RUNBOOK.md`](OPERATIONS_RUNBOOK.md), `AllocatorControllerTest`, `ApiContractTest` |
| Container default | The checked-in Dockerfile defaults to `SPRING_PROFILES_ACTIVE=prod`. Operators must provide `LOADBALANCERPRO_API_KEY` at runtime for protected prod container use. Local/demo override is documented as loopback/private only. | [`CONTAINER_DEPLOYMENT.md`](CONTAINER_DEPLOYMENT.md), [`CONTAINER_SIGNING_DECISION_RECORD.md`](CONTAINER_SIGNING_DECISION_RECORD.md) |
| API abuse guardrails | App-level validation, request-size limits, safe error envelopes, and an optional process-local rate limiter protect the main demo/control surfaces. The limiter is disabled by default for local/demo convenience, can be enabled with `loadbalancerpro.api.rate-limit.enabled=true`, returns `429 rate_limited` with `Retry-After`, and covers allocation, routing, scenario replay, remediation, proxy, and LASE shadow surfaces. Distributed edge rate limiting is still required for shared or public deployments. | [`API_SECURITY.md`](API_SECURITY.md), [`OPERATIONS_RUNBOOK.md`](OPERATIONS_RUNBOOK.md), `ApiRateLimitFilterTest`, `ApiRateLimitIntegrationTest` |
| Adaptive-routing lab evidence | `POST /api/allocate/evaluate` exposes an optional `laseShadow` response summary when `loadbalancerpro.lase.shadow.enabled=true`. It lists signals considered, including tail latency, queue depth, and adaptive concurrency, records shadow-only observation evidence, and states that it does not alter live allocation. The adaptive-routing experiment harness compares baseline, shadow, and explicit opt-in influence outcomes under ignored local evidence; default runtime allocation behavior remains unchanged. | [`API_CONTRACTS.md`](API_CONTRACTS.md), [`DEMO_WALKTHROUGH.md`](DEMO_WALKTHROUGH.md), [`SRE_DEMO_HIGHLIGHTS.md`](SRE_DEMO_HIGHLIGHTS.md), `LaseAllocationShadowIntegrationTest` |
| Supply-chain evidence | CI covers tests, package, smoke, Docker runtime checks, Dependency Review, Trivy, and CycloneDX SBOM artifacts. CodeQL runs as a separate Java/Kotlin SAST workflow. Semantic-tag release workflow produces deterministic JAR/SBOM/checksum GitHub Release assets and GitHub artifact attestations. | [`../evidence/SUPPLY_CHAIN_EVIDENCE.md`](../evidence/SUPPLY_CHAIN_EVIDENCE.md), [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md), [`PRODUCTION_CANDIDATE_EVIDENCE_GATE.md`](PRODUCTION_CANDIDATE_EVIDENCE_GATE.md) |
| Dependency/SAST triage | CodeQL, Dependency Review, Trivy, SBOM, and dependency findings have an owner/rationale workflow with severity handling, accepted-risk and false-positive templates, remediation targets, and a high/critical no-silent-dismissal rule. | [`DEPENDENCY_SAST_RISK_WORKFLOW.md`](DEPENDENCY_SAST_RISK_WORKFLOW.md) |
| Release evidence gate | Production-candidate and release-ready labels have a checklist that separates automated checks from manual operator verification. The release-candidate dry-run packet records commit, build/test/package, SBOM, checksum, smoke, security gate, and publication-boundary evidence without publishing. The authorized `v2.5.0` JAR/docs-first release is now verified with exact tag, exact commit, workflow success, expected assets, checksum pass, SBOM JSON/XML presence, and artifact attestation status. | [`PRODUCTION_CANDIDATE_EVIDENCE_GATE.md`](PRODUCTION_CANDIDATE_EVIDENCE_GATE.md), [`RELEASE_CANDIDATE_DRY_RUN_PACKET.md`](RELEASE_CANDIDATE_DRY_RUN_PACKET.md), [`RELEASE_NOTES_v2.5.0.md`](RELEASE_NOTES_v2.5.0.md), [`V2_5_0_POST_RELEASE_VERIFICATION.md`](V2_5_0_POST_RELEASE_VERIFICATION.md), [`V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md`](V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md) |

## Current Validation Posture

Use the latest successful PR and `main` checks as the source of truth for a specific commit. Required reviewer evidence includes CI, CodeQL, Dependency Review, package, smoke, SBOM, and Trivy results for that commit. The two-track release decision is summarized in [`RELEASE_READINESS_DECISION_SUMMARY.md`](RELEASE_READINESS_DECISION_SUMMARY.md): `v2.5.0` is released as JAR/docs-first with verified assets, while container distribution remains deferred to [`CONTAINER_REGISTRY_SIGNING_ROLLOUT.md`](CONTAINER_REGISTRY_SIGNING_ROLLOUT.md).

This summary branch is docs/static-test only. Its local validation target is:

- Focused summary/docs tests pass.
- `mvn -q clean test` passes with 0 failures, 0 errors, and 0 skips; use Surefire reports for the exact test count for the commit under review.
- `mvn -q verify` passes.
- `mvn -q -DskipTests package` passes.
- Operator run-profile and Postman enterprise lab dry-runs pass without external network, cloud credentials, release actions, or `release-downloads/` mutation.
- `git diff --check` passes.

Future branches should update this summary or point to the latest PR report if the test count, release evidence, or evidence posture changes.

## What Is Not Production-Certified Or Container-Ready Yet

- No container image is published to a registry.
- No container signing, cosign signature, registry attestation, rollback policy, or retention policy is implemented.
- No PGP-style release artifact signing, notarization, Maven Central publication, or package-manager distribution exists.
- No production TLS, IAM, firewall, WAF, managed ingress, distributed rate limiting, monitoring, log retention, backup, incident response, or secret-rotation implementation is provided by the application.
- No real enterprise IdP tenant configuration, tenant IDs, client secrets, real JWTs, or browser login/session UX is included.
- No live AWS sandbox validation is part of the default Maven/CI evidence.
- No production SLO, benchmark, chaos validation, compliance certification, or legal chain-of-custody claim is made.
- `release-downloads/` remains manual and must not be mutated by normal docs, tests, smoke scripts, or PR review.

## Reviewer Go/No-Go

Use a controlled production-like demo or reviewer evaluation when:

- The exact commit has passing CI, CodeQL, Dependency Review, Trivy, package, smoke, and SBOM evidence.
- The production-candidate evidence gate is complete.
- Any non-fixed dependency or SAST finding has an owner decision under the risk workflow.
- Container use stays local/private or behind a trusted deployment edge, with `prod` profile and runtime API key configured.
- Local/default mode is not exposed on public interfaces.

The `v2.5.0` JAR/docs-first release evidence is produced and verified for the exact version being distributed. Do not call the container path release-ready until the container signing/publication decision record is completed and implemented in a separate approved change. Do not treat the JAR/docs-first release as production deployment certification.

## Evidence Index

- Reviewer navigation: [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md)
- Product charter: [`ENTERPRISE_LAB_PRODUCT_CHARTER.md`](ENTERPRISE_LAB_PRODUCT_CHARTER.md)
- Product roadmap: [`ENTERPRISE_LAB_ROADMAP.md`](ENTERPRISE_LAB_ROADMAP.md)
- Next goal prompts: [`NEXT_GOAL_PROMPTS.md`](NEXT_GOAL_PROMPTS.md)
- SRE demo highlights: [`SRE_DEMO_HIGHLIGHTS.md`](SRE_DEMO_HIGHLIGHTS.md)
- Adaptive-routing experiment evidence: run `scripts/smoke/adaptive-routing-experiment.ps1 -Package` and inspect ignored output under `target/adaptive-routing-experiments/`; it compares baseline vs shadow vs opt-in influence while keeping default behavior unchanged.
- Security posture: [`../evidence/SECURITY_POSTURE.md`](../evidence/SECURITY_POSTURE.md)
- Supply-chain posture: [`../evidence/SUPPLY_CHAIN_EVIDENCE.md`](../evidence/SUPPLY_CHAIN_EVIDENCE.md)
- Test posture: [`../evidence/TEST_EVIDENCE.md`](../evidence/TEST_EVIDENCE.md)
- Residual risks: [`../evidence/RESIDUAL_RISKS.md`](../evidence/RESIDUAL_RISKS.md)
- Deployment hardening: [`DEPLOYMENT_HARDENING_GUIDE.md`](DEPLOYMENT_HARDENING_GUIDE.md)
- Release-candidate dry-run packet: [`RELEASE_CANDIDATE_DRY_RUN_PACKET.md`](RELEASE_CANDIDATE_DRY_RUN_PACKET.md)
- Release-intent review: [`RELEASE_INTENT_REVIEW.md`](RELEASE_INTENT_REVIEW.md)
- v2.5.0 release notes: [`RELEASE_NOTES_v2.5.0.md`](RELEASE_NOTES_v2.5.0.md)
- v2.5.0 post-release verification: [`V2_5_0_POST_RELEASE_VERIFICATION.md`](V2_5_0_POST_RELEASE_VERIFICATION.md)
- v2.5.0 authorization checklist: [`V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md`](V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md)
- Release readiness decision summary: [`RELEASE_READINESS_DECISION_SUMMARY.md`](RELEASE_READINESS_DECISION_SUMMARY.md)
- Container signing decision: [`CONTAINER_SIGNING_DECISION_RECORD.md`](CONTAINER_SIGNING_DECISION_RECORD.md)
- Container registry/signing rollout: [`CONTAINER_REGISTRY_SIGNING_ROLLOUT.md`](CONTAINER_REGISTRY_SIGNING_ROLLOUT.md)
- IdP claim examples: [`IDP_CLAIM_MAPPING_EXAMPLES.md`](IDP_CLAIM_MAPPING_EXAMPLES.md)
- Dependency/SAST workflow: [`DEPENDENCY_SAST_RISK_WORKFLOW.md`](DEPENDENCY_SAST_RISK_WORKFLOW.md)
