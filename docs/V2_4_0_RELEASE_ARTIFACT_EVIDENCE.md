# LoadBalancerPro v2.4.0 Release Artifact Evidence

## Release

- Release version: `v2.4.0`
- Release purpose: package namespace migration
- Release commit: `355ee8f3cf0cc6dfbd6b3736dacb39e48963ae09`
- Tag object: `ab750375550c0c1a6633122f455dab7f719331eb`
- GitHub Release status: published, latest release, not pre-release

## Artifacts

The following files were attached to the GitHub Release:

- `LoadBalancerPro-2.4.0.jar`
- `LoadBalancerPro-2.4.0-bom.json`
- `LoadBalancerPro-2.4.0-bom.xml`
- `LoadBalancerPro-2.4.0-SHA256SUMS.txt`

The Release Artifacts ZIP contents were manually verified with the same four files.

## Namespace Migration Summary

- Maven `groupId`: `com.example` -> `com.richmond423`
- Root namespace: `com.richmond423.loadbalancerpro`
- Package mapping:
  - `api` -> `com.richmond423.loadbalancerpro.api`
  - `api.config` -> `com.richmond423.loadbalancerpro.api.config`
  - `cli` -> `com.richmond423.loadbalancerpro.cli`
  - `core` -> `com.richmond423.loadbalancerpro.core`
  - `gui` -> `com.richmond423.loadbalancerpro.gui`
  - `util` -> `com.richmond423.loadbalancerpro.util`
- Moved 115 production Java files.
- Moved 45 test Java files.
- Confirmed 160 package declarations under the new namespace.
- Confirmed 146 imports under the new namespace.

## Verification Summary

- `mvn -q test` passed.
- `mvn -q -DskipTests package` passed.
- JAR `--version` reported `2.4.0`.
- LASE healthy demo passed.
- LASE overloaded demo passed.
- LASE invalid-name exited `2` as expected.
- CycloneDX SBOM generation passed.
- SHA-256 checksum generation and verification passed.
- JAR provenance attestation passed.
- JAR SBOM attestation passed.
- `git diff --check` passed.

## Safety Notes

- No application behavior changes intended.
- No dependency updates included.
- No workflow changes included.
- No resource changes included.
- No deprecated shims removed.
- `gui.messages` resource path stayed stable.
- `server_types` resource loading stayed stable.
- `public/main` untouched.

## Compatibility Note

External Java consumers using old flat imports must update to `com.richmond423.loadbalancerpro.*`.
