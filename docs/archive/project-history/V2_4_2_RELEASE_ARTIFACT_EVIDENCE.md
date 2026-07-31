# v2.4.2 Release Artifact Evidence

## Release

- Release version: `v2.4.2`
- Release purpose: dependency maintenance
- Release commit: `da85ca0c20c0e2d3760aad9df9d5faf5976946bf`
- Tag object: `65cb85b77ebfbf685224030bdd838b4acfe0c57b`
- GitHub Release status: published, latest release, not pre-release

## Release Assets

The following files were attached to the GitHub Release:

- `LoadBalancerPro-2.4.2.jar`
- `LoadBalancerPro-2.4.2-bom.json`
- `LoadBalancerPro-2.4.2-bom.xml`
- `LoadBalancerPro-2.4.2-SHA256SUMS.txt`

## Dependency Maintenance Summary

- `org.json:json` updated from `20231013` to `20251224`.
- AWS SDK v2 BOM updated from `2.42.35` to `2.44.1`.

## Verification Summary

- Maven version/tag check passed.
- Packaged executable JAR passed.
- Release JAR smoke test passed.
- CycloneDX SBOM generation passed.
- Deterministic release artifact staging passed.
- SHA-256 checksum generation and verification passed.
- JAR provenance attestation passed.
- JAR SBOM attestation passed.
- Artifact upload passed.
- Cloud guardrail tests passed.
- LASE invalid-name explicit exit code was `2` during pre-tag verification.

## Safety Notes

- No deferred major PRs were included.
- No Spring Boot 4, JavaFX 26, springdoc 3, Java 25, or Java 26 migration was included.
- No deprecated shims were removed.
- No routing/allocation behavior changes are intended.
- No cloud/AWS guardrail changes are intended.
- `public/main` was untouched.
