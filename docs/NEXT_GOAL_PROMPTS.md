# Next Goal Prompts

These prompts are paste-ready starting points for the next long-running product pushes. They preserve the current safety posture: Enterprise Adaptive Routing Lab first, Production Gateway Candidate second, no release or container publication unless a future prompt explicitly authorizes it.

## 1. Adaptive Routing Lab Workflow

```text
/goal Build the LoadBalancerPro Enterprise Adaptive Routing Lab workflow with deterministic scenario and run surfaces, scorecards, browser/demo documentation, and ignored evidence output. Do not change default allocation behavior.

Automation mode:
Work autonomously across safe, reviewable PRs; merge passing PRs; resync main after each merge.

Hard boundaries:
Do not create tags, GitHub Releases, release assets, container publications, container signatures, workflows, rulesets, default-branch changes, native executables, installers, vendored binaries, external services, secrets, network discovery, DNS probing, port scanning, or live cloud/private-network execution. Preserve release-downloads/. Do not weaken auth, proxy, Swagger/OpenAPI, Prometheus/privacy, DTO validation, rate-limit, container prod-profile, or adaptive-routing safe defaults.

Preflight:
Run pwd, git branch --show-current, git remote -v, git status --short, git fetch origin --prune --tags, git checkout main, git pull --ff-only, git rev-parse main, git rev-parse origin/main, git rev-list --left-right --count main...origin/main, git log --oneline -12, and verify v2.5.0 still points to 4cc03750be5479d9f8f88f8ef8014e05a8dc587a.

Checkpoints:
1. Audit current adaptive-routing experiment harness, replay fixtures, API contracts, docs, and static tests.
2. Design lab scenario and run model with memory or ignored-file storage first.
3. Add deterministic scenario/run evidence generation under target/adaptive-routing-lab/.
4. Add scorecards for baseline, shadow, recommend, and active-experiment comparison without changing defaults.
5. Add browser/demo documentation and reviewer walkthroughs.
6. Add static and unit tests for determinism, auth/profile expectations, no secrets, and ignored output.
7. Validate and summarize product value.

Validation:
Run focused tests, mvn -q clean test, mvn -q verify, mvn -q -DskipTests package, git diff --check, operator run-profile dry-run, Postman enterprise lab dry-run, and adaptive experiment evidence script if changed.

Final report:
Overall Status, Goal Status, Branch, PRs opened/merged, Final main, Final divergence, Scenario surfaces, Run surfaces, Scorecards, Evidence output, Default behavior, Tests, Files changed, Safety confirmation, Remaining risks, Recommended next goal.
```

## 2. Controlled Active LASE Policy Gate

```text
/goal Extend the controlled LASE policy gate for off, shadow, recommend, and active-experiment modes with richer operator review, severity labels, and recommendation-acceptance evidence. Keep default behavior unchanged and keep any influence explicit, auditable, reversible, and local/experiment-scoped unless separately approved.

Automation mode:
Work autonomously through audit, implementation, tests, docs, PR, merge, and main resync.

Hard boundaries:
Do not promote LASE to unmanaged production traffic control. Do not create releases, tags, assets, containers, container signatures, workflows, rulesets, native artifacts, external services, secrets, live cloud calls, private-network execution, DNS probing, port scanning, or network discovery. Preserve release-downloads/. Do not weaken existing security or rate-limit defaults.

Preflight:
Run the standard repo state commands, confirm main is synced, confirm v2.5.0 tag target, and confirm no tracked changes.

Checkpoints:
1. Audit existing policy engine, lab runs, allocation evaluation, policy config, audit events, smoke evidence, and docs.
2. Add reviewer severity labels for guardrail and rollback reasons.
3. Add optional mock operator-acceptance evidence for recommend mode without changing production defaults.
4. Add tests proving off/shadow defaults preserve allocation, recommend does not mutate by default, and active-experiment changes only expected local experiment outcomes.
5. Update docs, status output, and reviewer flows.
6. Validate all tests and safe smoke dry-runs.

Validation:
Run focused LASE/allocation tests, full Maven validation, package, diff check, operator/Postman dry-runs, and adaptive experiment evidence generation.

Final report:
Overall Status, Goal Status, Policy modes, Default behavior, Audit output, Rollback reasons, Runtime behavior changed, Tests, PR URL, Merge commit, Safety confirmation, Remaining risks.
```

