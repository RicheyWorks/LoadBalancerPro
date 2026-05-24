# Verification Protocol

This protocol defines the usual verification escalation for Codex and reviewer-agent work. It is procedural guidance only; it does not add CI/Maven wiring or runtime behavior.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) so checkpoints, pause/resume decisions, and merge-readiness claims stay tied to current verification. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md) to apply these tiers one scoped PR at a time.

## Tier 1: Focused Failing Test Or Focused Doc Guard

Run the exact failing test first when CI or local output identifies one. For documentation-only work, run the new or affected documentation guard test first.

Use this tier while editing so failures stay close to the change.

## Tier 2: Relevant Focused Selector Bundle

Run related tests that cover adjacent docs, guardrails, and contracts. Use this when a change touches README positioning, reviewer trust docs, local-lab docs, Compose guardrails, or agent contracts.

## Tier 3: Full Test Suite

Run:

```powershell
mvn -q test
```

Use full tests before merge decisions or before claiming that a branch is locally green.

## Tier 4: Package Checks

These are the package checks:

Run:

```powershell
mvn -q "-DskipTests" package
mvn -B package
```

These confirm packaging posture in both skipped-test and full Maven package modes.

## Tier 5: Diff Checks

These are the diff checks:

Run:

```powershell
git diff --check
git diff --check origin/main...HEAD
git diff --cached --check
```

These catch whitespace and staged/unstaged diff issues.

## Tier 6: Enterprise Lab Package Smoke

This is the enterprise lab package smoke tier.

Run:

```powershell
.\scripts\smoke\enterprise-lab-workflow.ps1 -Package
```

This writes ignored local evidence under `target/` and does not prove production activation.

## Tier 7: Remote PR Checks

Use GitHub PR checks for required remote status:

- Build/Test/Package/Smoke;
- Analyze Java / CodeQL;
- Dependency Review when applicable;
- any additional required branch-protection checks.

Do not accept stale, failed, cancelled, or pending required checks as green.

## Tier 8: Main Post-Merge Checks

After merge, update local `main`, verify the merge commit, rerun the requested local checks, and inspect main remote CI/CodeQL for the merge commit.

Do not claim fully green main while remote checks are pending.
