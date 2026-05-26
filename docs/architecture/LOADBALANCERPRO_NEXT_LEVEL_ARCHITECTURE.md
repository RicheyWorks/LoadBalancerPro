# LoadBalancerPro Next-Level Architecture Campaign Skeleton

- **Repository target:** `RicheyWorks/LoadBalancerPro`
- **Recommended repo path:** `docs/architecture/LOADBALANCERPRO_NEXT_LEVEL_ARCHITECTURE.md`
- **Document status:** living skeleton / architecture control plane
- **Created:** 2026-05-26
- **Primary users:** Richey, Codex, ChatGPT, Grok, Claude, Gemini, reviewer/operator agents
- **Program size:** 500 PR-sized slots, organized as 50 ten-PR campaigns
- **Core rule:** one scoped PR at a time; never turn this document into permission to overclaim production readiness

---

## 0. Why This Document Exists

This document is the durable architecture planning surface for taking LoadBalancerPro from a strong adaptive-routing lab into a modern, research-backed, next-level load-balancing platform while preserving the repository's existing trust boundaries.

It is designed so multiple AI agents can update the same architectural direction daily without bloating the repo, losing context, or letting old research quietly become false authority.

This document should answer five questions every day:

1. What is the current architecture direction?
2. What research has changed the direction?
3. What old information is now superseded or archived?
4. What is the next safe PR-sized step?
5. What must Codex not change, claim, or automate yet?

---

## 1. Constitutional Source Anchors

Before editing this document or starting a campaign slot, an agent must inspect the current versions of these repo files:

| Anchor | Purpose |
|---|---|
| `README.md` | Public claim boundary, reviewer starting point, not-proven boundary source |
| `AGENTS.md` | Agent operating rules and scope discipline |
| `BUILD_CONTRACT.md` | Per-PR task contract and final report shape |
| `docs/agent/CAMPAIGN_SYSTEM_INDEX.md` | Campaign navigation |
| `docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md` | Ten-PR campaign loop, merge gate, stop conditions |
| `docs/agent/VERIFICATION_PROTOCOL.md` | Focused/full/local/remote verification discipline |
| `docs/agent/SESSION_MANAGER.md` | Current branch, PR, SHA, verification, and blocker ledger |
| `docs/agent/FAILURE_LOG.md` | Required failure and recovery record |
| `docs/agent/LASE_CORE_EXPANSION_GOALS.md` | Current LASE goal ledger and near-term adaptive-routing backlog |
| `docs/REVIEWER_TRUST_MAP.md` | Reviewer-facing evidence path and claim boundaries |

### Non-negotiable inherited boundaries

This architecture skeleton does **not** prove or authorize claims of:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement beyond explicitly implemented and verified code paths;
- load/stress/benchmark evidence unless actually run and reported;
- throughput, p95, or p99 production evidence;
- replay/evidence/report/storage/export proof unless implemented and verified in a scoped PR;
- broader automation, CI/Maven/Docker/Compose/runtime changes, secrets, external targets, cloud targets, or tenant targets unless explicitly scoped.

---

## 2. Architecture North Star

LoadBalancerPro should evolve into an **enterprise adaptive routing lab** with a credible modern data-center architecture shape:

```text
Research Intake + Architecture Decisions
              ↓
Control Plane: policy, routing tables, scoring configuration, safety gates
              ↓
LASE Engine: telemetry → state vector → scoring → decision → explanation
              ↓
Data Plane / Lab Fast Path: route selection, proxy/lab execution, synthetic fast-path simulation
              ↓
Evidence + Replay Cockpit: traces, scenario reports, what-if analysis, reviewer proof paths
```

The strategic direction is to build toward:

- a clear control-plane/data-plane split;
- closed-loop LASE adaptive routing;
- tail-latency-aware and saturation-aware scoring;
- overload protection, adaptive concurrency, load shedding, and priority/fairness classes;
- locality, multi-region, and sharding simulation;
- decision trace, explainability, replay, and what-if analysis;
- energy/carbon-aware scoring experiments;
- eBPF/Katran-inspired fast-path simulation without pretending it is production kernel enforcement;
- hybrid service-mesh thinking: embedded routing library simulation plus proxy/lab mode where useful;
- strict evidence boundaries and reviewer-friendly proof paths.

