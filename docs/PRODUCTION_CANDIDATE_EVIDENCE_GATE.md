# Production-Candidate Evidence Gate

Use this gate before describing a LoadBalancerPro build as production-candidate or release-ready. It is an evidence checklist, not a release command. Completing it does not create tags, GitHub Releases, release assets, registry images, cloud resources, or files under `release-downloads/`.

This gate assumes the deployment still needs environment-specific TLS, IAM, network policy, secret storage, monitoring, incident response, rollback, and operator approval. It also assumes local/demo mode remains intentionally permissive and must not be exposed publicly.

For a source-visible local packet that records commit, build/test/package, SBOM, checksum, smoke, security gate, and publication-boundary evidence under ignored `target/release-candidate-dry-run/`, use [`RELEASE_CANDIDATE_DRY_RUN_PACKET.md`](RELEASE_CANDIDATE_DRY_RUN_PACKET.md).

## Gate Levels

`production-candidate` means the source revision has current test, security, release-artifact, and deployment-hardening evidence that a reviewer can inspect before a controlled production-like demo or operator evaluation.

`release-ready` means a separately approved release run has also produced and verified the semantic-tag release evidence for the exact version being distributed. A release-ready claim requires the production-candidate checklist plus successful tag-triggered Release Artifacts evidence.

## Automated Evidence Today

The repository currently automates these checks in CI or the tag-triggered release workflow:

- Full Maven test pass through `mvn -B test`.
- Maven packaging through `mvn -B package`.
- CI packaged-jar smoke evidence.
- CodeQL Java/Kotlin SAST workflow.
- Dependency Review on pull requests.
- Trivy image scan in CI for fixed high/critical findings.
- CycloneDX SBOM generation as `bom.json` and `bom.xml`.
- Docker image build/runtime smoke with digest-pinned base images in `Dockerfile`.
- Release Artifacts workflow version-alignment guard for semantic tags.
- Release Artifacts workflow SHA-256 checksum generation and verification.
- Release Artifacts workflow GitHub artifact attestations for release JAR provenance and the JAR/SBOM JSON relationship.
- Release Artifacts workflow GitHub Release asset publication and exact asset-name verification on semantic tag runs.

## Manual Or Operator Verification

Reviewers or operators must still verify these items before making a production-candidate or release-ready statement:

- Confirm the source revision under review is the exact commit being described.
- Confirm the latest relevant CI run passed for that commit.
- Confirm the CodeQL workflow passed for that commit.
- Confirm Dependency Review did not report unresolved blocking findings.
- Confirm Trivy did not report unresolved fixed high/critical findings.
- Confirm `mvn -q clean test`, `mvn -q verify`, and `mvn -q -DskipTests package` passed locally when local Maven is trusted and available.
- Confirm the container/default run path uses the prod profile and requires an operator-provided API key.
- Confirm container registry publication and signing remain deferred unless `CONTAINER_SIGNING_DECISION_RECORD.md` has been completed in a separate focused change.
- Confirm Docker base image references are pinned by digest.
- Confirm no secrets, API keys, bearer tokens, credentials, or tenant-specific values are committed or persisted in evidence.
- Confirm `release-downloads/` was not mutated unless a separate explicit release-evidence task approved that path.
- Confirm no native executables, installers, wrappers, launch4j, jpackage, native-image outputs, self-extracting archives, or vendored third-party binaries were introduced.
- Confirm any accepted dependency or SAST risk follows [`DEPENDENCY_SAST_RISK_WORKFLOW.md`](DEPENDENCY_SAST_RISK_WORKFLOW.md) with owner, severity, rationale, expiry or review date, and remediation follow-up.

## Production-Candidate Checklist

Before using the production-candidate label for a source revision:

