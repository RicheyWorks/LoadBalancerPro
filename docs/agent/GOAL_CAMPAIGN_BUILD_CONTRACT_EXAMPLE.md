# Goal Campaign Build Contract Example

This filled example shows how BUILD_CONTRACT.md can be used for the **LoadBalancerPro Goal Mode 10-PR Trial**. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this example with [`BUILD_CONTRACT.md`](../../BUILD_CONTRACT.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`SESSION_MANAGER.md`](SESSION_MANAGER.md), [`FAILURE_LOG.md`](FAILURE_LOG.md), and [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md).

## Goal

Build and execute a bounded 10-PR `/goal` campaign that exercises long-running Codex work through small, separately scoped docs/test-only PRs. Stop after 10 successfully merged PRs with green post-merge main checks, or pause if scope, safety, verification, GitHub state, or human decision-making requires it.

## Context And Constraints

- Campaign name: LoadBalancerPro Goal Mode 10-PR Trial.
- Start from clean main for each slot after the prior slot is merged and main CI/CodeQL are green.
- Work one scoped PR at a time.
- Prefer docs/test-only changes.
- Allowed default files: README.md, AGENTS.md, BUILD_CONTRACT.md, docs/agent/*, docs/REVIEWER_TRUST_MAP.md, and documentation guard tests.
- Forbidden unless explicitly separately approved: src/main/java, Maven config, CI/workflow files, Dockerfile, Compose behavior, app behavior, endpoints, k6 behavior, Bruno behavior, Toxiproxy behavior, scripts, runtime resources, runner services, automation, secrets, external/cloud/tenant targets, and unsupported production/readiness/performance/runtime-enforcement/replay/storage/export claims.
- Preserve the Advanced README / public trust surface, AGENTS.md operating rules, BUILD_CONTRACT.md execution contract, and docs/agent operational scaffolding.

## Deliverables

- One branch per PR slot.
- One PR per slot.
- One new or updated documentation guard test per slot.
- Campaign board updates that record slot status, PR URL, head SHA, merge SHA, and post-merge main status.
- SESSION_MANAGER.md updates after every checkpoint.
- FAILURE_LOG.md entries for every local, remote, scope-audit, or merge-gate failure.
- Final report after 10 merged PRs or a required pause.

## Slot Plan

| Slot | Intended Deliverable | Default Scope |
| --- | --- | --- |
| 1 | Goal campaign template architecture | docs/agent templates and guard |
| 2 | Initialize goal campaign board for this trial | board/session docs and guard |
| 3 | Filled BUILD_CONTRACT example for this campaign | this example, board/session docs, guard |
| 4 | SESSION_MANAGER campaign checkpoint examples | session/checkpoint docs and guard |
| 5 | FAILURE_LOG campaign recovery examples | failure/recovery docs and guard |
| 6 | VERIFICATION_PROTOCOL campaign mode refinement | verification docs and guard |
| 7 | README goal-mode campaign summary | README and guard |
| 8 | Reviewer Trust Map goal-mode campaign navigation | reviewer map and guard |
| 9 | AGENTS.md campaign discipline section | AGENTS.md and guard |
| 10 | Goal-mode trial final handoff/report | final handoff docs and guard |

## Verification Requirements

Use focused checks while editing. Use full verification before opening a merge decision.

Each slot should run:

- `git status`;
- `git diff --name-status origin/main...HEAD`;
- `git diff --stat origin/main...HEAD`;
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"`;
- the focused guard test for the slot;
- the relevant focused selector bundle;
- `mvn -q test`;
- `mvn -q "-DskipTests" package`;
- `mvn -B package`;
- `git diff --check`;
- `git diff --check origin/main...HEAD`;
- `git diff --cached --check`;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package`.

Remote verification must confirm Build/Test/Package/Smoke, Analyze Java / CodeQL, and Dependency Review where applicable. Failed/cancelled/stale/pending required checks are not acceptable, and duplicate-only checks do not count as green.

## Evidence And Reporting

For every slot, record:

- branch;
- PR URL;
- head SHA;
- changed files;
- focused verification;
- full local verification;
- remote PR check status;
- merge status;
- post-merge main head;
- main CI/CodeQL status;
- scope/safety audit;
- remaining not-proven boundaries;
- next action.

## Stop Conditions

Pause the campaign if:

- main becomes red;
- a required check fails and cannot be fixed safely;
- scope requires production behavior changes;
- scope requires secrets, external targets, cloud/tenant targets, or production-looking defaults;
- scope requires CI/Maven/Docker/Compose/runtime changes not explicitly allowed;
- GitHub check state is ambiguous;
- Codex is uncertain whether to continue;
- human approval is needed;
- 10 PRs are merged.

## Scope Boundaries

This example does not authorize production code changes, Maven config changes, CI/workflow changes, Dockerfile changes, Compose behavior changes, runtime behavior changes, endpoint changes, k6/Bruno/Toxiproxy behavior changes, scripts, runner services, automated execution, secrets, external/cloud/tenant targets, or unsupported claims.

## Not-Proven Boundaries

This campaign example does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof unless implemented and verified, or broader automation.

## Final Report Format

- Overall Classification: PASS / WARN / FAIL
- Campaign Goal
- Elapsed Time
- PRs Attempted
- PRs Merged
- Open PRs
- Blocked PRs
- Current Main HEAD
- Main CI/CodeQL
- Session Manager Updated
- Failures Logged
- Verification Summary
- Scope/Safety Audit
- Remaining Not-Proven Boundaries
- Recommended Next Goal
