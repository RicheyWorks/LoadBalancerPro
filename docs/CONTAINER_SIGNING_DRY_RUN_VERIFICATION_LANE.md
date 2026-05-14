# Container Signing Dry-Run Verification Lane

## Verdict

This is a **dry-run verification lane** for future container distribution and signing evidence.

- No container is published.
- No container is signed.
- No release, tag, or GitHub Release is created.
- No registry credentials, signing keys, or secrets are required.
- This does not make LoadBalancerPro production certified.
- LoadBalancerPro remains not enterprise-production ready.

This lane is reviewer evidence planning and local verification guidance only. It prepares checks that can run before any future registry publication or signing approval.

## Purpose

This lane produces reviewer-visible evidence before any future publish/signing approval:

- image buildability;
- local image identity;
- digest capture where available;
- SBOM and scanning command expectations;
- signing command planning;
- verification command planning;
- evidence retention expectations;
- rollback and removal planning.

## Current Prerequisites

For local dry-run review:

- Java and Maven are required for test and package verification.
- A Docker-compatible daemon is required for container build and inspect steps because this repo has a checked-in [`../Dockerfile`](../Dockerfile).
- No registry login is required.
- No signing keys are required.
- Optional scanner tooling, such as Trivy or Syft, is needed only if a reviewer chooses to run optional local image scanning or image SBOM checks.

## Current Safe Local Verification

These commands are safe current checks and do not publish or sign:

```bash
mvn -q test
mvn -q -DskipTests package
git diff --check
```

The current Maven/CycloneDX SBOM command used by CI is:

```bash
mvn -B org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom -DoutputFormat=all -DoutputDirectory=target -DoutputName=bom -Dcyclonedx.skipAttach=true
```

If Docker is available, reviewers can run local-only image commands:

```bash
docker build -t loadbalancerpro:dry-run .
docker image inspect loadbalancerpro:dry-run
docker image ls loadbalancerpro
```

The existing CI smoke path also proves a loopback-bound runtime check. A local reviewer may run the same shape only if Docker is available and the container is kept local:

```bash
docker run --rm -d --name loadbalancerpro-dry-run -p 127.0.0.1:18081:8080 loadbalancerpro:dry-run
curl -fsS http://127.0.0.1:18081/api/health
docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' loadbalancerpro-dry-run
docker stop loadbalancerpro-dry-run
```

Cleanup is local-only:

```bash
docker image rm loadbalancerpro:dry-run
```

Do not run `docker push`, registry login, signing commands, release commands, tag commands, live cloud commands, or private-network validation as part of this lane.

## CI Dry-Run Evidence Artifact

The normal CI workflow now uploads a no-publish/no-sign dry-run evidence artifact after the local Docker runtime smoke passes and the local image scan step runs.

- Artifact name: `container-dry-run-evidence-no-publish-no-sign`.
- Evidence directory in CI: `target/container-dry-run-evidence/`.
- Source image tag: `loadbalancerpro:ci`.
- Local dry-run image tag: `loadbalancerpro:ci-dry-run-<source-commit-sha>`.

Expected evidence files:

- `dry-run-summary.md`
- `docker-version.txt`
- `image-inspect.json`
- `image-history.txt`
- `image-list.txt`
- `image-id.txt`
- `repo-digests.json`
- `image-labels.json`
- `image-entrypoint.json`
- `image-cmd.json`
- `image-exposed-ports.json`
- `trivy-summary.txt`

What the artifact proves:

- CI built a local container image from the checked-in Dockerfile.
- CI ran the loopback-bound Docker runtime smoke before evidence capture.
- CI captured local image identity, configuration, history, and Docker environment details.
- CI recorded both the source commit SHA and the workflow SHA so pull-request merge refs are reviewable without ambiguity.
- CI ran the configured Trivy image scan and stored its table output when the scan step completed.

What the artifact does not prove:

- It does not prove registry publication.
- It does not prove container signing.
- It does not prove registry attestation, published digest retention, or rollback readiness.
- It does not prove production certification, production SLO/SLA behavior, live cloud validation, private-network validation, or real tenant proof.

The artifact is local-only image evidence. No registry login is performed, no registry credentials are used, no container is published, no container is signed, and no release, tag, or GitHub Release is created.

