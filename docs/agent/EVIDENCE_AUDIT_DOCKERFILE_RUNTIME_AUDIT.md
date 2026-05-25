# Evidence Audit Dockerfile Runtime Audit

This note is slot 7 of the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation/test-only. It audits the current [`Dockerfile`](../../Dockerfile) runtime packaging posture without changing Dockerfile contents, Maven configuration, CI workflow behavior, Compose behavior, scripts, runtime resources, endpoints, app behavior, runner services, automation, secrets, external targets, or production behavior.

## Audit Timestamp

- Audit timestamp: 2026-05-25T02:44-07:00.
- Audited repository: `RicheyWorks/LoadBalancerPro`.
- Audited base branch: `main`.
- Slot 7 branch: `codex/evidence-audit-dockerfile-runtime`.
- Starting main HEAD: `06d800c478b308ef836b0ab01d8b641d8b1a35f0`.
- Audited Dockerfile source: [`Dockerfile`](../../Dockerfile).
- Prior slot fact: slot 6 PR #321 merged as `06d800c478b308ef836b0ab01d8b641d8b1a35f0` and post-merge main CI/CodeQL were green before slot 7 started.

## Purpose

This audit gives reviewers a source-readable summary of the container build and runtime posture currently declared by `Dockerfile`. It is not a Dockerfile behavior change, not a container publishing lane, not a container signing lane, not a release approval, not a registry posture claim, not production hardening, and not production certification.

## Digest-Pinned Base Images

The Dockerfile currently uses digest-pinned base images:

- builder image: `maven:3-eclipse-temurin-26@sha256:1fc9415e0626a5893bbc352149d25a413e334a7ac5cd514bd99a2828fb082071`;
- runtime image: `eclipse-temurin:17-jre-jammy@sha256:642d45bf22d3cb9face159181732ed9fa70873b2681e50445eff7d4785c176bb`.

The digest pins make the reviewed source more stable, but they do not prove future image freshness, future vulnerability absence, registry availability, provenance, signing, SBOM completeness, or production readiness.

## Build Stage Posture

The build stage currently:

- names the stage `build`;
- uses a Maven builder image;
- sets `WORKDIR /workspace`;
- copies `pom.xml`;
- runs `mvn -q -DskipTests dependency:go-offline`;
- copies `src`;
- runs `mvn -q -DskipTests package spring-boot:repackage`;
- selects a packaged `target/LoadBalancerPro-*.jar` while excluding source, javadoc, and tests jars;
- copies the selected JAR to `/workspace/app.jar`.

This build stage packages the application inside the container build. It does not publish the image, sign the image, push to a registry, create a GitHub Release, mutate CI wiring, or change Maven behavior in this slot.

## Runtime Stage Posture

The runtime stage currently:

- uses Java 17 JRE Jammy as the runtime image;
- sets `WORKDIR /app`;
- installs `curl` for the container healthcheck;
- creates a system group and system user named `loadbalancer`;
- copies `/workspace/app.jar` from the build stage as `app.jar` with `loadbalancer:loadbalancer` ownership;
- sets `SPRING_PROFILES_ACTIVE=prod`;
- switches to `USER loadbalancer:loadbalancer`;
- declares `EXPOSE 8080`;
- declares a healthcheck that calls `http://127.0.0.1:8080/api/health`;
- uses `ENTRYPOINT ["java", "-jar", "/app/app.jar"]`;
- uses `CMD ["--server.address=0.0.0.0"]`.

The non-root runtime user and loopback healthcheck are useful runtime posture signals. They do not prove production deployment safety, production network policy, orchestrator readiness, runtime enforcement, tenant isolation, or production monitoring.

## Builder JDK And Runtime JDK Parity Question

The Dockerfile currently uses an Eclipse Temurin 26 Maven builder image and an Eclipse Temurin 17 runtime image. The Maven project declares Java 17 compilation, and CI uses Java 17 for Maven verification. Reviewers should keep this as an explicit builder JDK versus runtime JDK parity question for future Dockerfile changes. This audit records the current posture only; it does not change the builder image, runtime image, Maven release target, CI Java version, or local-lab Compose runtime.

## Relationship To CI

The CI workflow builds the Docker image, runs a Docker runtime smoke check against `127.0.0.1`, captures container dry-run evidence under workflow artifacts, and runs Trivy against the local CI image. Those checks are useful current-head CI evidence, but they do not publish or sign the image and do not create a production deployment claim.

## Reviewer Questions

- Did the PR preserve `Dockerfile` exactly?
- Did the PR avoid Dockerfile edits?
- Are both base images still digest-pinned?
- Is the builder stage still Maven-based and named `build`?
- Does the runtime stage still use Java 17 JRE Jammy?
- Does the runtime image still create and use the non-root `loadbalancer` user?
- Is `SPRING_PROFILES_ACTIVE=prod` still the Dockerfile default?
- Is `EXPOSE 8080` still declarative only?
- Does the healthcheck still use loopback `127.0.0.1` and `/api/health`?
- Does the PR avoid image publishing, image signing, registry login, release creation, Dockerfile behavior changes, Compose changes, CI/Maven wiring changes, scripts, runtime resources, endpoints, app behavior, secrets, external targets, runner services, and automation?
- Does the PR avoid claiming production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, registry publication, container signing, or broader automation?

## Remaining Limits

Reviewers should keep these limits attached to this Dockerfile runtime audit:

- It is a static source audit of `Dockerfile`, not a runtime hardening implementation.
- It does not update base image digests.
- It does not prove future image freshness.
- It does not prove all vulnerabilities are absent.
- It does not prove registry publication or image signing.
- It does not prove container provenance.
- It does not prove Kubernetes, cloud, tenant, or production deployment readiness.
- It does not prove production monitoring, alerting, incident response, or runtime enforcement.
- It does not replace human review of future Dockerfile, CI, Compose, registry, or deployment changes.

## Not-Proven Boundaries

This Dockerfile runtime audit does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, registry publication, container signing, production telemetry, production monitoring, release approval, vulnerability remediation completeness, incident response readiness, remediation SLA compliance, or broader automation.