---

## 3. Target Architecture Layers

| Layer | Role | Future capability | Proof boundary |
|---|---|---|---|
| Public trust layer | README, reviewer docs, claim contracts | Clear proof map for implemented behavior | No marketing overclaims |
| Agent/campaign layer | Codex workflow, campaign boards, session manager | Repeatable multi-PR execution | One scoped PR at a time |
| Research layer | Source cards, ADRs, stale-info retirement | Daily research intake without bloat | Research is not proof |
| Control plane | Policies, routing table model, scoring config | Safe decision authority | Lab/control simulation until verified |
| Telemetry/state layer | Latency, tail, error, saturation, recovery, locality, carbon signals | Rich adaptive signal model | Synthetic/local evidence until real telemetry exists |
| LASE decision layer | Score calculation, policy gates, action mode | Shadow → recommend → active-experiment evolution | Action mode must remain bounded and explicit |
| Data-plane/lab layer | Reverse proxy, route execution, fast-path simulation | CP/DP seam and high-performance path model | Not production eBPF unless actually implemented and verified |
| Resilience layer | Concurrency limits, load shedding, brownout recovery | Graceful degradation experiments | Lab-only until proven otherwise |
| Evidence/replay layer | Decision traces, replay fixtures, scenario artifacts | Reviewer-grade explainability | No broad storage/export claim without implementation |
| Cockpit layer | Operator/reviewer UI and visualizations | Decision timelines, heatmaps, what-if views | UI does not imply production monitoring |

---

## 4. Daily Multi-Agent Update Protocol

Use this section every day before asking Codex to code.

### 4.1 Daily update checklist

```markdown
## Daily Update - YYYY-MM-DD - Agent: <ChatGPT|Codex|Grok|Claude|Gemini|Human>

### Files inspected
- [ ] README.md
- [ ] AGENTS.md
- [ ] CAMPAIGN_SYSTEM_ARCHITECTURE.md
- [ ] LASE_CORE_EXPANSION_GOALS.md
- [ ] SESSION_MANAGER.md
- [ ] FAILURE_LOG.md
- [ ] Current branch diff / PR status

### New information added
- Source card IDs:
- Architecture sections touched:
- Campaign slots affected:

### Old information changed
- Superseded source card IDs:
- Archived sections:
- Deleted/compacted material:

### Decision impact
- [ ] No architecture decision changed
- [ ] Existing ADR updated
- [ ] New ADR proposed
- [ ] Campaign order changed
- [ ] Stop condition triggered

### Next Codex-safe action
- Next campaign:
- Next slot:
- Branch name:
- Scope:
- Verification expected:
- Boundaries to repeat in PR:
```

### 4.2 Research intake states

Every new research note must use one of these states:

| State | Meaning | Action |
|---|---|---|
| `candidate` | Interesting but not accepted | Keep as compact card only |
| `accepted` | Useful enough to shape architecture | Link to affected layer/campaign |
| `implemented` | Reflected in merged/main-green PR | Link PR and evidence |
| `superseded` | Replaced by newer/better information | Link replacement card |
| `rejected` | Not useful or unsafe for this repo | Keep one-line reason, then archive/delete detail |
| `archived` | Historically useful but no longer active | Move out of daily context |

### 4.3 Research source card template

Keep research cards short. Do **not** paste whole articles, papers, transcripts, PDFs, screenshots, or giant model outputs into the repo.

```markdown
### SRC-YYYYMMDD-### - <short title>

- Date added:
- Added by:
- Source type: paper / company architecture / docs / benchmark / issue / model analysis / other
- Link or file reference:
- Reliability: high / medium / low / unknown
- Summary, max 5 bullets:
- Relevance to LoadBalancerPro:
- Affected layers:
- Affected campaign slots:
- Decision: candidate / accepted / rejected / superseded / archived
- Replaces:
- Replaced by:
- Follow-up PR slot:
```

