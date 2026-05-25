# Evidence Audit Repository Evidence Map

This note is slot 3 of the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation/test-only. It maps repository evidence surfaces for reviewer navigation without changing code, Maven, CI, Docker, Compose, scripts, runtime resources, endpoints, k6, Bruno, Toxiproxy, runner services, automation, secrets, external targets, or production behavior.

## Audit Timestamp

- Audit timestamp: 2026-05-25T00:29-07:00.
- Audited repository: `RicheyWorks/LoadBalancerPro`.
- Audited base branch: `main`.
- Slot 3 branch: `codex/evidence-audit-repository-map`.
- Starting main HEAD: `7dd64becaefd589ff94ed2fea93b017397b4a747`.

## Purpose

This evidence map gives reviewers a single first-pass index across public claims, agent rules, build contracts, CI, CodeQL, Docker, Compose, local-lab, smoke evidence, runtime configuration, proxy demo fixtures, and campaign state. It is not an implementation audit result for every surface. Later campaign slots perform deeper, separately scoped audits for CI, CodeQL, Maven, Dockerfile, Compose/local-lab, runtime configuration, proxy demo fixtures, smoke scripts, public claims, and not-proven gaps.

## Evidence Surface Map

| Surface | Tracked path | Reviewer question | Current evidence boundary |
| --- | --- | --- | --- |
| README public trust surface | [`README.md`](../../README.md) | What is LoadBalancerPro, what is it not, and where should a reviewer start? | Human front door and high-level claim contract only; it does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, or replay/evidence/report/storage/export proof. |
| Reviewer Trust Map | [`docs/REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md) | Which docs or local pages answer reviewer questions? | Navigation surface only; it links evidence paths and not-proven boundaries but does not create new evidence. |
| Agent operating rules | [`AGENTS.md`](../../AGENTS.md) | What rules should Codex/agents follow while editing? | Agent discipline and scope rules only; it does not authorize production behavior changes or weaker guardrails. |
| Task contract template | [`BUILD_CONTRACT.md`](../../BUILD_CONTRACT.md) | How should a scoped task be framed and reported? | Reusable task contract only; each branch still needs its own explicit scope and verification. |
| Verification protocol | [`docs/agent/VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md) | Which focused, full, remote, and post-merge checks are expected? | Procedure guidance only; a green claim requires actual current-head and post-merge checks. |
| Session and failure state | [`docs/agent/SESSION_MANAGER.md`](SESSION_MANAGER.md), [`docs/agent/FAILURE_LOG.md`](FAILURE_LOG.md) | What is the current checkpoint and what failures occurred? | Moving campaign/session record only; it must be updated factually and cannot replace verification. |
| Campaign docs | [`docs/agent/EVIDENCE_AUDIT_CAMPAIGN_CONTRACT.md`](EVIDENCE_AUDIT_CAMPAIGN_CONTRACT.md), [`docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md`](EVIDENCE_AUDIT_CAMPAIGN_BOARD.md), [`docs/agent/EVIDENCE_AUDIT_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](EVIDENCE_AUDIT_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`docs/agent/EVIDENCE_AUDIT_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](EVIDENCE_AUDIT_CAMPAIGN_FINAL_REPORT_TEMPLATE.md) | What is this 20-PR campaign and how are slots counted? | Campaign control plane only; a slot counts only after its PR merges and main CI/CodeQL are green. |
| CI workflow | [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml), [`docs/agent/EVIDENCE_AUDIT_CI_WORKFLOW_AUDIT.md`](EVIDENCE_AUDIT_CI_WORKFLOW_AUDIT.md) | What does CI run for build, tests, packaging, smoke, SBOM, Docker, Trivy, dependency review, and artifacts? | Workflow definition plus slot 4 audit note only; slot 4 audits posture without editing workflow behavior. |
| CodeQL workflow | [`.github/workflows/codeql.yml`](../../.github/workflows/codeql.yml), [`docs/agent/EVIDENCE_AUDIT_CODEQL_DEPENDENCY_REVIEW_AUDIT.md`](EVIDENCE_AUDIT_CODEQL_DEPENDENCY_REVIEW_AUDIT.md) | How does static analysis and pull-request dependency review run, and what do they not prove? | Workflow definitions plus slot 5 audit note only; slot 5 audits CodeQL/dependency-review posture without editing workflow behavior. |
| Release artifacts workflow | [`.github/workflows/release-artifacts.yml`](../../.github/workflows/release-artifacts.yml) | What release-artifact automation exists? | Workflow definition only; this map does not create tags, releases, assets, or release approval. |
| Maven/dependency posture | [`pom.xml`](../../pom.xml), [`docs/agent/EVIDENCE_AUDIT_MAVEN_DEPENDENCY_POSTURE_AUDIT.md`](EVIDENCE_AUDIT_MAVEN_DEPENDENCY_POSTURE_AUDIT.md) | Which Java, Spring Boot, dependency, and plugin choices are declared? | Build configuration surface plus slot 6 audit note only; slot 6 audits posture without dependency upgrades or Maven config changes. |
| Docker runtime packaging | [`Dockerfile`](../../Dockerfile), [`docs/agent/EVIDENCE_AUDIT_DOCKERFILE_RUNTIME_AUDIT.md`](EVIDENCE_AUDIT_DOCKERFILE_RUNTIME_AUDIT.md) | What container build/runtime posture is declared? | Dockerfile source plus slot 7 audit note only; slot 7 audits posture without Dockerfile edits or publish/sign/release claims. |
| Local-lab Compose | [`lab/docker-compose/local-lab-compose.yml`](../../lab/docker-compose/local-lab-compose.yml), [`docs/agent/EVIDENCE_AUDIT_COMPOSE_LOCAL_LAB_AUDIT.md`](EVIDENCE_AUDIT_COMPOSE_LOCAL_LAB_AUDIT.md) | What local-lab services are currently declared? | Manual/local-lab-only Compose surface with app-under-test and Toxiproxy; slot 8 audits without Compose edits and without adding runner services. |
| Local-lab docs | [`docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md), [`docs/LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md`](../LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md), [`docs/LOCAL_LAB_PROGRESS_HANDOFF.md`](../LOCAL_LAB_PROGRESS_HANDOFF.md) | How should reviewers interpret the app-service, runner gates, and local-lab handoffs? | Documentation boundary only; k6 and Bruno remain manual and separate, and no runner services exist. |
| Smoke scripts | [`scripts/smoke/enterprise-lab-workflow.ps1`](../../scripts/smoke/enterprise-lab-workflow.ps1) and sibling `scripts/smoke/*.ps1` files | Which local smoke helpers exist and where do they write evidence? | Script source only; slot 12 audits the enterprise lab workflow smoke boundary without editing scripts or claiming cloud mutation. |
| Runtime configuration | [`src/main/resources/application.properties`](../../src/main/resources/application.properties), [`src/main/resources/application-prod.properties`](../../src/main/resources/application-prod.properties), profile-specific `src/main/resources/application-*.properties`, [`docs/agent/EVIDENCE_AUDIT_RUNTIME_CONFIGURATION_AUDIT.md`](EVIDENCE_AUDIT_RUNTIME_CONFIGURATION_AUDIT.md) | Which actuator, auth, CORS, proxy, telemetry, and demo defaults exist? | Runtime config source plus slot 9 audit note only; slot 9 audits configuration without changing runtime resources or behavior. |
| Proxy demo fixture | [`src/main/java/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.java`](../../src/main/java/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.java), [`scripts/proxy-demo.ps1`](../../scripts/proxy-demo.ps1), [`scripts/proxy-demo.sh`](../../scripts/proxy-demo.sh), proxy demo profile files, [`docs/PROXY_DEMO_FIXTURE_LAUNCHER.md`](../PROXY_DEMO_FIXTURE_LAUNCHER.md), [`docs/agent/EVIDENCE_AUDIT_PROXY_DEMO_FIXTURE_AUDIT.md`](EVIDENCE_AUDIT_PROXY_DEMO_FIXTURE_AUDIT.md) | How do local proxy demo fixtures and profiles stay loopback-only and non-production? | Source-visible fixture/profile/script surfaces plus slot 10 audit note only; slot 10 audits without running scripts, starting servers, calling endpoints, changing scripts, or claiming production gateway readiness. |
| Test-scope local-lab chain | `src/test/java/**/lab/**` documentation/test-scope guards and fixtures | Which local-lab test-scope proof chain exists? | Test-scope evidence only; slot 14 summarizes what it proves and does not prove. |
| Open PR hygiene | [`docs/agent/EVIDENCE_AUDIT_OPEN_PR_HYGIENE.md`](EVIDENCE_AUDIT_OPEN_PR_HYGIENE.md) | Which unrelated open PRs need human hygiene review? | Audit note only; it does not close, rebase, edit, or merge unrelated PRs. |

## Suggested Reviewer Path

1. Read [`README.md`](../../README.md) for the public trust surface and not-proven boundaries.
2. Use [`docs/REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md) for reviewer navigation.
3. Use this map to choose the evidence surface that matches the question.
4. Use later slot artifacts for deeper per-surface audits as they are added.
5. Treat `SESSION_MANAGER.md`, `FAILURE_LOG.md`, PR checks, and main checks as factual evidence only when they match the current head and current repository state.

## Not-Proven Boundaries

This repository evidence map does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, registry publication, container signing, production telemetry, production monitoring, or broader automation.
