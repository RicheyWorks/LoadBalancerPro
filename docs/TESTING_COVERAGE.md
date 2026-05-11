# Testing Coverage

LoadBalancerPro now generates JaCoCo coverage output during CI so reviewers can inspect coverage numbers directly instead of relying on prose claims.

## How To Run Locally

```bash
mvn -B test
mvn -B jacoco:report
```

Or run the full verification lifecycle:

```bash
mvn -B verify
```

The report output is:

- HTML report: `target/site/jacoco/index.html`
- XML report: `target/site/jacoco/jacoco.xml`
- CSV report: `target/site/jacoco/jacoco.csv`

On the current workstation used for recent Codex runs, Maven dependency resolution may fail before tests execute because the local Java trust store cannot validate Maven Central certificates. When that happens, GitHub CI is the source of truth for test, package, smoke, and coverage generation.

## CI Coverage Evidence

The `Build, Test, Package, Smoke` workflow generates JaCoCo coverage after `mvn -B test` and uploads the report directory as a GitHub Actions artifact:

```text
jacoco-coverage-report
```

Reviewers can inspect:

- `target/site/jacoco/index.html` inside the artifact for package/class coverage.
- `target/site/jacoco/jacoco.xml` for machine-readable coverage.
- `target/site/jacoco/jacoco.csv` for line, branch, and instruction totals.
- The CI log step `Summarize JaCoCo coverage` for computed instruction, branch, and line percentages from `jacoco.csv`.

The project intentionally does not publish a coverage badge yet. A badge should only be added once there is a stable public reporting source or artifact-backed status that cannot drift from the generated report.

## CI Packaged Artifact Smoke Evidence

The same workflow verifies the packaged Spring Boot jar after `mvn -B package` and uploads the release-free inspection output as a GitHub Actions artifact:

```text
packaged-artifact-smoke
```

The artifact contains `artifact-smoke-summary.txt`, `artifact-sha256.txt`, and `jar-resource-list.txt`. The lane checks the jar manifest, `/proxy-status.html`, `/load-balancing-cockpit.html`, proxy demo profiles, and `ProxyDemoFixtureLauncher.class`. It is workflow artifact evidence only; it does not create tags, GitHub releases, release assets, or `release-downloads/` files.

For a single reviewer workflow covering JaCoCo download, packaged-artifact-smoke inspection, SBOM artifact handling, local-vs-CI SHA-256 comparison, and a release-free candidate checklist, see [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md).

For the release-free go/no-go packet that records CI status, CodeQL status, skipped-test count, coverage review, packaged smoke evidence, SBOM review, and operator demo checks together, see [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md).

## Skipped-Test Evidence

CI parses `target/surefire-reports/*.xml` after the test step and prints:

- total Surefire tests reported
- total Surefire skipped tests reported

The CI step fails when the skipped count is nonzero. At the time this document was added, the test tree did not contain intentional `@Disabled` or JUnit assumption-based skips. If skipped tests are introduced later, they should be documented explicitly before relaxing the zero-skipped check.

## Coverage Map

| Area | Representative coverage | Notes |
| --- | --- | --- |
| Core load-balancer behavior | `LoadBalancerTest`, `LoadBalancerCloudMetricsTest`, routing strategy tests | Covers simulator and strategy behavior, not a production reverse proxy data plane. |
| Routing comparison API | `RoutingControllerTest`, `RoutingDecisionDemoTest`, `RoutingOpenApiContractTest` | Uses caller-provided synthetic telemetry and verifies deterministic strategy output. |
| Allocation and load-shedding API | `AllocatorControllerTest`, `ApiContractTest`, `LoadBalancingCockpitDemoTest` | Covers capacity-aware allocation, evaluation, and advisory remediation responses. |
| Cloud safety guardrails | `CloudManagerGuardrailTest`, `CloudManagerSafetyTest`, cloud sandbox API tests | Uses mocked AWS SDK clients and dry-run defaults; does not mutate live AWS resources. |
| Security and API hardening | prod auth/profile tests, request-size tests, OpenAPI/security tests | Covers local API behavior and mock auth paths, not full deployment identity-provider operations. |
| CLI and evidence workflows | CLI tests under `src/test/java/com/richmond423/loadbalancerpro/cli` | Covers offline deterministic file/console workflows and no-cloud construction boundaries. |
| Browser/Postman operator demos | browser static tests, Postman collection tests, cockpit tests, `ProxyOperatorStatusPageTest` | Static and API contract coverage for local demo parity and read-only proxy status UI safety; not browser automation or visual regression testing. |
| Lightweight reverse proxy mode | `ReverseProxyDisabledTest`, `ReverseProxyControllerTest`, `ReverseProxyHealthAwareTest`, `ReverseProxyHealthMetricsTest`, `ReverseProxyFailureTest`, `ReverseProxyRetrySafetyTest`, `ReverseProxyRetryCooldownTest`, `ReverseProxyStrategyDemoLabTest`, `ProxyOperatorStatusPageTest`, `ProxyDemoStackUsabilityTest`, `ProxyDemoFixtureLauncherTest`, `OperatorPackagingPolishTest`, `OperatorDistributionSmokeKitTest`, `LocalArtifactVerificationTest`, `CiPackagedArtifactSmokeWorkflowTest` | Uses in-process loopback upstream fixtures and static safety checks to prove real local HTTP forwarding, query/body preservation, configured and active health-aware skipping, selected-upstream and strategy headers for `ROUND_ROBIN`, `WEIGHTED_ROUND_ROBIN`, and health-aware failover demos, read-only status output, forwarding/failure/retry/cooldown/status-class counters, bounded GET retry behavior, non-idempotent no-retry defaults, cooldown skip/recovery, upstream failure handling, static status UI safety, checked-in demo profile/script/docs safety, classpath availability of packaged static resources and demo profiles, Maven exec launcher docs, real-backend example profile safety, release-free distribution smoke docs/scripts, local artifact verification docs/scripts, CI packaged artifact smoke workflow safety, package naming documentation, JavaFX optional note, Java fixture launcher parsing/output/health/echo behavior, default proxy disabled behavior, and no `CloudManager` construction. |

## Mocked And Simulated Boundaries

Default CI deliberately avoids real cloud mutation and live external services. Cloud-adjacent behavior is covered with mocks, dry-run guards, synthetic scenario payloads, same-origin local API requests, and local reverse-proxy fixture servers. This is useful regression evidence for the current simulator, lightweight forwarding path, and safety boundaries, but it does not prove:

- live AWS IAM behavior
- production network policy
- production reverse proxy throughput
- production-grade active health, retry, cooldown, or observability behavior
- public internet traffic balancing
- live autoscaling side effects
- end-to-end identity-provider behavior

## Current Limitations

- No coverage percentage is claimed in docs unless it comes from a generated JaCoCo report or CI log.
- No coverage gate is enforced yet because the initial goal is reviewer visibility, not gaming a threshold.
- Generated coverage reports are not committed to the repository.
- Default tests now exercise a lightweight local reverse proxy path against loopback fixture servers, including strategy-specific selected-upstream behavior, active health checks, bounded retry/cooldown behavior, local in-memory metrics/status output, a read-only static proxy status UI, Java fixture launcher behavior, Maven exec/package docs, real-backend example profile safety, release-free distribution smoke docs/scripts, local artifact verification docs/scripts, CI packaged artifact smoke workflow safety, namespace decision docs, and static safety checks for demo profiles/scripts/docs, but not a production gateway data plane.

## Next Credibility Step

The highest-value next testing move is richer real-backend proxy examples and long-running soak-style checks that stay outside the default CI hot path.