### 4.4 Architecture Decision Record template

```markdown
## ADR-### - <decision title>

- Status: proposed / accepted / superseded / rejected
- Date:
- Agents involved:
- Context:
- Decision:
- Consequences:
- Alternatives considered:
- Source cards:
- Affected campaign slots:
- Verification needed before claim:
- Supersedes:
- Superseded by:
```

---

## 5. Old Information and Disk-Space Control

The repo should stay useful on a machine with limited disk space. Do not let the architecture document become a junk drawer.

### 5.1 Storage policy

| Content type | Keep in main doc? | Storage rule |
|---|---:|---|
| Current architecture direction | Yes | Keep concise |
| Current campaign table | Yes | Keep ranges and statuses, not full PR essays |
| Research summaries | Yes, as cards | Max 5 bullets each |
| Full articles/papers/PDF text | No | Store source link or local file reference only |
| Old model conversations | No | Compress into source card or ADR |
| Generated reports | Usually no | Keep in `target/` or artifact storage unless PR explicitly tracks them |
| Superseded decisions | Briefly | Move details to archive after replacement |
| Failed experiments | Yes, compact | Link `FAILURE_LOG.md`; do not hide failures |

### 5.2 Bloat limits

- Keep this main architecture document under roughly **250 KB**.
- Keep each source card under roughly **250 words**.
- Keep each daily update under roughly **300 words**.
- After 10 daily updates, compress them into one monthly state capsule.
- Do not track large generated files unless a PR explicitly requires them.
- Prefer links, hashes, source IDs, and concise summaries over raw copied text.

### 5.3 Retirement workflow

When old information is no longer current:

1. Mark the source card or ADR as `superseded`.
2. Add the replacement source or ADR ID.
3. Update affected campaign slots.
4. Remove duplicate prose from this main document.
5. Move detailed historical text to `docs/architecture/archive/YYYY-MM/` only if it is still valuable.
6. Delete local generated artifacts that are not tracked, needed, or intentionally preserved.

---

## 6. Campaign Model: 500 PR-Sized Slots

The 500-slot program is organized as **50 campaigns of 10 PR-sized slots**. This matches the repo's existing campaign discipline better than one giant roadmap.

Each campaign must be small enough that Codex can complete one PR at a time from healthy `main`, with focused verification, full verification, remote checks, merge gate, post-merge main checks, and a factual handoff.

### 6.1 Slot states

| State | Meaning |
|---|---|
| `planned` | Slot exists but no branch started |
| `ready` | Inputs and dependencies are clear enough to start |
| `active` | Current branch or local work exists |
| `PR-opened` | Pull request exists, checks/review pending |
| `merged-awaiting-main-green` | Merged, but post-merge main checks not proven green |
| `merged/main-green` | Merged and main checks proven green |
| `paused/WARN` | Blocked by scope, checks, conflicts, or human decision |
| `closed/superseded` | Replaced by a different scoped slot |

### 6.2 PR slot template

```markdown
### SLOT-### - <short title>

- Campaign:
- Proposed PR title:
- State:
- Branch:
- Actual GitHub PR:
- Primary scope:
- Expected changed files/areas:
- Dependencies:
- Success criteria:
- Focused verification:
- Full verification:
- Remote verification:
- Evidence produced:
- Not-proven boundaries:
- Stop conditions:
- Follow-up slots:
```

---

## 7. 50-Campaign Skeleton Through Slot 500

> Slot IDs are planning IDs, not guaranteed GitHub PR numbers. Map each slot to the actual GitHub PR after the PR is opened.

