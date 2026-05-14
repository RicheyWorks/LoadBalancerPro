# LoadBalancerPro v2.5.0 Release Notes

These notes describe the completed `v2.5.0` JAR/docs-first release. [`V2_5_0_POST_RELEASE_VERIFICATION.md`](V2_5_0_POST_RELEASE_VERIFICATION.md) records the exact tag, commit, release workflow, verified asset set, checksum verification, SBOM JSON/XML presence, and artifact attestation status. This document does not create or mutate tags, GitHub Releases, release assets, registry images, or container signatures.

## Summary

`v2.5.0` is the minor release after the `v2.4.2` dependency-maintenance baseline. The release scope is enterprise readiness hardening for production-like review while preserving local developer convenience.

Recommended distribution: JAR/docs-first.

Container registry publication and container signing remain deferred to [`CONTAINER_REGISTRY_SIGNING_ROLLOUT.md`](CONTAINER_REGISTRY_SIGNING_ROLLOUT.md).

## Enterprise Readiness Improvements

- Containers default to the protected `prod` profile, so container-style startup no longer inherits permissive local/demo exposure.
- Prod/cloud-sandbox API-key mode is deny-by-default for non-`OPTIONS` `/api/**`, with documented public `GET /api/health`.
- `/proxy/**`, `/api/proxy/status`, `/v3/api-docs`, and Swagger UI remain protected in prod/cloud-sandbox API-key mode.
- OAuth2 application roles come from dedicated role claims, not ordinary scope-only claims.
- Scope-only `operator` or `admin` values do not silently grant `ROLE_operator` or `ROLE_admin`.
- Required allocation DTO fields reject omitted JSON instead of silently defaulting primitive values to `0`, `0.0`, or `false`.
- Release-candidate dry-run packet docs and scripts record build/test/package/SBOM/checksum/smoke posture under ignored `target/` output without publication.
- Production-candidate evidence gates separate automated CI evidence from manual reviewer/operator verification.
- IdP claim mapping examples document dedicated role-claim expectations without tenant IDs, secrets, or external IdP dependencies.
- Dependency/SAST risk workflow documents owner, severity, rationale, review date, remediation target, and high/critical no-silent-dismissal expectations.
- Container registry/signing rollout planning records the future registry, tag, digest, signing, scan, rollback, retention, and credential gates without publishing images.

## Operator Compatibility Notes

- Local/default mode remains intentionally permissive for developer demos and must not be exposed on public interfaces.
- Container/default deployment uses `prod` profile behavior and requires an operator-provided `LOADBALANCERPRO_API_KEY` for protected API-key mode.
- Prod/cloud-sandbox callers must send `X-API-Key` for protected API paths unless the endpoint is an explicitly documented public exception.
- OAuth2 integrations must map application roles through dedicated role claims such as `roles`, `role`, `authorities`, or `realm_access.roles`.
- Requests that omit enterprise-required allocation or evaluation fields should expect validation errors instead of defaulted numeric or boolean values.

## Verified Release Artifacts

The authorized semantic-tag Release Artifacts workflow produced and verified:

- `LoadBalancerPro-2.5.0.jar`
- `LoadBalancerPro-2.5.0-bom.json`
- `LoadBalancerPro-2.5.0-bom.xml`
- `LoadBalancerPro-2.5.0-SHA256SUMS.txt`
- GitHub artifact attestations from the semantic-tag Release Artifacts workflow
- GitHub Release assets produced by the approved workflow run

The release tag is `v2.5.0` and the release commit is `4cc03750be5479d9f8f88f8ef8014e05a8dc587a`.

No native installer, wrapper, launch4j, jpackage, native-image, self-extracting archive, vendored binary, Maven Central publication, registry image, Helm chart, Kubernetes manifest, Terraform module, or container signature is included in this release intent.

## Validation Evidence

The release evidence includes:

- `mvn -q clean test`
- `mvn -q verify`
- `mvn -q -DskipTests package`
- `git diff --check`
- `target/release-candidate-dry-run/release-candidate-dry-run-packet.md`
- `target/release-candidate-dry-run/release-candidate-dry-run-packet.json`
- `target/release-intent-review/release-intent-review.md`
- `target/release-intent-review/release-intent-review.json`
- latest matching CI, CodeQL, Dependency Review, Trivy, package, smoke, and SBOM evidence
- [`V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md`](V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md)
- [`V2_5_0_POST_RELEASE_VERIFICATION.md`](V2_5_0_POST_RELEASE_VERIFICATION.md)

Generated `target/` evidence is ignored local output and must not be committed.

## Known Limitations And Remaining Risks

- No production deployment certification is claimed.
- No real enterprise IdP tenant, client secret, or browser login/session UX is included.
- No live AWS sandbox validation is part of default Maven, CI, or release-intent evidence.
- No production TLS, IAM, ingress, WAF, rate limiting, secret rotation, backup, incident response, SLO, or compliance approval is provided by the application.
- Container publication, container signing, registry attestations, image rollback, and image retention remain future-gated.
- GitHub artifact attestations are provenance evidence from the release workflow, not PGP signing, notarization, vulnerability proof, or production certification.
- `release-downloads/` is ignored local verification output and must not be committed.

## Release Boundary

The completed release created only the authorized tag `v2.5.0` and the workflow-defined GitHub Release assets. It did not publish registry images, sign containers, push to registries, add secrets, add external services, or change runtime behavior.
