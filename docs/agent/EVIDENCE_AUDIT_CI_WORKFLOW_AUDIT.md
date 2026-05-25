# Evidence Audit CI Workflow Audit

This note is slot 4 of the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation/test-only. It audits the current `.github/workflows/ci.yml` source without changing workflow behavior, Maven configuration, Dockerfile contents, Compose behavior, scripts, runtime resources, endpoints, app behavior, runner services, automation, secrets, external targets, or production behavior.

## Audit Timestamp

- Audit timestamp: 2026-05-25T01:11-07:00.
- Audited repository: `RicheyWorks/LoadBalancerPro`.
- Audited base branch: `main`.
- Slot 4 branch: `codex/evidence-audit-ci-workflow`.
- Starting main HEAD: `65fad4a65f0297ba6e7d085bd84cacf5aa966f38`.
- Audited workflow source: [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml).

## Purpose

This audit gives reviewers a single source-readable summary of the CI workflow posture. It records what the workflow is configured to do and what reviewers may not infer from a green run. It is not a CI behavior change, not production hardening, not release approval, not registry publication, not container signing, and not production certification.

## Trigger And Permissions Posture

- The workflow is named `CI`.
- It runs on `push` and `pull_request`.
- The top-level permission is `contents: read`.
- The build job is named `Build, Test, Package, Smoke`, runs on `ubuntu-latest`, and has a `25` minute timeout.
- The dependency-review job runs only on pull requests in `RicheyWorks/LoadBalancerPro` and uses job permissions `contents: read` and `pull-requests: read`.
- The workflow does not define write permissions, release permissions, package publishing permissions, registry login, or secret-writing behavior.

## Pinned Action Posture

The workflow pins major actions to explicit commit SHAs in comments and `uses:` references:

- `actions/checkout` is pinned to `de0fac2e4500dabe0009e67214ff5f5447ce83dd`.
- `actions/setup-java` is pinned to `be666c2fcd27ec809703dec50e508c2fdc7f6654`.
- `actions/upload-artifact` is pinned to `043fb46d1a93c77aae656e7c1c64a875d1fc6a0a`.
- `aquasecurity/trivy-action` is pinned to `ed142fd0673e97e23eac54620cfb913e5ce36c25`.
- `actions/dependency-review-action` is pinned to `a1d282b36b6f3519aa1f3fc636f609c47dddb294`.

The workflow also invokes the CycloneDX Maven plugin by version `2.9.1`. This is Maven plugin execution inside CI, not a GitHub Action pin.

## Build, Test, Package, Smoke Lane

The primary CI lane currently performs the following source-visible steps:

1. Checks out the repository.
2. Sets up Temurin Java `17` with Maven cache.
3. Resolves the dependency tree with `mvn -B -DskipTests dependency:tree`.
4. Runs tests with `mvn -B test`.
5. Verifies zero skipped tests by parsing Surefire XML reports and failing if skipped tests are non-zero.
6. Generates a JaCoCo coverage report with `mvn -B jacoco:report`.
7. Summarizes JaCoCo instruction, branch, and line coverage from `target/site/jacoco/jacoco.csv`.
8. Uploads the `jacoco-coverage-report` artifact with `retention-days: 30`.
9. Packages the executable JAR with `mvn -B package`.
10. Verifies packaged artifact resources, including the manifest, static pages, demo profiles, and `ProxyDemoFixtureLauncher.class`.
11. Uploads the `packaged-artifact-smoke` artifact with `retention-days: 30`.
12. Generates CycloneDX SBOM output in JSON and XML through `org.cyclonedx:cyclonedx-maven-plugin:2.9.1`.
13. Uploads the `loadbalancerpro-sbom` artifact with `retention-days: 30`.
14. Smoke tests the LASE demo command for `healthy`, `overloaded`, and invalid scenario handling while checking that CLI mode does not start Spring.
15. Smoke tests the packaged JAR on `127.0.0.1:18080` against `/api/health`.
16. Builds a local Docker image tagged `loadbalancerpro:ci`.
17. Smoke tests Docker runtime with a loopback binding `127.0.0.1:18081:8080` and Docker health status checks.
18. Captures container dry-run evidence under `target/container-dry-run-evidence`.
19. Scans the CI Docker image with Trivy for HIGH and CRITICAL OS/library vulnerabilities, using `.trivyignore`, `ignore-unfixed: true`, and `exit-code: '1'`.
20. Uploads `container-dry-run-evidence-no-publish-no-sign` when evidence files exist.

## Artifact And Evidence Boundary

CI uploads workflow artifacts for reviewer inspection:

- `jacoco-coverage-report`.
- `packaged-artifact-smoke`.
- `loadbalancerpro-sbom`.
- `container-dry-run-evidence-no-publish-no-sign`.

These artifacts are workflow artifacts only. They are not GitHub Release assets, registry publications, signed containers, deployment artifacts, production telemetry, production monitoring proof, or production certification evidence.

## Dependency Review Boundary

The dependency-review job:

- runs on pull requests only;
- is scoped to the canonical `RicheyWorks/LoadBalancerPro` repository;
- checks out the repository;
- uses `actions/dependency-review-action`;
- fails on high severity dependency review findings.

Dependency Review is not a replacement for CodeQL, Trivy, Maven dependency posture review, manual dependency lifecycle review, or production vulnerability management.

## Remaining Limits

Reviewers should keep these limits attached to any CI pass:

- `ubuntu-latest` is not a fully pinned runner image.
- Maven plugin resolution still depends on configured repositories and the current dependency graph.
- CI Docker build/runtime smoke is local to the GitHub runner and does not publish, sign, deploy, or run in production.
- Loopback JAR and Docker smoke checks prove only the checked workflow path for that run.
- JaCoCo output reports test coverage, not behavioral completeness or production readiness.
- SBOM output lists dependency metadata; it is not legal, licensing, vulnerability, or supply-chain certification by itself.
- Trivy results are scan-time findings with the configured ignore and severity settings; they are not a guarantee of zero vulnerabilities.
- Dependency Review covers pull-request dependency changes; it does not prove all future dependency posture.
- CI pass/fail status is evidence for that exact commit and workflow run only.

## Not-Proven Boundaries

This CI workflow audit does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, registry publication, container signing, production telemetry, production monitoring, release approval, or broader automation.
