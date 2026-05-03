# LoadBalancerPro v1.2.1 Version And Routing Docs Plan

Date: 2026-05-03

## Current v1.2.0 Release State

- Current planning branch: `planning/v1.2.1-version-and-routing-docs`
- Branch before planning work: `loadbalancerpro-clean`
- Working tree before branch creation: clean
- `loadbalancerpro-clean` was ahead of `origin/loadbalancerpro-clean` by one local audit commit.
- Current HEAD at planning start: `6aad68b Add enterprise readiness audit`
- Published `v1.2.0` tag: `4eab1f9 Merge v1.2.0 routing comparison API`
- Public `main` is not part of the release branch flow and must remain untouched.
- Existing tags present at planning start: `v1.0-rc1` through `v1.0-rc9`, `v1.0.0`, `v1.0.1`, `v1.1.0`, `v1.1.1`, and `v1.2.0`.

v1.2.0 added the routing comparison engine and exposed `POST /api/routing/compare` as a read-only, recommendation-only API. The enterprise audit found the repository credible for enterprise demos, but not enterprise-production ready. The highest-priority blocker is release metadata mismatch. The second priority is missing README documentation for the new routing comparison API.

## Version Mismatch Issue

The Git release is `v1.2.0`, but runtime, artifact, CLI, telemetry, and active README examples still identify the project as `1.1.1`. This creates a provenance and support problem: a user running the v1.2.0 release can see `1.1.1` in the generated JAR name, API health response, CLI version output, telemetry resource attributes, and README commands.

Because `v1.2.0` is already published, do not move or replace that tag. Fix the mismatch with a new narrow patch release: `v1.2.1`.

## Files That Still Report 1.1.1

- `pom.xml`
  - Project version is `<version>1.1.1</version>`.
  - This drives generated artifacts such as `target/LoadBalancerPro-1.1.1.jar`.

- `src/main/resources/application.properties`
  - `management.opentelemetry.resource-attributes[service.version]=1.1.1`
  - `loadbalancerpro.app.version=1.1.1`
  - `info.app.version=1.1.1`

- `src/main/java/api/AllocatorController.java`
  - `/api/health` returns the injected `loadbalancerpro.app.version` or `info.app.version`.
  - The controller behavior can stay unchanged, but the backing properties currently resolve to `1.1.1`.

- `src/main/java/api/LoadBalancerApiApplication.java`
  - `FALLBACK_VERSION` is `1.1.1`.
  - Packaged execution uses Maven implementation metadata when available, but dev/test fallback still reports `1.1.1`.

- `src/main/java/cli/LoadBalancerCLI.java`
  - `VERSION` is hard-coded as `1.1.1`.
  - `--version` prints `LoadBalancerCLI version 1.1.1`.

- `README.md`
  - Active examples reference `target/LoadBalancerPro-1.1.1.jar` across local run, quick demo, load-test, deployment profile, LASE demo, replay, and production-like examples.

- Version-sensitive tests
  - `src/test/java/api/AllocatorControllerTest.java` expects `/api/health` version `1.1.1`.
  - `src/test/java/api/OpenTelemetryMetricsConfigurationTest.java` expects telemetry `service.version` `1.1.1`.
  - `src/test/java/api/LoadBalancerApiApplicationTest.java` expects the application fallback version `1.1.1`.

Historical docs that describe older releases may intentionally keep older version values. The v1.2.1 implementation should update active user-facing README examples and any new v1.2.1 release note, not rewrite unrelated historical planning/review records.

## README Routing API Documentation Gap

The README REST API section currently lists:

```text
GET  /api/health
GET  /api/lase/shadow
POST /api/allocate/capacity-aware
POST /api/allocate/predictive
```

It does not document:

```text
POST /api/routing/compare
```

That is the main v1.2.0 product capability, so it should be visible in the primary README with request and response examples, validation behavior, safety boundaries, and auth behavior.

Recommended README request example:

