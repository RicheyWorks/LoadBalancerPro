# ADR-0007 Engineering Evidence And Reviewer Trust

## Status

Accepted. This decision supersedes the earlier planning-only, documentation-first reviewer trust model.

## Date

2026-08-09.

## Context

LoadBalancerPro needs reviewers to understand routing decisions, safety controls, and operational evidence without
turning documentation volume into a proxy for trust. The earlier model repeated boundaries across maps, templates,
campaign records, and exact-prose tests. That made documentation expensive while adding little independently
verifiable evidence.

## Decision

Trust is earned primarily through:

1. runtime behavior and secure defaults;
2. deterministic tests of behavior and invariants;
3. required CI, CodeQL, dependency review, SBOM, image scanning, and protected merge gates;
4. reproducible local or loopback operational evidence; and
5. concise documentation that identifies the evidence and its limitations.

Documentation describes evidence; it does not create evidence. Reviewer-facing material must distinguish observed
facts from inferred, synthetic, stale, unavailable, or hypothetical claims and retain provenance to the relevant
commit, test, workflow, or artifact.

Reviewer evidence should explain the decision considered, selected and rejected options, relevant signals and their
provenance, policy or safety mode, uncertainty, and required operator authority. It must not become hidden scoring,
routing, cloud-mutation, or production-promotion authority.

Safety remains strict where a mistake affects authentication, authorization, TLS, secrets, external targets, required
checks, vulnerable dependencies or images, destructive operations, durable state, concurrency bounds, failure
containment, or rollback. Documentation and campaign bookkeeping use proportional controls and must not block an
engineering change solely because obsolete wording or historical narration is missing.

## Consequences

- Engineering implementation and executable verification are the default work product.
- Reviewer navigation points to current source, tests, workflows, and artifacts instead of campaign history.
- Exact-prose, historical SHA/PR, repeated limitation, and cross-link-web tests are not part of the trust model.
- Human or operator authority remains necessary for production deployment, external systems, cloud mutation, and other
  explicitly controlled actions.
- Explainability and safety precede autonomy, but explanation output alone does not prove correctness or authorize
  traffic changes.

This decision changes governance and documentation-test coupling only. It adds no runtime behavior, storage, export,
network call, deployment action, or production-routing authority.
