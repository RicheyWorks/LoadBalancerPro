# Enterprise Lab Roadmap

This roadmap turns LoadBalancerPro's post-`v2.5.0` state into a product direction: Enterprise Adaptive Routing Lab first, Production Gateway Candidate second. It is a planning and acceptance-criteria document only; it does not change runtime behavior or claim production deployment certification.

## Product Direction

Track 1, Enterprise Adaptive Routing Lab, is the default product path. It should make scenario design, deterministic replay, LASE shadow/recommendation behavior, policy gates, scorecards, evidence export, and SRE walkthroughs easy to run and review.

Track 2, Production Gateway Candidate, remains optional and gated. It should harden proxy/runtime behavior only when an approved product goal requires live gateway work, deployment guides, and eventual container distribution.

## P0: Truth And Identity Alignment

Purpose: remove stale or contradictory claims so reviewers can see what the project is becoming.

Scope:

- product charter;
- stale evidence cleanup;
- resilience score consistency;
- roadmap index;
- reviewer/operator entry-point updates;
- static documentation tests.

Acceptance criteria:

- `README.md` introduces LoadBalancerPro Enterprise Lab and the Production Gateway Candidate distinction.
- `docs/ENTERPRISE_LAB_PRODUCT_CHARTER.md` exists and is linked from reviewer entry points.
- `docs/ENTERPRISE_LAB_ROADMAP.md` exists and is linked from reviewer entry points.
- OAuth role evidence states that `scope` and `scp` claims do not grant application roles.
- resilience score summary and detailed sections agree.
- performance baseline evidence is marked template/unmeasured unless measured output exists.
- no docs claim production deployment certification.

## P1: Adaptive Routing Lab Workflow

Purpose: make adaptive-routing experiments feel like a product workflow instead of a collection of scattered scripts and tests.

Implemented first slice:

- `GET /api/lab/scenarios`;
- `GET /api/lab/scenarios/{id}`;
- `POST /api/lab/runs`;
- `GET /api/lab/runs`;
- `GET /api/lab/runs/{runId}`;
- deterministic scenario summaries and run results;
- scorecards for baseline, shadow, recommend, and explicit active-experiment comparison;
- `/enterprise-lab.html` browser lab page;
- bounded process-local in-memory run storage, with memory or ignored-file run storage first as the durable product rule;
- source-visible evidence export to `target/enterprise-lab-runs/`.

Next candidate improvements:

- scorecard severity labels;
- richer reviewer comparison tables;
- optional ignored-file run archive under `target/` only;
- stricter JSON schema documentation;
- richer integration into controlled active LASE policy evidence.

Acceptance criteria:

- default app behavior is unchanged when lab features are disabled.
- scenario fixtures are deterministic and CI-safe.
- run evidence includes scenario name, mode, selected backend or allocation result, signals considered, recommendation, guardrail reason, and explanation.
- no live cloud mutation, release action, external network call, or private-network discovery occurs.
- prod/cloud-sandbox auth boundaries apply to any runtime API surface.
- docs explain local/demo usage, prod restrictions, and remaining limitations.
- evidence export writes only ignored `target/` files and never mutates `release-downloads/`.

## P1: Controlled Active LASE Policy Gate

Purpose: create the policy language and guardrails needed before any LASE influence can move beyond shadow comparison.

Implemented first slice:

- `off`, `shadow`, `recommend`, and `active-experiment` modes;
- safe default of `off`;
- `active-experiment` disabled unless `loadbalancerpro.lase.policy.active-experiment-enabled=true`;
- health, eligibility, capacity, freshness, conflict, all-unhealthy, rollback, and bounded-context gates;
- policy result model with baseline, recommendation, final decision, guardrail reasons, rollback reason, and explanation summary;
- bounded process-local audit events;
- protected `GET /api/lab/policy` and `GET /api/lab/audit-events` endpoints;
- `/enterprise-lab.html` policy status and audit-event display;
- ignored evidence output under `target/controlled-adaptive-routing/`.

Modes:

- `off`;
- `shadow`;
- `recommend`;
- `active-experiment`.

Required guardrails:

