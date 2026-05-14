# Release-Candidate Dry-Run Packet

This packet is the reviewer/operator path for proving what the current `main` revision would carry into a release-candidate review without publishing anything. It records the commit under review, local build and smoke evidence, CI/security gate posture, SBOM/checksum posture, and remaining blockers before any real release or container publication is approved.

This is not a release procedure. It does not create tags, GitHub Releases, release assets, registry images, container signatures, cloud resources, or files under `release-downloads/`.

Use this packet with [`PRODUCTION_CANDIDATE_EVIDENCE_GATE.md`](PRODUCTION_CANDIDATE_EVIDENCE_GATE.md), [`PRODUCTION_READINESS_SUMMARY.md`](PRODUCTION_READINESS_SUMMARY.md), [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md), and [`RELEASE_INTENT_CHECKLIST.md`](RELEASE_INTENT_CHECKLIST.md).

## Output Location

Generated packet evidence belongs only under ignored build output:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\release-candidate-dry-run-packet.ps1 -DryRun
```

The safe default writes:

- `target/release-candidate-dry-run/release-candidate-dry-run-packet.md`
- `target/release-candidate-dry-run/release-candidate-dry-run-packet.json`

For a fuller local packet after normal validation is already expected to be safe on the workstation, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\release-candidate-dry-run-packet.ps1 -Package
```

`-Package` runs local Maven build/package/SBOM/checksum evidence and local smoke commands only. It still does not publish releases or containers, does not require secrets, and does not mutate `release-downloads/`.

Full `mvn -q clean test` and `mvn -q verify` remain explicit validation commands for the reviewer workflow. The packet script records those command names and marks them as externally validated unless `-IncludeValidation` is supplied. Use `-IncludeValidation` only when intentionally allowing the packet script to re-run the heavier local Maven validation inside the generator.

## Packet Contents

The packet must record:

- Commit under review from `git rev-parse HEAD`.
- Branch and working-tree status from local Git.
- Full test command: `mvn -q clean test`.
- Verification command: `mvn -q verify`.
- Package command: `mvn -q -DskipTests package`.
- CycloneDX SBOM generation path and locally generated SBOM files when `-Package` is used.
- SHA-256 checksum generation and verification path for the locally built JAR and SBOM files.
- GitHub artifact attestation posture: tag-triggered release workflow creates release JAR provenance and JAR/SBOM relationship attestations; local dry runs do not create attestations.
- CodeQL status posture: check the latest successful `main` CodeQL workflow run for the same commit before calling the revision release-ready.
- Dependency Review status posture: check the required PR gate and any dependency/SAST owner decisions for the same commit.
- Trivy/container scan posture: CI Docker image smoke and Trivy scan are the current automated evidence path.
- Docker base digest pinning posture: Dockerfile base image references are expected to remain digest-pinned.
- Container default prod profile posture: Dockerfile defaults to the protected prod profile and requires a runtime `LOADBALANCERPRO_API_KEY` for protected prod container use.
- Prod API-key deny-by-default posture: prod/cloud-sandbox API-key mode requires `X-API-Key` for non-`OPTIONS` `/api/**` except documented `GET /api/health`.
- OAuth2 dedicated role claim posture: application roles come from dedicated role claims, while standard scope-only claims do not grant app roles.
- DTO omitted-field validation posture: required allocation/evaluation request fields reject omitted JSON instead of defaulting to primitive values.
- Postman enterprise lab dry-run command and optional local evidence path.
- Operator run profiles dry-run command and optional packaged-jar security smoke result.
- Confirmation that no tags, GitHub Releases, release assets, registry images, container signatures, secrets, external services, cloud resources, or `release-downloads/` mutations were performed.

## Manual Or Operator-Verified Items

The script can gather local evidence, but these items remain manual/operator verified for the exact commit:

