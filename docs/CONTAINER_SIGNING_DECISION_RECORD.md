# Container Signing And Publication Decision Record

Status: deferred, documented gate required before implementation.

For the concrete future implementation plan covering registry choice, image naming, tag/digest policy, signing/attestation options, vulnerability scan evidence, credential handling, rollback, retention, promotion, and approvals, use [`CONTAINER_REGISTRY_SIGNING_ROLLOUT.md`](CONTAINER_REGISTRY_SIGNING_ROLLOUT.md).

Use this record before proposing any container registry publication, release image tagging, container signing, or container attestation path. It is documentation only. It does not publish images, push to a registry, create tags, create GitHub Releases, mutate release assets, add secrets, or change workflows.

## Current Container Posture

- The repository includes a checked-in multi-stage `Dockerfile`.
- The image builds the Spring Boot JAR inside the build stage.
- The runtime stage runs as the non-root `loadbalancer` user.
- The Dockerfile includes a container-local `HEALTHCHECK` against `http://127.0.0.1:8080/api/health`.
- The Dockerfile defaults `SPRING_PROFILES_ACTIVE=prod`.
- Container/default deployment mode is protected by the prod API-key profile.
- Operators must provide `LOADBALANCERPRO_API_KEY` at run time for protected prod container usage.
- Docker build and runtime base images are pinned by digest.
- CI builds the image, runs a loopback-bound container health smoke, and runs Trivy image scanning for fixed high/critical findings.
- The documented local container recipes bind the host port to `127.0.0.1`.

## Current Non-Goals

- No container registry publication path exists in this repository today.
- No release image naming policy exists today.
- No container signing or cosign publication path exists today.
- No container attestation publication path exists today.
- No Kubernetes, Helm, Terraform, or cloud registry deployment path is created by the container docs.
- No real secrets should be baked into images, labels, Dockerfile layers, commands, or source-controlled evidence.

## Future Decision Gate

Do not implement container publishing or signing until a focused PR answers all of these questions:

- Registry target: which registry owns release images, and who controls access?
- Image name: what repository/image name is canonical for LoadBalancerPro?
- Image tag policy: which tags are mutable, which tags are immutable, and how do semantic tags map to image tags?
- Immutable digest policy: how will operators consume and pin image digests rather than trusting mutable tags alone?
- Signing approach: will the project use keyless signing, key-managed signing, GitHub artifact attestations, or another approved mechanism?
- Attestation scope: which artifact relationships are attested, and which claims are explicitly out of scope?
- Vulnerability scan evidence: which Trivy or equivalent scan result blocks release image publication, and where is the evidence retained?
- SBOM relationship: how does the Maven SBOM relate to the container image layers and OS packages?
- Rollback policy: how are known-good image digests retained and selected during rollback?
- Retention policy: how long are images, signatures, attestations, and scan evidence retained?
- Owner and approver: who owns registry permissions, signing identity, accepted risk, and emergency rollback decisions?
- Secret handling: which secret-management path supplies registry or signing credentials, if any, without source persistence?

## Minimum Evidence Before Publication

A future container publication/signing PR should include evidence for:

- Passing production-candidate evidence gate for the source revision.
- Passing CI, CodeQL, Dependency Review, and Trivy.
- Dockerfile base image digest review.
- Image digest captured after build.
- Vulnerability scan result for the exact image digest.
- Explicit mapping from Git commit and Maven version to image tag and digest.
- Signature or attestation verification instructions for consumers.
- Rollback instructions using immutable image digests.
- Retention expectations for image, signature, attestation, SBOM, and scan evidence.
- Confirmation that no real secrets were committed, logged, baked into the image, or persisted in evidence.

## Stop Conditions

Stop before publication or signing if any of these are true:

- The registry target or image naming policy is undecided.
- The image tag policy relies on mutable tags without digest verification.
- The signing or attestation approach is undecided.
- Trivy or equivalent scan evidence has unresolved blocking findings.
- Secret handling requires source-controlled credentials or persistent local secrets.
- Rollback or retention policy is missing.
- The workflow would publish from an unreviewed branch, mismatched source revision, or unverified build.
- The change also tries to mutate release assets, rulesets, default-branch settings, or unrelated release policy.

## Related Evidence

- [`CONTAINER_DEPLOYMENT.md`](CONTAINER_DEPLOYMENT.md)
- [`CONTAINER_REGISTRY_SIGNING_ROLLOUT.md`](CONTAINER_REGISTRY_SIGNING_ROLLOUT.md)
- [`PRODUCTION_CANDIDATE_EVIDENCE_GATE.md`](PRODUCTION_CANDIDATE_EVIDENCE_GATE.md)
- [`DEPLOYMENT_HARDENING_GUIDE.md`](DEPLOYMENT_HARDENING_GUIDE.md)
- [`../evidence/SUPPLY_CHAIN_EVIDENCE.md`](../evidence/SUPPLY_CHAIN_EVIDENCE.md)
- [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md)
