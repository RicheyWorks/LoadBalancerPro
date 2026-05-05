# v2.4.1 Release Artifact Evidence

## Release Summary

- Release version: `v2.4.1`
- Release purpose: compatibility maintenance after the `v2.4.0` namespace migration
- Release commit: `01bf6f8141ec4cb2569a93699b1046e21c7fed7d`
- Tag object: `11d4896219e0f8f1034b386f0645ecfdc0e59b14`
- GitHub Release status: published, latest release, not pre-release

## Artifact Workflow

- Workflow: Release Artifacts #16
- Artifact bundle: `loadbalancerpro-release-2.4.1`
- Artifact ID: `6800629362`
- Artifact size: 81,229,518 bytes
- Artifact expired: false

## GitHub Release Assets

- `LoadBalancerPro-2.4.1.jar`
- `LoadBalancerPro-2.4.1-bom.json`
- `LoadBalancerPro-2.4.1-bom.xml`
- `LoadBalancerPro-2.4.1-SHA256SUMS.txt`

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

## Compatibility Maintenance Summary

- `handleFailover()` test usage migrated to `checkServerHealth()`.
- `getCloudManager()` test usage migrated to `getCloudManagerOptional()`.
- `getServerMonitor()` test usage migrated to public shutdown behavior.
- Cloud metrics implementation moved behind `updateCloudMetricsIfAvailable()`.
- Deprecated shims preserved for compatibility.
- `balanceLoad()` intentionally left unchanged as public legacy compatibility API.

## Safety Notes

- No dependency or plugin versions changed.
- No deprecated shims were removed.
- No routing or allocation behavior changes are intended.
- No cloud or AWS guardrail changes are intended.
- `public/main` was untouched.
