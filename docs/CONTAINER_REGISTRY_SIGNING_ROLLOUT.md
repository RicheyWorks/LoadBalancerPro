# Container Registry And Signing Rollout

This rollout plan is the implementation gate for future container distribution. It is documentation only today: no registry push, no container signing, no registry attestation, no image publication, no workflow mutation, no secrets, and no `release-downloads/` mutation are performed by this plan.

Use this only if deployable container images become a required distribution channel. If distribution remains JAR/docs-first, the semantic-tag Release Artifacts workflow remains the release path and this container rollout stays deferred. The current two-track recommendation is summarized in [`RELEASE_READINESS_DECISION_SUMMARY.md`](RELEASE_READINESS_DECISION_SUMMARY.md).

## Decision Point

Container distribution is required only when operators need a deployable image as the release artifact. It is not required for a JAR/docs-first release that distributes the executable JAR, CycloneDX SBOMs, SHA-256 checksums, GitHub Release assets, and GitHub artifact attestations.

Before first container publication, a focused implementation PR must satisfy every gate below and pass the production-candidate evidence gate for the exact source revision.

## Registry Choices

Candidate registries may include:

- GitHub Container Registry when repository-adjacent package visibility and GitHub identity are preferred.
- AWS ECR or another cloud registry when deployment infrastructure already owns access control and retention.
- Docker Hub or another public OCI registry only if public image distribution is intentionally approved.
- A private enterprise OCI registry when customer/operator access policy requires private distribution.

Do not hardcode registry credentials, tenant IDs, account IDs, tokens, or signing keys in source, docs, scripts, image labels, Dockerfile layers, logs, or evidence.

## Image Name Policy

The implementation PR must choose:

- canonical registry host;
- canonical namespace or organization;
- canonical image name, expected to be equivalent to `loadbalancerpro`;
- ownership and CODEOWNER review path for image name changes;
- public/private visibility policy.

Example placeholders may use `<registry>/<namespace>/loadbalancerpro`; replace them only in the approved implementation PR.

## Tag And Immutable Digest Policy

Required policy:

- Semantic version image tags map to the matching JAR release version, for example `2.5.0`.
- Optional convenience tags such as `latest` are allowed only if the release process defines when they move.
- Consumers must verify or pin immutable image digests; mutable tags alone are not sufficient release evidence.
- The source commit, Maven version, release tag, image tag, and image digest must be recorded together.
- Rebuilding the same semantic version must be treated as a repair event with explicit evidence, not a silent overwrite.

## Build Provenance And Base Digest Expectations

- Dockerfile base images remain pinned by digest.
- Base-image tag and digest refreshes require focused review, container build smoke, and vulnerability scan review.
- The build source commit, builder workflow identity, Maven project version, and Dockerfile digest posture must be recorded.
- GitHub artifact attestation expectations must be defined for any produced container image or image metadata before publication.
- Local developer image builds are not release images.

## SBOM And Vulnerability Scan Expectations

- Keep the Maven CycloneDX SBOM for the JAR and dependency graph.
- Add or retain container-image SBOM evidence if the registry path publishes OS-layer images.
- Trivy or equivalent vulnerability scan evidence must be tied to the exact image digest.
- Fixed high/critical findings must block publication unless a documented owner decision accepts a non-fixed risk.
- Scan evidence must be retained with the release evidence bundle or linked from the release-intent review.

## Signing And Attestation Options

Choose one signing and attestation approach before first publication:

- keyless signing through an approved OIDC identity;
- key-managed signing where key ownership, rotation, and emergency revocation are documented;
- platform-native registry signing if the target registry provides a verified mechanism;
- GitHub artifact attestations for build provenance and artifact relationships where supported.

The implementation must include verification instructions for operators. A signature or attestation that cannot be verified by consumers is not sufficient evidence.

## Credential Handling Model

- Registry credentials, signing identities, and cloud account permissions must come from approved CI secret or OIDC mechanisms.
- No credential may be committed, stored under `release-downloads/`, written to `target/` evidence, baked into images, printed in logs, or persisted in scripts.
- Least privilege should restrict publication to the release workflow and approved maintainers.
- Credential rotation, revocation, and break-glass ownership must be documented before publication.

## Promotion Environments And Manual Approvals

Define promotion stages before first publication:

| Stage | Required evidence | Approval |
| --- | --- | --- |
| Build candidate | CI, package, Docker build, SBOM, scan | Maintainer review |
| Staging candidate | Digest captured, signature/attestation verified, smoke path passed | Operator review |
| Release image | Version/digest mapping, release notes, rollback/retention accepted | Explicit release approval |

Promotion must use immutable digests, not unverified mutable tags.

## Rollback And Retention Policy

- Retain known-good image digests for the supported rollback window.
- Document how an operator selects the previous digest and verifies signature/attestation evidence.
- Retain image, SBOM, vulnerability scan, signature, attestation, and checksum evidence for the same or longer period as JAR release evidence.
- Do not delete or overwrite published image evidence without a separate repair/retention decision.

## Automation Required Before First Publication

A future implementation PR must add or confirm:

- release-image build workflow or release workflow extension with explicit approval gates;
- registry authentication through OIDC or approved CI secrets;
- deterministic image naming and semantic tag policy;
- immutable digest capture;
- vulnerability scan for the exact digest;
- signing and attestation generation;
- consumer verification instructions;
- rollback and retention evidence;
- tests/static guards proving publish/sign commands exist only in the approved release path.

Until those items are implemented and approved, the repository remains no-registry/no-container-signing.

## Stop Conditions

Stop before publication or signing if:

- registry target, image name, tag policy, or digest policy is undecided;
- the release version and Maven project version do not match;
- vulnerability scan evidence is missing or blocking findings lack an owner decision;
- signing or attestation verification cannot be demonstrated;
- credential handling would persist secrets in source, image layers, logs, `target/`, or `release-downloads/`;
- rollback or retention policy is missing;
- the change tries to combine container publication with unrelated workflow, ruleset, default-branch, release asset, or branch-deletion changes.

## Current Status

No container publication or container signing was performed in this sprint. The rollout remains a future gate.
