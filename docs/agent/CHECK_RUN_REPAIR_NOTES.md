# Check Run Repair Notes

Status: WARN / workflow-repair diagnostic.

## Context

Decision Explorer DX-G01 PR #348 is docs/test-only and locally verified, but GitHub did not emit normal `pull_request` CI or CodeQL runs for the PR head. A manual CodeQL dispatch for the same head emitted `Analyze Java (java-kotlin)`, but failed during job setup while downloading the pinned `actions/setup-java` archive.

## Narrow Repair

This repair keeps the required CI and CodeQL workflows intact. It does not remove required checks, weaken branch rules, change Maven steps, change test/package commands, or change production behavior.

Changes:
- Adds `workflow_dispatch` to the CI workflow so maintainers can manually trigger the required CI workflow for diagnostic recovery when automatic check emission stalls.
- Moves the shared `actions/setup-java` pin from the v5.2.0 commit to the verified v4.8.0 commit so the action archive URL changes while preserving SHA pinning.
- Moves the `actions/dependency-review-action` pin from the v5.0.0 commit to the verified v4.8.0 commit after the repair PR reproduced the same codeload setup failure on the v5.0.0 archive.

## Boundaries

This repair does not prove production readiness, production certification, live-cloud validation, real-tenant validation, benchmark/load/stress evidence, throughput/p95/p99 evidence, replay/export/storage proof, or broader automation.
