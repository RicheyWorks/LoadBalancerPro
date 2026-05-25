# Evidence Audit Proxy Demo Fixture Audit

This note is slot 10 of the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation/test-only. It audits the local proxy demo fixture and demo profiles without changing proxy fixture code, scripts, runtime resources, endpoints, Maven, CI, Dockerfile, Compose behavior, k6, Bruno, Toxiproxy, runner services, automation, secrets, external targets, or production behavior.

## Audit Timestamp

- Audit timestamp: 2026-05-25T04:16-07:00.
- Audited repository: `RicheyWorks/LoadBalancerPro`.
- Audited base branch: `main`.
- Slot 10 branch: `codex/evidence-audit-proxy-demo-fixture`.
- Starting main HEAD: `6f5d0d88502fb86fdc94f5261c709a2356dee65a`.
- Prior slot fact: PR #324 merged as `6f5d0d88502fb86fdc94f5261c709a2356dee65a`; final slot 9 head was `ecc0dbca270ff4f6b96c1f41c4ca7c0037569681`; post-merge main CI and CodeQL were green before slot 10 started.

## Scope

This audit is an inspection-only reviewer record for the local proxy demo fixture and demo profiles:

- `src/main/java/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.java`;
- `scripts/proxy-demo.ps1`;
- `scripts/proxy-demo.sh`;
- `src/main/resources/application-proxy-demo-round-robin.properties`;
- `src/main/resources/application-proxy-demo-weighted-round-robin.properties`;
- `src/main/resources/application-proxy-demo-failover.properties`;
- `docs/PROXY_DEMO_FIXTURE_LAUNCHER.md`;
- `docs/PROXY_DEMO_STACK.md`;
- `docs/examples/operator-run-profiles/proxy-loopback.properties`;
- `docs/examples/operator-run-profiles/local-demo.properties`.

It does not start the fixture, run scripts, launch Spring Boot, call proxy endpoints, open ports, or produce proxy evidence. It does not run scripts. It does not call proxy endpoints. It does not claim production gateway readiness.

## Loopback-Only Fixture Boundary

`ProxyDemoFixtureLauncher` is a Java-only local fixture for proxy demos. Static inspection shows these boundaries:

- The default fixture host is `127.0.0.1`.
- `--host` accepts only `127.0.0.1` or `localhost`.
- Non-loopback hostnames and broad host patterns are rejected by the launcher option parser before servers start.
- Fixture servers bind through `InetAddress.getByName(host)` after host normalization.
- The launcher help text states the fixture starts loopback fixture servers only and does not contact cloud services, require Python/Node/Docker/public internet, or persist demo state.
- The launcher source does not construct `CloudManager`.

Reviewer interpretation: the fixture is a local loopback helper for controlled reviewer/operator demos. It is not a security boundary, production gateway, cloud validation path, or external-network proof.

## Default Fixed Ports

The fixture source declares default fixed ports:

- `backend-a`: `127.0.0.1:18081`.
- `backend-b`: `127.0.0.1:18082`.

The checked-in demo profiles also point to those fixed loopback ports:

- `application-proxy-demo-round-robin.properties`;
- `application-proxy-demo-weighted-round-robin.properties`;
- `application-proxy-demo-failover.properties`.

All three profiles bind the app itself to `server.address=127.0.0.1` and `server.port=8080`.

Reviewer interpretation: the defaults are easy to inspect and repeat locally, but fixed ports can conflict with other local processes. Port-conflict behavior should be reviewed by humans when a local run fails to bind, and any future remediation should be separately scoped.

## Demo Profile Upstreams

The demo profiles are explicit opt-in Spring profiles:

- `proxy-demo-round-robin` uses `ROUND_ROBIN` with equal weights for `backend-a` and `backend-b`.
- `proxy-demo-weighted-round-robin` uses `WEIGHTED_ROUND_ROBIN` with weights `3.0` and `1.0`.
- `proxy-demo-failover` uses `ROUND_ROBIN` with active health checks so an unhealthy fixture can be skipped.

Each profile sets `loadbalancerpro.proxy.enabled=true` only inside the explicit demo profile and uses `http://127.0.0.1:18081` plus `http://127.0.0.1:18082` as upstream defaults. The default application profile keeps proxy mode disabled, as covered by the slot 9 runtime configuration audit.

