# LoadBalancerPro v1.6.0 CodeQL SAST Plan

Date: 2026-05-03

## A. Current Release State

- `v1.5.0` is shipped and pushed.
- Remote release branch hash: `55dfcd36d89580b99bb5f9c8e04f3f1fa94d6d22`.
- Remote `v1.5.0` annotated tag object: `4c317938bffe044603a78ced1568d4e8091102fe`.
- Remote `v1.5.0` tag target: `55dfcd36d89580b99bb5f9c8e04f3f1fa94d6d22`.
- CI-generated CycloneDX SBOM artifacts now exist through the main CI workflow.
- Governance basics exist:
  - `LICENSE`
  - `SECURITY.md`
  - `CONTRIBUTING.md`
  - `.github/CODEOWNERS`
  - `.github/dependabot.yml`
- Existing tags must remain immutable.
- Public `main` remains untouched.

## B. Current Security And Supply-Chain Baseline

Confirmed controls:

- Existing CI runs Maven dependency tree resolution.
- Existing CI runs Maven tests.
- Existing CI packages the executable Spring Boot JAR.
- Existing CI runs deterministic LASE demo smoke checks.
- Existing CI runs packaged JAR health smoke checks.
- Existing CI builds the Docker image.
- Existing CI runs Docker runtime health checks.
- Existing CI runs Trivy image scanning for fixed high/critical OS and library vulnerabilities.
- Pull requests run GitHub dependency review and fail on high-severity dependency findings.
- Dependabot is configured for Maven, GitHub Actions, and Docker.
- GitHub Actions in `.github/workflows/ci.yml` are pinned to reviewed commit SHAs, with comments preserving upstream action names and versions.
- Docker build and runtime base images are pinned by digest.
- CI generates CycloneDX SBOM files with `org.cyclonedx:cyclonedx-maven-plugin:2.9.1`.
- CI uploads `target/bom.json` and `target/bom.xml` as the `loadbalancerpro-sbom` workflow artifact.
- `SECURITY.md` exists and defines supported versions, reporting guidance, security scope, and out-of-scope cases.
- `.github/CODEOWNERS` exists and assigns ownership for API, routing, CloudManager, workflows, Dockerfile, `pom.xml`, evidence, and docs.

## C. Current Gap

- No CodeQL or equivalent SAST workflow exists yet.
- `.github/workflows/codeql.yml` is absent.
- No formal static-analysis triage process is documented yet.
- No artifact attestations or signing workflows exist yet.
- Artifact attestations, release artifact signing, container signing, deployment, Kubernetes, Terraform, live AWS, and operations docs are out of scope for this slice.

## D. Proposed CodeQL Workflow

Add a separate workflow:

```text
.github/workflows/codeql.yml
```

Recommended triggers:

```yaml
on:
  push:
    branches: [loadbalancerpro-clean]
  pull_request:
    branches: [loadbalancerpro-clean]
  schedule:
    - cron: "0 12 * * 1"
```

Recommended language:

```yaml
languages: [java-kotlin]
```

Recommended build mode:

- Prefer manual build for the first implementation:
  - `mvn -B -DskipTests package`
- Rationale:
  - This is a Maven/Spring Boot project with a known package path.
  - The existing CI already proves the Maven lifecycle and packaging command.
  - Manual build is more explicit and reviewable than relying on CodeQL autobuild.
  - Skipping tests keeps the SAST workflow focused and avoids duplicating the full CI test job.
- Evaluate CodeQL autobuild only if manual build causes CodeQL setup problems that are specific to this repository.

Recommended permissions:

```yaml
permissions:
  actions: read
  contents: read
  security-events: write
```

Add `packages: read` only if a future workflow uses package registry dependencies that require it.

Pinning plan:

- Follow the repository policy of pinning GitHub Actions by commit SHA.
- During implementation, resolve the supported CodeQL action commit SHA for each action used.
- Preserve the upstream action name and version in a comment, matching current CI style.
- If CodeQL action SHA pinning is blocked by repository tooling or GitHub guidance, document the exact upstream action version used and resolve pinning before release.

Expected CodeQL action components:

- Initialize CodeQL for Java/Kotlin.
- Run the manual Maven build.
- Analyze and upload results to GitHub code scanning.

The CodeQL workflow should be separate from `.github/workflows/ci.yml` and should not weaken or replace the current tests/package/JAR smoke/Docker/Trivy/dependency-review gates.

## E. Triage Policy

CodeQL findings should be reviewed before being treated as automatic release blockers.

Recommended triage posture:

- High or critical findings in production code should block release unless explicitly documented with rationale, owner, and follow-up.
- Findings in tests, demos, documentation examples, or intentionally offline lab paths may be triaged separately.
- False positives should be documented with the reason they are believed to be false positives and any follow-up needed to make that decision clearer.
- Findings that touch these areas should receive priority:
  - `core.CloudManager`
  - AWS mutation guardrails
  - API authentication and authorization
  - request validation and JSON parsing
  - CSV/JSON import/export and file parsing
  - deserialization behavior
  - command execution or process startup
  - path handling and file I/O
  - telemetry redaction and error surfaces