| Campaign | Slots | Theme | Main deliverable | Guardrail |
|---:|---:|---|---|---|
| C01 | 001-010 | Architecture skeleton and daily update loop | Establish this document, source-card format, ADR format, and daily handoff protocol | Docs-only unless explicitly scoped |
| C02 | 011-020 | Current repo evidence map | Map current implementation, docs, tests, cockpits, and not-proven boundaries | Do not invent evidence |
| C03 | 021-030 | Claim-boundary hardening | Guard README/reviewer claims against unsupported readiness/performance language | Preserve trust wording |
| C04 | 031-040 | Research intake pipeline | Add compact research cards, stale-info lifecycle, archive policy, source reliability tags | No raw research dumps |
| C05 | 041-050 | Campaign operations hardening | Improve campaign templates, handoff reports, failure recovery, merge gates | No CI/runtime automation unless scoped |
| C06 | 051-060 | LASE scoring foundation | Strengthen tail-latency and score-breakdown semantics | No real-world p95/p99 claims |
| C07 | 061-070 | Server state-vector expansion | Model latency, saturation, recovery, error, locality, and risk signals | Keep immutable/deterministic |
| C08 | 071-080 | Latency windows and trend signals | EWMA/rolling-window lab signals and tests | Bounded synthetic evidence only |
| C09 | 081-090 | Anti-flapping and hysteresis | Stable routing under near-tie oscillation | Do not hide material degradation |
| C10 | 091-100 | Recovery and saturation refinement | Brownout recovery, saturation pressure, safe re-entry | Lab/local proof only |
| C11 | 101-110 | Adaptive concurrency lab model | Opt-in limiter simulation and focused tests | No production enforcement claim |
| C12 | 111-120 | Load shedding and priority classes | Explainable shedding, fairness, and request class model | Lab-mode only |
| C13 | 121-130 | Decision trace model | Immutable trace of candidates, factors, decision, unknowns | Redact sensitive data |
| C14 | 131-140 | Explanation renderer | Stable human-readable LASE explanations | Avoid brittle wording tests |
| C15 | 141-150 | Strategy comparison harness | Compare baseline vs LASE in deterministic scenarios | Label synthetic/local evidence |
| C16 | 151-160 | Replay fixture foundation | Deterministic replay of synthetic decisions | No broad storage/export claim |
| C17 | 161-170 | Scenario report artifact | Local evidence artifact for scenario runs | Do not track large generated output |
| C18 | 171-180 | LASE evidence map | Connect features to tests, docs, reviewer path | Only map implemented evidence |
| C19 | 181-190 | Cockpit decision visualization | Timeline, score breakdown, candidate heatmap concepts | UI is not production monitoring |
| C20 | 191-200 | What-if analysis | Replay alternative decisions and explain deltas | Deterministic local fixtures |
| C21 | 201-210 | Reviewer/operator workflows | Guided proof path for common questions | Keep trust boundaries visible |
| C22 | 211-220 | Evidence redaction and audit | Redaction rules, unknown handling, artifact safety | No secrets or external targets |
| C23 | 221-230 | Control-plane domain model | Policy, route table, routing config, safety gate objects | Simulation/control model only |
| C24 | 231-240 | Policy engine evolution | Off/shadow/recommend/active-experiment semantics | Active mode must stay explicit |
| C25 | 241-250 | Routing Information Base simulation | Local RIB model with versioned route snapshots | No distributed-systems claim yet |
| C26 | 251-260 | Control-plane/data-plane seam | Interface between scoring decisions and route execution | Contract tests first |
| C27 | 261-270 | Fast-path simulator | Optimized lab path inspired by high-performance LB ideas | Not production kernel/eBPF |
| C28 | 271-280 | Reverse proxy lab evolution | Safer lab proxy seams and decision injection | No production proxy claim |
| C29 | 281-290 | eBPF/Katran-inspired architecture model | Document and simulate fast-path constraints | No kernel enforcement claim |
| C30 | 291-300 | CP/DP integration tests | End-to-end lab tests across control and data seams | Local deterministic only |
| C31 | 301-310 | Locality model | Region/zone/ring candidate metadata | No live geo claim |
| C32 | 311-320 | Multi-cluster scenarios | Synthetic cross-cluster routing scenarios | No cloud validation claim |
| C33 | 321-330 | Sharding and routing identity | Shard-aware routing keys and tests | Avoid tenant/customer data |
| C34 | 331-340 | Hybrid mesh simulation | Embedded-router vs proxy-mode lab comparison | No service-mesh production claim |
| C35 | 341-350 | Service discovery model | Local registry/discovery fixtures and stale endpoint handling | No external registry integration unless scoped |
| C36 | 351-360 | Failover and locality recovery | Region/ring degradation and recovery behavior | Synthetic scenarios only |
| C37 | 361-370 | Failure-injection fixtures | Latency spike, network risk, error pressure, saturation, recovery | Local deterministic fixtures |
| C38 | 371-380 | Brownout/degradation campaigns | Brownout, partial outage, noisy neighbor scenarios | No SLA/SLO proof |
| C39 | 381-390 | Backpressure scenarios | Queue depth, overload, limiter interaction | Lab-mode only |
| C40 | 391-400 | Fairness and priority behavior | Request class fairness and starvation prevention | No real-tenant claim |
| C41 | 401-410 | Lab SLO language and boundaries | Internal lab objectives and evidence wording | Do not imply customer SLA |
| C42 | 411-420 | Security and policy gates | Safer configs, redaction, forbidden target guards | No secrets/external targets |
| C43 | 421-430 | Energy/carbon signal model | Carbon/energy metadata and scoring inputs | Experimental/lab only |
| C44 | 431-440 | Carbon-aware routing scenarios | Route scoring with carbon and latency tradeoffs | No real carbon-impact claim |
| C45 | 441-450 | Predictive scoring experiments | Simple trend/forecast scoring in local scenarios | Explain uncertainty |
| C46 | 451-460 | AI-burst workload scenarios | Spiky traffic simulation and graceful degradation | Synthetic only |
| C47 | 461-470 | Verification bundle hardening | Selector bundles, guard tests, package/smoke paths | No unchecked green claims |
| C48 | 471-480 | Benchmark boundary framework | Define when load/stress evidence may be claimed | Do not fabricate benchmarks |
| C49 | 481-490 | Documentation consolidation | Compress old notes, update evidence maps, archive stale material | Keep main docs small |
| C50 | 491-500 | Program closeout and PR-500 handoff | Final architecture checkpoint, remaining risks, next roadmap | Claim only merged/main-green proof |