Reviewer interpretation: these profiles are local demo activation profiles, not production defaults and not evidence that proxy mode is enabled for default application startup.

## Failover Controls

The fixture exposes local control paths:

- `/fixture/health/fail`;
- `/fixture/health/ok`;
- `/health`.

Failover mode starts `backend-b` as unhealthy, and the launcher prints local curl recipes to observe status and restore `backend-b` health. These controls are process-local to the fixture and disappear when the fixture is stopped.

Reviewer interpretation: the controls support local failover observation only. They do not create runtime enforcement, durable state, production failover proof, or benchmark evidence.

## Helper Script Boundary

`scripts/proxy-demo.ps1` and `scripts/proxy-demo.sh` expose the same local demo modes:

- `round-robin`;
- `weighted-round-robin`;
- `failover`;
- `status`.

The PowerShell script now prefers the cross-platform Java fixture launcher and retains a legacy PowerShell fixture fallback. This audit does not edit either script and does not run either script.

Reviewer interpretation: helper scripts are source-visible local helpers. They are not automation in CI, not Maven lifecycle wiring, not service installation, not scheduled tasks, not release tooling, and not production packaging.

## No Cloud Or External Network Claim

The inspected fixture and demo profile defaults use loopback URLs. This audit did not find source-visible public-cloud endpoint defaults, production-looking domains, secrets, credentials, registry pushes, release actions, or external target defaults in the audited proxy demo fixture lane.

The adjacent `ProxyBackendUrlClassifier` allows loopback and private-network URL classification for separate proxy validation surfaces, while rejecting public-network URLs and ambiguous host shapes. This slot does not change that classifier and does not turn private-network validation into live traffic proof.

Reviewer interpretation: loopback defaults are a local safety boundary. They do not prove safe public ingress, TLS, identity, WAF behavior, production networking, private-network live validation, or production monitoring.

## Reviewer Questions

- Does the fixture launcher still default to `127.0.0.1`?
- Does the launcher still accept only `127.0.0.1` or `localhost` for `--host`?
- Do the demo profiles still bind the app to `127.0.0.1:8080`?
- Do demo upstreams still point to `127.0.0.1:18081` and `127.0.0.1:18082`?
- Are fixed-port conflicts documented as a human review item rather than hidden production readiness proof?
- Do failover controls remain local fixture paths only?
- Do helper scripts remain local/manual helpers rather than CI/Maven automation?
- Are cloud, external-network, tenant, production-domain, secret, and credential defaults absent?
- Does the audit avoid changing proxy behavior, endpoints, scripts, runtime resources, or production packaging?

## Remaining Limits

This audit is static and source-visible only. It does not run the fixture, run helper scripts, run Docker, run Compose, start Spring Boot, call proxy endpoints, generate proxy evidence, or validate a deployed environment.

This audit does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, production gateway readiness, TLS termination, WAF behavior, identity integration, production telemetry, production monitoring, registry publication, container signing, or broader automation.

## Navigation

- Repository evidence map: [`EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md`](EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md).
- Campaign board: [`EVIDENCE_AUDIT_CAMPAIGN_BOARD.md`](EVIDENCE_AUDIT_CAMPAIGN_BOARD.md).
- Runtime configuration audit: [`EVIDENCE_AUDIT_RUNTIME_CONFIGURATION_AUDIT.md`](EVIDENCE_AUDIT_RUNTIME_CONFIGURATION_AUDIT.md).
- Proxy demo fixture launcher: [`../PROXY_DEMO_FIXTURE_LAUNCHER.md`](../PROXY_DEMO_FIXTURE_LAUNCHER.md).
- Proxy demo stack: [`../PROXY_DEMO_STACK.md`](../PROXY_DEMO_STACK.md).
- Reverse proxy mode: [`../REVERSE_PROXY_MODE.md`](../REVERSE_PROXY_MODE.md).
- Operator install run matrix: [`../OPERATOR_INSTALL_RUN_MATRIX.md`](../OPERATOR_INSTALL_RUN_MATRIX.md).
