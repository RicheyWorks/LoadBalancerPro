# LoadBalancerPro Release Artifact Evidence

- Verified: 2026-07-30
- Release: [`v2.5.0`](https://github.com/RicheyWorks/LoadBalancerPro/releases/tag/v2.5.0)
- Release workflow: [run 25838247936](https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/25838247936)
- Release commit: `4cc03750be5479d9f8f88f8ef8014e05a8dc587a`

## Verified Public Artifacts

The `Release Artifacts` workflow completed successfully on 2026-05-14. Its package,
executable-JAR smoke, CycloneDX generation, checksum verification, provenance
attestation, JAR/SBOM attestation, publication, and release-asset verification steps
all passed.

The public release currently exposes:

| Asset | Bytes | GitHub-recorded SHA-256 |
| --- | ---: | --- |
| `LoadBalancerPro-2.5.0.jar` | 92,109,974 | `04457ad3404835301a4b0763a77877967750ec03753af23dea0ff2db18372859` |
| `LoadBalancerPro-2.5.0-bom.json` | 380,320 | `ca5fc9498589a1833dbe478fe139ce87b6130791b4feb37b38ca80fbf6e1a75b` |
| `LoadBalancerPro-2.5.0-bom.xml` | 345,206 | `2d30c91e413e614305efe8b6316d93438219a12b5a7008b8545d5be1ac640090` |
| `LoadBalancerPro-2.5.0-SHA256SUMS.txt` | 285 | `82f3feec70fe7d4c66d63f4d3670b32c5d234a2671d8b00a56183c9c02cf908d` |

These are historical `v2.5.0` release artifacts. They are not artifacts for the
current `main` head, and they predate later dependency maintenance. Current-head
claims require current-head CI artifacts and checks.

## Operator Verification

Download all four assets from the release page, then verify the bundle checksum:

```sh
sha256sum --check LoadBalancerPro-2.5.0-SHA256SUMS.txt
```

Where GitHub CLI attestation verification is available:

```sh
gh attestation verify LoadBalancerPro-2.5.0.jar \
  --repo RicheyWorks/LoadBalancerPro
```

The release workflow is the authoritative source for the build and attestation
steps. GitHub's release API is the authoritative source for the published asset
names, sizes, and recorded digests in the table above.

## Evidence Boundary

This proves that the named release workflow completed and published the listed
JAR, SBOM, and checksum assets with the recorded digests. It does not prove:

- that the current `main` head is identical to the release;
- absence of all vulnerabilities;
- PGP signing or notarization;
- container signing or registry publication;
- Maven Central publication;
- live-cloud or real-tenant validation;
- production capacity, SLOs, or readiness.

Artifact integrity, provenance, dependency review, CodeQL, image scanning,
runtime smoke, and deployment evidence remain distinct controls.
