# v2.3.5 Release Artifact Evidence

## Summary

- Release version: v2.3.5
- Release purpose: GitHub Actions Node.js 24 workflow maintenance
- Release commit: `5c75484fb608b82aa8325f69e1633b2c8249828f`
- Tag object: `521acc0294ef8889fd8b905e0ece5f2f5bd1c2b6`
- Workflow run: Release Artifacts #14
- Artifact bundle: `loadbalancerpro-release-2.3.5`

The first Release Artifacts run failed at the Maven/tag version check, was rerun, and then succeeded.

## Workflow Updates

- `actions/checkout` `4.3.1` -> `6.0.2`
- `actions/setup-java` `4.8.0` -> `5.2.0`
- `actions/upload-artifact` `4.6.2` -> `7.0.1`

## Artifact Files

Artifact contents were manually verified through GitHub UI ZIP download.

- `LoadBalancerPro-2.3.5.jar`
- `LoadBalancerPro-2.3.5-bom.json`
- `LoadBalancerPro-2.3.5-bom.xml`
- `LoadBalancerPro-2.3.5-SHA256SUMS.txt`

## Verification

- `mvn -q test` passed.
- `mvn -q -DskipTests package` passed.
- JAR `--version` reported 2.3.5.
- LASE healthy demo passed.
- LASE overloaded demo passed.
- LASE invalid-name exited 2 as expected.
- CycloneDX SBOM generation passed.
- SHA-256 checksum generation and verification passed.
- JAR provenance attestation passed.
- JAR SBOM attestation passed.
- Artifact upload passed under `actions/upload-artifact@v7`.
- `git diff --check` passed.

## Workflow Warning Note

`actions/checkout`, `actions/setup-java`, and `actions/upload-artifact` were moved to Node.js 24 action versions. The remaining Node.js 20 warning appears tied to `actions/dependency-review-action` and should be audited separately later.

## Safety Notes

- No application behavior changes are intended.
- Java version remained `17`.
- Java distribution remained `temurin`.
- Maven cache behavior remained unchanged.
- Release artifact names, upload paths, and retention remained unchanged.
- Namespace migration has not started.
- `public/main` was untouched.