---

## 8. Immediate Next 10 Slots

Use these as the first practical campaign after this skeleton is committed.

### C01: Architecture Skeleton and Daily Update Loop

| Slot | Proposed PR title | Scope | Success criteria |
|---:|---|---|---|
| 001 | Add next-level architecture skeleton | Add this document and link it from the appropriate agent/reviewer index if safe | Document exists, boundaries preserved, docs checks pass |
| 002 | Add architecture source-card template | Add/confirm compact research-card format | Agents can add research without bloating repo |
| 003 | Add architecture ADR template | Add ADR section/template or separate index | Decisions can be tracked and superseded |
| 004 | Add daily update block template | Add multi-agent update protocol | Codex/Grok/Claude/Gemini/human handoffs become repeatable |
| 005 | Add stale-information retirement policy | Add archive/superseded workflow | Old info has a deletion/compaction path |
| 006 | Add architecture size guard proposal | Add docs-only limit language and review checklist | Document bloat has a visible guard |
| 007 | Map current LASE goals into architecture layers | Cross-reference current LASE ledger into this document | Current work fits the north-star model |
| 008 | Add first architecture source cards from existing research | Convert existing model research into compact cards | No raw dumps; only concise cards |
| 009 | Add Codex campaign prompt block | Add startup prompt for next campaign slots | Codex can start from clear boundaries |
| 010 | Close C01 with checkpoint report | Summarize what changed, what remains, next campaign | C01 closeout is factual and bounded |

---

## 9. Codex Startup Prompt Block

Use this block when starting a new Codex session for this program.

