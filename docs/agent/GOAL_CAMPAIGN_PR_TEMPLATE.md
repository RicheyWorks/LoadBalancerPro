# Goal Campaign PR Template

This template defines the required PR record for each scoped slot in a Codex `/goal` campaign. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this template with GOAL_CAMPAIGN_CONTRACT.md, GOAL_CAMPAIGN_BOARD.md, GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md, GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md.

## PR Template

```text
Campaign:
Slot:
Goal:
Branch:
PR URL:
Head SHA:
Base main SHA:

Scope:
Changed files:
Out-of-scope files:

What changed:
Why this slot is separate:

Focused verification:
Relevant selector bundle:
Full local verification:
Package checks:
Diff checks:
Enterprise lab package smoke:

Remote checks:
- Build/Test/Package/Smoke:
- Analyze Java / CodeQL:
- Dependency Review:

Merge decision:
Post-merge main checks:
SESSION_MANAGER.md checkpoint:
FAILURE_LOG.md entries:

Scope/safety audit:
Remaining not-proven boundaries:
Next action:
```

## Required PR Rules

- Open one scoped PR at a time.
- Prefer docs/test-only scope unless explicitly separately approved.
- Include a new or updated documentation guard test.
- Run focused verification while editing.
- Run full local verification before opening or merging.
- Update SESSION_MANAGER.md after every checkpoint.
- Log failures in FAILURE_LOG.md.
- Merge only when latest required checks are green.
- Failed/cancelled/stale/pending required checks are not acceptable.
- Duplicate-only checks are not acceptable evidence.

## Merge Rules

Use the normal GitHub merge commit. Do not squash. Do not rebase. Do not delete branches. Do not create releases or tags. Do not mutate GitHub settings, rulesets, secrets, environments, or required checks.

## Scope Boundaries

Forbidden unless explicitly separately approved:

- `src/main/java`;
- Maven config;
- CI/workflow;
- Dockerfile;
- Compose behavior;
- app behavior;
- endpoints;
- k6 behavior;
- Bruno behavior;
- Toxiproxy behavior;
- scripts;
- runtime resources;
- runner services;
- automation;
- secrets;
- external/cloud/tenant targets;
- production/readiness/performance/runtime-enforcement/replay/storage/export claims.

## Not-Proven Boundaries

Each PR must preserve not-proven boundaries: no production readiness/certification, no live-cloud/real-tenant validation, no runtime enforcement, no load/stress/benchmarking or throughput/p95/p99 evidence, and no replay/evidence/report/storage/export proof unless implemented and verified.