## Dry-Run Image Identity Evidence

For local review, capture:

- source commit SHA;
- local image tag, for example `loadbalancerpro:dry-run`;
- local image ID;
- `RepoDigests`, if present;
- note that `RepoDigests` may be empty until an image is pushed to a registry;
- image labels, if present;
- created timestamp;
- entrypoint and command;
- exposed ports;
- local smoke result, if the loopback smoke was run safely.

## SBOM And Vulnerability Scanning Dry-Run

Current supported SBOM evidence:

- Maven/CycloneDX SBOM generation for the JAR and dependency graph.
- CI-uploaded `loadbalancerpro-sbom` workflow artifacts.

Optional local image evidence, pending reviewer tooling availability:

```bash
trivy image --severity HIGH,CRITICAL --ignore-unfixed loadbalancerpro:dry-run
trivy image --format cyclonedx --output target/container-dry-run-sbom.json loadbalancerpro:dry-run
syft loadbalancerpro:dry-run -o cyclonedx-json=target/container-dry-run-sbom-syft.json
```

These optional commands do not require registry publication. They are local dry-run checks only and may be skipped if the tools are unavailable.

## Signing Dry-Run Planning

Do not sign anything in this sprint.

The following are future examples only. They are not run in this sprint, require approval, and require an OIDC or key-management decision first:

```bash
cosign sign --yes <approved-image-ref>@sha256:<digest>
cosign verify <approved-image-ref>@sha256:<digest>
cosign attest --predicate image-sbom.json <approved-image-ref>@sha256:<digest>
cosign verify-attestation <approved-image-ref>@sha256:<digest>
```

Any real signing path must be added only after registry target, image naming, signing approach, OIDC/key-management, CI workflow, evidence retention, rollback/removal, and maintainer approval gates are satisfied.

## Evidence Output Template

Use this template for future local dry-run evidence. Store generated evidence under ignored `target/` output unless a future approved PR chooses a different evidence-retention path.

```markdown
# Container Signing Dry-Run Evidence

- Commit SHA:
- Local build command:
- Image tag:
- Image ID:
- RepoDigests, if available:
- RepoDigests absent because image was not pushed?:
- SBOM command:
- SBOM output path:
- Vulnerability scan command:
- Vulnerability scan result:
- Local smoke command:
- Local smoke result:
- Cleanup command:
- Cleanup result:
- Non-publish statement: no registry push was performed.
- Non-sign statement: no container signing was performed.
```

## CI Integration Recommendation

The current CI artifact lane captures reviewer-visible pre-publish/pre-sign evidence without registry login, registry push, signing, release creation, or secrets.

Future improvements may add image SBOM output or richer scan summaries, but must preserve this shape:

- build a local image;
- inspect the local image;
- run Trivy scan;
- generate Maven SBOM and optional image SBOM;
- upload dry-run evidence as workflow artifacts;
- do not log in to a registry;
- do not push;
- do not sign;
- do not require secrets.

## Non-Goals

- No registry publish.
- No signing.
- No release, tag, or GitHub Release.
- No production certification.
- No enterprise-production-ready claim.
- No live AWS or cloud validation.
- No real tenant or IdP proof.
- No private-network validation.
- No secrets committed.

## Approval Gates Before Real Execution

Before any real publish/signing execution, maintainers must approve:

- registry target;
- image naming and versioning;
- signing approach;
- OIDC and key-management model;
- CI workflow;
- evidence retention;
- rollback and removal plan;
- maintainer approval for the exact execution path.

## Reviewer Checklist

- [ ] Local Maven verification passed.
- [ ] Local Docker dry-run passed or unavailable environment documented.
- [ ] Image identity captured.
- [ ] SBOM and scanning plan documented.
- [ ] Signing commands clearly marked future examples and not run.
- [ ] No publish performed.
- [ ] No signing performed.
- [ ] No secrets used.
- [ ] No production-certification language.

## Safety Boundary

LoadBalancerPro remains **Enterprise Lab ready**, **not production certified**, and **not enterprise-production ready**.

This is a dry-run only lane. It does not publish containers, sign containers, create releases, create tags, create GitHub Releases, mutate GitHub settings, mutate cloud resources, perform private-network validation, prove real tenant identity, or certify production SLO/SLA behavior.
