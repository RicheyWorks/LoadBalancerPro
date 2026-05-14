# v2.5.0 Release Authorization Checklist

This checklist is the final human gate for a future `v2.5.0` JAR/docs-first release. It is not a publishing procedure and it did not create tags, GitHub Releases, release assets, registry images, container signatures, or `release-downloads/` evidence in this sprint.

## Exact Release Target

| Item | Required value |
| --- | --- |
| Release tag | `v2.5.0` |
| Maven project version | `2.5.0` |
| Release type | JAR/docs-first minor release |
| Exact commit | Record the exact `git rev-parse HEAD` value after the release-prep PR is merged to `main` |
| Release notes | [`RELEASE_NOTES_v2.5.0.md`](RELEASE_NOTES_v2.5.0.md) reviewed |
| Release-intent packet | [`RELEASE_INTENT_REVIEW.md`](RELEASE_INTENT_REVIEW.md) and ignored `target/release-intent-review/` output reviewed |
| Release-candidate packet | [`RELEASE_CANDIDATE_DRY_RUN_PACKET.md`](RELEASE_CANDIDATE_DRY_RUN_PACKET.md) and ignored `target/release-candidate-dry-run/` output reviewed |

## Required Evidence Before Approval

- Latest matching `main` checks are green for the exact commit.
- CI, CodeQL, Dependency Review, Trivy, package, smoke, and SBOM evidence are reviewed for the exact commit.
- `mvn -q clean test` passed.
- `mvn -q verify` passed.
- `mvn -q -DskipTests package` passed.
- `git diff --check` passed before opening the release authorization request.
- Release notes were reviewed and accepted.
- Dry-run packet and release-intent packet were generated under ignored `target/` paths and reviewed.
- SBOM JSON/XML and SHA-256 checksum evidence were generated and reviewed.
- GitHub artifact attestation expectation is understood as semantic-tag workflow output only.
- No container publication is included unless a separate prompt explicitly authorizes that later track.
- Rollback or withdrawal plan is reviewed: use the last known-good released JAR/checksum evidence or fix forward with a new reviewed version if publication fails.
- `release-downloads/` remains untouched unless a separate approved evidence task requires it.

## Release Boundary Confirmation

- This release-prep sprint did not create tags.
- This release-prep sprint did not create GitHub Releases.
- This release-prep sprint did not upload or mutate release assets.
- This release-prep sprint did not publish containers.
- This release-prep sprint did not sign releases or containers.
- This release-prep sprint did not mutate release workflow files, rulesets, default-branch settings, branch deletion, or `release-downloads/`.
- This release-prep sprint did not add secrets, external services, secret persistence, network discovery, DNS probing, port scanning, scheduled tasks, installers, native executables, or vendored binaries.

## DO NOT RUN In This PR

These commands are examples of the later human-approved release action shape. They are intentionally not run by this checklist, this PR, release-intent review, dry-run packet, or any smoke script.

```bash
git tag v2.5.0
git push origin v2.5.0
```

After the tag-triggered Release Artifacts workflow finishes, the operator must verify the expected JAR, SBOM JSON/XML, checksum file, GitHub artifact attestations, and GitHub Release assets for exact version `2.5.0`.

## Approval Record

| Question | Required answer | Status | Notes |
| --- | --- | --- | --- |
| Exact tag is `v2.5.0`? | Yes |  |  |
| Exact commit recorded after merge to `main`? | Yes |  |  |
| Checks are green? | Yes |  |  |
| Release notes reviewed? | Yes |  |  |
| Release-candidate dry-run packet reviewed? | Yes |  |  |
| Release-intent packet reviewed? | Yes |  |  |
| SBOM and checksums reviewed? | Yes |  |  |
| Artifact attestation expectation understood? | Yes |  |  |
| Container publication excluded unless separately authorized? | Yes |  |  |
| Rollback/withdrawal plan reviewed? | Yes |  |  |
| Separate explicit release authorization provided? | Required |  |  |

If any required item is missing, remain in release-intent mode and do not perform a real release action.
