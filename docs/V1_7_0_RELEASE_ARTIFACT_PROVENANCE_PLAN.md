# LoadBalancerPro v1.7.0 Release Artifact Provenance Plan

Date: 2026-05-03

## A. Current Release State

- `v1.6.0` is shipped and pushed.
- Remote release branch hash: `a7897d6f4c040336444c40927187a983b05aa95e`.
- Remote `v1.6.0` annotated tag object: `bd56560bb03019bd11623fab7fcd284ee4537100`.
- Remote `v1.6.0` tag target: `a7897d6f4c040336444c40927187a983b05aa95e`.
- Governance basics exist from `v1.4.0`.
- CI-generated CycloneDX SBOM artifacts exist from `v1.5.0`.
- A separate CodeQL SAST workflow exists from `v1.6.0`.
- Existing tags must remain immutable.
- Public `main` remains untouched.

## B. Current Provenance Baseline

Confirmed baseline:

- `.github/workflows/ci.yml` builds the executable JAR with `mvn -B package`.
- `.github/workflows/ci.yml` generates CycloneDX SBOM files with direct `org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom` invocation.
- `.github/workflows/ci.yml` uploads `target/bom.json` and `target/bom.xml` as the short-lived `loadbalancerpro-sbom` GitHub Actions artifact with 30-day retention.
- `.github/workflows/codeql.yml` runs Java/Kotlin CodeQL separately with manual Maven build mode.
- CI runs tests, packaging, LASE/JAR smoke checks, Docker build/runtime health, Trivy image scanning, and pull-request dependency review.
- Dependabot is configured for Maven, GitHub Actions, and Docker.
- GitHub Actions are pinned to reviewed commit SHAs with upstream action comments.
- Docker base images are pinned by digest.

Current gaps:

- No durable release JAR artifact publishing workflow exists yet.
- No tag-tied release SBOM artifact publishing workflow exists yet.
- Current CI SBOM artifacts are useful but short-lived workflow artifacts, not durable release assets.
- No GitHub artifact attestation workflow exists yet.
- No release artifact signing exists yet.
- No container signing exists yet.
- No container registry or release image naming policy exists yet.
- No GitHub Releases asset publishing policy exists yet.
- `pom.xml` currently reports version `1.3.1` while the latest Git release tag is `v1.6.0`; release artifact naming needs an explicit source-of-truth policy before publishing durable artifacts.

## C. Proposed Next Phases

### Phase 1: Release Artifact Workflow

Add a separate workflow:

```text
.github/workflows/release-artifacts.yml
```

Recommended behavior:

- Trigger on version tags such as `v*.*.*`.
- Check out the repository.
- Set up Java 17 with Maven cache.
- Build the executable JAR.
- Generate CycloneDX SBOM JSON and XML.
- Rename or copy the executable JAR and SBOM files to deterministic release-artifact names.
- Upload the JAR and SBOM files as GitHub Actions artifacts.
- Do not publish to Maven Central.
- Do not publish Docker images.
- Do not modify existing `.github/workflows/ci.yml`.
- Do not modify existing `.github/workflows/codeql.yml`.

Recommended release workflow checks:

- Confirm the workflow is running from a semantic version tag.
- Extract the tag version by removing the leading `v`.
- Read the Maven project version with Maven help tooling or another structured Maven command.
- Fail the workflow if the Git tag version and Maven project version do not match, unless the project explicitly decides that release artifact names are tag-derived and Maven version is app-runtime metadata only.
- Run at least a lightweight packaged JAR smoke check before upload.

### Phase 2: Artifact Attestations

Add GitHub artifact attestations after Phase 1 artifact names and workflow behavior are stable.

Likely required workflow permissions:

```yaml
permissions:
  contents: read
  id-token: write
  attestations: write
```

Attest these outputs:

- executable JAR
- `bom.json`
- `bom.xml`

Attestations should be tied to tag-triggered workflow outputs and generated artifacts, not source-tree files.

Keep language precise: GitHub artifact attestations provide provenance evidence for workflow-produced artifacts. They are not notarization, PGP signing, container signing, a vulnerability scan, or a production-readiness guarantee.

