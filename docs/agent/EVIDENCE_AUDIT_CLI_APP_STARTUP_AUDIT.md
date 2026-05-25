# Evidence Audit CLI App Startup Audit

This note is slot 11 of the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation/test-only. It audits CLI mode dispatch and Spring Boot startup boundaries without changing production code, Maven, CI/workflow files, Dockerfile, Compose behavior, scripts, runtime resources, endpoints, k6, Bruno, Toxiproxy, runner services, automation, secrets, external targets, or production behavior.

Short label: CLI mode and app startup audit.

## Audit Timestamp

- Audit timestamp: 2026-05-25T04:41-07:00.
- Audited repository: `RicheyWorks/LoadBalancerPro`.
- Audited base branch: `main`.
- Slot 11 branch: `codex/evidence-audit-cli-app-startup`.
- Starting main HEAD: `d4a07057c7e0475e012e610a551733184d26791d`.
- Prior slot fact: PR #325 merged as `d4a07057c7e0475e012e610a551733184d26791d`; final slot 10 head was `4bad0291be2a36ed7695bb47fa3b9a3e63d4dbb0`; post-merge main CI and CodeQL were green before slot 11 started.

## Scope

This audit is an inspection-only reviewer record for CLI mode and app startup dispatch:

- `src/main/java/com/richmond423/loadbalancerpro/api/LoadBalancerApiApplication.java`;
- `src/main/java/com/richmond423/loadbalancerpro/cli/AdaptiveRoutingExperimentCommand.java`;
- `src/main/java/com/richmond423/loadbalancerpro/cli/EnterpriseLabWorkflowCommand.java`;
- `src/main/java/com/richmond423/loadbalancerpro/cli/LaseReplayCommand.java`;
- `src/main/java/com/richmond423/loadbalancerpro/cli/LaseDemoCommand.java`;
- `src/test/java/com/richmond423/loadbalancerpro/api/LoadBalancerApiApplicationTest.java`;
- adjacent command tests under `src/test/java/com/richmond423/loadbalancerpro/cli/`.

It does not run CLI commands, start Spring Boot, call endpoints, write evidence, launch scripts, open ports, execute replay, mutate routing state, or change app startup behavior. It does not start Spring Boot. It does not change app startup behavior.

## Dispatch Boundary

`LoadBalancerApiApplication.main` has a small dispatch order that reviewers can inspect:

1. `--version` is checked first, prints `LoadBalancerPro version ...`, and returns before Spring starts.
2. `shouldStartApi(args)` returns `false` for known local/offline CLI modes.
3. `AdaptiveRoutingExperimentCommand.runIfRequested` handles `--adaptive-routing-experiment` before API startup.
4. `EnterpriseLabWorkflowCommand.runIfRequested` handles `--enterprise-lab-workflow` before API startup.
5. `LaseReplayCommand.runIfRequested` handles `--lase-replay=<path>` before API startup.
6. `LaseDemoCommand.runIfRequested` handles `--lase-demo` before API startup.
7. `SpringApplication.run(LoadBalancerApiApplication.class, args)` is reached only when those CLI modes are not requested.

Reviewer interpretation: CLI modes are explicit startup alternatives, not background services, endpoint additions, CI jobs, Maven lifecycle hooks, or production gateway behavior.

## Mode Audit

| Mode | Dispatch evidence | Current boundary |
| --- | --- | --- |
| `--version` | `isVersionRequested(args)` runs before `shouldStartApi(args)` and `SpringApplication.run`. | Version output exits early and does not start the API server. The fallback version is `2.5.0` when package metadata is unavailable. |
| `--adaptive-routing-experiment` | `AdaptiveRoutingExperimentCommand.isRequested(args)` is part of `shouldStartApi(args)`. | Deterministic local experiment harness; command output states no API server, no live cloud mutation, no external network, and no release action. |
| `--enterprise-lab-workflow` | `EnterpriseLabWorkflowCommand.isRequested(args)` is part of `shouldStartApi(args)`. | Local workflow output goes under `target/enterprise-lab-runs` by default and states no API server, live cloud, external network, release, tag, asset, container, or registry action. |
| `--lase-replay=<path>` | `LaseReplayCommand.isRequested(args)` is part of `shouldStartApi(args)`. | Offline/read-only replay over a caller-provided local file; usage text states no API server, network access, CloudManager calls, or cloud mutation. |
| `--lase-demo` | `LaseDemoCommand.isRequested(args)` is part of `shouldStartApi(args)`. | Synthetic recommendation-only local demo; output states no live AWS resources, real routing mutation, CloudManager calls, AWS keys, network access, or API server requirement. |
| normal API startup | `SpringApplication.run` executes when no CLI mode is requested. | Spring Boot startup is the default app path for ordinary server args, such as `--server.port=18080`, and for empty args. |

## Smoke Coverage Expectations

Current test coverage includes focused startup and command expectations:

- `LoadBalancerApiApplicationTest` asserts `--version`, `--adaptive-routing-experiment`, `--lase-replay`, and `--lase-demo` do not start the API path through `shouldStartApi`.
- `LoadBalancerApiApplicationTest` asserts ordinary server args and empty args still start the API path.
- `EnterpriseLabWorkflowCommandTest` covers `--enterprise-lab-workflow` request parsing, and `AdaptiveRoutingExperimentCommandTest`, `EnterpriseLabWorkflowCommandTest`, and `LaseReplayCommandTest` assert command output does not include `Started LoadBalancerApiApplication`.
- `LaseDemoCommandTest` covers LASE demo request parsing and safe command results.
- The enterprise lab package smoke path exercises the packaged workflow command as local target evidence only; it does not promote the command to CI/Maven wiring beyond existing checks.

Reviewer interpretation: these tests and smoke checks are useful for dispatch confidence, but they are not production readiness, endpoint validation, runtime enforcement, or performance evidence.

## Reviewer Questions

- Does `--version` return before any Spring Boot startup?
- Do known CLI modes make `shouldStartApi` return `false`?
- Do normal server args and empty args still reach Spring Boot startup?
- Do local CLI modes state their no-cloud/no-network/no-API-server boundaries?
- Does enterprise lab workflow evidence stay under `target/`?
- Does LASE replay remain offline/read-only and caller-file based?
- Does LASE demo remain synthetic and recommendation-only?
- Do tests cover CLI dispatch without inventing new endpoints or runtime behavior?

## Remaining Limits

This audit is static and source-visible only. It does not execute CLI commands, start Spring Boot, run scripts, call endpoints, run Docker, run Compose, produce replay evidence, export reports, or validate a deployed environment.

This audit does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, production gateway readiness, registry publication, container signing, production telemetry, production monitoring, or broader automation.

## Navigation

- Repository evidence map: [`EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md`](EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md).
- Campaign board: [`EVIDENCE_AUDIT_CAMPAIGN_BOARD.md`](EVIDENCE_AUDIT_CAMPAIGN_BOARD.md).
- Runtime configuration audit: [`EVIDENCE_AUDIT_RUNTIME_CONFIGURATION_AUDIT.md`](EVIDENCE_AUDIT_RUNTIME_CONFIGURATION_AUDIT.md).
- Proxy demo fixture audit: [`EVIDENCE_AUDIT_PROXY_DEMO_FIXTURE_AUDIT.md`](EVIDENCE_AUDIT_PROXY_DEMO_FIXTURE_AUDIT.md).
- Enterprise lab workflow docs: [`../ENTERPRISE_LAB_WORKFLOW.md`](../ENTERPRISE_LAB_WORKFLOW.md).