- Latest `main` CI run passed for the commit under review.
- Latest CodeQL workflow passed for the commit under review.
- Dependency Review and dependency/SAST risk-owner workflow have no unowned high or critical findings.
- Trivy image scan evidence is present from CI or an approved local scan path.
- GitHub artifact attestations exist only after an approved semantic-tag release workflow run.
- GitHub Release assets exist only after an approved semantic-tag release workflow run.
- Container registry publication/signing remains deferred until [`CONTAINER_SIGNING_DECISION_RECORD.md`](CONTAINER_SIGNING_DECISION_RECORD.md) is completed and implemented.
- `release-downloads/` was not needed for this dry run and remains untouched.
- No real tenant IDs, client secrets, API keys, bearer tokens, credentials, or private-network targets were added to evidence.

## Runtime Smoke Posture

Use local loopback-only smoke paths:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\operator-run-profiles-smoke.ps1 -DryRun
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\postman-enterprise-lab-safe-smoke.ps1 -DryRun
```

The packaged-jar security smoke, when run by the packet script with `-Package`, uses loopback-bound local/prod/proxy profiles and verifies:

- Local/demo health and root page remain available for developer convenience.
- Prod API-key mode rejects protected status without `X-API-Key`.
- Prod API-key mode allows protected status with the operator-provided local test key.
- Proxy-loopback smoke uses only `127.0.0.1` backends started by the script.

Do not run live private-network validation, DNS probing, discovery, scanning, registry publication, release publication, or container signing as part of this packet.

## Container Registry And Signing Recommendation

The current release path can remain GitHub Release artifact-based when distribution is JAR/docs-first and the semantic-tag release workflow is approved separately.

Registry publication and container signing become required if distribution depends on deployable container images. Before that path is implemented, require:

- Registry target decision.
- Image naming and immutable tag/digest policy.
- Vulnerability scan evidence location.
- Signing and attestation approach.
- Rollback and retention policy.
- Credential handling plan.

No container publication or container signing is performed by this dry-run packet.

## Remaining Release Blockers

Actual release/publication remains blocked until:

- A separate release-intent request authorizes the real release path.
- Semantic-tag release workflow evidence exists and is verified for the exact version being distributed.
- Any required GitHub artifact attestation and GitHub Release asset evidence is produced by the approved release workflow.
- Container publication/signing decisions are completed if container distribution is required.
- Operator/deployment controls for TLS, IAM, network boundaries, secret rotation, monitoring, rate limiting, rollback, retention, and incident response are accepted for the target environment.

## Reviewer Checklist

| Evidence item | Source | Expected result | Status |
| --- | --- | --- | --- |
| Commit under review | Packet output | Exact commit recorded |  |
| Full tests | `mvn -q clean test` | Pass |  |
| Verify | `mvn -q verify` | Pass |  |
| Package | `mvn -q -DskipTests package` | Pass |  |
| SBOM | CycloneDX output | JSON/XML present for local packet or CI artifact reviewed |  |
| Checksums | SHA-256 output | JAR/SBOM hashes recorded and verified |  |
| CodeQL | GitHub Actions | Latest matching commit successful |  |
| Dependency Review | PR/check evidence | No unowned blocking findings |  |
| Trivy | CI container scan | High/critical fixed findings handled |  |
| Docker digest pinning | Dockerfile | Base images pinned by digest |  |
| Container prod default | Dockerfile/docs | Prod profile default and API key requirement confirmed |  |
| Prod API-key boundary | Tests/smoke/docs | Non-`OPTIONS` `/api/**` protected except `GET /api/health` |  |
| OAuth2 role claims | Tests/docs | Dedicated role claims only for app roles |  |
| DTO validation | Tests/docs | Omitted required values fail validation |  |
| Postman dry run | Smoke script | Dry-run path reviewed or passed |  |
| Operator run profiles | Smoke script | Dry-run or packaged local smoke passed |  |
| Publication boundary | Packet output | No release or container publication performed |  |
| `release-downloads/` | Git status | Preserved and not required |  |