- The first CodeQL run should be treated as a baseline review, not proof of complete security.
- Do not claim production readiness solely because CodeQL is present.

## F. Exact Future Implementation Scope

The first implementation slice should add only:

- `.github/workflows/codeql.yml`
- `evidence/SUPPLY_CHAIN_EVIDENCE.md` update
- `evidence/SECURITY_POSTURE.md` update if it improves the current SAST posture summary
- `evidence/RESIDUAL_RISKS.md` update if moving CodeQL from a gap to partially addressed is appropriate
- `docs/V1_6_0_CODEQL_SAST_PLAN.md`

Scope boundaries:

- No Java code changes.
- No test changes.
- No `pom.xml` changes.
- No Maven dependency changes.
- No existing `.github/workflows/ci.yml` edits unless an implementation blocker requires a narrow and documented exception.
- No artifact attestations.
- No release signing.
- No container signing.
- No deployment, Kubernetes, Terraform, live AWS, or operations docs.

## G. Verification Plan

Before implementation:

```text
git status
git branch --show-current
```

After implementation:

```text
mvn -q test
mvn -q -DskipTests package
git diff --check
git diff --name-only
git diff -- src/main/java src/test/java pom.xml .github/workflows/ci.yml
```

Expected:

- No `src/main/java` changes.
- No `src/test/java` changes.
- No `pom.xml` changes.
- Existing `.github/workflows/ci.yml` remains unchanged unless explicitly justified.
- New `.github/workflows/codeql.yml` is added.
- Evidence docs are updated.
- CodeQL workflow syntax is reviewed for pinned actions, scoped permissions, and release-branch triggers.

If a local or repository-supported workflow syntax check is available, run it before committing. Do not require live GitHub code-scanning results for the local planning or implementation branch, because SARIF upload requires GitHub Actions context and repository permissions.

## H. Risks

- CodeQL action pinning can be awkward because examples often use moving version tags.
- CodeQL findings may be noisy on a demo/lab repository and need triage before becoming release blockers.
- The workflow can increase CI minutes and pull-request feedback time.
- Manual Maven build may differ from CodeQL autobuild behavior.
- Autobuild may miss or misrepresent project-specific Maven packaging behavior.
- `security-events: write` is required for SARIF upload and can fail if repository permissions are not enabled.
- Pull requests from forks may have different permissions and may not upload code-scanning results the same way as trusted branches.
- Making CodeQL required before the first findings are reviewed could block safe docs/config work unnecessarily.
- Generated `target/` content should not be treated as source to review.
- Accidentally editing existing CI behavior would broaden the slice.
- Evidence docs could overstate CodeQL as a complete security review rather than a SAST baseline.

## I. What Not To Change

- Do not change production code.
- Do not change tests.
- Do not change dependencies.
- Do not move tags.
- Do not touch public `main`.
- Do not change `CloudManager` or AWS behavior.
- Do not change routing behavior.
- Do not change allocation endpoint behavior.
- Do not change CLI behavior.
- Do not implement artifact attestations.
- Do not implement release artifact signing.
- Do not implement container signing.
- Do not add deployment, Kubernetes, Terraform, live AWS, or operations docs.
- Do not edit the existing CI workflow unless a narrow implementation blocker is found and documented.

## J. Recommended First Implementation Slice

Proceed with CodeQL/SAST before the next feature.

Recommended slice:

1. Add `.github/workflows/codeql.yml` as a separate workflow.
2. Use Java/Kotlin CodeQL analysis.
3. Use manual Maven build mode with `mvn -B -DskipTests package`.
4. Pin CodeQL actions to commit SHAs if possible and preserve upstream action version comments.
5. Keep the workflow advisory until the first findings are reviewed.
6. Update evidence docs to describe CodeQL as a SAST baseline.
7. Run Maven tests/package and planning-scope diff checks.
8. Commit as one workflow/evidence-only change.

Recommended commit message:

```text
Add CodeQL SAST workflow
```

Do not make CodeQL a release blocker until the first findings have been reviewed and triage rules are stable.

## K. Open Questions

- Should CodeQL run on every push, or only on `loadbalancerpro-clean`, pull requests targeting `loadbalancerpro-clean`, and a weekly schedule?
- Should the first run be advisory and non-blocking until findings are reviewed?
- Should manual Maven build remain the default, or should autobuild be tested as a fallback?
- Should generated sources or `target/` be explicitly excluded, or is default CodeQL behavior sufficient?
- Should CodeQL ship as `v1.6.0` or as a patch `v1.5.1`?
- Should evidence docs call this a SAST baseline rather than a complete security review?
- Should future CodeQL results be linked from release evidence once GitHub code scanning is available?

## L. Recommendation

Proceed with CodeQL/SAST before the next feature.

Keep the first implementation workflow/evidence-only: add a separate CodeQL workflow, document the static-analysis triage posture, and update evidence docs. Do not change Java code, tests, dependencies, `pom.xml`, existing CI gates, artifact attestations, signing, deployment, Kubernetes, Terraform, live AWS, or operations docs in the first implementation slice.
