# Evidence Audit Maven Dependency Posture Audit

This note is slot 6 of the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation/test-only. It audits the current `pom.xml` dependency and Maven plugin posture without changing Maven configuration, dependency versions, CI workflow behavior, Dockerfile contents, Compose behavior, scripts, runtime resources, endpoints, app behavior, runner services, automation, secrets, external targets, or production behavior.

## Audit Timestamp

- Audit timestamp: 2026-05-25T02:14-07:00.
- Audited repository: `RicheyWorks/LoadBalancerPro`.
- Audited base branch: `main`.
- Slot 6 branch: `codex/evidence-audit-maven-dependency-posture`.
- Starting main HEAD: `a58d61511d84b8d9013d5a2652dc696fb555e83c`.
- Audited Maven source: [`pom.xml`](../../pom.xml).
- Prior slot fact: slot 5 PR #320 merged as `a58d61511d84b8d9013d5a2652dc696fb555e83c` and post-merge main CI/CodeQL were green before slot 6 started.

## Purpose

This audit gives reviewers a source-readable summary of the Maven dependency and plugin declarations currently in `pom.xml`. It records what the build declares and what reviewers may not infer from that declaration. It is not a dependency upgrade, not a Maven behavior change, not a CI behavior change, not a vulnerability remediation claim, not production hardening, not release approval, and not production certification.

## Declared Project And Java Posture

The Maven project currently declares:

- `groupId` `com.richmond423`;
- `artifactId` `LoadBalancerPro`;
- version `2.5.0`;
- Java release property `java.version` set to `17`;
- UTF-8 build and reporting encodings.

The compiler plugin uses `<release>${java.version}</release>` with `proc` set to `none`. This makes Java 17 the declared Maven compile target. It does not by itself prove runtime JDK parity across local machines, CI runners, Docker images, local-lab Compose services, or production environments.

## Dependency Management Posture

The `dependencyManagement` section currently includes:

- Netty BOM `io.netty:netty-bom` version `${netty.version}`, currently `4.2.13.Final`;
- Spring Boot dependency BOM `org.springframework.boot:spring-boot-dependencies` version `${spring-boot.version}`, currently `3.5.14`;
- explicit Tomcat embedded overrides for `tomcat-embed-core`, `tomcat-embed-el`, and `tomcat-embed-websocket` at `${tomcat.version}`, currently `10.1.55`;
- AWS SDK v2 BOM `software.amazon.awssdk:bom` version `${aws-sdk-v2.version}`, currently `2.44.4`.

This posture centralizes several high-impact version families. It does not prove dependency freshness, absence of future vulnerabilities, absence of transitive risk, runtime safety, or production suitability.

## Runtime Dependency Families

The runtime dependency surface currently includes:

- Spring Boot web starter;
- Spring Boot actuator starter;
- Spring Boot security starter;
- Spring Boot OAuth2 resource server starter;
- Springdoc OpenAPI WebMVC UI `2.8.17`;
- Micrometer Prometheus and OTLP registries;
- JavaFX controls `17.0.19`;
- Spring Boot validation starter;
- Log4j API and Core;
- optional JSON libraries `org.json:json` version `20251224` and `com.google.code.gson:gson` version `2.14.0`;
- Caffeine `3.2.4`;
- Reactor Core;
- AWS SDK v2 clients for Auto Scaling, CloudWatch, and EC2.

The presence of AWS SDK clients does not mean the default app path mutates cloud resources. Cloud behavior remains bounded by the project guardrails, runtime configuration, and tests that keep live mutation disabled unless explicitly configured.

## Test Dependency Posture

The test dependency surface currently includes:

- `spring-boot-starter-test` with test scope;
- an exclusion for `com.vaadin.external.google:android-json`;
- `spring-security-test` with test scope.

These dependencies support local and CI test coverage. They do not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, or replay/evidence/report/storage/export proof.

## Maven Plugin Posture

The build plugin surface currently includes:

- `maven-compiler-plugin` `3.15.0`, configured for Java 17 release compilation and no annotation processing;
- `maven-surefire-plugin` `3.5.5`, configured with the Mockito Java agent path;
- `exec-maven-plugin` `3.5.0`, present for optional local operator launcher recipes with no execution bound to the default lifecycle;
- `jacoco-maven-plugin` `${jacoco.version}`, currently `0.8.13`, with `prepare-agent` and `report` executions;
- `maven-jar-plugin` `3.5.0`, adding default implementation and specification manifest entries;
- `spring-boot-maven-plugin` `${spring-boot.version}`, currently `3.5.14`, configured with Spring Boot main class `com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication`, `build-info`, and `repackage` executions.

This plugin posture supports tests, coverage, executable JAR packaging, and build metadata. It does not add CI/Maven wiring in this slot, does not publish artifacts, does not create releases, does not create container images, and does not deploy anything.

## Relationship To CI, CodeQL, And Dependency Review

This audit complements:

- the slot 4 [CI workflow audit](EVIDENCE_AUDIT_CI_WORKFLOW_AUDIT.md);
- the slot 5 [CodeQL and Dependency Review audit](EVIDENCE_AUDIT_CODEQL_DEPENDENCY_REVIEW_AUDIT.md);
- the slot 3 [repository evidence map](EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md).

CI runs Maven dependency tree, tests, packaging, artifact smoke checks, SBOM generation, Docker build/runtime smoke, Trivy scanning, and dependency review where applicable. CodeQL performs source/static-analysis scanning. This Maven audit describes the source Maven declaration only and does not replace those checks.

## Reviewer Questions

- Did the PR preserve `pom.xml` exactly?
- Did the PR avoid dependency upgrades or plugin changes?
- Did the PR avoid Maven wiring changes?
- Did the PR preserve Java 17 as the declared compile target?
- Did the PR preserve Spring Boot, Tomcat, Netty, AWS SDK, JavaFX, Log4j, org.json, Gson, Caffeine, Reactor, and test dependency declarations?
- Did the PR avoid CI workflow changes, Dockerfile changes, Compose changes, scripts, runtime resources, endpoints, app behavior, secrets, external targets, runner services, and automation?
- Does the PR avoid claiming production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, registry publication, container signing, or broader automation?

## Remaining Limits

Reviewers should keep these limits attached to this Maven posture audit:

- It is a static source audit of `pom.xml`, not dependency remediation.
- It does not prove every transitive dependency is safe forever.
- It does not prove dependency freshness.
- It does not prove all optional runtime paths are exercised.
- It does not prove runtime JDK parity across every environment.
- It does not prove CI, Docker, Compose, or production behavior.
- It does not create SBOMs, releases, registry publications, signatures, deployments, or runtime enforcement.
- It does not replace human review of future dependency changes.

## Not-Proven Boundaries

This Maven dependency posture audit does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, registry publication, container signing, production telemetry, production monitoring, release approval, full vulnerability management, incident response readiness, remediation SLA compliance, or broader automation.
