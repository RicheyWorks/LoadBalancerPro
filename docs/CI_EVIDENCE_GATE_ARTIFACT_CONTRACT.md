# CI Evidence Gate Artifact Contract

Status: local prototype contract. This document defines the source-visible JSON packet shape for future CI Evidence Gate review. It does not enable live CI enforcement, mutate required checks, mutate branch protection, call GitHub, call cloud services, publish artifacts, sign containers, or create release assets.

LoadBalancerPro is becoming the evidence, governance, and explainability layer for adaptive routing: a local flight simulator and black-box recorder for routing decisions. This contract defines the black-box recorder packet that a later CI/CD gate could consume or emit after the evidence schema and failure policy are approved.

## Purpose

The artifact summarizes local Enterprise Lab evidence into a deterministic reviewer handoff packet. It gives reviewers one compact JSON shape for:

- candidate local evidence inputs;
- pass/warn/fail-style readiness semantics;
- manual review steps;
- safety boundaries;
- not-proven boundaries;
- recommended next steps before any future gate is enforced.

The contract is schema-like documentation, not a JSON Schema runtime dependency.

## When It Would Be Used

Today the running app exposes the same static information through `/ci-evidence-gate.html` and `GET /api/enterprise-lab/ci-evidence-gate-summary`. Reviewers can compare those fields with the template in [`examples/ci-evidence-gate-summary.template.json`](examples/ci-evidence-gate-summary.template.json).

In a future sprint, a local parser or CI job could either:

- consume an artifact that follows this contract; or
- emit an artifact that follows this contract after reading approved local evidence.

That future work still needs explicit approval. This sprint does not generate artifact files and does not make the artifact a required check.

## Direction

The artifact is both a future input and a future output contract:

- **Input:** a future gate could read a local artifact to decide whether a change is ready for manual review.
- **Output:** a future gate could emit the same shape as a reviewer handoff summary after it validates approved local evidence.

The current template is a template only. It is not a generated run result, not a CI result, and not proof that evidence was produced.

## Required Top-Level Fields

| Field | Meaning |
| --- | --- |
| `artifactVersion` | Contract version. Current value: `ci-evidence-gate-artifact/v1`. |
| `artifactKind` | Artifact family. Current value: `ci-evidence-gate-summary-template` for the template and `ci-evidence-gate-summary` for the local endpoint. |
| `generatedBy` | Producer label. For templates use a template-only label; for future generated artifacts use the approved local producer name. |
| `generatedAtPolicy` | Timestamp policy. Templates must use `TEMPLATE_ONLY_NO_TIMESTAMP`; future generated artifacts need an approved deterministic or CI-provided timestamp policy. |
| `mode` | Review mode, such as `prototype/local-review`. |
| `decision` | Local review decision value. See allowed decisions below. |
| `enforcementStatus` | Enforcement state. See allowed statuses below. |
| `evidenceInputs` | Array of local evidence input descriptors. |
| `readinessChecks` | Array of pass/warn/fail-style checks. |
| `manualReviewSteps` | Array of reviewer actions that remain manual today. |
| `safetyBoundaries` | Array of actions and behaviors the prototype must not perform. |
| `notProvenBoundaries` | Array of proof boundaries that the artifact must not claim as completed. |
| `recommendedNextSteps` | Array of next actions for local review or future implementation. |

Optional helper fields may include `gateName`, `dashboardPath`, `apiPath`, `artifactContract`, `artifactTemplatePath`, `linkedReviewerPages`, `adaptiveRoutingScenarioSummary`, `adaptiveRoutingScenarioDetail`, and `adaptiveRoutingScenarioEvidencePacket` when they improve reviewer navigation or future gate parser design.

## Field Details

### `evidenceInputs`

Each entry should include:

- `name`: human-readable evidence input name.
- `localEvidencePath`: ignored local `target/` path or directory.
- `localProducer`: source-visible local command or producer label.
- `reviewPurpose`: what a reviewer should learn from that evidence.
- `templateValuePolicy`: only for templates; explains that the entry is a placeholder and not a produced run result.

Local evidence paths must stay under ignored `target/` directories. The contract must not point to `release-downloads/`, private endpoints, cloud accounts, local absolute paths, or generated tracked files.

The adaptive routing scenario runner may appear as an optional future evidence input through `/adaptive-routing-scenarios.html`, `GET /api/enterprise-lab/adaptive-routing-scenario-summary`, `GET /api/enterprise-lab/adaptive-routing-scenario-detail`, `GET /api/enterprise-lab/adaptive-routing-scenario-evidence-packet`, and the reserved ignored paths `target/adaptive-routing-scenarios/adaptive-routing-scenario-summary.json` and `target/adaptive-routing-scenarios/adaptive-routing-scenario-evidence-packet.json`. That runner output is deterministic selected-server distribution, explanation drilldown, and reviewer packet-shape evidence from synthetic local inputs; it is not a production benchmark, live traffic validation, or generated artifact result in this sprint.

