# Measured Performance Baseline + Enterprise Auth Proof Lane

Status: local evidence lane ready.

This lane connects two existing Enterprise Lab proof paths into one reviewer story: repeatable local performance baseline evidence and local enterprise-style auth proof.

LoadBalancerPro is not trying to out-Envoy Envoy. It is becoming the evidence, governance, and explainability layer for adaptive routing: a local flight simulator and black-box recorder for routing decisions.

## What This Lane Proves

This lane proves that a reviewer can generate local, repeatable, source-visible evidence for:

- loopback latency and error-rate summaries across deterministic Enterprise Lab fixtures;
- warning-only performance thresholds that help flag local regressions without becoming production gates;
- OAuth2 role-claim behavior using synthetic mock IdP/JWKS fixtures;
- dedicated role claims granting operator access where configured;
- scope-only, missing-role, ambiguous-role, expired, wrong-issuer, and wrong-audience cases failing closed.

The proof is intentionally local. It uses checked-in fixtures, Maven/JUnit tests, PowerShell smoke scripts, and ignored `target/` evidence output.

## Reviewer Sequence

Dry-run the performance baseline plan:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\performance-baseline.ps1 -DryRun
```

Generate the local packaged-JAR performance baseline:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\performance-baseline.ps1 -Package
```

Dry-run the auth proof plan:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\enterprise-auth-proof.ps1 -DryRun
```

Generate the local enterprise auth proof evidence:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\enterprise-auth-proof.ps1 -Package
```

Use the focused tests when reviewing the lane without generating fresh smoke evidence:

```powershell
mvn -q -Dtest=PerformanceBaselineFixtureCatalogTest,EnterpriseAuthProofLaneTest,OAuth2AuthorizationTest,PerformanceAuthProofLaneDocumentationTest test
```

## Evidence Outputs

The performance runner writes ignored evidence under `target/performance-baseline/`:

- `performance-report.json`
- `performance-dashboard.json`
- `performance-threshold-results.json`
- `performance-summary.md`
- `performance-evidence-manifest.json`

The auth proof runner writes ignored evidence under `target/enterprise-auth-proof/`:

- `enterprise-auth-proof-results.json`
- `mock-idp-jwks-fixture-summary.json`
- `enterprise-auth-proof-summary.md`
- `enterprise-auth-proof-manifest.json`

These files are generated evidence. They should not be committed unless a future sprint explicitly adds static templates instead of run output.

## Existing Source Of Truth

- [`../evidence/PERFORMANCE_BASELINE.md`](../evidence/PERFORMANCE_BASELINE.md) defines the measured local loopback performance baseline.
- [`ENTERPRISE_AUTH_PROOF_LANE.md`](ENTERPRISE_AUTH_PROOF_LANE.md) defines the local/test-backed auth proof lane.
- [`performance-fixtures.json`](performance/performance-fixtures.json) lists deterministic local request fixtures.
- [`performance-thresholds.example.json`](performance/performance-thresholds.example.json) defines warning-only threshold examples.
- `src/test/resources/auth-proof/mock-idp-claims.json` contains synthetic mock IdP/JWKS claim examples without private key material.
- [`CI_EVIDENCE_GATE_READINESS_LANE.md`](CI_EVIDENCE_GATE_READINESS_LANE.md), [`CI_EVIDENCE_GATE_ARTIFACT_CONTRACT.md`](CI_EVIDENCE_GATE_ARTIFACT_CONTRACT.md), [`examples/ci-evidence-gate-summary.template.json`](examples/ci-evidence-gate-summary.template.json), `/ci-evidence-gate.html`, and `GET /api/enterprise-lab/ci-evidence-gate-summary` explain and prototype how these local evidence outputs could become inputs to a future CI evidence gate without enforcing it today.

## Safety Boundaries

This lane does not use real secrets, tokens, tenant IDs, customer data, private endpoints, cloud resources, private-network validation, external benchmark services, GitHub settings, releases, tags, registry publication, or container signing. No real secrets are required or recorded.

The performance baseline starts the packaged app on `127.0.0.1` and records local loopback evidence. The auth proof lane uses mocked-resource-server tests and synthetic fixtures; it does not start or call a real IdP.

Boundary sentence: this is not production certification, not live cloud proof, not real tenant proof, and not SLO/SLA proof. It provides no real enterprise IdP tenant validation.

## What This Does Not Prove

Do not claim:

- production certification;
- production performance;
- production SLO or SLA proof;
- live-cloud validation;
- real-tenant validation;
- real enterprise IdP validation;
- signed-container proof;
- registry publish completion;
- governance-applied proof;
- cloud capacity or real user traffic capacity;
- production SSO certification.

This lane is useful because it is narrow. It helps reviewers inspect local behavior, compare local evidence across commits, and separate implemented proof from future deployment claims.
