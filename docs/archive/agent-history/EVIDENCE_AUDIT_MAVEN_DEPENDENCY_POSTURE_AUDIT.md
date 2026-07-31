# Evidence Audit Maven Dependency Posture Audit

This note is slot 6 of the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation/test-only. It audits the current `pom.xml` dependency and Maven plugin posture without changing Maven configuration, dependency versions, CI workflow behavior, Dockerfile contents, Compose behavior, scripts, runtime resources, endpoints, app behavior, runner services, automation, secrets, external targets, or production behavior.

> Prospective current-state update (campaign slot L-4.2): this is a historical exact-commit audit. L-4.2 later removed the JavaFX desktop source, `javafx.version`, `org.openjfx:javafx-controls`, and their packaging exclusions. Historical `17.0.19` statements below remain evidence for the audited commit, not a description of the current `pom.xml`; the removal does not imply broader dependency remediation or security certification.

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

- Jackson BOM `com.fasterxml.jackson:jackson-bom` version `${jackson.version}`, currently `2.21.4`;
- Netty BOM `io.netty:netty-bom` version `${netty.version}`, currently `4.2.16.Final`;
- Spring Boot dependency BOM `org.springframework.boot:spring-boot-dependencies` version `${spring-boot.version}`, currently `3.5.16`;
- explicit Tomcat embedded overrides for `tomcat-embed-core`, `tomcat-embed-el`, and `tomcat-embed-websocket` at `${tomcat.version}`, currently `10.1.55`;
- AWS SDK v2 BOM `software.amazon.awssdk:bom` version `${aws-sdk-v2.version}`, currently `2.44.4`.

This posture centralizes several high-impact version families. It does not prove dependency freshness, absence of future vulnerabilities, absence of transitive risk, runtime safety, or production suitability.

The later 2026-07-16 security-maintenance refresh moved the centrally managed Netty family from `4.2.13.Final` to
`4.2.15.Final` and added Jackson BOM `2.21.4` ahead of Spring Boot's imported BOM so the packaged Jackson family no
longer resolves to vulnerable `jackson-databind` `2.21.2`; the historical slot 6 audit itself remained
documentation/test-only.

The 2026-07-29 blocking-image-scan recovery moves only the centrally managed Netty family from `4.2.15.Final` to
`4.2.16.Final`. Exact-head push and pull-request Trivy reports independently identified fixed HIGH findings
`CVE-2026-59901`, `CVE-2026-55831`, `CVE-2026-55833`, and `CVE-2026-56745` in the packaged `4.2.15.Final` family.
The recovery does not add an allowlist, weaken the scan, change other dependencies or plugins, or claim that the later
baseline is free from present or future vulnerabilities.

The 2026-07-30 isolated Spring security prerequisite moves only the Spring Boot BOM/plugin property from `3.5.14`
to `3.5.16`. Maven's resolved 171-coordinate dependency list has no added or removed coordinate and resolves Spring
Framework `6.2.19` and Spring Security `6.5.11`. This removes the `spring-expression`/`spring-webmvc` `6.2.18`
versions behind the exact-head image scan's HIGH `CVE-2026-41850`, `CVE-2026-41842`, and `CVE-2026-41845`
findings. It does not add a CVE allowlist or suppression, weaken a gate, or change application behavior,
configuration defaults, plugins, workflows, Docker configuration, credentials, or external targets.

The complete resolved-version delta from the Boot patch is:

| Managed family | From | To | Resolved artifacts |
| --- | --- | --- | --- |
| Spring Boot | `3.5.14` | `3.5.16` | 16: `spring-boot`, `spring-boot-actuator`, `spring-boot-actuator-autoconfigure`, `spring-boot-autoconfigure`, 10 `spring-boot-starter*` artifacts, `spring-boot-test`, and `spring-boot-test-autoconfigure` |
| Spring Framework | `6.2.18` | `6.2.19` | 9: `spring-aop`, `spring-beans`, `spring-context`, `spring-core`, `spring-expression`, `spring-jcl`, `spring-test`, `spring-web`, and `spring-webmvc` |
| Spring Security | `6.5.10` | `6.5.11` | 8: `spring-security-config`, `spring-security-core`, `spring-security-crypto`, `spring-security-oauth2-core`, `spring-security-oauth2-jose`, `spring-security-oauth2-resource-server`, `spring-security-test`, and `spring-security-web` |
| Micrometer | `1.15.11` | `1.15.12` | 6: `micrometer-commons`, `micrometer-core`, `micrometer-jakarta9`, `micrometer-observation`, `micrometer-registry-otlp`, and `micrometer-registry-prometheus` |
| Logback | `1.5.32` | `1.5.34` | 2: `logback-classic` and `logback-core` |
| SLF4J | `2.0.17` | `2.0.18` | 2: `jul-to-slf4j` and `slf4j-api` |
| Reactor | `3.7.18` | `3.7.19` | 1: `reactor-core` |
| Jakarta XML Bind | `4.0.4` | `4.0.5` | 1: `jakarta.xml.bind-api` |

All 45 version changes are members of those Boot-managed patch families; separately pinned Jackson, Netty, Tomcat,
AWS SDK, JavaFX, Log4j, JSON, Gson, Caffeine, Maven plugin, workflow, and container declarations remain unchanged.
This inventory establishes the effective local graph for this prerequisite; it does not claim future vulnerability
absence or broaden the repository's production-readiness evidence.

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
- `spring-boot-maven-plugin` `${spring-boot.version}`, currently `3.5.16`, configured with Spring Boot main class `com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication`, `build-info`, and `repackage` executions.

This plugin posture supports tests, coverage, executable JAR packaging, and build metadata. It does not add CI/Maven wiring in this slot, does not publish artifacts, does not create releases, does not create container images, and does not deploy anything.

## Relationship To CI, CodeQL, And Dependency Review

This audit complements:

- the slot 4 [CI workflow audit](../../agent/EVIDENCE_AUDIT_CI_WORKFLOW_AUDIT.md);
- the slot 5 [CodeQL and Dependency Review audit](../../agent/EVIDENCE_AUDIT_CODEQL_DEPENDENCY_REVIEW_AUDIT.md);
- the slot 3 [repository evidence map](../../agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md).

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
