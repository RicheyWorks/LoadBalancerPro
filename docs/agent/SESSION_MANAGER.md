# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Campaign: 10-PR LoadBalancerPro goal campaign

Checkpoint: PR 1 opened

Started from main SHA: dfd4b4a750d0fca5cc1b0858a21aa31470718b0b

Current campaign branch: codex/goal-campaign-system-architecture

Current campaign PR: https://github.com/RicheyWorks/LoadBalancerPro/pull/296

Completed campaign PRs: 0 / 10

Current blocker: none

Checks completed: AgentCampaignSystemArchitectureDocumentationTest; AgentCampaignSystemArchitectureDocumentationTest, AgentGoalModeLongRunProtocolDocumentationTest, AgentWorkflowQuickstartDocumentationTest, AdvancedReadmeAgentContractDocumentationTest; mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"; mvn -q test; mvn -q "-DskipTests" package; mvn -B package; git diff --check; git diff --cached --check; .\scripts\smoke\enterprise-lab-workflow.ps1 -Package

Next campaign action: push PR URL checkpoint and wait for current-head remote checks.

## Current Branch

Name:

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/296

## Current Goal

Short goal:

## Current Head SHA

SHA:

## What Changed

- Files changed:
- Behavioral surface:
- Documentation surface:

## Checks Run

- Focused checks:
- Full checks:
- Package checks:
- Diff checks:
- Smoke checks:
- Remote checks:

## Blockers

- Current blocker:
- Owner or next decision:

## Next Action

One concrete next step:

## Recovery Notes

- How to resume:
- Commands already run:
- Safety boundaries to re-check:
- Remote checks that must be refreshed:
