# Demo Walkthrough

Use this outline for a 60 to 90 second local demo or short screen recording. It is a script, not a requirement to publish a video.

## Setup

```bash
mvn spring-boot:run
```

Open:

- `http://localhost:8080/`
- `http://localhost:8080/load-balancing-cockpit.html`
- `http://localhost:8080/proxy-status.html` when proxy status is part of the demo

Keep the demo local. Do not use cloud credentials, public upstreams, real secrets, registry publishing, tags, GitHub Releases, release assets, or `release-downloads/` evidence.

## 60 To 90 Second Talk Track

1. Start at the root landing page and say that LoadBalancerPro is a Java/Spring load-balancing simulator with an operator-focused lightweight proxy foundation.
2. Open the cockpit and run the packaged local scenarios. Point out routing comparisons, allocation pressure, overload/load-shedding signals, raw JSON, and copyable curl snippets.
3. Mention that `/proxy/**` is optional and disabled by default. Operators can configure local/private backend targets through application configuration.
4. Show [`OPERATOR_RUN_PROFILES.md`](OPERATOR_RUN_PROFILES.md) and [`DEPLOYMENT_SMOKE_KIT.md`](DEPLOYMENT_SMOKE_KIT.md) as the copyable validation path for packaged jar, API-key boundary, and proxy-loopback checks.
5. If security posture matters, open [`API_SECURITY.md`](API_SECURITY.md) and explain that prod/cloud-sandbox API-key modes protect proxy/status surfaces, while TLS termination remains a deployment responsibility.
6. If adaptive-routing posture matters, run `POST /api/allocate/evaluate` with `loadbalancerpro.lase.shadow.enabled=true` and point to the response `laseShadow` block. It is shadow-only, lists signals considered such as tail latency, queue depth, and adaptive concurrency, and explicitly says it does not alter live allocation.
7. If observability matters, open `/proxy-status.html` or [`PROXY_OPERATOR_STATUS_UI.md`](PROXY_OPERATOR_STATUS_UI.md) and point to route/backend counts, status summaries, retry/cooldown counters, and reload status.
8. If packaging matters, open [`CONTAINER_DEPLOYMENT.md`](CONTAINER_DEPLOYMENT.md) and explain the local-only Docker build/run path with no registry publish.
9. Close with [`SRE_DEMO_HIGHLIGHTS.md`](SRE_DEMO_HIGHLIGHTS.md), [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md), `jacoco-coverage-report`, `packaged-artifact-smoke`, and `loadbalancerpro-sbom` as the evidence trail.

## Optional Command Clips

Packaged-jar smoke:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\operator-run-profiles-smoke.ps1 -Package -BackendBPort 18183
```

Dry-run smoke:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\operator-run-profiles-smoke.ps1 -DryRun
```

Postman safe smoke dry-run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\postman-enterprise-lab-safe-smoke.ps1 -DryRun
```

Local proxy evidence export:

```bash
mvn -Dtest=LocalProxyEvidenceExportTest test
```

Output: `target/proxy-evidence/local-proxy-evidence.md` and `target/proxy-evidence/local-proxy-evidence.json`.

Private-network validation dry-run evidence:

```bash
mvn -Dtest=PrivateNetworkProxyDryRunEvidenceTest test
```

Output: `target/proxy-evidence/private-network-validation-dry-run.md` and `target/proxy-evidence/private-network-validation-dry-run.json`.

Private-network live loopback proof:

```bash
mvn -Dtest=PrivateNetworkLiveValidationExecutorTest test
```

Output: `target/proxy-evidence/private-network-live-loopback-validation.md` and `target/proxy-evidence/private-network-live-loopback-validation.json`.

Container build, local only:

```bash
docker build -t loadbalancerpro:local .
docker run --rm --name loadbalancerpro-demo -p 127.0.0.1:8080:8080 -e LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY loadbalancerpro:local
```

The Dockerfile defaults to the prod API-key profile. Health and the root page remain reachable for local review, while `/api/**`, OpenAPI, Swagger, and proxy routes require `X-API-Key` except for `GET /api/health` and unauthenticated `OPTIONS` preflight requests. Use `-e SPRING_PROFILES_ACTIVE=local` only for an explicitly loopback-bound local/demo container.

## Limitations To Say Out Loud

- The project is not a managed gateway, compliance proof, certification outcome, or benchmark result.
- No fixed coverage percentage is claimed in docs; inspect a generated JaCoCo report or CI log for exact run-specific numbers.
- Proxy mode is optional and disabled by default.
- Private-network validation is config-only plus dry-run evidence plus a default-off live gate with JUnit-only loopback proof; runtime private-LAN live traffic execution is not implemented yet.
- Generated proxy evidence is ignored `target/` output and should not contain API keys, bearer tokens, credentials, or secrets.
- Safe proof paths do not use DNS resolution, discovery, subnet scanning, port scanning, native tooling, release assets, or `release-downloads/` mutation.
- TLS termination and public ingress controls belong to the deployment boundary.
- Demo paths do not construct or mutate `CloudManager`, do not mutate cloud state, and do not publish release assets.