## 3. Observability Packs

```text
/goal Add LoadBalancerPro Enterprise Lab observability packs with dashboard JSON, alert examples, SLO templates, and local evidence generation while keeping claims lab-grade and source-visible.

Automation mode:
Work autonomously through docs/tests/scripts PRs and merge passing checks.

Hard boundaries:
No external telemetry services, secrets, scheduled tasks, persistence services, workflow/ruleset/default-branch mutations, release actions, container publication, container signing, live cloud calls, private-network execution, DNS probing, port scanning, native binaries, or vendored executables. Preserve release-downloads/.

Preflight:
Run standard git state and tag verification commands from the roadmap.

Checkpoints:
1. Audit current metrics, proxy status, actuator/Prometheus docs, and evidence.
2. Add dashboard JSON examples for lab metrics and proxy/routing surfaces.
3. Add alert examples that distinguish local lab thresholds from production thresholds.
4. Add SLO templates marked as templates.
5. Add source-visible local evidence generation under target/observability-pack/.
6. Add static tests for dashboard links, no production SLO overclaiming, and no secrets.
7. Validate.

Validation:
Focused docs/static tests, mvn -q clean test, mvn -q verify, mvn -q -DskipTests package, git diff --check, safe smoke dry-runs.

Final report:
Overall Status, Goal Status, Dashboard pack, Alerts, SLO templates, Evidence path, Claims boundary, Tests, Files changed, Safety confirmation, Remaining risks.
```

## 4. Measured Performance Baseline

```text
/goal Replace the unmeasured performance baseline template with a safe measured local baseline path using stable fixtures, a source-visible script, ignored target/performance-baseline/ output, and honest lab-only interpretation.

Automation mode:
Work autonomously across safe PRs; merge passing checks and resync main.

Hard boundaries:
No production benchmark claims, external load services, live cloud/private-network traffic, DNS probing, port scanning, service installation, native helper binaries, vendored executables, release actions, container publication, container signing, workflow/ruleset/default-branch mutation, or release-downloads mutation.

Preflight:
Run standard repo state, main sync, and tag verification commands.

Checkpoints:
1. Audit current performance baseline docs/tests and available request fixtures.
2. Add or stabilize local request fixtures for health, allocation, evaluation, routing comparison, and lab scenarios.
3. Add a PowerShell source-visible local baseline script that writes only target/performance-baseline/.
4. Capture environment metadata, commands, response summaries, and limitations.
5. Update docs to distinguish local measurements from production SLOs.
6. Add static tests preventing unmeasured claims from being presented as measured results.
7. Validate.

Validation:
Focused performance/docs tests, full Maven test/verify/package, git diff --check, safe dry-run script mode, and measured mode only against loopback when supported.

Final report:
Overall Status, Goal Status, Fixture set, Script path, Evidence path, Measurements captured, Limitations, Tests, Safety confirmation, Remaining risks.
```

## 5. Enterprise Auth Proof Lane