### Phase 3: Release Page Assets

Optionally upload artifacts to GitHub Releases in a later slice.

This needs decisions about:

- Whether the workflow creates releases or only attaches assets to existing releases.
- Required permissions.
- Asset naming policy.
- What happens if assets already exist.
- Whether release body generation is manual or automated.

Keep GitHub Releases asset publication separate from Phase 1 if a simpler Actions-artifact workflow reduces risk.

### Phase 4: Container Signing

Defer container signing until image publication exists.

Future container signing should wait for:

- Container registry decision.
- Image name decision.
- Tag and digest publication policy.
- Release image trigger policy.
- Keyless or key-managed signing decision.

Future cosign/keyless signing can be considered after release images are published by digest. Do not sign mutable image tags alone.

## D. Proposed Release Artifact Naming

Recommended deterministic artifact names:

```text
LoadBalancerPro-${version}.jar
LoadBalancerPro-${version}-bom.json
LoadBalancerPro-${version}-bom.xml
```

Recommended source-of-truth policy:

- Treat the Git tag as the release trigger.
- Treat Maven project version as the build artifact version.
- Before uploading release artifacts, verify that the tag version and Maven project version match.
- If they do not match, fail the release artifact workflow and require a narrow metadata-alignment patch or an explicit documented policy exception.

Rationale:

- The Git tag identifies the release event.
- The Maven version influences the generated JAR filename and build metadata.
- Using both without validation can create misleading artifacts, such as a `v1.7.0` release containing a `LoadBalancerPro-1.3.1.jar`.
- A hard mismatch check is safer than silently renaming artifacts from a mismatched build.

If the project deliberately wants governance/supply-chain releases to keep application runtime metadata at `1.3.1`, document that policy before implementing release artifact publication. Otherwise, align Maven/API/CLI/telemetry/README metadata before durable artifact publication.

## E. Exact Future Implementation Scope For Phase 1

Recommended first implementation adds only:

- `.github/workflows/release-artifacts.yml`
- tag-triggered release artifact build/upload
- tag-triggered release SBOM build/upload
- Maven-version versus Git-tag validation
- packaged JAR smoke check before upload
- `evidence/SUPPLY_CHAIN_EVIDENCE.md` update
- `evidence/SBOM_GUIDE.md` update
- `docs/V1_7_0_RELEASE_ARTIFACT_PROVENANCE_PLAN.md`
- optional short README evidence note if discoverability materially improves

Scope boundaries:

- No Java code.
- No tests.
- No `pom.xml` edits in the release-artifact workflow slice unless a separate metadata-alignment decision is made first.
- No dependency changes.
- No existing `.github/workflows/ci.yml` edits.
- No existing `.github/workflows/codeql.yml` edits.
- No generated SBOM files committed.
- No artifact attestations in Phase 1.
- No release signing in Phase 1.
- No container signing in Phase 1.
- No Docker image publishing.
- No Maven Central publishing.
- No deployment, Kubernetes, Terraform, live AWS, or operations docs.

## F. Verification Plan

Before implementation:

```text
git status
git branch --show-current
```

After implementation:

```text
mvn -q test
mvn -q -DskipTests package
mvn -B org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom -DoutputFormat=all -DoutputDirectory=target -DoutputName=bom -Dcyclonedx.skipAttach=true
git diff --check
git diff --name-only
git diff -- src/main/java src/test/java pom.xml .github/workflows/ci.yml .github/workflows/codeql.yml
```

Expected:

- No `src/main/java` changes.
- No `src/test/java` changes.
- No `pom.xml` changes unless a separate metadata-alignment exception is explicitly approved.
- Existing `.github/workflows/ci.yml` unchanged.
- Existing `.github/workflows/codeql.yml` unchanged.
- New `.github/workflows/release-artifacts.yml` added.
- Evidence docs updated.
- Generated SBOM files remain under ignored `target/` and are not committed.

Additional workflow review checks:

