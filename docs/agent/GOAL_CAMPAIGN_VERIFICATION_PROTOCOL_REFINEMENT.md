# Goal Campaign Verification Protocol Refinement

This refinement explains how to apply VERIFICATION_PROTOCOL.md during the LoadBalancerPro Goal Mode 10-PR Trial. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this refinement with [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`SESSION_MANAGER.md`](SESSION_MANAGER.md), and [`FAILURE_LOG.md`](FAILURE_LOG.md).

## Purpose

A campaign slot is not green because one command passed, a PR opened, or an older remote run succeeded. Each slot must advance through the same verification ladder on the current branch head, then through current-head remote checks, merge, and post-merge main checks.

This refinement keeps the campaign on one scoped PR at a time and makes verification claims depend on the current head SHA.

## Campaign Verification Order

For each PR slot, follow this order:

1. Start from clean main after the prior slot is merged and main CI/CodeQL are green.
2. Create the slot branch and confirm scope before editing.
3. Run the focused documentation guard while editing.
4. Run the relevant focused selector bundle.
5. Run `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"`.
6. Run `mvn -q test`.
7. Run `mvn -q "-DskipTests" package`.
8. Run `mvn -B package`.
9. Run `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check`.
10. Run `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package`.
11. Open the PR only after local verification passes.
12. If a PR-opened checkpoint commit changes the branch, rerun final-head verification for the new head SHA.
13. Wait for remote PR checks on the latest/current head SHA.
14. Merge only when required remote checks are green for the latest/current head SHA.
15. Pull main, confirm the PR head is contained in main, rerun the requested post-merge checks, and verify main CI/CodeQL before counting the slot.

## Focused Checks While Editing

Focused checks are for short feedback loops. They include the new slot guard and any adjacent documentation guard tests that cover the touched files.

Focused checks are not a merge decision. They are a way to catch wording, cross-link, checkpoint, and not-proven-boundary drift before full verification.

## Full Verification Before Merge

Before opening a PR and before any merge decision, run the full local verification ladder from VERIFICATION_PROTOCOL.md:

- focused guard;
- focused selector bundle;
- dependency tree;
- `mvn -q test`;
- `mvn -q "-DskipTests" package`;
- `mvn -B package`;
- diff checks;
- enterprise lab package smoke.

If the PR-opened checkpoint creates a new head, the final head must be verified again before remote checks can support a merge decision.

## Remote PR Check Rules

Remote PR checks must be current for the latest/current head SHA. Build/Test/Package/Smoke must pass, Analyze Java / CodeQL must pass, and Dependency Review must pass where applicable.

Failed, cancelled, stale, pending, or duplicate-only required checks are not acceptable. A remote run for an older head SHA does not make the current head green.

## Main Post-Merge Rules

Do not start the next slot until main is current locally, the merged PR head is contained in main, the requested post-merge local checks pass, and main CI/CodeQL are green for the merge commit.

Do not claim green main while remote checks are pending.

## Checkpoints And Failures

Update SESSION_MANAGER.md after every checkpoint: branch created, edit batch completed, focused verification, full local verification, PR opened, final-head verification, remote checks green, merge completed, and post-merge main green.

Log failures in FAILURE_LOG.md before continuing when a focused guard, focused selector bundle, dependency tree, full test, package check, diff check, enterprise smoke, remote check, scope audit, GitHub operation, merge decision, or main post-merge check fails.

Pause instead of improvising when GitHub check state is ambiguous, required checks fail or remain pending, scope becomes unsafe, human approval is needed, or the recovery path would leave the current BUILD_CONTRACT.md scope.

## Scope Boundaries

This refinement does not authorize production code changes, Maven config changes, CI/workflow changes, Dockerfile changes, Compose behavior changes, runtime behavior changes, endpoint changes, k6/Bruno/Toxiproxy behavior changes, scripts, runner services, automated execution, secrets, external/cloud/tenant targets, or unsupported claims.

## Not-Proven Boundaries

The campaign verification protocol preserves not-proven boundaries. It does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof unless implemented and verified, or broader automation.
