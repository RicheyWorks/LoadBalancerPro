# LoadBalancerPro Release Process

This process applies to future semantic releases after the repository migration cleanup. It does not change or recreate historical tags or repaired releases.

## Release Readiness

Before creating a release tag:

- Update `pom.xml` so the Maven project version matches the intended release version.
- Keep README, CLI/API version metadata, release notes, and compatibility docs aligned with the same version when they mention release identity.
- Open and merge normal pull requests for the version change and any release notes before tagging.
- Wait for `main` CI and CodeQL to pass.

## Tag Format

Release tags must use semantic version format:

```text
v<major>.<minor>.<patch>
```

Examples:

```text
v2.4.3
v2.5.0
```

The tag version must match the Maven project version without the leading `v`. For example, tag `v2.4.3` requires:

```xml
<version>2.4.3</version>
```

## Automated Publishing

Pushing a tag matching `v*.*.*` triggers `.github/workflows/release-artifacts.yml`.

The workflow:

- verifies the tag is semantic,
- verifies the Maven project version matches the tag,
- packages the executable Spring Boot JAR,
- smoke-tests the release JAR,
- generates CycloneDX SBOM files in JSON and XML formats,
- stages deterministic release filenames,
- writes and verifies SHA-256 checksums,
- creates provenance/SBOM attestations,
- creates or updates the GitHub Release for the tag,
- uploads exactly the four release assets,
- preserves a workflow artifact bundle as backup evidence.

The GitHub Release assets are:

```text
LoadBalancerPro-<version>.jar
LoadBalancerPro-<version>-bom.json
LoadBalancerPro-<version>-bom.xml
LoadBalancerPro-<version>-SHA256SUMS.txt
```

The workflow marks releases as non-prerelease by default. It marks the release as latest only when the tag is the highest semantic version among existing GitHub Releases; older backfill tags are explicitly not marked latest.

## Dry-Run Release Verification

Use the manual `workflow_dispatch` path in `.github/workflows/release-artifacts.yml` to verify release asset generation before creating a tag.

The dry run:

- runs from the selected branch or commit, normally `main`,
- uses the optional `version` input or falls back to `pom.xml` project version,
- verifies the Maven project version matches the resolved release version,
- builds and smoke-tests the executable JAR,
- generates CycloneDX SBOM files,
- stages the exact release asset filenames,
- writes and verifies `LoadBalancerPro-<version>-SHA256SUMS.txt`,
- uploads a GitHub Actions workflow artifact named `loadbalancerpro-release-<version>-dry-run`.

The dry run does not create tags, create GitHub Releases, upload GitHub Release assets, overwrite existing assets, or mark anything latest.

To run it:

1. Open the `Release Artifacts` workflow in GitHub Actions.
2. Choose `Run workflow`.
3. Select `main`.
4. Leave `version` blank to use `pom.xml`, or enter the intended semantic version without the leading `v`.
5. Wait for the run to finish and download the workflow artifact bundle.

After the dry run, verify the artifact contains exactly:

```text
LoadBalancerPro-<version>.jar
LoadBalancerPro-<version>-bom.json
LoadBalancerPro-<version>-bom.xml
LoadBalancerPro-<version>-SHA256SUMS.txt
```

Then verify the checksum file against the JAR and both SBOM files before creating a real release tag.

## Checksums And SBOMs

`LoadBalancerPro-<version>-SHA256SUMS.txt` contains SHA-256 hashes for the JAR and both SBOM files. Use it to verify downloaded release assets.

The SBOM files are generated with CycloneDX:

- `LoadBalancerPro-<version>-bom.json`
- `LoadBalancerPro-<version>-bom.xml`

## Post-Release Verification

After the workflow finishes:

1. Confirm the release exists for the tag.
2. Confirm the release is not draft and not prerelease.
3. Confirm the release has exactly the four expected assets.
4. Download the assets to a temporary directory.
5. Verify the downloaded JAR, BOM JSON, and BOM XML against the checksum file.
6. Confirm the intended latest status. The newest semantic release should be latest; older backfill releases should not be latest.
7. Confirm `main` CI and CodeQL remain green.

Useful commands:

```powershell
$tag = "v2.4.3"
$version = $tag.TrimStart("v")
$verifyRoot = "$env:TEMP\LoadBalancerPro_$tag_release_verify"
New-Item -ItemType Directory -Force -Path $verifyRoot | Out-Null

gh release view $tag --repo RicheyWorks/LoadBalancerPro --json tagName,name,isDraft,isPrerelease,assets,url
gh release download $tag --repo RicheyWorks/LoadBalancerPro --dir $verifyRoot --clobber
Get-ChildItem -File $verifyRoot | Select-Object Name, Length, LastWriteTime
Get-FileHash "$verifyRoot\*" -Algorithm SHA256
Get-Content "$verifyRoot\LoadBalancerPro-$version-SHA256SUMS.txt"
```

## Manual Repair Policy

Do not manually overwrite or delete GitHub Release assets unless the replacement artifacts have been reproduced from the release tag and their hashes are proven.

Manual repair should use the same four asset names and should include:

- tag commit verification,
- clean-worktree artifact reproduction,
- checksum file verification,
- upload of exactly the four expected files,
- post-upload download and checksum verification.

Do not move existing tags to repair a release.
