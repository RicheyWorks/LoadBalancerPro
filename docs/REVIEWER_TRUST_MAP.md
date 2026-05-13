# Reviewer Trust Map

Use this map when you want to review LoadBalancerPro evidence without opening every operator document first. It points each reviewer question to the smallest useful evidence path.

## Start Here

LoadBalancerPro is a Java/Spring load-balancing simulator and API project with guarded cloud boundaries, optional static browser review pages, optional lightweight reverse proxy mode, operator-configured route/target examples, packaged local proxy demos, release-free artifact checks, and CI-published workflow evidence.

What it is not claiming:

- It is not a managed cloud load balancer.
- It is not a production gateway, TLS terminator, WebSocket proxy, WAF, identity system, legal compliance proof, or certification outcome.
- It does not treat short local demo sequences as performance benchmark evidence.
- Release-free review docs do not create tags, GitHub Releases, or release assets.
- `release-downloads/` remains manual and explicit only.
- Proxy, demo, artifact, and docs review paths do not construct or mutate `CloudManager`.

Recommended first paths:

- I want the shortest public-facing overview: start with [`EXECUTIVE_SUMMARY.md`](EXECUTIVE_SUMMARY.md), then use [`DEMO_WALKTHROUGH.md`](DEMO_WALKTHROUGH.md) for a local demo script.
- I want to verify tests and coverage: start with [`TESTING_COVERAGE.md`](TESTING_COVERAGE.md), then inspect the `jacoco-coverage-report` workflow artifact and CI skipped-test log output.
- I want to verify real HTTP proxy behavior: start with [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md), [`REVERSE_PROXY_HEALTH_AND_METRICS.md`](REVERSE_PROXY_HEALTH_AND_METRICS.md), [`REVERSE_PROXY_RESILIENCE.md`](REVERSE_PROXY_RESILIENCE.md), [`PROXY_OPERATOR_STATUS_UI.md`](PROXY_OPERATOR_STATUS_UI.md), the source-visible `LocalOnlyRealBackendProxyValidationTest`, and the reviewer export from `LocalProxyEvidenceExportTest` under `target/proxy-evidence/local-proxy-evidence.md`.
- I want to run local proxy demos: start with [`PROXY_DEMO_STACK.md`](PROXY_DEMO_STACK.md), [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md), and [`PROXY_STRATEGY_DEMO_LAB.md`](PROXY_STRATEGY_DEMO_LAB.md).
- I want to adapt proxy mode to local/private backends: start with [`REAL_BACKEND_PROXY_EXAMPLES.md`](REAL_BACKEND_PROXY_EXAMPLES.md).
- I want to choose the right run profile: start with [`OPERATOR_RUN_PROFILES.md`](OPERATOR_RUN_PROFILES.md) for local demo, packaged jar, prod API-key, cloud-sandbox API-key, OAuth2, proxy-loopback, and container recipes.
- I want to build and run the app in a local container: start with [`CONTAINER_DEPLOYMENT.md`](CONTAINER_DEPLOYMENT.md) for Docker build/run recipes, API-key checks, proxy-loopback caveats, and no-registry-publish boundaries.
- I want one local smoke path for the packaged jar, API-key boundary, and proxy-loopback recipe: start with [`DEPLOYMENT_SMOKE_KIT.md`](DEPLOYMENT_SMOKE_KIT.md).
- I want to verify the proxy auth/TLS boundary: start with [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md#auth-and-tls-boundary), then check [`API_SECURITY.md`](API_SECURITY.md) and [`OPERATIONS_RUNBOOK.md`](OPERATIONS_RUNBOOK.md).
- I want to verify antivirus-safe tooling and live/proxy containment: start with [`ANTIVIRUS_SAFE_DEVELOPMENT.md`](ANTIVIRUS_SAFE_DEVELOPMENT.md) and [`LIVE_PROXY_CONTAINMENT.md`](LIVE_PROXY_CONTAINMENT.md).
- I want to review the future private-network proxy profile and current config-only safety gate: start with [`PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md`](PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md), run the dry-run evidence recipe in [`PRIVATE_NETWORK_PROXY_DRY_RUN.md`](PRIVATE_NETWORK_PROXY_DRY_RUN.md), then inspect the future live gate in [`PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md`](PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md).
- I want to review the CSRF disposition: start with [`API_SECURITY.md`](API_SECURITY.md#csrf-disposition), then check [`DEPLOYMENT_HARDENING_GUIDE.md`](DEPLOYMENT_HARDENING_GUIDE.md).
- I want to inspect CI artifacts: start with [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md), then review `jacoco-coverage-report`, `packaged-artifact-smoke`, and `loadbalancerpro-sbom`.
- I want to evaluate release readiness without releasing: start with [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md) and [`RELEASE_INTENT_CHECKLIST.md`](RELEASE_INTENT_CHECKLIST.md).
- I want install/run commands: start with [`OPERATOR_INSTALL_RUN_MATRIX.md`](OPERATOR_INSTALL_RUN_MATRIX.md) and [`OPERATOR_PACKAGING.md`](OPERATOR_PACKAGING.md).
- I want to understand JavaFX: start with [`JAVAFX_OPTIONAL_UI.md`](JAVAFX_OPTIONAL_UI.md). JavaFX is optional.
- I want to understand the repository/package naming split: start with [`PACKAGE_NAMING.md`](PACKAGE_NAMING.md).

## Reviewer Demo Path

Use this path when a reviewer wants to understand the project and verify the current evidence story in about five minutes without enabling live private-network behavior.

What this project is: LoadBalancerPro is a Java/Spring load-balancing simulator and operator-focused proxy foundation with guarded cloud boundaries, local browser demos, source-visible proxy evidence, API-key/OAuth2 deployment boundaries, and CI-published testing artifacts.

What can be proven quickly:

1. Local cockpit behavior: run `mvn spring-boot:run`, then open `http://localhost:8080/` and `http://localhost:8080/load-balancing-cockpit.html`.
2. Local proxy forwarding evidence: run `mvn -Dtest=LocalProxyEvidenceExportTest test`, then inspect `target/proxy-evidence/local-proxy-evidence.md` and `target/proxy-evidence/local-proxy-evidence.json`.
3. Private-network profile dry-run evidence: run `mvn -Dtest=PrivateNetworkProxyDryRunEvidenceTest test`, then inspect `target/proxy-evidence/private-network-validation-dry-run.md` and `target/proxy-evidence/private-network-validation-dry-run.json`.
4. Operator run-profile dry-run: run `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\operator-run-profiles-smoke.ps1 -DryRun`.
5. Postman safe smoke dry-run: run `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\postman-enterprise-lab-safe-smoke.ps1 -DryRun`.
6. CI evidence: inspect the latest successful CI, CodeQL, Dependency Review, `jacoco-coverage-report`, `packaged-artifact-smoke`, and `loadbalancerpro-sbom` artifacts.

Safety boundaries preserved by this path:

- generated evidence is ignored `target/` output and is not tracked documentation;
- local proxy evidence uses loopback/local-only JDK `HttpServer` fixtures;
- private-network evidence is config-validation-only and sends no traffic;
- API keys, bearer tokens, credentials, and secrets are redacted or not written;
- no DNS resolution, reachability checks, discovery, subnet scanning, or port scanning are used;
- no native tooling, downloaded helper binaries, service installation, scheduled tasks, persistence, release assets, or `release-downloads/` mutation are introduced;
- live private-network validation is not implemented yet and remains blocked on [`PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md`](PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md).

## Evidence Matrix

| Reviewer question | Evidence source | Primary doc | Runtime path or artifact | What it proves | What it does not prove |
| --- | --- | --- | --- | --- | --- |
| Are tests and skipped-test checks visible? | CI Surefire parsing and coverage docs | [`TESTING_COVERAGE.md`](TESTING_COVERAGE.md) | CI logs, `target/surefire-reports`, `jacoco-coverage-report` | CI reports zero skipped tests and publishes coverage output for inspection | Complete behavioral proof for every deployment condition |
| Is JaCoCo coverage available? | GitHub Actions workflow artifact | [`TESTING_COVERAGE.md`](TESTING_COVERAGE.md) | `jacoco-coverage-report` | Reviewers can inspect HTML/XML/CSV coverage output | A coverage threshold or quality guarantee by itself |
| Does proxy forwarding have a documented contract? | Reverse proxy docs, loopback tests, and ignored evidence export | [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md), `LocalOnlyRealBackendProxyValidationTest`, `LocalProxyEvidenceExportTest` | `/proxy/**`, `X-LoadBalancerPro-Upstream`, `X-LoadBalancerPro-Strategy`, `target/proxy-evidence/local-proxy-evidence.md` | Optional proxy mode forwards real local HTTP traffic when explicitly enabled, including source-visible JUnit evidence with JDK loopback backends on Java-assigned ephemeral ports and redacted local evidence export | A managed gateway or internet-edge deployment claim |
| Are antivirus-safe development and live/proxy containment documented? | Static policy docs and guard tests | [`ANTIVIRUS_SAFE_DEVELOPMENT.md`](ANTIVIRUS_SAFE_DEVELOPMENT.md), [`LIVE_PROXY_CONTAINMENT.md`](LIVE_PROXY_CONTAINMENT.md) | Java source, Maven tests, Spring Boot JARs, PowerShell scripts, Postman JSON, Markdown docs, Dockerfile/docs, GitHub Actions | Future tooling and proxy validation have safe default artifact types, avoided native/binary patterns, opt-in live mode, localhost/private-network defaults, and no persistence/port-scanning/service-install behavior | Approval to add native binaries, public scanning, hidden agents, service installation, or production exposure |
| Is the future private-network proxy profile bounded before live execution? | Design plan, config-only validation gate, dry-run evidence, live gate design, and static guard tests | [`PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md`](PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md), [`PRIVATE_NETWORK_PROXY_DRY_RUN.md`](PRIVATE_NETWORK_PROXY_DRY_RUN.md), [`PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md`](PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md) | Explicit operator-provided backend URLs, local/private-network allowlist model, `PrivateNetworkProxyDryRunEvidenceTest`, `target/proxy-evidence/private-network-validation-dry-run.md`, `target/proxy-evidence/private-network-validation-dry-run.json` | The future profile path has opt-in startup/reload URL validation, dry-run-only classifier evidence, future explicit operator approval gates, and forbids scanning, discovery, persistence, service installation, scheduled tasks, native tooling, secret persistence, public internet validation, and `release-downloads/` mutation before any live execution | Private-network live execution, public exposure approval, or resolver policy approval |
| Are proxy health, metrics, and reload status visible? | Status endpoint and operator page | [`REVERSE_PROXY_HEALTH_AND_METRICS.md`](REVERSE_PROXY_HEALTH_AND_METRICS.md), [`PROXY_OPERATOR_STATUS_UI.md`](PROXY_OPERATOR_STATUS_UI.md) | `/api/proxy/status`, `/proxy-status.html` | Health, counters, selected upstream, route/backend summary, boundary mode, reload generation/status, and status JSON can be reviewed read-only | Durable monitoring, alerting, distributed config, or reset/admin controls |
| Is the proxy auth/TLS boundary explicit? | Security config, docs, and boundary tests | [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md#auth-and-tls-boundary), [`API_SECURITY.md`](API_SECURITY.md) | `/proxy/**`, `/api/proxy/status`, `/proxy-status.html` | Prod API-key mode and OAuth2 mode have documented access behavior, while TLS termination is a deployment responsibility | A complete identity system, TLS implementation, or public exposure approval |
| Is the CSRF posture explicit? | Security config and disposition docs | [`API_SECURITY.md`](API_SECURITY.md#csrf-disposition), [`DEPLOYMENT_HARDENING_GUIDE.md`](DEPLOYMENT_HARDENING_GUIDE.md) | `/api/**`, `/proxy/**`, `/actuator/**` | API/proxy paths are documented as stateless header-auth surfaces, with form login, HTTP Basic, logout, and stateful sessions disabled | Approval to add cookie/session auth without re-review |
| Are retries and cooldowns documented? | Resilience docs and counters | [`REVERSE_PROXY_RESILIENCE.md`](REVERSE_PROXY_RESILIENCE.md) | `/api/proxy/status`, retry/cooldown counters | Optional bounded retry and process-local cooldown behavior is explainable and visible | Infinite retry safety or distributed cooldown state |
| Is there an operator status page? | Static browser UI docs | [`PROXY_OPERATOR_STATUS_UI.md`](PROXY_OPERATOR_STATUS_UI.md) | `/proxy-status.html` | Operators can manually refresh and inspect read-only proxy status | A full monitoring console |
| Are strategy demos available? | Demo lab and fixture stack | [`PROXY_STRATEGY_DEMO_LAB.md`](PROXY_STRATEGY_DEMO_LAB.md) | `ROUND_ROBIN`, `WEIGHTED_ROUND_ROBIN`, failover profiles | Reviewers can observe selected-upstream and strategy headers through local HTTP traffic | Throughput, capacity, or latency evidence |
| Can examples target non-fixture local services? | Copy/adapt example profiles | [`REAL_BACKEND_PROXY_EXAMPLES.md`](REAL_BACKEND_PROXY_EXAMPLES.md) | `docs/examples/proxy/*.properties` | Operators can adapt loopback/private backend examples and named route targets safely | Proof against public upstreams or external services |
| Which run mode should an operator use? | Profile matrix and copyable recipes | [`OPERATOR_RUN_PROFILES.md`](OPERATOR_RUN_PROFILES.md) | Local demo, packaged jar, prod API-key, cloud-sandbox API-key, OAuth2, proxy-loopback, container | Operators can choose and verify supported run profiles without changing defaults | Deployment automation, release publication, or production readiness |
| Can the documented run profiles be smoked locally? | Local-only smoke script and guide | [`DEPLOYMENT_SMOKE_KIT.md`](DEPLOYMENT_SMOKE_KIT.md) | `scripts/smoke/operator-run-profiles-smoke.ps1`, packaged jar, `/api/proxy/status`, `/proxy/api/smoke` | Packaged jar startup, API-key boundary, and proxy-loopback forwarding can be checked on localhost | Certification, benchmark evidence, release publication, or public exposure readiness |
| Is there a Java fixture launcher? | Packaged demo launcher docs | [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md) | `ProxyDemoFixtureLauncher` | Two loopback fixture backends can be launched from project Java code | A change to default app startup behavior |
| Where are operator commands? | Side-by-side command matrix | [`OPERATOR_INSTALL_RUN_MATRIX.md`](OPERATOR_INSTALL_RUN_MATRIX.md) | Packaged jar, Maven exec, Spring profiles, scripts | Reviewers can choose Windows/Unix install/run paths | Release publication |
| Can local artifacts be inspected? | Local checksum and jar inspection guide | [`LOCAL_ARTIFACT_VERIFICATION.md`](LOCAL_ARTIFACT_VERIFICATION.md) | `jar tf`, SHA-256, static resources, demo profiles | A locally built jar can be inspected without publishing | Signed release provenance |
| Does CI inspect the packaged jar? | CI smoke artifact | [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md) | `packaged-artifact-smoke` | CI verifies required jar resources/classes and emits checksum evidence | A GitHub Release asset |
| Is SBOM evidence available? | CI SBOM workflow artifact | [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md) | `loadbalancerpro-sbom`, `bom.json`, `bom.xml` | SBOM files are generated by CI and downloadable as workflow artifacts | A signed release SBOM or vulnerability-free guarantee |
| Can a release candidate be rehearsed without publishing? | Dry-run checklist | [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md) | Reviewer packet template, go/no-go table | CI artifacts, local verification, SBOM, jar smoke, status UI, and demos map into one review packet | Any tag, GitHub Release, or asset upload |
| Is there a hard stop before future release action? | Release-intent checklist | [`RELEASE_INTENT_CHECKLIST.md`](RELEASE_INTENT_CHECKLIST.md) | Intent questions and explicit approval gates | Reviewers see what must be true before a separate real release request | A release process by itself |
| Is JavaFX required? | Optional JavaFX doc | [`JAVAFX_OPTIONAL_UI.md`](JAVAFX_OPTIONAL_UI.md) | Desktop UI launch guidance | JavaFX is optional and API/proxy/static browser paths do not require it | Server/headless desktop support across all platforms |
| Why does the Java namespace differ from the GitHub org? | Package naming note | [`PACKAGE_NAMING.md`](PACKAGE_NAMING.md) | `RicheyWorks/LoadBalancerPro`, `com.richmond423.loadbalancerpro` | The package name is a stable legacy namespace decision for now | A functional defect or runtime behavior change |

## Recommended Reviewer Flows

### 10-Minute Quick Review

1. Read this trust map.
2. Open [`TESTING_COVERAGE.md`](TESTING_COVERAGE.md) for zero skipped tests and JaCoCo artifact expectations.
3. Open [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md) for `jacoco-coverage-report`, `packaged-artifact-smoke`, and `loadbalancerpro-sbom`.
4. Open [`PROXY_OPERATOR_STATUS_UI.md`](PROXY_OPERATOR_STATUS_UI.md) for the read-only proxy status page summary.
5. Open [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md) for the release-free go/no-go packet.

### Proxy-Focused Review

1. Read [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md) for the forwarding contract.
2. Read [`REVERSE_PROXY_HEALTH_AND_METRICS.md`](REVERSE_PROXY_HEALTH_AND_METRICS.md) and [`REVERSE_PROXY_RESILIENCE.md`](REVERSE_PROXY_RESILIENCE.md) for active health, counters, retries, and cooldowns.
3. Run [`PROXY_DEMO_STACK.md`](PROXY_DEMO_STACK.md) or [`PROXY_STRATEGY_DEMO_LAB.md`](PROXY_STRATEGY_DEMO_LAB.md) for loopback selected-upstream evidence.
4. Use the focused [Local Proxy Evidence Export](#local-proxy-evidence-export) recipe when you want ignored Markdown/JSON evidence under `target/proxy-evidence/`.
5. Read [`PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md`](PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md) before designing any private-network validation beyond loopback.
6. Use the focused [Private-Network Validation Dry Run](#private-network-validation-dry-run) recipe when you want config-only classifier evidence without traffic.
7. Read [`PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md`](PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md) for the future live approval gates before any private-network traffic is implemented.
8. Use [`REAL_BACKEND_PROXY_EXAMPLES.md`](REAL_BACKEND_PROXY_EXAMPLES.md) to adapt the same verification model to local/private HTTP services.
9. Read the auth/TLS boundary in [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md#auth-and-tls-boundary) before exposing proxy mode beyond loopback.
10. Verify status through `/proxy-status.html` and `/api/proxy/status`.

### Local Proxy Evidence Export

Run `mvn -Dtest=LocalProxyEvidenceExportTest test`, then inspect `target/proxy-evidence/local-proxy-evidence.md` and `target/proxy-evidence/local-proxy-evidence.json`. The Markdown file is the human review path: it shows the loopback/local-only JDK `HttpServer` fixture, the `/proxy/**` request, backend receipt, forwarded status/body/header proof, and prod API-key `401`/`200` boundary. The JSON file is the structured evidence path: it records the same proof labels, `loopbackOnly=true`, `apiKeyRedacted="<REDACTED>"`, and no native tools/downloads/scanning/persistence/service installation. The files are generated under ignored `target/` output, are not tracked docs, do not write API keys or secrets, and do not add external network behavior.

### Private-Network Validation Dry Run

Run `mvn -Dtest=PrivateNetworkProxyDryRunEvidenceTest test`, then inspect `target/proxy-evidence/private-network-validation-dry-run.md` and `target/proxy-evidence/private-network-validation-dry-run.json`. The Markdown file is the human review path: it shows the opt-in `loadbalancerpro.proxy.private-network-validation.enabled=true` gate, loopback/local-only and private literal URL samples, rejected public/domain/user-info/unsupported/malformed examples, and fail-closed-before-active-config expectations. The JSON file is the structured evidence path: it records the same `LOOPBACK_ALLOWED`, `PRIVATE_NETWORK_ALLOWED`, `PUBLIC_NETWORK_REJECTED`, `AMBIGUOUS_HOST_REJECTED`, `USERINFO_REJECTED`, `UNSUPPORTED_SCHEME_REJECTED`, and `INVALID_REJECTED` labels with `dryRunOnly=true`, `trafficSent=false`, `dnsResolution=false`, `reachabilityChecks=false`, `portScanning=false`, `postmanExecution=false`, `smokeExecution=false`, `apiKeyPersisted=false`, and `secretPersisted=false`. The files are ignored `target/` output, are not tracked docs, do not write API keys or secrets, and do not add private-network live execution.

### Private-Network Live Validation Gate

Read [`PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md`](PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md) before proposing any private-network live validation. The gate states that live validation is not implemented yet and requires a separate approved task, future default-off live flags, explicit operator approval, operator-provided literal backend URLs, `ProxyBackendUrlClassifier` approval before activation, bounded timeout behavior, redacted ignored `target/` evidence, API-key/OAuth2 boundary proof, no DNS, no discovery, no scanning, no persistence, no service installation, no scheduled tasks, no native tooling, no secret persistence, and fail-closed startup/reload behavior.

### Release-Readiness Review

1. Select one successful `main` CI run.
2. Use [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md) to download `jacoco-coverage-report`, `packaged-artifact-smoke`, and `loadbalancerpro-sbom`.
3. Use [`LOCAL_ARTIFACT_VERIFICATION.md`](LOCAL_ARTIFACT_VERIFICATION.md) if local Maven can build and inspect the jar.
4. Fill out [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md) and the review packet template.
5. Stop at [`RELEASE_INTENT_CHECKLIST.md`](RELEASE_INTENT_CHECKLIST.md) unless a future prompt explicitly approves a real release action.

### Operator Install/Run Review

1. Start with [`OPERATOR_INSTALL_RUN_MATRIX.md`](OPERATOR_INSTALL_RUN_MATRIX.md).
2. Use [`OPERATOR_PACKAGING.md`](OPERATOR_PACKAGING.md) for packaged jar and Maven exec details.
3. Use [`OPERATOR_DISTRIBUTION_SMOKE_KIT.md`](OPERATOR_DISTRIBUTION_SMOKE_KIT.md) for release-free smoke helpers.
4. Use [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md) for Java fixture launcher options.
5. Use [`JAVAFX_OPTIONAL_UI.md`](JAVAFX_OPTIONAL_UI.md) only if the optional desktop UI is part of your review.

### Docs/Trust Review

1. Confirm this trust map links to the current evidence docs.
2. Confirm [`PACKAGE_NAMING.md`](PACKAGE_NAMING.md) explains the `RicheyWorks/LoadBalancerPro` and `com.richmond423.loadbalancerpro` naming relationship.
3. Confirm release-free docs separate workflow artifacts from GitHub Release assets.
4. Confirm no review flow asks for tags, release assets, default branch changes, ruleset changes, or `release-downloads/` changes.

## Safety Boundaries

- Proxy is disabled by default.
- JavaFX is optional and not required for API, proxy, static browser, artifact, or operator smoke paths.
- Release-free docs do not create tags, GitHub Releases, or release assets.
- `release-downloads/` remains manual and explicit only.
- Development tooling avoids native executable wrappers, installers, packers, self-extracting archives, and vendored third-party binaries unless explicitly approved.
- Live/proxy validation is opt-in, defaults to localhost or private-network backends, and does not use port scanning, persistence mechanisms, scheduled tasks, service installation, credential storage, or hidden background agents.
- Local real-backend proxy validation uses source-visible Maven/JUnit and JDK loopback fixtures, not native helper tools or downloaded servers.
- Local proxy evidence export writes redacted reviewer Markdown/JSON only under ignored `target/proxy-evidence/`.
- Future private-network proxy validation must follow [`PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md`](PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md), [`PRIVATE_NETWORK_PROXY_DRY_RUN.md`](PRIVATE_NETWORK_PROXY_DRY_RUN.md), and [`PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md`](PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md): explicit operator-provided backend URLs only, local/private-network allowlisting, offline `ProxyBackendUrlClassifier` review, opt-in startup/reload configuration validation, dry-run-only evidence under ignored `target/`, explicit operator approval before live traffic, no DNS or reachability checks, no discovery or scanning, no secret persistence, and no private-network live execution until separately approved.
- The Postman smoke harness is source-visible PowerShell and remains dry-run safe unless `-Package` is explicitly passed.
- Workflow artifacts are not GitHub Release assets.
- Proxy/demo/status/docs paths do not construct or mutate `CloudManager`.
- Real-backend examples use loopback/private placeholders and must not include secrets or public upstream URLs.
- Local/default proxy demos are not a security boundary; prod API-key and OAuth2 modes document app-level proxy access checks, while TLS termination and public ingress controls remain deployment responsibilities.
- No production gateway claim, performance benchmark claim, certification claim, legal compliance claim, identity claim, or security guarantee is made by the proxy/operator docs.

## Current Limitations

- Local Maven on the current workstation can be blocked by a PKIX trust-chain issue; GitHub CI is the source of truth when that happens.
- The proxy is intentionally lightweight and optional.
- TLS termination, WebSocket proxying, WAF behavior, distributed rate limiting, durable monitoring, and deployment identity controls are outside the documented proxy evidence.
- CI artifacts are workflow artifacts for review; they are not release assets.
- JavaFX desktop behavior can be platform-sensitive and may not be available in headless/server environments.
