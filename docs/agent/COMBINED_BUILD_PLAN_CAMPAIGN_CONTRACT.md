# Combined Build-Plan Goal Campaign Contract

This is the execution contract for the **Combined Deployable Proxy and Lab/Shadow/Analysis Build Campaign**. It
imports Claude's July 21 build-layout artifacts and turns their two roadmaps into a single ordered, machine-checked
campaign. It does not itself implement a roadmap item or prove any imported audit statement.

Use this contract with [`COMBINED_BUILD_PLAN_CAMPAIGN_BOARD.md`](COMBINED_BUILD_PLAN_CAMPAIGN_BOARD.md),
[`COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json`](COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json), the source plans,
[`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md),
[`SESSION_MANAGER.md`](SESSION_MANAGER.md), and [`FAILURE_LOG.md`](FAILURE_LOG.md).

## Provenance And Inventory

- Source layout commit: `74b1f6758304bc5a3a85ff4888039e7309324ddf`.
- Source layout base: `e800ba06875d0897f8459ad14a5d5cf60dc34568`.
- Campaign start main: `0f1e97b9ce4acceaad02877bf1fc2185997aba9d`.
- Imported source artifacts:
  - [`../AUDIT_2026-07-21.md`](../AUDIT_2026-07-21.md);
  - [`../AUDIT_LAB_SHADOW_2026-07-21.md`](../AUDIT_LAB_SHADOW_2026-07-21.md);
  - [`../BUILD_PLAN_DEPLOYABLE.md`](../BUILD_PLAN_DEPLOYABLE.md);
  - [`../BUILD_PLAN_LAB_SHADOW.md`](../BUILD_PLAN_LAB_SHADOW.md);
  - [`../strategy-playground.html`](../strategy-playground.html).
- The deployable plan contributes 27 planned items and the lab plan contributes 23. Proxy P-0.5 and lab L-0.1
  explicitly describe the same security fix, so the campaign contains 50 source items represented by 49 unique slots.
- The deployable plan's four Milestone 4 bullets do not carry PR identifiers in the source. This campaign assigns them
  stable source-order labels P-4.1 through P-4.4; those labels are campaign bookkeeping, not a claim about the source.
- `CONTRACT-00`, the current import/contract PR, is a prerequisite record and is not one of the 49 implementation
  slots.

The five imported artifacts remain source snapshots attributable to the source commit. Their assertions are audit
hypotheses and planning inputs, not current-main facts or completed acceptance evidence.

## Current-State Audit

The source layout was based on `e800ba06875d0897f8459ad14a5d5cf60dc34568`. Between that base and campaign-start
main, the repository merged the Netty 4.2.16 baseline remediation and command-ledger restart reconciliation. Those
changes removed a blocking container vulnerability and repaired a restart-history path, but they do not prove the
complete acceptance contract of any source-plan item.

Two exact campaign-start probes make the conservative classification especially important:

- `ProdApiKeyFilter` remains profile-gated while the non-OAuth security chain permits requests generally, so the
  combined P-0.5/L-0.1 security slot is open.
- Both command ledgers retain 256-byte chunked writes and truncated-tail classification. Restart reconciliation is
  adjacent work, but it does not prove the concurrent append/replay acceptance required by L-0.6.

Every one of the 49 slots therefore starts `OPEN`. A slot may discover that some implementation already exists, but it
can advance only after current-main evidence proves every source-plan acceptance criterion, the replacement contract,
and all applicable safety gates. “Not found,” imported line numbers, deleted paths, or a partial existing feature never
count as completion.

## Authorization And Safety Boundary

The user authorized implementation of all 49 slots, including production-code, test, dependency, Maven, CI, Docker,
Compose, runtime, endpoint, documentation, and script changes when a slot's acceptance contract actually requires
them. That authorization remains bounded:

- one scoped PR at a time;
- no required-check, branch-policy, dependency-review, CodeQL, SBOM, Docker/runtime, or Trivy weakening;
- no secrets, real credentials, external production targets, or public production deployment;
- no live production target;
- no live tenant traffic,
  billable cloud resources, or production-looking defaults;
- cloud behavior must use mocks with `liveMode=false` unless the user separately expands authority;
- deployment, TLS, DNS, benchmark, soak, and proxy traffic evidence must use deterministic local/loopback fixtures;
- irreversible destructive operations and material scope expansion are stop conditions;
- imported “real,” “deployable,” “production,” performance, and size-reduction language is a target, not evidence.

Independent human review is advisory when no independent reviewer is available. In that case, the unchanged current
head requires a documented full-diff self-review after local and remote gates pass. This substitutes for reviewer
availability only; it does not substitute for or bypass a required check.

## One-PR State Machine

The canonical state path is:

`OPEN -> IN_PROGRESS -> LOCAL_GREEN -> PR_OPEN -> REMOTE_GREEN -> MERGED -> MAIN_GREEN`

`PAUSED` and `BLOCKED` are exceptional states. Only one slot may be `IN_PROGRESS` or later without being `MAIN_GREEN`.
The next slot cannot start until the exact merge commit for the current slot is on main and main CI and CodeQL are
green. Update the board, manifest, `SESSION_MANAGER.md`, and `FAILURE_LOG.md` in the active slot PR; reconcile their
expected prepend/table conflicts without dropping either history.

At each slot:

1. Start from clean, current, remotely green main and mark exactly one slot `IN_PROGRESS`.
2. Re-audit current source, tests, configuration, packaging, and acceptance criteria.
3. Record the narrow build contract, touched surfaces, prohibited surfaces, and verification profile.
4. Implement the smallest complete acceptance slice. If a source item cannot fit safely in one PR, amend this contract
   first with ordered child slots; never silently omit or combine acceptance criteria.
5. Run focused and adjacent verification, then the applicable full local ladder.
6. Verify the unchanged PR head and complete diff; record a scope, security, claim, and not-proven audit.
7. Require all applicable current-head remote CI, CodeQL, dependency-review, Docker/runtime, SBOM, and Trivy results.
8. Merge automatically only when every required gate passes.
9. Verify the exact merge commit on main and main CI/CodeQL before marking `MAIN_GREEN`.
10. Continue immediately to the lowest-ordinal dependency-ready `OPEN` slot.

The verification-profile text in the manifest is a minimum. `VERIFICATION_PROTOCOL.md`, changed-surface requirements,
workflow-required checks, and the acceptance criteria can add gates; they cannot remove them.

## Completion Contract

The campaign is complete only when:

- all 49 unique slots are `MAIN_GREEN`;
- every one of the 50 source-plan items maps to a completed canonical slot;
- a final repository-wide scope, dependency, security, packaging, and claim audit passes;
- full local verification passes at the final main head;
- exact final-main CI and CodeQL are green;
- the final handoff lists every PR head, merge commit, verification record, failure/recovery, and remaining boundary.

The source plan calls L3.4 and L4 stretch work, but the user's complete-campaign instruction makes them required here.
Milestone wording never upgrades evidence: production readiness/certification, live-cloud or real-tenant validation,
public/external traffic control, production throughput/p95/p99, distributed durability, and broader automation remain
not proven unless a later bounded result explicitly establishes them.

## Immediate Next Slot

After `CONTRACT-00` reaches `MAIN_GREEN`, start `SEC-DEFAULT-DENY`. No other implementation slot is open for active
work before that gate.
