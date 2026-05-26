# Evidence Audit CodeQL And Dependency Review Audit

This note is slot 5 of the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation/test-only. It audits the current CodeQL workflow and dependency-review posture without changing workflow behavior, Maven configuration, Dockerfile contents, Compose behavior, scripts, runtime resources, endpoints, app behavior, runner services, automation, secrets, external targets, or production behavior.

## Audit Timestamp

- Audit timestamp: 2026-05-25T01:41-07:00.
- Audited repository: `RicheyWorks/LoadBalancerPro`.
- Audited base branch: `main`.
- Slot 5 branch: `codex/evidence-audit-codeql-dependency-review`.
- Starting main HEAD: `bc62bef7fb5843e2ab143a47a65f81dd6fc46f8f`.
- Audited CodeQL workflow source: [`.github/workflows/codeql.yml`](../../.github/workflows/codeql.yml).
- Audited dependency-review source: [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml).

## Purpose

This audit gives reviewers a source-readable summary of static-analysis and pull-request dependency-review posture. It records what the workflows are configured to do and what reviewers may not infer from green CodeQL or Dependency Review checks. It is not a workflow behavior change, not production hardening, not vulnerability management by itself, not release approval, not production certification, and not a replacement for manual review.

## CodeQL Trigger Posture

The `CodeQL` workflow currently runs on:

- `push` to `main`;
- `pull_request` targeting `main`;
- a scheduled scan with cron `0 12 * * 1`;
- `workflow_dispatch`.

This means CodeQL has PR, main-branch, weekly scheduled, and manual trigger coverage. A successful run is evidence for the exact commit and workflow run; it is not proof that all future commits, branches, dependencies, runtime configurations, containers, deployments, or production environments are free of security issues.

## CodeQL Permissions And Scope

The workflow declares top-level permissions:

- `actions: read`;
- `contents: read`;
- `security-events: write`.

The analyze job:

- is named `Analyze Java`;
- runs on `ubuntu-latest`;
- has a `30` minute timeout;
- is scoped to `github.repository == 'RicheyWorks/LoadBalancerPro'`;
- uses a matrix language of `java-kotlin`;
- uses pinned checkout and setup-java actions, with `actions/setup-java` pinned to `c1e323688fd81a25caa38c78aa6df2d33d3e20d9`;
- sets up Temurin Java `17` with Maven cache.

The `security-events: write` permission is necessary for publishing CodeQL analysis results. The workflow does not grant release, package publishing, registry, environment mutation, secret-writing, deployment, or production-operation permissions.

## CodeQL Manual Build Mode

The workflow initializes CodeQL with:

- `github/codeql-action/init`;
- language `${{ matrix.language }}`;
- `build-mode: manual`.

The workflow then builds with `mvn -B -DskipTests package` before `github/codeql-action/analyze`. Manual build mode gives CodeQL a built Java/Kotlin target without running the test suite inside CodeQL. Tests still run in the CI workflow, not inside this CodeQL job.

## Dependency Review Posture

Dependency Review is configured in the `CI` workflow as a separate job. It:

- runs only when `github.event_name == 'pull_request'`;
- is scoped to the canonical `RicheyWorks/LoadBalancerPro` repository;
- uses job permissions `contents: read` and `pull-requests: read`;
- checks out the repository;
- uses `actions/dependency-review-action` pinned to `56339e523c0409420f6c2c9a2f4292bbb3c07dd3`;
- uses `fail-on-severity: high`;
- fails on high severity dependency review findings.

Dependency Review is pull-request-change evidence. It is not a full dependency lifecycle program, not a replacement for CodeQL, not a replacement for Trivy, not a replacement for Maven dependency posture review, and not proof that unchanged transitive dependencies are risk-free forever.

## Relationship To CI And Trivy

CodeQL and Dependency Review are complementary to the slot 4 [CI workflow audit](EVIDENCE_AUDIT_CI_WORKFLOW_AUDIT.md):

- CI runs tests, packaging, artifact smoke checks, SBOM generation, Docker build/runtime smoke, and Trivy scanning.
- CodeQL performs source/static-analysis scanning for Java/Kotlin with manual build mode.
- Dependency Review reviews pull-request dependency changes.
- Trivy scans the CI Docker image for configured HIGH and CRITICAL OS/library vulnerabilities.

No single lane replaces the others. A green CI, CodeQL, Dependency Review, or Trivy result is bounded to its workflow configuration, exact commit, runner state, and scan-time inputs.

## Remaining Limits

Reviewers should keep these limits attached to any CodeQL or Dependency Review pass:

- `ubuntu-latest` is not a fully pinned runner image.
- The CodeQL workflow is scoped to the canonical repository and does not run for every possible fork context.
- Manual build mode skips tests in the CodeQL job; CI owns test execution.
- `mvn -B -DskipTests package` resolves the current Maven graph and does not prove dependency freshness.
- Static analysis can miss vulnerabilities, design risks, runtime configuration risks, or environment-specific risks.
- Dependency Review focuses on dependency changes in a pull request and does not prove all existing dependencies are safe.
- Scheduled CodeQL scans are periodic, not continuous runtime monitoring.
- Green checks do not publish, sign, release, deploy, or certify artifacts.
- Green checks do not prove production telemetry, production monitoring, incident response, or vulnerability remediation SLAs.
- Green checks do not replace human review of claims, scope, workflow changes, dependency changes, or security findings.

## Reviewer Questions

- Did the PR preserve CodeQL workflow behavior?
- Did the PR preserve dependency-review behavior?
- Are any required checks pending, failed, cancelled, stale, or duplicate-only?
- Are CodeQL and Dependency Review green for the current head SHA?
- Did the PR avoid Maven config changes, CI workflow changes, Dockerfile changes, Compose changes, runtime behavior changes, scripts, endpoints, secrets, external targets, and automation?
- Does the PR avoid claiming production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, registry publication, container signing, or broader automation?

## Not-Proven Boundaries

This CodeQL and Dependency Review audit does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, registry publication, container signing, production telemetry, production monitoring, release approval, full vulnerability management, incident response readiness, remediation SLA compliance, or broader automation.
