# Release Readiness Decision Summary

This page gives reviewers the current two-track decision after the release-intent and container-rollout preparation work.

## Recommended Next Human Decision

`v2.5.0` has completed the JAR/docs-first release path. Review [`V2_5_0_POST_RELEASE_VERIFICATION.md`](V2_5_0_POST_RELEASE_VERIFICATION.md) for the exact tag, exact commit, release workflow, asset verification, checksum verification, SBOM JSON/XML presence, artifact attestation verification, and deferred container publication/signing status. The next human decision is whether the GitHub Release JAR/docs bundle is sufficient for distribution, or whether container distribution now needs a separately approved registry/signing implementation.

## Exact Version Recommendation

Released exact version: `v2.5.0`.

Recommended exact release version: `v2.5.0`.

Reason: the work since `v2.4.2` includes enterprise hardening with production-style compatibility impact, so a patch release would understate the change. A minor release is sufficient because local/default developer behavior, JAR/docs-first distribution, Java namespace, and public compatibility shims remain intact.

## Option 1: JAR/Docs-First Release

JAR/docs-first is sufficient when distribution means:

- executable Spring Boot JAR;
- CycloneDX SBOM JSON/XML;
- SHA-256 checksums;
- GitHub Release assets;
- GitHub artifact attestations from the semantic-tag Release Artifacts workflow;
- reviewer/operator docs and dry-run evidence.

Use [`RELEASE_NOTES_v2.5.0.md`](RELEASE_NOTES_v2.5.0.md) for reviewer-ready release notes, [`V2_5_0_POST_RELEASE_VERIFICATION.md`](V2_5_0_POST_RELEASE_VERIFICATION.md) for completed release evidence, [`RELEASE_INTENT_REVIEW.md`](RELEASE_INTENT_REVIEW.md) for the pre-release decision record, and [`V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md`](V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md) as the historical exact-version authorization gate that preceded the release.

## Option 2: Container Distribution Later

Container distribution adds a deployable image, registry consumption, immutable image digests, container scan evidence, signing or registry attestations, image rollback, and retention policy. It also adds operational cost: registry ownership, credential handling, signing identity governance, scan triage, promotion approvals, and consumer verification instructions.

Use [`CONTAINER_REGISTRY_SIGNING_ROLLOUT.md`](CONTAINER_REGISTRY_SIGNING_ROLLOUT.md) before treating container images as release artifacts.

## What Is Still Not Production-Certified

This is not Production-Certified.

Neither track certifies production deployment. Operators still need deployment-specific TLS, IAM, network policy, secret rotation, ingress controls, rate limiting, monitoring, logging retention, backup, rollback execution, incident response, SLOs, and legal/compliance approval.

No real enterprise IdP tenant, live AWS sandbox validation, container registry publication, container signing, Maven Central publication, native installer, or unmanaged public deployment is included.

## Current Recommendation

Use the `v2.5.0` JAR/docs-first GitHub Release when reviewers only need the executable JAR and evidence bundle. Defer container distribution until a focused implementation PR completes the registry/signing rollout gates.
