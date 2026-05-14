# v2.5.0 Post-Release Verification

This note records the completed LoadBalancerPro `v2.5.0` JAR/docs-first release verification. It is post-release evidence only; it does not create tags, mutate the GitHub Release, upload assets, publish containers, sign containers, or change runtime behavior.

## Release Identity

| Field | Value |
| --- | --- |
| Release tag | `v2.5.0` |
| Release commit | `4cc03750be5479d9f8f88f8ef8014e05a8dc587a` |
| Project version | `2.5.0` |
| GitHub Release | <https://github.com/RicheyWorks/LoadBalancerPro/releases/tag/v2.5.0> |
| Release workflow | <https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/25838247936> |
| Tag-push CI workflow | <https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/25838247926> |
| Published at | `2026-05-14T02:34:38Z` |

## Verified Release Assets

The release contains exactly the expected JAR/docs-first assets:

| Asset | SHA-256 |
| --- | --- |
| `LoadBalancerPro-2.5.0.jar` | `04457ad3404835301a4b0763a77877967750ec03753af23dea0ff2db18372859` |
| `LoadBalancerPro-2.5.0-bom.json` | `ca5fc9498589a1833dbe478fe139ce87b6130791b4feb37b38ca80fbf6e1a75b` |
| `LoadBalancerPro-2.5.0-bom.xml` | `2d30c91e413e614305efe8b6316d93438219a12b5a7008b8545d5be1ac640090` |
| `LoadBalancerPro-2.5.0-SHA256SUMS.txt` | Checksum manifest for the JAR, SBOM JSON, and SBOM XML assets |

Verification status:

- Release Artifacts workflow completed successfully for `refs/tags/v2.5.0`.
- The workflow verified Maven version alignment, packaged the executable JAR, ran the release JAR smoke check, generated CycloneDX SBOM JSON/XML, generated SHA-256 checksums, verified checksums, created GitHub artifact attestations, published the GitHub Release assets, and verified the release asset names.
- Downloaded assets under ignored `release-downloads/v2.5.0/` matched `LoadBalancerPro-2.5.0-SHA256SUMS.txt`.
- SBOM JSON and SBOM XML assets are present and checksum-valid.
- `gh attestation verify` verified SLSA provenance for `LoadBalancerPro-2.5.0.jar` from `.github/workflows/release-artifacts.yml` at `refs/tags/v2.5.0` and commit `4cc03750be5479d9f8f88f8ef8014e05a8dc587a`.
- The release workflow's JAR/SBOM attestation step also completed successfully.
- Tag-push CI completed successfully, including tests, package, smoke, Docker build/runtime smoke, and Trivy scan. Dependency Review was skipped on the tag-push run by workflow condition; PR #134 Dependency Review passed before merge.

## Distribution Boundary

- Distribution is JAR/docs-first through GitHub Release assets.
- No container image was published.
- No container signing was performed.
- No registry push was performed.
- No extra release assets were manually uploaded.
- `release-downloads/v2.5.0/` is ignored local verification output and is not tracked evidence.
- Container registry publication and signing remain deferred to [`CONTAINER_REGISTRY_SIGNING_ROLLOUT.md`](CONTAINER_REGISTRY_SIGNING_ROLLOUT.md).

## Remaining Risks

- No production deployment certification is claimed.
- No real enterprise IdP tenant proof is included.
- Production TLS, IAM, ingress, WAF, rate limiting, monitoring, log retention, backup, incident response, SLOs, and legal/compliance approval remain operator-owned deployment responsibilities.
- Container publication, container signing, registry attestations, image rollback, and image retention remain future-gated.
- GitHub artifact attestations provide provenance evidence, not PGP signing, notarization, vulnerability proof, or production certification.