```bash
curl -X POST http://localhost:8080/api/routing/compare \
  -H "Content-Type: application/json" \
  -d '{
    "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
    "servers": [
      {
        "serverId": "green",
        "healthy": true,
        "inFlightRequestCount": 5,
        "configuredCapacity": 100.0,
        "estimatedConcurrencyLimit": 100.0,
        "averageLatencyMillis": 20.0,
        "p95LatencyMillis": 40.0,
        "p99LatencyMillis": 80.0,
        "recentErrorRate": 0.01,
        "queueDepth": 1,
        "networkAwareness": {
          "timeoutRate": 0.0,
          "retryRate": 0.0,
          "connectionFailureRate": 0.0,
          "latencyJitterMillis": 4.0,
          "recentErrorBurst": false,
          "requestTimeoutCount": 0,
          "sampleSize": 120
        }
      },
      {
        "serverId": "blue",
        "healthy": true,
        "inFlightRequestCount": 75,
        "configuredCapacity": 100.0,
        "estimatedConcurrencyLimit": 100.0,
        "averageLatencyMillis": 35.0,
        "p95LatencyMillis": 120.0,
        "p99LatencyMillis": 220.0,
        "recentErrorRate": 0.15,
        "queueDepth": 10
      }
    ]
  }'
```

Recommended README response example:

```json
{
  "requestedStrategies": ["TAIL_LATENCY_POWER_OF_TWO"],
  "candidateCount": 2,
  "timestamp": "2026-05-03T00:00:00Z",
  "results": [
    {
      "strategyId": "TAIL_LATENCY_POWER_OF_TWO",
      "status": "SUCCESS",
      "chosenServerId": "green",
      "reason": "Chose green based on lower tail-latency and pressure signals.",
      "candidateServersConsidered": ["green", "blue"],
      "scores": {
        "green": 15.23,
        "blue": 64.91
      }
    }
  ]
}
```

The README should also state that omitting `strategies` defaults to the registered routing strategy set, currently `TAIL_LATENCY_POWER_OF_TWO`, and that invalid payloads return structured JSON errors.

## Exact Patch Scope

Implement only the following in the future v1.2.1 patch:

- Update Maven/project version to `1.2.1`.
- Update `/api/health` version to `1.2.1` through the existing application metadata properties.
- Update packaged app `--version` fallback to `1.2.1`.
- Update CLI `--version` to `1.2.1`.
- Update telemetry `management.opentelemetry.resource-attributes[service.version]` to `1.2.1`.
- Update `loadbalancerpro.app.version` and `info.app.version` to `1.2.1`.
- Update active README JAR examples to `LoadBalancerPro-1.2.1.jar`.
- Document `POST /api/routing/compare` in README with request and response examples.
- Document routing comparison validation and structured error behavior at a high level.
- Describe read-only/recommendation-only safety boundaries.
- Describe auth behavior for local, prod API-key, cloud-sandbox API-key, and OAuth2 operator role.

No production behavior change is needed for `AllocatorController`: it already sources `/api/health` from version properties. No routing behavior change is needed for `RoutingController` or `RoutingComparisonService`.

## Routing Safety Boundaries To Document

`POST /api/routing/compare` is a comparison and recommendation endpoint only. It should be documented as follows:

- It evaluates caller-provided candidate telemetry.
- It returns strategy comparison results and explanations.
- It does not call `CloudManager`.
- It does not call AWS.
- It does not mutate cloud resources.
- It does not mutate existing `LoadBalancer` allocation state.
- It does not alter `POST /api/allocate/capacity-aware`.
- It does not alter `POST /api/allocate/predictive`.
- All-unhealthy candidates can return a safe no-decision result rather than causing a routing mutation.

## Auth Behavior To Document

- Local/default profile:
  - `POST /api/routing/compare` works without an API key for local demos.
  - `/api/health` remains public.

- Prod profile with API-key mode:
  - `ProdApiKeyFilter` protects `POST`, `PUT`, and `PATCH` requests under `/api/**`.
  - `POST /api/routing/compare` requires `X-API-Key` with the configured `LOADBALANCERPRO_API_KEY`.
  - Missing or blank configured API key fails closed with HTTP 401 for protected API requests.
  - `/api/health` remains public.

- Cloud-sandbox profile with API-key mode:
  - Same `X-API-Key` behavior as prod for `POST /api/routing/compare`.
  - The sandbox profile remains dry-run by default and does not require AWS credentials just to start.
  - Routing comparison itself is read-only and does not touch AWS.

- OAuth2 mode:
  - `POST /api/routing/**` requires the configured allocation role, which defaults to `operator`.
  - Missing or invalid bearer tokens return HTTP 401.
  - Authenticated users without the operator role, including viewer or observer-only tokens, return HTTP 403.
  - `GET /api/lase/shadow` remains available to observer or operator roles.
  - `/api/health` remains public.

## Tests That Need Updating

Update version-sensitive tests only:

- `src/test/java/api/AllocatorControllerTest.java`
  - Change `/api/health` version expectation from `1.1.1` to `1.2.1`.

- `src/test/java/api/OpenTelemetryMetricsConfigurationTest.java`
  - Change expected OpenTelemetry `service.version` from `1.1.1` to `1.2.1`.

- `src/test/java/api/LoadBalancerApiApplicationTest.java`
  - Change fallback version expectation from `1.1.1` to `1.2.1`.

Keep existing routing/auth tests in place:

- `src/test/java/api/RoutingControllerTest.java`
- `src/test/java/api/ProdApiKeyProtectionTest.java`
- `src/test/java/api/CloudSandboxProfileConfigurationTest.java`
- `src/test/java/api/OAuth2AuthorizationTest.java`

Those tests already cover the endpoint's read-only behavior, validation/error behavior, prod API-key behavior, cloud-sandbox API-key behavior, and OAuth2 operator-role behavior. They should not need behavior changes for a docs and metadata patch.

Optional low-risk test improvement:

- In `AllocatorControllerTest`, extend the OpenAPI docs assertion to include `/api/routing/compare` if the team wants README/API-doc discoverability guarded by test coverage.

## Verification Plan

Before implementation:

```powershell
git status
git branch --show-current
rg -n "1\.1\.1|LoadBalancerPro-1\.1\.1|service.version|info.app.version|loadbalancerpro.app.version|FALLBACK_VERSION|VERSION" pom.xml src/main/resources src/main/java README.md src/test/java
```

After implementation:

```powershell
mvn -q test
mvn -q -DskipTests package
java -jar target\LoadBalancerPro-1.2.1.jar --version
java -jar target\LoadBalancerPro-1.2.1.jar --lase-demo=healthy
java -jar target\LoadBalancerPro-1.2.1.jar --lase-demo=overloaded
java -jar target\LoadBalancerPro-1.2.1.jar --lase-demo=invalid-name
```

API smoke verification:

```powershell
Start-Process -FilePath java -ArgumentList '-jar','target\LoadBalancerPro-1.2.1.jar','--server.address=127.0.0.1','--server.port=18080','--spring.profiles.active=local' -WindowStyle Hidden
curl.exe -fsS http://127.0.0.1:18080/api/health
curl.exe -fsS -X POST http://127.0.0.1:18080/api/routing/compare -H "Content-Type: application/json" --data-binary "@examples/routing-compare-request.json"
```

If no committed `examples/routing-compare-request.json` exists, use the README request body manually for the smoke check rather than adding a new example file in the v1.2.1 patch unless that file is explicitly chosen as part of the README docs work.

Security/profile verification:

- Run existing prod API-key tests.
- Run existing cloud-sandbox API-key tests.
- Run existing OAuth2 authorization tests.
- Confirm local `POST /api/routing/compare` works without `X-API-Key`.
- Confirm prod/cloud-sandbox protected requests require `X-API-Key`.
- Confirm OAuth2 observer/viewer cannot call routing compare and operator can.

Release verification:

- Docker build and health smoke.
- Docker healthcheck smoke.
- GitHub Actions test/package/Docker/Trivy run before any release tag.
- Final `rg` pass to ensure active release metadata no longer reports `1.1.1`.

## What Not To Change

- Do not move `v1.2.0`.
- Do not move or rewrite any existing tag.
- Do not push during planning.
- Do not tag during planning.
- Do not change remotes.
- Do not touch public `main`.
- Do not alter routing behavior.
- Do not change `RoutingComparisonService` behavior.
- Do not change `RoutingController` behavior unless a documentation-only annotation is explicitly chosen later.
- Do not change existing allocation endpoint behavior.
- Do not change `CloudManager`.
- Do not change AWS mutation logic or cloud guardrails.
- Do not broaden this patch into governance, deployment, SBOM, signing, production-readiness, or new-routing-algorithm work.
- Do not rewrite historical docs solely to update old version references.

## Recommendation

Proceed with a narrow v1.2.1 release metadata and README routing API documentation patch.

Use `v1.2.1` because `v1.2.0` is already published and must remain immutable. Keep the implementation branch focused on version constants/properties/tests plus README documentation. After local verification and CI/Trivy pass, tag a new `v1.2.1` release instead of changing any existing tag.

This gives the published routing comparison capability a truthful runtime identity and discoverable API documentation without changing routing, allocation, CloudManager, AWS behavior, public `main`, remotes, or existing tags.