### `readinessChecks`

Each entry should include:

- `name`
- `state`
- `summary`
- `futureGateMeaning`

Allowed states are:

- `PASS_STYLE`
- `WARN_STYLE`
- `FAIL_STYLE_BLOCKER`

These are review semantics only until a later sprint explicitly implements enforcement.

## Allowed Decision Values

- `TEMPLATE_ONLY`
- `READY_FOR_LOCAL_REVIEW`
- `NEEDS_MANUAL_REVIEW`
- `BLOCKED_FOR_LOCAL_REVIEW`

Templates should use `TEMPLATE_ONLY`. The local prototype endpoint may use `READY_FOR_LOCAL_REVIEW` because it is a deterministic local orientation surface, not a live gate result.

## Allowed Enforcement Status Values

- `NOT_ENFORCED`
- `DOCUMENTATION_ONLY`
- `LOCAL_REVIEW_ONLY`

Do not use values that imply merges are blocked, branch protection changed, required checks were mutated, or GitHub governance settings were applied.

## Manual Review Expectations

Reviewers should confirm:

- latest CI and CodeQL passed for the exact commit under review;
- the local prototype still reports `NOT_ENFORCED`;
- generated evidence, if produced, stays under ignored `target/` paths;
- no generated `target/` evidence is committed;
- `release-downloads/` was not changed;
- no branch protection, rulesets, required checks, GitHub settings, secrets, or environments changed;
- no release, tag, registry publication, container signing, cloud mutation, private-network validation, real tenant validation, or real enterprise IdP validation occurred.

## Not-Proven Boundaries

The artifact must not claim:

- production certification;
- production performance proof;
- production SLO/SLA proof;
- live-cloud validation;
- real tenant validation;
- real enterprise IdP validation;
- signed-container proof;
- registry publish completion;
- GitHub governance-applied proof.

## Safety Boundaries

The current prototype and any future parser must stay inside these boundaries unless a separate approved sprint changes them:

- no live CI gate enforcement;
- no branch protection mutation;
- no required-check mutation;
- no GitHub settings, rulesets, secrets, or environment mutation;
- no release, tag, or GitHub Release creation;
- no registry login or publish;
- no container signing;
- no live cloud or private-network validation;
- no real tenant or real enterprise IdP validation;
- no environment variable or secret reads;
- no process execution;
- no external network calls;
- no filesystem mutation from the read-only endpoint.

## Future CI Gate Integration Path

A future implementation can build on this contract in small approved steps:

1. Keep the template and documentation as the source-visible contract.
2. Add a local validator that reads an explicit artifact path under `target/` only.
3. Teach the validator to reject malformed fields, non-`target/` evidence paths, and unsafe proof claims.
4. Add CI execution only after runtime cost, failure policy, and reviewer workflow are approved.
5. Consider required-check or branch-protection changes only as a separate manual governance action.

## Reviewer Checklist

- Open `/enterprise-lab-reviewer.html`.
- Follow the CI Evidence Gate Prototype link to `/ci-evidence-gate.html`.
- Confirm the page shows `prototype/local-review` and `NOT_ENFORCED`.
- Compare the endpoint fields with this contract.
- Inspect [`examples/ci-evidence-gate-summary.template.json`](examples/ci-evidence-gate-summary.template.json) as a template only.
- Open `/evidence-timeline.html` for run sequence context.
- Open `/evidence-export-packet.html` for the reviewer handoff packet.
- Confirm all not-proven boundaries remain explicit.

## Example Validation Checklist

For the template or a future local artifact, validate that:

- JSON parses successfully;
- `artifactVersion` is `ci-evidence-gate-artifact/v1`;
- `artifactKind` matches the expected artifact family;
- `mode` is local or prototype-scoped;
- `enforcementStatus` is `NOT_ENFORCED`, `DOCUMENTATION_ONLY`, or `LOCAL_REVIEW_ONLY`;
- every `localEvidencePath` starts with `target/`;
- no field contains real secrets, tokens, tenant IDs, private endpoints, customer data, registry digests, signatures, cloud resource identifiers, or GitHub check IDs;
- no field claims live enforcement, branch-protection mutation, required-check mutation, production certification, SLO/SLA proof, live-cloud validation, real tenant proof, real enterprise IdP proof, signed-container proof, registry publish completion, or governance-applied proof;
- any generated artifact is ignored under `target/` and is not committed.
