# v1.6.1 Release Metadata Alignment Plan

## A. Current Release State

v1.6.0 has shipped and was pushed to the release branch. That release added the separate CodeQL SAST workflow and related evidence updates.

v1.5.0 added CI-generated CycloneDX SBOM artifacts, and v1.4.0 added repository governance basics including LICENSE, SECURITY.md, CONTRIBUTING.md, CODEOWNERS, and Dependabot configuration.

The v1.7.0 release artifact provenance planning pass found active release metadata drift: the latest shipped Git tag is v1.6.0, but active Maven, API, CLI, telemetry, README, and version-sensitive test references still report 1.3.1.

Existing tags must remain immutable. v1.6.0 must not be moved or replaced. The public main branch remains untouched.

## B. Audit Findings

The active release metadata still reports 1.3.1 in these files:

- `pom.xml`
  - Maven project version is still `1.3.1`.
- `src/main/resources/application.properties`
  - `management.opentelemetry.resource-attributes[service.version]` is still `1.3.1`.
  - `loadbalancerpro.app.version` is still `1.3.1`.
  - `info.app.version` is still `1.3.1`.
- `src/main/java/api/LoadBalancerApiApplication.java`
  - API fallback version constant is still `1.3.1`.
- `src/main/java/cli/LoadBalancerCLI.java`
  - CLI `VERSION` constant is still `1.3.1`.
- `README.md`
  - Active packaged-JAR examples still reference `LoadBalancerPro-1.3.1.jar`.
- `src/test/java/api/AllocatorControllerTest.java`
  - Health/version expectation still uses `1.3.1`.
- `src/test/java/api/OpenTelemetryMetricsConfigurationTest.java`
  - OpenTelemetry `service.version` expectation still uses `1.3.1`.
- `src/test/java/api/LoadBalancerApiApplicationTest.java`
  - API fallback version expectation still uses `1.3.1`.

Historical docs and evidence may continue to mention older release numbers when they describe earlier releases, audits, or planning decisions. Those should not be rewritten merely because they contain older versions.

The audit also found generic `VERSION` constants in non-release contexts, such as schema or model constants. Those are not active application release metadata and should not be changed as part of this patch.

## C. Why v1.6.1 Is Needed

v1.6.0 is already shipped and must not be moved. Replacing or retagging it would weaken the release history that the governance and supply-chain work is trying to establish.

The planned v1.7.0 release artifact workflow should be able to verify that the Git tag and Maven project version agree before publishing durable artifacts. Starting durable release artifact publishing while active metadata still says 1.3.1 would create confusing artifact names, API health output, CLI output, telemetry labels, and README instructions.

The correct fix is a narrow v1.6.1 patch that aligns active release metadata without changing runtime behavior, routing behavior, CI behavior, or supply-chain workflows.

## D. Exact Future Patch Scope

If the audit findings are confirmed during implementation, the v1.6.1 patch should only:

- Update Maven/project version to `1.6.1`.
- Update `/api/health` version properties to `1.6.1`.
- Update telemetry/app metadata fields to `1.6.1`:
  - `management.opentelemetry.resource-attributes[service.version]`
  - `info.app.version`
  - `loadbalancerpro.app.version`
- Update the API fallback version to `1.6.1`.
- Update CLI `--version` output metadata to `1.6.1`.
- Update README active packaged-JAR examples to `LoadBalancerPro-1.6.1.jar`.
- Update version-sensitive tests to expect `1.6.1`.
- Include `docs/V1_6_1_RELEASE_METADATA_PLAN.md`.
- Preserve historical docs and evidence that intentionally describe older releases.

## E. What Not To Change

- Do not move v1.6.0.
- Do not move or rewrite any existing tag.
- Do not change CodeQL workflow behavior.
- Do not change CI SBOM workflow behavior.
- Do not change release artifact workflow behavior because it does not exist yet.
- Do not change `WEIGHTED_LEAST_LOAD` behavior.
- Do not change optional routing-only weight behavior.
- Do not change `TAIL_LATENCY_POWER_OF_TWO` behavior.
- Do not change `RoutingComparisonService` behavior.
- Do not change `RoutingController` behavior.
- Do not change CloudManager/AWS behavior.
- Do not change allocation endpoints.
- Do not change CLI workflows beyond version metadata if needed.
- Do not touch public main.
- Do not broaden into artifact publishing, attestations, signing, deployment, operations, or additional supply-chain feature work.

## F. Tests To Update If Patch Is Needed

- `src/test/java/api/AllocatorControllerTest.java`
  - Update the API health/version expectation to `1.6.1`.
- `src/test/java/api/OpenTelemetryMetricsConfigurationTest.java`
  - Update the OpenTelemetry `service.version` expectation to `1.6.1`.
- `src/test/java/api/LoadBalancerApiApplicationTest.java`
  - Update the fallback version expectation to `1.6.1`.
- Any CLI version test, if one is present during implementation.

## G. Verification Plan

Before implementation, run:

```powershell
rg -n "1\.3\.1|LoadBalancerPro-1\.3\.1|service.version|info.app.version|loadbalancerpro.app.version|FALLBACK_VERSION|VERSION" pom.xml src/main/resources src/main/java README.md src/test/java
```

After implementation, run:

```powershell
mvn -q test
mvn -q -DskipTests package
java -jar target\LoadBalancerPro-1.6.1.jar --version
java -jar target\LoadBalancerPro-1.6.1.jar --lase-demo=healthy
java -jar target\LoadBalancerPro-1.6.1.jar --lase-demo=overloaded
java -jar target\LoadBalancerPro-1.6.1.jar --lase-demo=invalid-name
git diff --check
```

The invalid-name LASE smoke check should fail safely with valid-scenario guidance and no raw stack trace.

Run this protected-area check:

```powershell
git diff -- src/main/java/cloud src/main/java/core/CloudManager.java src/main/java/core/TailLatencyPowerOfTwoStrategy.java src/main/java/api/AllocatorController.java src/main/java/api/RoutingComparisonService.java src/main/java/api/RoutingController.java src/main/java/core/WeightedLeastLoadStrategy.java .github/workflows/ci.yml .github/workflows/codeql.yml
```

Expected active metadata search after implementation:

- No active `1.3.1` matches remain in `pom.xml`, `src/main/resources`, `src/main/java`, `README.md`, or `src/test/java`.
- No active `LoadBalancerPro-1.3.1.jar` matches remain in README packaged-JAR examples.
- Any remaining `1.3.1` references should be historical docs or evidence only.

## H. Recommendation

v1.6.1 is needed. Implement the v1.6.1 metadata alignment patch before starting v1.7.0 release artifact publishing.

Keep the patch narrow: version metadata, README packaged-JAR examples, version-sensitive tests, and this planning doc only. Do not move v1.6.0 and do not broaden the work into artifact publishing or signing.