```text
You are working in RicheyWorks/LoadBalancerPro.

Before editing, read README.md, AGENTS.md, BUILD_CONTRACT.md,
docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md,
docs/agent/VERIFICATION_PROTOCOL.md,
docs/agent/SESSION_MANAGER.md,
docs/agent/FAILURE_LOG.md,
docs/agent/LASE_CORE_EXPANSION_GOALS.md, and
this architecture document.

Work one scoped PR slot only. Preserve all not-proven boundaries.
Do not add production readiness, production certification, live-cloud,
real-tenant, runtime enforcement, load/stress/benchmark, throughput/p95/p99,
or broad automation claims unless the current PR explicitly implements and verifies them.

Update SESSION_MANAGER.md at required checkpoints.
Log any local, remote, scope, or tooling failure in FAILURE_LOG.md.
Run focused verification while editing, full verification before merge decisions,
and report exact checks, changed files, branch, PR, head SHA, remote status,
scope/safety audit, remaining not-proven boundaries, and next recommended action.
```

---

## 10. Active Architecture Questions

Keep this list short. Do not let it become a second backlog.

| Question | Status | Needed before decision | Candidate campaign |
|---|---|---|---|
| What is the safest CP/DP seam in the current Java/Spring structure? | open | Repo code map and tests | C23-C26 |
| How far can LASE move from shadow/recommend toward active-experiment safely? | open | Policy gate tests and lab-only scenarios | C24 |
| What telemetry signals are deterministic enough for local lab proof? | open | State-vector and scenario tests | C06-C10 |
| What replay evidence is valuable without becoming a storage/export overclaim? | open | Trace model and artifact boundaries | C16-C18 |
| How should old research be compacted after daily multi-agent updates? | open | First 10 daily update trial | C01-C04 |
| Can eBPF/Katran ideas be represented honestly as simulation only? | open | ADR and fast-path architecture doc | C27-C29 |
| Can carbon-aware routing be useful as a local lab differentiator? | open | Signal model and scenario tests | C43-C44 |

---

## 11. Current Source Cards

### SRC-20260526-001 - Uploaded strategic analysis for LoadBalancerPro upgrade

- Date added: 2026-05-26
- Added by: ChatGPT from user-provided research text
- Source type: model-assisted strategic analysis
- Reliability: medium until external sources are individually verified
- Summary:
  - Current build has strong Java/Spring Boot structure, safety boundaries, LASE seeds, reverse proxy mode, evidence/documentation posture, and reviewer cockpit direction.
  - Gaps are modern data-plane realism, closed-loop control, locality, overload protection, and efficiency-aware routing.
  - Recommended top moves include CP/DP split, closed-loop LASE, explainability/replay, locality, overload protection, energy/carbon-aware scoring, eBPF-inspired fast path, chaos/failure injection, and better cockpits.
  - Strongest near-term differentiator is explainability and replay tied to LASE decisions.
  - Medium-term direction is CP/DP split, overload protection, and locality.
- Relevance to LoadBalancerPro: becomes the initial north-star input for C06-C50.
- Affected layers: research, control plane, LASE, data plane/lab, evidence/replay, cockpit, locality, resilience, energy/carbon.
- Affected campaign slots: 051-500.
- Decision: accepted as candidate architecture direction, not accepted as proof of external facts.
- Replaces: none.
- Replaced by: TBD after external source verification.
- Follow-up PR slot: 008.

---

## 12. Status Counters

Update this section only during campaign checkpoints, not every tiny edit.

| Counter | Value |
|---|---:|
| Total planned slots | 500 |
| Slots ready | 0 |
| Slots active | 0 |
| Slots PR-opened | 0 |
| Slots merged/main-green | 0 |
| Slots paused/WARN | 0 |
| Source cards active | 1 |
| Source cards superseded | 0 |
| ADRs proposed | 0 |
| ADRs accepted | 0 |

---

## 13. Final Guardrail

This document is the architecture map, not the evidence itself. A feature only becomes real for reviewer claims when its scoped PR is implemented, verified, merged, and post-merge main checks are green. Research can guide the roadmap, but implementation and verification are the only claim upgrade path.
