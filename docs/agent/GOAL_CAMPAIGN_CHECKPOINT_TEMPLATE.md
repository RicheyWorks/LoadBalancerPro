# Goal Campaign Checkpoint Template

This template defines the checkpoint record for long-running Codex `/goal` campaigns. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this template with GOAL_CAMPAIGN_CONTRACT.md, GOAL_CAMPAIGN_BOARD.md, GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md, GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md, GOAL_CAMPAIGN_PR_TEMPLATE.md, GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md.

## Checkpoint Template

```text
Timestamp:
Goal name:
Current PR slot:
Current branch:
PR URL:
Head SHA:
Base/main SHA:
Changed files:
Checks run:
Remote status:
Blocker:
Next action:
Decision: continue / pause / merge / abandon
```

## Required Checkpoints

Update SESSION_MANAGER.md after every checkpoint:

- campaign start;
- branch created;
- edit batch completed;
- focused verification completed;
- full local verification completed;
- PR opened;
- remote checks completed;
- merge decision completed;
- post-merge main checks completed;
- pause;
- final report.

Log failures in FAILURE_LOG.md when a focused check, selector bundle, full verification, package check, diff check, smoke check, remote check, scope audit, GitHub operation, or merge decision fails.

## Verification Levels

Each checkpoint should identify the verification level reached:

- focused guard;
- relevant focused selector bundle;
- `mvn -q test`;
- `mvn -q "-DskipTests" package`;
- `mvn -B package`;
- diff checks;
- enterprise lab package smoke;
- remote PR checks;
- post-merge main checks.

## Remote Check Rules

Merge only when latest required checks are green. Failed/cancelled/stale/pending required checks are not acceptable. Do not claim green main while remote checks are pending.

## Stop Conditions

Pause if main becomes red, scope becomes unsafe, GitHub check state is ambiguous, required checks fail, human approval is needed, or 10 PRs are merged.

## Not-Proven Boundaries

Checkpoint records must preserve not-proven boundaries: no production readiness/certification, no live-cloud/real-tenant validation, no runtime enforcement, no load/stress/benchmarking or throughput/p95/p99 evidence, and no replay/evidence/report/storage/export proof unless implemented and verified.
