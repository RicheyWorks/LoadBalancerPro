# Decision Explorer Architecture Bootstrap Campaign Board

## Campaign

- Campaign name: Decision Explorer Architecture Bootstrap Campaign.
- Classification: WARN / decision-explorer-bootstrap.
- Goal: convert the next-level architecture skeleton into a repo-native, verifiable Decision Explorer architecture, contract, evidence, and planning system.
- Scope: docs/test-only bootstrap unless a later separately scoped PR explicitly authorizes implementation work.
- Baseline prerequisite: PR #334 imported the next-level architecture skeleton at `docs/architecture/LOADBALANCERPRO_NEXT_LEVEL_ARCHITECTURE.md`.
- Operating model: one scoped PR at a time, one open campaign PR at a time, no stacked feature branches, and no slot counted complete until the PR is merged and post-merge main CI/CodeQL are green.

The Interactive Decision Explorer is a planned reviewer/operator and agent-readable explanation surface for routing decisions. During this bootstrap campaign it remains read-only and simulation-only. This board does not create endpoints, runtime behavior, storage/export behavior, automation, deployment behavior, cloud behavior, tenant behavior, production defaults, or production traffic-control behavior.

## Hard Boundaries

- Keep the Decision Explorer read-only and simulation-only until a later implementation campaign proves otherwise.
- Do not add live mutation; no live mutation, no autonomous production action, real cloud calls, tenant targets, secrets, credentials, external targets, or production-looking defaults.
- Do not change CI/workflow, Maven config, Docker, Compose, runtime resources, endpoints, scripts, deployment, or automation in this campaign.
- Do not claim production readiness, production certification, live-cloud validation, real-tenant validation, load/stress/benchmark proof, throughput/p95/p99 production evidence, real-world latency improvement, replay/export/storage proof, or broader automation.
- Treat architecture, schema, and implementation plans as planning contracts until code is implemented and verified in separate scoped PRs.

## Phase List

1. DX-G01 - campaign index and board.
2. DX-G02 - README and Reviewer Trust Map links.
3. DX-G03 - ADR for Interactive Decision Explorer architecture.
4. DX-G04 - Decision Explorer data contract.
5. DX-G05 - agent-readable schema contract.
6. DX-G06 - evidence lane and source cards.
7. DX-G07 - Phase 0 verification guardrails.
8. DX-G08 - implementation plan.
9. DX-G09 - reviewer walkthrough.
10. DX-G10 - bootstrap closeout.

## Status Table

| Slot | Title | Branch | PR | Head SHA | Status | Evidence / guard | Checkpoint |
| --- | --- | --- | --- | --- | --- | --- | --- |
| DX-G01 | Campaign index and board | `codex/dx-g01-campaign-index-and-board` | [#348](https://github.com/RicheyWorks/LoadBalancerPro/pull/348) | pending | PR-opened | `AgentDecisionExplorerCampaignBoardDocumentationTest` | Create campaign board and index entry |
| DX-G02 | README and Reviewer Trust Map links | `codex/dx-g02-readme-trust-map-links` | pending | pending | planned | `AgentDecisionExplorerReadmeTrustMapDocumentationTest` | Reviewer-facing discovery |
| DX-G03 | ADR for Decision Explorer architecture | `codex/dx-g03-adr-decision-explorer-architecture` | pending | pending | planned | `Adr0010DecisionExplorerArchitectureDocumentationTest` | Architecture decision record |
| DX-G04 | Decision Explorer data contract | `codex/dx-g04-decision-explorer-data-contract` | pending | pending | planned | `AgentDecisionExplorerDataContractDocumentationTest` | Versioned payload planning |
| DX-G05 | Agent-readable schema contract | `codex/dx-g05-agent-readable-schema-contract` | pending | pending | planned | `AgentDecisionExplorerAgentSchemaDocumentationTest` | Agent parseability contract |
| DX-G06 | Evidence lane and source cards | `codex/dx-g06-evidence-lane-and-source-cards` | pending | pending | planned | `AgentDecisionExplorerEvidenceLaneDocumentationTest` | Research and evidence intake |
| DX-G07 | Phase 0 verification guardrails | `codex/dx-g07-phase0-verification-guardrails` | pending | pending | planned | `AgentDecisionExplorerPhase0VerificationGateDocumentationTest` | Pre-implementation gate |
| DX-G08 | Decision Explorer implementation plan | `codex/dx-g08-decision-explorer-implementation-plan` | pending | pending | planned | `AgentDecisionExplorerImplementationPlanDocumentationTest` | Future implementation slices |
| DX-G09 | Reviewer walkthrough | `codex/dx-g09-reviewer-walkthrough` | pending | pending | planned | `AgentDecisionExplorerReviewerWalkthroughDocumentationTest` | Human and agent walkthrough |
| DX-G10 | Bootstrap closeout | `codex/dx-g10-bootstrap-closeout` | pending | pending | planned | `AgentDecisionExplorerBootstrapCloseoutDocumentationTest` | Close bootstrap and prepare implementation campaign |

## Checkpoint Rules

- Update the status table only with factual state observed in the repository or GitHub.
- Use `pending` for unknown PR, head SHA, merge commit, or check state.
- Use `active-local` only for the currently scoped local branch before PR creation.
- Use `PR-opened` only after a PR URL exists.
- Use `merged-awaiting-main-green` only after merge and before main CI/CodeQL are both green.
- Use `merged-main-green` only after the merge commit's main CI and CodeQL are both completed successfully.
- Do not mark a later DX slot active while an earlier DX slot is open, pending checks, merged-awaiting-main-green, blocked, or unresolved.
- Do not treat a skipped-only or duplicate-only check as satisfying a required check.

## Verification Expectations

Each DX slot should run:

- focused documentation guard for the slot;
- relevant documentation selector bundle;
- `mvn -q test`;
- `mvn -q "-DskipTests" package`;
- `mvn -B package`;
- `git diff --check`;
- `git diff --cached --check`;
- `git diff --check origin/main...HEAD`;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package`;
- current-head PR checks before merge;
- post-merge main CI/CodeQL before counting the slot complete.

## Closeout Criteria

The bootstrap campaign is complete only when:

- PR #334 has a factual resolved outcome recorded in the final closeout.
- DX-G01 through DX-G10 each have a PR URL, branch, head SHA, merge commit, and verification summary.
- DX-G10 is merged and post-merge main CI/CodeQL are green.
- The closeout preserves read-only, simulation-only, and not-proven boundaries.
- The recommended next campaign is Decision Explorer Implementation Phase 1.

## Remaining Not-Proven Boundaries

This campaign board does not prove:

- no production readiness;
- no production certification;
- no live-cloud validation;
- no real-tenant validation;
- no runtime enforcement beyond separately implemented and verified code paths;
- no load/stress/benchmarking evidence;
- no throughput/p95/p99 production evidence;
- no real-world latency improvement;
- no replay/evidence/report/storage/export proof;
- no broader automation.