```text
findstr /C:"name:" .github\workflows\release-artifacts.yml
findstr /C:"v*.*.*" .github\workflows\release-artifacts.yml
findstr /C:"mvn -B -DskipTests package" .github\workflows\release-artifacts.yml
findstr /C:"cyclonedx-maven-plugin:2.9.1:makeAggregateBom" .github\workflows\release-artifacts.yml
findstr /C:"actions/upload-artifact" .github\workflows\release-artifacts.yml
```

If local workflow syntax tooling is available, run it. Live artifact upload and attestation behavior require GitHub Actions context and should be validated on the tag-triggered workflow run after merge/tag.

## G. Risks

- Tag-triggered workflow patterns can be too broad and run on non-release tags.
- Tag-triggered workflow patterns can be too narrow and miss intended releases.
- Maven project version and Git tag can drift, producing misleading artifact names.
- Artifact naming can drift between CI artifacts, release artifacts, and documentation.
- Attestation permissions may not be enabled or may fail on repository settings.
- Short-lived CI artifacts can be confused with durable release artifacts.
- Generated JAR/SBOM files could be accidentally committed if outputs move outside ignored build directories.
- Publishing artifacts can be misread as a production-readiness claim.
- GitHub Releases permissions and asset overwrite behavior need explicit policy before automation.
- Release assets can be overwritten or duplicated if the workflow is rerun without guardrails.
- Maven Central publishing would introduce a separate release-management problem and is out of scope.
- Future container signing is blocked until registry, image naming, and image publication policy are decided.
- If release artifacts are produced before metadata alignment, reviewers may see tag/version mismatch as a release integrity failure.

## H. What Not To Change

- Do not change production code.
- Do not change tests.
- Do not change dependencies.
- Do not move tags.
- Do not touch public `main`.
- Do not change `CloudManager` or AWS behavior.
- Do not change routing behavior.
- Do not change allocation endpoint behavior.
- Do not change CLI behavior.
- Do not implement container signing.
- Do not publish Docker images.
- Do not publish to Maven Central.
- Do not add deployment, Kubernetes, Terraform, live AWS, or operations docs.
- Do not edit existing `.github/workflows/ci.yml` in the planning slice.
- Do not edit existing `.github/workflows/codeql.yml` in the planning slice.

## I. Recommended First Implementation Slice

Recommended conservative sequence:

1. Phase 1: add release artifacts only.
2. Phase 2: add attestations after artifact names and workflow stability are confirmed.

Phase 1 should create a separate tag-triggered release artifact workflow and update evidence docs. It should not include attestations yet because the artifact naming, tag trigger, Maven-version validation, retention, and release workflow behavior should be proven first.

Recommended Phase 1 commit message:

```text
Add release artifact workflow
```

Phase 2 can add GitHub artifact attestations after Phase 1 has produced the expected JAR and SBOM artifacts from a real tag-triggered workflow run.

## J. Open Questions

- Should release artifacts be GitHub Actions artifacts only, or also GitHub Release assets?
- Should artifact retention be explicit, and if so should it be 30, 90, or another number of days?
- Should Maven version and Git tag mismatch fail the workflow? Recommended answer: yes.
- Should attestations ship in `v1.7.1` or `v1.8.0` after Phase 1 stabilizes?
- Should the release artifact workflow run on all `v*` tags or only semantic `v*.*.*` tags? Recommended answer: semantic `v*.*.*`.
- Should generated SBOM filenames include the release version? Recommended answer: yes.
- Should the JAR smoke checks be repeated in the release workflow? Recommended answer: at least a lightweight `--version` and LASE demo smoke if runtime cost is acceptable.
- Should release artifact naming use the Git tag version, Maven project version, or require both to match? Recommended answer: require both to match.
- Should release artifacts include checksums in a later slice before attestations?

## K. Recommendation

Proceed with a release artifact workflow before another feature.

Keep the first implementation workflow/evidence-only: add a separate tag-triggered workflow that builds the executable JAR, generates CycloneDX SBOM JSON/XML, validates Git tag and Maven project version alignment, uploads deterministic JAR/SBOM artifacts, and updates evidence docs. Defer attestations, GitHub Release assets, release signing, container signing, Docker image publishing, Maven Central publishing, deployment, Kubernetes, Terraform, live AWS, and operations docs to later focused slices.
