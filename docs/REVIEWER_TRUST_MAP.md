# Reviewer Trust Map

This map directs reviewers to current executable evidence. Documentation explains what to inspect; passing prose checks
does not establish runtime behavior, security, or readiness.

## Evidence Map

| Reviewer question | Executable evidence | Source or configuration | Test or workflow | Material limitation |
| --- | --- | --- | --- | --- |
| Are protected APIs deny-by-default? | Startup and request authorization behavior | `ApiSecurityConfig`, `AuthModeConfiguration`, `application.properties` | `CheckedInSecurityDefaultsTest`, `AuthModeConfigurationTest`, `OAuth2AuthorizationTest`, CI | The `local` profile disables authentication and is loopback-only. |
| Are secrets and TLS handled safely? | API-key/JWT validation, PEM loading, redacted output, TLS-enabled Compose smoke | `api/config`, `deploy/docker-compose.proxy-prod.yml`, `application-proxy-prod.properties` | security tests; proxy-prod Compose smoke in CI | Deployment identity lifecycle, rotation, and ingress policy remain operator responsibilities. |
| Can proxy targets escape network policy? | Syntax-only planning plus literal-address validation before use | `ProxyBackendUrlClassifier`, `ReverseProxyRoutePlanner`, `ProxyDnsDiscovery` | classifier, routing, reload, DNS discovery, and private-network validation tests | Public targets are possible only when the operator disables private-network validation. |
| Are requests, retries, and concurrency bounded? | Body/time limits, admission caps, retry budgets, queue bounds, cooldown, and shutdown behavior | `ReverseProxyService` and proxy configuration | proxy unit/integration suites; benchmark smoke | Process-local limits do not prove distributed capacity or production SLOs. |
| Does DNS discovery fail closed? | Bounded asynchronous lookup, last-known-good expiry, stable per-address members | `ProxyDnsDiscoveryRuntime`, `ProxyDnsEffectiveConfig` | DNS configuration/runtime/routing/health tests | HTTP address authority only; no logical-host TLS identity or authoritative TTL proof. |
| Is durable lab state protected? | Locked append/replay, integrity checks, atomic replacement, reconciliation | `ChainedJsonlStore` and Enterprise Lab stores/reconcilers | durable-state, corruption, concurrency, and recovery tests | Local durable evidence is not multi-region durability or tenant validation. |
| Can lab or explanation output shift production traffic? | Read-only/simulation services are separated from proxy and cloud mutation paths | Decision Explorer, LASE shadow, Enterprise Lab controllers/services | controller/service/security tests | Review output and recommendations are not production authority. |
| Is cloud mutation guarded? | Dry-run default plus explicit live-mode/account/region/capacity gates | `CloudManager` and cloud configuration | CloudManager and security-boundary tests | Default tests use mocks; live-cloud operation is not established. |
| Is the artifact reproducible and inspectable? | Executable JAR, required/forbidden entry checks, SBOM | `pom.xml`, `Dockerfile`, resolver and artifact scripts | package/artifact jobs in CI | Reproducible build identity and signed publication are separate release concerns. |
| Is the container hardened? | Pinned bases, non-root user, read-only root, dropped capabilities, secret mounts | `Dockerfile`, proxy-prod Compose | runtime and Compose smoke; Trivy scans | Host, orchestrator, ingress, and network policy remain deployment-specific. |
| Are dependencies and source risks gated? | Dependency tree, dependency review, CodeQL, SBOM, image scans | `pom.xml`, pinned workflow actions | CI, CodeQL, Dependabot, Trivy | A green scan is evidence for one exact commit, not future vulnerability absence. |
| Is performance evidence reproducible? | Loopback scenario harness and explicit diagnostic access-log lane | `scripts/bench`, benchmark workflow | benchmark smoke and access-log benchmark artifacts | Local/hosted regression budgets do not establish production capacity or p95/p99 guarantees. |

## Review Entry Points

- Runtime and API contracts: [`API_CONTRACTS.md`](API_CONTRACTS.md), [`API_SECURITY.md`](API_SECURITY.md), and
  [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md).
- Deployment controls: [`DEPLOYMENT_HARDENING_GUIDE.md`](DEPLOYMENT_HARDENING_GUIDE.md),
  [`CONTAINER_DEPLOYMENT.md`](CONTAINER_DEPLOYMENT.md), and [`OPERATIONS_RUNBOOK.md`](OPERATIONS_RUNBOOK.md).
- Architecture decisions: [`adr/`](adr/), including the reviewer evidence decision in
  [`ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md`](adr/ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md).
- Active implementation campaign state: [`agent/COMBINED_BUILD_PLAN_CAMPAIGN_BOARD.md`](agent/COMBINED_BUILD_PLAN_CAMPAIGN_BOARD.md)
  and [`agent/COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json`](agent/COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json). These are
  bookkeeping, not trust evidence.
- Exact commit evidence: the pull request, GitHub Actions, CodeQL, dependency review, generated test reports, SBOMs,
  image scans, and ignored local benchmark output.

## Evidence Interpretation

Observed runtime/test results, generated artifacts, and workflow conclusions should retain their commit identity and
provenance. Inferred or synthetic conclusions must be labeled as such. Human or operator authority remains required
where configuration, deployment, cloud mutation, or production traffic is involved. Repository evidence supports
review; it does not certify an environment that was not exercised.
