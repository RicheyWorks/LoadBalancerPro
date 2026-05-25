# Failure Log Template

Use this template to record failures during PR health passes, docs guard updates, local verification, and remote CI review.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and record blockers before pause/resume decisions. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md), then log any local, remote, or scope-audit failure before pausing.

## Entry

Date/time:

Branch/PR:

Failure type:

Failing check:

Suspected cause:

Fix attempted:

Result:

Follow-up action:

## Entry

Date/time: 2026-05-24T19:16-07:00

Branch/PR: codex/goal-campaign-board-initialization / pending

Failure type: focused documentation guard after checkpoint update

Failing check: `mvn test "-Dtest=AgentGoalCampaignBoardInitializationDocumentationTest,AgentGoalCampaignTemplateArchitectureDocumentationTest"`

Suspected cause: SESSION_MANAGER.md retained the slot 1 merge facts but the checkpoint line no longer contained the exact phrase "slot 1 merged and main green" expected by the new board initialization guard.

Fix attempted: restored the exact phrase in the active checkpoint line.

Result: focused guard rerun passed.

Follow-up action: commit, push, and open PR slot 2 after final diff check.

## Entry

Date/time: 2026-05-24T19:12-07:00

Branch/PR: codex/goal-campaign-board-initialization / pending

Failure type: focused documentation guard bundle

Failing check: `mvn test "-Dtest=AgentGoalCampaignBoardInitializationDocumentationTest,AgentGoalCampaignTemplateArchitectureDocumentationTest,AgentGoalModeLongRunProtocolDocumentationTest,AgentWorkflowQuickstartDocumentationTest,AdvancedReadmeAgentContractDocumentationTest,AgentCampaignSystemIndexDocumentationTest,AgentCampaignSystemArchitectureDocumentationTest"`

Suspected cause: AgentGoalCampaignTemplateArchitectureDocumentationTest froze SESSION_MANAGER.md to `current pr slot: 1`, but the campaign protocol advances the active checkpoint after each slot.

Fix attempted: changed the slot 1 architecture guard to verify active trial checkpoint presence and reusable template links instead of requiring slot 1 to remain the current PR slot.

Result: focused bundle rerun passed.

Follow-up action: continue full local verification for PR slot 2.

## Entry

Date/time: 2026-05-24T17:24-07:00

Branch/PR: codex/goal-campaign-scope-audit-checklist / pending

Failure type: focused documentation guard

Failing check: `mvn test "-Dtest=AgentCampaignScopeAuditChecklistDocumentationTest"`

Suspected cause: CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md did not contain the exact phrases "pause instead of improvising" and "does not prove production certification" required by the new guard test.

Fix attempted: tightened the scope audit checklist wording to state the exact pause rule and production certification not-proven boundary.

Result: focused rerun passed.

Follow-up action: continue PR 7 focused selector bundle and full verification.

## Entry

Date/time: 2026-05-24T15:52-07:00

Branch/PR: codex/goal-campaign-checkpoint-ledger / pending

Failure type: local tooling command

Failing check: gh pr create body quoting attempt

Suspected cause: PowerShell passed part of the multi-line PR body as command flags, causing gh to reject `-q` as an unknown shorthand flag.

Fix attempted: switched PR creation to `gh pr create --body-file -` with stdin body content.

Result: retry succeeded and opened https://github.com/RicheyWorks/LoadBalancerPro/pull/297.

Follow-up action: retry PR creation, then update SESSION_MANAGER.md with the PR URL and final branch head.

## Entry

Date/time: 2026-05-24T16:45-07:00

Branch/PR: codex/goal-campaign-merge-gate / pending

Failure type: focused documentation guard

Failing check: `mvn test "-Dtest=AgentCampaignMergeGateDocumentationTest"`

Suspected cause: CAMPAIGN_MERGE_GATE.md said "Do not squash, rebase" but did not contain the exact explicit phrase "Do not rebase" required by the new guard test.

Fix attempted: changed the merge method section to state "Do not squash. Do not rebase."

Result: focused rerun passed.

Follow-up action: continue PR 5 full local verification.

## Notes

- Keep entries factual.
- Include exact failing test names or job names when available.
- Distinguish local failures from remote PR failures.
- Do not treat a fixed local failure as remotely green until current remote checks complete successfully.
