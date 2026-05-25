# Evidence Audit Compose Local-Lab Audit

This note is slot 8 of the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation/test-only. It audits `lab/docker-compose/local-lab-compose.yml` without changing Compose behavior, Dockerfiles, Maven, CI, scripts, runtime resources, endpoints, k6, Bruno, Toxiproxy behavior, runner services, automation, secrets, external targets, or production behavior.

## Audit Timestamp

- Audit timestamp: 2026-05-25T03:20-07:00.
- Audited repository: `RicheyWorks/LoadBalancerPro`.
- Audited base branch: `main`.
- Slot 8 branch: `codex/evidence-audit-compose-local-lab`.
- Starting main HEAD: `399f83ba0fec96542c544643ad214d8e4937072d`.
- Prior slot fact: PR #322 merged as `399f83ba0fec96542c544643ad214d8e4937072d`; post-merge main CI and CodeQL were green before slot 8 started.

## Scope

This audit is an inspection-only reviewer record for the local-lab Compose file. It does not edit `lab/docker-compose/local-lab-compose.yml`, does not run Docker, does not run Compose, does not add services, and does not turn optional local-lab commands into CI or Maven behavior.

## Current Compose Services

The current local-lab Compose file declares two services:

| Service | Current role | Current boundary |
| --- | --- | --- |
| `app-under-test` | Optional manual local-lab app container using an already-built local JAR | Local-lab-only, manual-only, package-first, read-only `target/` mount, loopback published port |
| `toxiproxy` | Optional manual local-lab Toxiproxy service using the checked-in local-lab Toxiproxy config | Local-lab-only, manual-only, loopback published ports, read-only config mount |

No k6 runner service exists. No Bruno runner service exists. No new Compose services are added by this audit.

## App-Under-Test Service Audit

The `app-under-test` service currently declares:

- image: `eclipse-temurin:21-jre`;
- working directory: `/opt/loadbalancerpro`;
- command: `java -jar /opt/loadbalancerpro/LoadBalancerPro-2.5.0.jar`;
- published port: `127.0.0.1:8080:8080`;
- volume: `../../target:/opt/loadbalancerpro:ro`;
- local-lab labels for `local-lab-only`, `manual-only`, `not-ci-gated`, `not-wired`, and `manual-package-first`.

Reviewer interpretation:

- The service depends on a manual Maven package step before optional Compose use.
- The service mounts the local `target/` directory read-only.
- The service does not build an image.
- The service does not modify Dockerfile packaging.
- The service does not publish an image.
- The service is not CI-gated and not Maven-wired.
- The service is not production Docker packaging and not production Compose.

## Toxiproxy Service Audit

The `toxiproxy` service currently declares:

- image: `shopify/toxiproxy:2.12.0`;
- command: `toxiproxy-server -config /etc/toxiproxy/local-lab-toxiproxy.json`;
- published ports: `127.0.0.1:8474:8474`, `127.0.0.1:18080:18080`, and `127.0.0.1:18081:18081`;
- read-only config volume: `../toxiproxy/local-lab-toxiproxy.json:/etc/toxiproxy/local-lab-toxiproxy.json:ro`;
- local-lab labels for `local-lab-only`, `manual-only`, `not-ci-gated`, and `not-wired`.

The referenced Toxiproxy config keeps loopback-only upstreams:

- `local-lab-app-loopback`: listens on `127.0.0.1:18080` and targets `127.0.0.1:8080`;
- `local-lab-backend-loopback`: listens on `127.0.0.1:18081` and targets `127.0.0.1:18082`.

Reviewer interpretation:

- Toxiproxy remains optional/manual/local-lab-only.
- Toxiproxy remains loopback-targeted.
- This audit does not add toxics, automate fault injection, or change Toxiproxy behavior.

## Loopback And Target Boundary

Current published ports bind to `127.0.0.1` only. The audit found no `0.0.0.0` Compose exposure, no external URL, no production-looking domain, no cloud endpoint, no tenant target, no private-network default target, and no secrets or credentials in `lab/docker-compose/local-lab-compose.yml`.

Reviewers should continue to stop any future Compose PR if a default target widens away from loopback, introduces production/cloud/tenant/external endpoints, introduces secrets, or adds a runner service without a separately scoped gate.

## CI, Maven, And Packaging Boundary

The current local-lab Compose file is optional/manual/local-lab-only. It is not CI-gated. It is not Maven-wired. It does not change production Docker packaging. It does not change production Compose. It does not change production runtime behavior.

The app service uses a public JRE image and a local read-only JAR mount; this is a local-lab convenience only. It is not a production image build, a registry publication, a signing lane, or a release lane.

## Java Runtime Parity Question

The project and Dockerfile runtime posture are Java 17-oriented. The local-lab Compose app service currently uses `eclipse-temurin:21-jre`. This audit records a Java 21 Compose runtime versus Java 17 project/runtime parity question for reviewers.

This is not treated as a production failure in this docs/test-only slot, and this slot does not change the Compose image. A future separately scoped PR should decide whether the local-lab Compose app service should remain on Java 21, move to Java 17 for parity with the Dockerfile runtime, or document a stronger reason for the mismatch.

## Reviewer Questions

- Does `lab/docker-compose/local-lab-compose.yml` still contain only `app-under-test` and `toxiproxy`?
- Are all published ports bound to `127.0.0.1`?
- Is there any `0.0.0.0` default exposure?
- Are there any secrets or credentials?
- Are there any external, cloud, tenant, private-network, or production-looking targets?
- Are k6 and Bruno still manual and separate?
- Is there any k6 runner service or Bruno runner service?
- Is Compose still not CI-gated and not Maven-wired?
- Is the app service still package-first and using a read-only `target/` mount?
- Has the Java 21 Compose runtime versus Java 17 project/runtime parity question been reviewed before any future implementation change?

## Remaining Limits

This audit is a static repository audit. It does not run Docker, run Compose, start services, call endpoints, create traffic, execute k6, execute Bruno, alter Toxiproxy toxics, inspect live container state, or produce runtime proof.

This audit does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, registry publication, container signing, production telemetry, production monitoring, or broader automation.

## Navigation

- Repository evidence map: [`EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md`](EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md).
- Campaign board: [`EVIDENCE_AUDIT_CAMPAIGN_BOARD.md`](EVIDENCE_AUDIT_CAMPAIGN_BOARD.md).
- Dockerfile runtime audit: [`EVIDENCE_AUDIT_DOCKERFILE_RUNTIME_AUDIT.md`](EVIDENCE_AUDIT_DOCKERFILE_RUNTIME_AUDIT.md).
- Compose app-service runbook: [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md).
- Compose manual runbook: [`../LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](../LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).
- Runner-service gate: [`../LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md`](../LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md).