```text
/goal Build an enterprise auth proof lane using mock IdP/JWKS fixtures and local tokens so reviewers can verify dedicated role claims, scope-only denial, missing-role fail-closed behavior, and key-rotation guidance without real tenant secrets.

Automation mode:
Work autonomously through implementation, docs, tests, PR, merge, and main resync.

Hard boundaries:
No real tenant IDs, client secrets, external IdP dependencies, secret persistence, live network validation, workflow/ruleset/default-branch mutation, release actions, container publication, container signing, native binaries, or release-downloads mutation. Do not weaken OAuth2 behavior.

Preflight:
Run standard repo state, main sync, and tag verification commands.

Checkpoints:
1. Audit OAuth2 config, tests, IdP docs, and claim mapping examples.
2. Add mock JWKS/token fixtures or test-only helpers with synthetic non-secret keys.
3. Prove scope-only operator/admin does not grant app roles.
4. Prove dedicated role claims grant expected authorities.
5. Prove missing/ambiguous role claims fail closed.
6. Document token lifetime, key rotation, role lifecycle, and local proof commands.
7. Add static tests against stale scope/scp role claims.
8. Validate.

Validation:
Focused OAuth2/security/docs tests, full Maven validation, package, diff check, operator/Postman dry-runs if docs reference them.

Final report:
Overall Status, Goal Status, Mock IdP/JWKS proof, Scope-only denial, Dedicated role grant, Fail-closed behavior, Docs, Tests, Safety confirmation, Remaining risks.
```

## 6. Container Distribution Readiness

```text
/goal Prepare container distribution readiness for LoadBalancerPro without publishing or signing containers. Produce registry, tag, digest, scan, signing, attestation, rollback, retention, and credential-handling gates.

Automation mode:
Work autonomously across docs/tests/scripts PRs and merge passing checks.

Hard boundaries:
Do not publish containers, push to registries, sign containers, create tags, create GitHub Releases, upload release assets, mutate workflows unless separately justified, mutate rulesets/default branch settings, add secrets, add external services, add native binaries, or mutate release-downloads/.

Preflight:
Run standard repo state, main sync, and v2.5.0 tag verification commands.

Checkpoints:
1. Audit Dockerfile, container docs, release evidence, Trivy posture, and rollout plan.
2. Select candidate registry policy without credentials.
3. Define image name, tag, immutable digest, provenance, SBOM, vulnerability scan, signing, attestation, rollback, and retention gates.
4. Add dry-run validation or static checks only; no publication.
5. Add exact future-release authorization prompt template.
6. Add static tests that no executable publication/signing command is present.
7. Validate.

Validation:
Focused docs/static tests, full Maven validation, package, diff check, Docker smoke only if already supported and local-safe.

Final report:
Overall Status, Goal Status, Registry decision, Tag/digest policy, Signing plan, Scan evidence, Rollback/retention, Publication status, Tests, Safety confirmation, Remaining risks.
```

## 7. Disposable Live Sandbox Lab

```text
/goal Design a disposable live sandbox lab for future AWS validation with IAM templates, budget guardrails, teardown, operator approval, and evidence requirements. Do not execute live cloud validation in this goal.

Automation mode:
Work autonomously through docs/tests PRs, merge passing checks, and resync main.

Hard boundaries:
No live cloud calls, no AWS resource creation, no secrets, no secret persistence, no external services, no DNS probing, no port scanning, no network discovery, no workflow/ruleset/default-branch mutation, no releases, no containers, no native binaries, and no release-downloads mutation.

Preflight:
Run standard repo state, main sync, and v2.5.0 tag verification commands. Confirm no AWS credentials are required or consumed.

Checkpoints:
1. Audit CloudManager guardrails, cloud-sandbox docs, safety invariants, and residual risks.
2. Define disposable sandbox account/region assumptions and operator-owned controls.
3. Draft least-privilege IAM template guidance without secrets.
4. Define budget, naming, ownership, capacity, teardown, and evidence gates.
5. Define exact future live-validation authorization prompt.
6. Add static tests that default CI and smoke paths do not call live cloud.
7. Validate.

Validation:
Focused docs/static tests, full Maven validation, package, diff check, operator dry-runs. No live cloud execution.

Final report:
Overall Status, Goal Status, Sandbox plan, IAM guidance, Budget/teardown guardrails, Evidence gates, Live execution performed: no, Tests, Safety confirmation, Remaining risks.
```

