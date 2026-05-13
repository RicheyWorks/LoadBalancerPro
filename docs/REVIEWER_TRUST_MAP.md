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
- I want to verify real HTTP proxy behavior: start with [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md), [`REVERSE_PROXY_HEALTH_AND_METRICS.md`](REVERSE_PROXY_HEALTH_AND_METRICS.md), [`REVERSE_PROXY_RESILIENCE.md`](REVERSE_PROXY_RESILIENCE.md), [`PROXY_OPERATOR_STATUS_UI.md`](PROXY_OPERATOR_STATUS_UI.md), and the source-visible `LocalOnlyRealBackendProxyValidationTest`.
- I want to run local proxy demos: start with [`PROXY_DEMO_STACK.md`](PROXY_DEMO_STACK.md), [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md), and [`PROXY_STRATEGY_DEMO_LAB.md`](PROXY_STRATEGY_DEMO_LAB.md).
- I want to adapt proxy mode to local/private backends: start with [`REAL_BACKEND_PROXY_EXAMPLES.md`](REAL_BACKEND_PROXY_EXAMPLES.md).
- I want to choose the right run profile: start with [`OPERATOR_RUN_PROFILES.md`](OPERATOR_RUN_PROFILES.md) for local demo, packaged jar, prod API-key, cloud-sandbox API-key, OAuth2, proxy-loopback, and container recipes.
- I want to build and run the app in a local container: start with [`CONTAINER_DEPLOYMENT.md`](CONTAINER_DEPLOYMENT.md) for Docker build/run recipes, API-key checks, proxy-loopback caveats, and no-registry-publish boundaries.
- I want one local smoke path for the packaged jar, API-key boundary, and proxy-loopback recipe: start with [`DEPLOYMENT_SMOKE_KIT.md`](DEPLOYMENT_SMOKE_KIT.md).
- I want to verify the proxy auth/TLS boundary: start with [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md#auth-and-tls-boundary), then check [`API_SECURITY.md`](API_SECURITY.md) and [`OPERATIONS_RUNBOOK.md`](OPERATIONS_RUNBOOK.md).
- I want to verify antivirus-safe tooling and live/proxy containment: start with [`ANTIVIRUS_SAFE_DEVELOPMENT.md`](ANTIVIRUS_SAFE_DEVELOPMENT.md) and [`LIVE_PROXY_CONTAINMENT.md`](LIVE_PROXY_CONTAINMENT.md).
- I want to review the CSRF disposition: start with [`API_SECURITY.md`](API_SECURITY.md#csrf-disposition), then check [`DEPLOYMENT_HARDENING_GUIDE.md`](DEPLOYMENT_HARDENING_GUIDE.md).
- I want to inspect CI artifacts: start with [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md), then review `jacoco-coverage-report`, `packaged-artifact-smoke`, and `loadbalancerpro-sbom`.
- I want to evaluate release readiness without releasing: start with [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md) and [`RELEASE_INTENT_CHECKLIST.md`](RELEASE_INTENT_CHECKLIST.md).
- I want install/run commands: start with [`OPERATOR_INSTALL_RUN_MATRIX.md`](OPERATOR_INSTALL_RUN_MATRIX.md) and [`OPERATOR_PACKAGING.md`](OPERATOR_PACKAGING.md).
- I want to understand JavaFX: start with [`JAVAFX_OPTIONAL_UI.md`](JAVAFX_OPTIONAL_UI.md). JavaFX is optional.
- I want to understand the repository/package naming split: start with [`PACKAGE_NAMING.md`](PACKAGE_NAMING.md).

## Evidence Matrix

| Reviewer question | Evidence source | Primary doc | Runtime path or artifact | What it proves | What it does not prove |
| --- | --- | --- | --- | --- | --- |
| Are tests and skipped-test checks visible? | CI Surefire parsing and coverage docs | [`TESTING_COVERAGE.md`](TESTING_COVERAGE.md) | CI logs, `target/surefire-reports`, `jacoco-coverage-report` | CI reports zero skipped tests and publishes coverage output for inspection | Complete behavioral proof for every deployment condition |
| Is JaCoCo coverage available? | GitHub Actions workflow artifact | [`TESTING_COVERAGE.md`](TESTING_COVERAGE.md) | `jacoco-coverage-report` | Reviewers can inspect HTML/XML/CSV coverage output | A coverage threshold or quality guarantee by itself |
| Does proxy forwarding have a documented contract? | Reverse proxy docs and loopback tests | [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md), `LocalOnlyRealBackendProxyValidationTest` | `/proxy/**`, `X-LoadBalancerPro-Upstream`, `X-LoadBalancerPro-Strategy` | Optional proxy mode forwards real local HTTP traffic when explicitly enabled, including source-visible JUnit evidence with JDK loopback backends on Java-assigned ephemeral ports | A managed gateway or internet-edge deployment claim |
| Are antivirus-safe development and live/proxy containment documented? | Static policy docs and guard tests | [`ANTIVIRUS_SAFE_DEVELOPMENT.md`](ANTIVIRUS_SAFE_DEVELOPMENT.md), [`LIVE_PROXY_CONTAINMENT.md`](LIVE_PROXY_CONTAINMENT.md) | Java source, Maven tests, Spring Boot JARs, PowerShell scripts, Postman JSON, Markdown docs, Dockerfile/docs, GitHub Actions | Future tooling and proxy validation have safe default artifact types, avoided native/binary patterns, opt-in live mode, localhost/private-network defaults, and no persistence/port-scanning/service-install behavior | Approval to add native binaries, public scanning, hidden agents, service installation, or production exposure |
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
4. Use [`REAL_BACKEND_PROXY_EXAMPLES.md`](REAL_BACKEND_PROXY_EXAMPLES.md) to adapt the same verification model to local/private HTTP services.
5. Read the auth/TLS boundary in [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md#auth-and-tls-boundary) before exposing proxy mode beyond loopback.
6. Verify status through `/proxy-status.html` and `/api/proxy/status`.

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
