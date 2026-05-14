# Release Intent Review

This review prepares the human release decision for the next LoadBalancerPro semantic release. It is review-only: it does not create tags or a semantic tag, create a GitHub Release, upload release assets, publish containers, sign artifacts, mutate `release-downloads/`, or approve production deployment. Put plainly, it does not create a semantic tag.

Use this packet after the release-candidate dry-run packet and before any separate, explicit release authorization.

## Current Source And Version State

- Current commit under review: record the exact value from `git rev-parse HEAD` in the generated review packet.
- Current Maven project version: `2.5.0`.
- Latest immutable release tag observed for this line: `v2.4.2`.
- Recommended exact next release version: `v2.5.0`.
- Recommended release type: JAR/docs-first minor release.
- Container distribution status: deferred; registry publication and container signing are not part of this release intent.
- Two-track decision summary: [`RELEASE_READINESS_DECISION_SUMMARY.md`](RELEASE_READINESS_DECISION_SUMMARY.md).
- Draft release notes: [`RELEASE_NOTES_v2.5.0.md`](RELEASE_NOTES_v2.5.0.md).
- Final release authorization checklist: [`V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md`](V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md).
- Container rollout gate if image distribution becomes required: [`CONTAINER_REGISTRY_SIGNING_ROLLOUT.md`](CONTAINER_REGISTRY_SIGNING_ROLLOUT.md).

Do not push `v2.5.0` or create any release assets from this review. This release-prep branch aligns Maven/runtime/docs metadata to `2.5.0`; after it merges, a separate explicit release authorization is still required before any real semantic tag or GitHub Release action. The release artifact workflow intentionally fails semantic tag runs when the tag version and Maven project version differ.

## Version Recommendation

Recommend `v2.5.0` rather than `v2.4.3` because the changes since `v2.4.2` are not a narrow dependency or docs patch. They include enterprise hardening that affects production-style operation:

- containers default to the protected `prod` profile;
- prod/cloud-sandbox API-key mode is deny-by-default for non-`OPTIONS` `/api/**` except documented `GET /api/health`;
- OAuth2 application roles come from dedicated role claims, not ordinary scope-only claims;
- required allocation DTO fields reject omitted JSON instead of silently defaulting primitive values.

Recommend a minor release rather than a major release because local/default developer convenience remains intentionally public, the core JAR/docs-first distribution model remains unchanged, public Java compatibility shims are not removed, and no broad source namespace or storage format break is introduced. The release notes must still carry an operator compatibility notice for stricter prod/container/API-key/OAuth2/DTO behavior.

## Release Type And Artifacts

The recommended release path is JAR/docs-first. After explicit approval and version alignment, the existing semantic-tag Release Artifacts workflow would produce:

- `LoadBalancerPro-2.5.0.jar`
- `LoadBalancerPro-2.5.0-bom.json`
- `LoadBalancerPro-2.5.0-bom.xml`
- `LoadBalancerPro-2.5.0-SHA256SUMS.txt`

The release is not a container distribution. No registry image, image digest, container signature, registry attestation, Helm chart, Kubernetes manifest, Terraform module, installer, native executable, or package-manager artifact is part of this review.

## Required Dry-Run Commands

Run these before a human release decision:

```powershell
mvn -q clean test
mvn -q verify
mvn -q -DskipTests package
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\release-candidate-dry-run-packet.ps1 -Package
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\release-intent-review.ps1 -DryRun -RecommendedVersion 2.5.0
git diff --check
```

