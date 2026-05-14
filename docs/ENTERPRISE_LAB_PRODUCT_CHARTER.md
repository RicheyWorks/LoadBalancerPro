# LoadBalancerPro Enterprise Lab Product Charter

## One-Sentence Identity

LoadBalancerPro Enterprise Lab is a source-visible Java/Spring lab for proving adaptive-routing ideas, safety guardrails, release evidence, and operator workflows before any production gateway path is promoted.

## Product Name

LoadBalancerPro Enterprise Lab.

## What It Is

- An Enterprise Adaptive Routing Lab for controlled scenario running, deterministic replay, LASE `off`/`shadow`/`recommend`/`active-experiment` comparison, implemented policy gates, scorecards, process-local lab metrics, evidence export, and SRE walkthroughs.
- A Production Gateway Candidate for optional future runtime proxy/gateway work, hardened auth, config reload, production-grade metrics, rate limiting, canary or shadow mode, rollback, deployment guides, and signed container distribution later.
- A reviewer-friendly evidence system: release notes, post-release verification, SBOM/checksum/attestation posture, static documentation guardrails, and ignored local evidence outputs under `target/`.
- A product-development track that keeps lab proof, runtime safety, and future deployment responsibilities separate.

## What It Is Not

- It is not production deployment certification.
- It is not a live enterprise gateway.
- It is not a managed cloud load balancer, WAF, TLS terminator, identity provider, compliance proof, benchmark result, or replacement for provider-native load balancing.
- It is not a container distribution product today; registry publication and container signing remain deferred gates.
- It is not a live private-network or cloud validation system by default.

## Primary Users

| User | What they need |
| --- | --- |
| Reviewers | A short path from product identity to evidence, limitations, and reproducible local proof. |
| SRE/interview evaluators | Guardrail depth, deterministic routing scenarios, failure-mode explanations, and a clear demo story. |
| Operators evaluating adaptive-routing concepts | Scenario fixtures, replay, shadow/recommendation evidence, scorecards, and safe dry-run outputs. |
| Future production-gateway implementers | A roadmap for hardening auth, config reload, metrics, rate limiting, canary/shadow behavior, rollback, deployment, and container distribution. |

## Track 1: Enterprise Adaptive Routing Lab

Track 1 is the primary product identity.

The lab should grow around:

- controlled scenario runner surfaces;
- deterministic replay and repeated-event ordering;
- LASE shadow-only, recommend, and active-experiment comparison;
- controlled policy gates before any active decision promotion;
- scorecards that explain baseline, shadow, recommendation, and active-experiment outcomes;
- protected lab-grade metrics for lab runs, scenarios, policy decisions, guardrails, rollback/fail-closed events, audit retention, and rate-limit interactions;
- evidence export under ignored `target/` paths;
- SRE walkthroughs that show what changed, what did not change, and why.

The first implemented lab workflow now provides `GET /api/lab/scenarios`, `POST /api/lab/runs`, bounded process-local in-memory run storage, deterministic run summaries, scorecards, protected `GET /api/lab/metrics` and `GET /api/lab/metrics/prometheus`, ignored `target/enterprise-lab-runs/` and `target/enterprise-lab-observability/` evidence export through source-visible smoke scripts, and `/enterprise-lab.html` for browser review. This is lab evidence only; it is not production traffic activation or production SLO certification.

Lab evidence can support product review and design decisions. It must not be presented as proof that an unmanaged production deployment is safe.

## Track 2: Production Gateway Candidate

Track 2 is secondary and optional until explicitly approved.

The candidate path should grow only through reviewable gates:

- optional proxy/runtime path with protected prod/cloud-sandbox auth boundaries;
- config reload with last-known-good behavior;
- metrics and status surfaces that do not expose sensitive state by default;
- process-local rate limiting now, distributed/edge rate limiting later;
- canary, shadow, and rollback controls before active routing promotion;
- deployment hardening guides for TLS, IAM, ingress, monitoring, and incident response;
- signed and scanned container distribution later, after a separate registry/signing rollout.

Production Gateway Candidate means "candidate architecture and evidence lane." It does not mean live gateway certification.

## Boundary Between Lab Evidence And Production Deployment

| Lab evidence can show | It does not prove |
| --- | --- |
| Deterministic scenario behavior | Capacity for real traffic. |
| Shadow/recommendation/active-experiment explanations | Approval to steer unmanaged production traffic. |
| Local proxy loopback forwarding | Internet-edge gateway safety. |
| API-key and OAuth2 route tests | Complete enterprise IAM deployment. |
| SBOM/checksum/attestation release evidence | Vulnerability-free or signed-container distribution. |
| Dry-run cloud guardrails | Real account IAM, budget, teardown, or live-cloud safety. |
| Process-local lab metrics and SLO templates | Production SLO certification, centralized monitoring, or scaling limits. |
| Local performance templates | Production SLOs or scaling limits. |

## Release Posture

- `v2.5.0` is the verified JAR/docs-first release.
- Release commit: `4cc03750be5479d9f8f88f8ef8014e05a8dc587a`.
- Release assets verified: JAR, CycloneDX SBOM JSON/XML, and SHA-256 sums.
- GitHub artifact attestation status is documented in the post-release verification note.
- current status: container publication and container signing are deferred.

## Safety Posture

- Local/default mode is intentionally convenient for source-checkout demos and should remain loopback/private.
- Container/default startup uses the protected `prod` profile and requires operator-provided API-key configuration for protected routes.
- No live private-network or cloud validation runs by default.
- Cloud mutation stays behind dry-run defaults, explicit operator intent, prefix/ownership/account/region/capacity guardrails, and separate live approval.
- Adaptive-routing influence over runtime allocation remains controlled: default behavior is unchanged, `shadow` and `recommend` are non-mutating, and `active-experiment` is explicit, bounded, guarded, audited, and lab/evaluation-grade rather than production deployment certification.

## Current Limitations

- No production deployment certification.
- No real enterprise IdP tenant proof, tenant IDs, client secrets, or browser login/session workflow.
- No production TLS, IAM, ingress, WAF, monitoring, log retention, backup, or incident-response implementation.
- No production SLO, benchmark, chaos validation, compliance certification, or legal chain-of-custody proof.
- No published or signed container image.
- No default live AWS or private-network validation.

## Next Product Milestones

1. Truth and identity alignment: keep this charter, roadmap, and evidence scorecards consistent.
2. Adaptive Routing Lab workflow hardening: richer scenario scorecards, reviewer exports, and lab-page polish on top of the first `/api/lab/**` slice.
3. Controlled active LASE policy gate follow-through: richer audit review, policy dashboards, and rollback evidence on top of implemented off, shadow, recommend, and active-experiment modes.
4. Observability packs follow-through: keep the Grafana JSON, alert examples, SLO templates, protected lab metrics endpoints, and `target/enterprise-lab-observability/` evidence aligned as scenarios and policy gates grow.
5. Measured performance baseline: stable fixtures and source-visible scripts under ignored `target/performance-baseline/`.
6. Enterprise auth proof lane: mock IdP/JWKS fixture mode, role lifecycle examples, and local proof without real tenant secrets.
7. Container distribution readiness: registry decision, immutable tag/digest policy, Trivy evidence, signing plan, rollback, retention, and credential-handling gates.
8. Disposable live sandbox lab: explicit AWS sandbox plan, IAM templates, budget guardrails, teardown, and no default CI live calls.