- safe default of `off`;
- explicit opt-in for `recommend` and `active-experiment`;
- audit events for mode, scenario, reason, and operator-visible decision;
- rollback reasons;
- tests proving default behavior unchanged;
- tests proving active-experiment changes only documented local experiment outcomes;
- route authorization consistent with API-key/OAuth2 boundaries.

Acceptance criteria:

- mode names and allowed transitions are documented.
- ambiguous or unsafe policy configuration fails closed.
- recommendation output is explainable and deterministic.
- active-experiment mode is explicit, bounded, guarded, and not presented as production traffic control.

Next candidate improvements:

- compact policy/audit dashboard panels;
- severity labels for guardrail reasons;
- exportable Markdown audit event tables;
- mock operator-acceptance flow for recommend mode;
- distributed control-plane design before any production gateway promotion.

## P1: Observability Packs

Purpose: give reviewers and operators a local evidence pack for dashboards, alerts, and SLO thinking without inventing production claims.

Scope:

- Prometheus/Grafana dashboard JSON;
- alert examples;
- SLO templates;
- local evidence generation;
- lab-grade measured claims only.

Acceptance criteria:

- dashboard JSON is source-visible and checked for required panels.
- alert examples distinguish lab thresholds from production thresholds.
- SLO templates are marked as templates until production requirements exist.
- evidence output writes only under ignored `target/` paths.
- no external telemetry service or secret is required.

## P2: Measured Performance Baseline

Purpose: replace the current unmeasured performance template with repeatable local evidence.

Scope:

- stable request fixtures;
- source-visible script;
- ignored `target/performance-baseline/` evidence;
- local deterministic run where possible;
- no live/private-network dependency.

Acceptance criteria:

- request fixtures cover health, allocation, evaluation, routing comparison, and lab scenario paths.
- the script records environment metadata, command lines, latency/throughput fields, and limitations.
- docs distinguish local loopback evidence from production SLOs.
- static tests prevent unmeasured claims from being described as measured results.

## P2: Enterprise Auth Proof Lane

Purpose: make OAuth2 role-claim hardening demonstrable without real tenant secrets.

Scope:

- mock IdP/JWKS fixture mode;
- token lifetime and key-rotation guidance;
- role lifecycle examples;
- local proof without real tenant IDs, real client secrets, or real IdP dependencies.

Acceptance criteria:

- scope-only `operator` or `admin` tokens authenticate but do not grant app roles.
- dedicated role claims grant expected authorities.
- missing or ambiguous role claims fail closed.
- docs include key-rotation and role-lifecycle guidance.
- evidence output redacts tokens and writes only ignored local files.

## P2: Container Distribution Readiness

Purpose: prepare for container distribution only if deployable images become the chosen distribution channel.

Scope:

- GHCR or chosen registry later;
- explicit release prompt only;
- immutable digest capture;
- Trivy evidence;
- cosign/keyless or platform-native signing plan;
- rollback and retention docs;
- credential-handling model.

Acceptance criteria:

- no container is published without a separate exact authorization prompt.
- registry target, image name, tag policy, digest policy, scan evidence, signing method, attestation method, rollback, retention, and credential handling are documented.
- scripts/workflows do not execute registry publication or signing until a future approved implementation.
- release docs keep JAR/docs-first and container distribution decisions separate.

## P3: Disposable Live Sandbox Lab

Purpose: create a safe, operator-owned path for bounded live-cloud validation without adding default CI live calls.

Scope:

- AWS sandbox validation plan;
- IAM templates;
- teardown;
- budget guardrails;
- explicit live approval;
- no default CI live calls.

Acceptance criteria:

- live sandbox execution requires a separate explicit approval prompt.
- IAM templates are least-privilege and scoped to sandbox resources.
- budget guardrails and teardown are documented before live execution.
- evidence names the account/region/profile without writing secrets.
- default Maven, CI, Postman, and smoke paths remain dry-run or loopback-only.

## Roadmap Review Cadence

Review this roadmap after any change that affects auth boundaries, adaptive-routing influence, proxy/runtime behavior, evidence generation, release workflow, container posture, performance claims, or live cloud/private-network validation.