Also run the local-only smoke paths when the workstation supports them:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\operator-run-profiles-smoke.ps1 -DryRun
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\postman-enterprise-lab-safe-smoke.ps1 -DryRun
```

## Required Evidence Files

Reviewers should collect or inspect:

- `target/release-candidate-dry-run/release-candidate-dry-run-packet.md`
- `target/release-candidate-dry-run/release-candidate-dry-run-packet.json`
- `target/release-candidate-dry-run/LoadBalancerPro-*-bom.json`
- `target/release-candidate-dry-run/LoadBalancerPro-*-bom.xml`
- `target/release-candidate-dry-run/LoadBalancerPro-*-SHA256SUMS.txt`
- `target/release-intent-review/release-intent-review.md`
- `target/release-intent-review/release-intent-review.json`
- [`RELEASE_NOTES_v2.5.0.md`](RELEASE_NOTES_v2.5.0.md)
- [`V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md`](V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md)
- the latest matching `main` CI, CodeQL, Dependency Review, Trivy, package, smoke, and SBOM evidence;
- the Release Artifacts workflow dry-run bundle for `2.5.0` after this release-prep alignment merges.

The generated `target/` evidence is ignored build output and must not be committed.

## Supply-Chain Expectations

- Full tests and `mvn verify` pass for the exact commit.
- `mvn -q -DskipTests package` builds the executable Spring Boot JAR.
- CycloneDX SBOM JSON and XML are generated and reviewed.
- SHA-256 checksums are generated and verified for the local JAR and SBOM files.
- CodeQL succeeds for the exact commit.
- Dependency Review succeeds or has documented non-blocking owner decisions.
- Trivy image scan evidence is reviewed for the exact commit or matching CI run.
- GitHub artifact attestations are expected only after an approved semantic-tag release workflow run.
- GitHub Release assets are expected only after an approved semantic-tag release workflow run.

## Release Notes Draft Outline

Use this outline for the real release PR or release notes:

1. Summary: LoadBalancerPro `v2.5.0` enterprise hardening release.
2. Security boundary changes: container prod default, prod/cloud-sandbox API-key deny-by-default, OAuth2 dedicated role claims, DTO omitted-field validation.
3. JAR/docs-first distribution: release JAR, CycloneDX SBOM JSON/XML, SHA-256 checksums, GitHub artifact attestations.
4. Operator compatibility notes: runtime API key required for protected prod container usage; local/default mode remains developer-only and must not be publicly exposed.
5. Evidence: CI, CodeQL, Dependency Review, Trivy, release-candidate dry-run packet, release-intent review packet.
6. Not included: container registry publication, container signing, real IdP tenant configuration, cloud deployment certification, native installers, Maven Central publication.

## Rollback And Withdrawal Considerations

- Do not move or replace existing tags.
- If a release workflow fails before publishing assets, fix forward with a new reviewed change or a new semantic version as appropriate.
- If a GitHub Release is created with unexpected assets, stop and perform a separate repair review using the release process and checksum evidence.
- If a deployment consumes the JAR directly, rollback should select the last known-good released JAR and checksum evidence.
- If container distribution later becomes required, use immutable image digests and the container rollout plan before treating images as release artifacts.
- Use [`RELEASE_READINESS_DECISION_SUMMARY.md`](RELEASE_READINESS_DECISION_SUMMARY.md) to choose between JAR/docs-first release intent and the deferred container rollout path.

## Final Human Approval Checklist

Before approving a real release action, confirm:

| Item | Required state |
| --- | --- |
| Recommended version | `v2.5.0` accepted |
| Version alignment PR | Maven/runtime/docs metadata updated to `2.5.0`, checks passed, and merged |
| Exact commit | Recorded in release-intent output |
| Full tests and verify | Passed |
| Package | Passed |
| Release-candidate packet | Generated and reviewed |
| Release-intent packet | Generated and reviewed |
| Release notes | [`RELEASE_NOTES_v2.5.0.md`](RELEASE_NOTES_v2.5.0.md) reviewed |
| Release authorization checklist | [`V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md`](V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md) completed |
| CI and CodeQL | Latest matching commit successful |
| Dependency Review and Trivy | Successful or owner-approved non-blocking findings |
| SBOM and checksums | Generated and verified |
| Artifact attestation expectation | Understood as semantic-tag workflow output only |
| Container decision | JAR/docs-first accepted; no container distribution required now |
| Explicit release authorization | Provided in a separate request before any real release action |
| Publication boundary | No tag, GitHub Release, release assets, registry image, or container signature created by this review |

If any required item is missing, remain in release-intent review mode.