- [ ] `mvn -q clean test` passed.
- [ ] `mvn -q verify` passed.
- [ ] `mvn -q -DskipTests package` passed.
- [ ] Focused tests for the changed area passed.
- [ ] `git diff --check` passed before the PR was opened.
- [ ] Required PR checks passed, including CI.
- [ ] CodeQL passed for the PR or resulting main revision.
- [ ] Dependency Review passed or every non-blocking finding has an owner-approved rationale.
- [ ] Trivy image scan passed or every non-blocking finding has an owner-approved rationale.
- [ ] CI SBOM artifact exists and includes CycloneDX JSON/XML.
- [ ] Packaged-jar smoke evidence exists for the CI run.
- [ ] Docker base image digest pinning is still present.
- [ ] Prod/cloud-sandbox API-key mode remains deny-by-default for `/api/**` except documented public exceptions.
- [ ] OAuth2 role mapping still requires dedicated role claims and does not treat scope-only tokens as app roles.
- [ ] Required DTO fields still fail validation when omitted.
- [ ] No secrets are committed, logged into evidence, or persisted by smoke scripts.
- [ ] No `release-downloads/` mutation was needed for the production-candidate review.
- [ ] No native or vendored binary artifacts were created or committed.

## Release-Ready Additions

Before using the release-ready label for a distributed semantic version:

- [ ] The semantic Git tag and Maven project version match.
- [ ] The Release Artifacts workflow passed for the semantic tag.
- [ ] The release JAR, SBOM JSON, SBOM XML, and SHA-256 checksum file were produced with deterministic names.
- [ ] `sha256sum -c` verification passed in the workflow.
- [ ] GitHub artifact attestations exist for release JAR provenance and the JAR/SBOM JSON relationship.
- [ ] GitHub Release assets were created or updated only by the semantic-tag Release Artifacts workflow.
- [ ] GitHub Release asset names exactly match the expected JAR/SBOM/checksum asset set.
- [ ] The versioned release evidence document records workflow run, checksum, attestation, and known limitations.
- [ ] Release evidence still states that GitHub artifact attestations are provenance evidence, not PGP signing, notarization, vulnerability proof, or production certification.

## Stop Conditions

Stop the release-readiness claim if any of these are true:

- Required checks are failing or missing.
- CodeQL, Dependency Review, or Trivy has an unresolved blocking finding.
- The reviewed source commit does not match the built artifact commit.
- Semantic tag and Maven version do not match.
- Release asset names differ from the expected set.
- Secrets or tenant-specific values appear in source, logs, evidence, or artifacts.
- Local/demo profile would be exposed publicly.
- `release-downloads/` mutation is required but was not separately approved.
- Native or vendored binary artifacts appear in the diff.
- A required control depends on an undocumented manual step.

## Evidence Locations

- CI, packaged smoke, SBOM, Dependency Review, and Trivy: GitHub Actions CI run for the commit.
- CodeQL: GitHub Actions CodeQL run for the commit.
- Release JAR, SBOM, checksums, attestations, and asset-name verification: semantic-tag Release Artifacts run.
- Supply-chain posture: [`../evidence/SUPPLY_CHAIN_EVIDENCE.md`](../evidence/SUPPLY_CHAIN_EVIDENCE.md).
- Test posture: [`../evidence/TEST_EVIDENCE.md`](../evidence/TEST_EVIDENCE.md).
- Security posture: [`../evidence/SECURITY_POSTURE.md`](../evidence/SECURITY_POSTURE.md).
- Deployment caveats: [`DEPLOYMENT_HARDENING_GUIDE.md`](DEPLOYMENT_HARDENING_GUIDE.md).
- Dependency/SAST findings: [`DEPENDENCY_SAST_RISK_WORKFLOW.md`](DEPENDENCY_SAST_RISK_WORKFLOW.md).
- Readiness summary: [`PRODUCTION_READINESS_SUMMARY.md`](PRODUCTION_READINESS_SUMMARY.md).
- Release-candidate dry-run packet: [`RELEASE_CANDIDATE_DRY_RUN_PACKET.md`](RELEASE_CANDIDATE_DRY_RUN_PACKET.md).
- Reviewer navigation: [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md).
