# Evidence Audit Campaign Checkpoint Template

Use this template for every checkpoint in the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation only and does not add automation or runtime behavior.

## Checkpoint Fields

- Timestamp:
- Campaign name:
- Current PR slot:
- Current branch:
- PR URL:
- Base/main SHA:
- Head SHA:
- Changed files:
- Checks run:
- Remote status:
- Blocker:
- Next action:
- Decision: continue / pause / merge / abandon

## Required Checkpoint Moments

- after branch creation;
- after focused local verification;
- after full local verification;
- after PR creation;
- after remote PR checks complete;
- after merge;
- after post-merge main local verification;
- after main CI/CodeQL complete.

## Failure Link

Every local, remote, scope, or tooling failure must be logged in [`FAILURE_LOG.md`](FAILURE_LOG.md) before continuing. Failed, cancelled, stale, pending, missing, or duplicate-only required checks are not acceptable.

## Boundaries

Checkpoints must preserve the docs/test-only scope by default and the not-proven boundaries: no production readiness, no production certification, no live-cloud validation, no real-tenant validation, no runtime enforcement, no load/stress/benchmarking, no throughput/p95/p99 evidence, no replay/evidence/report/storage/export proof, and no broader automation.
