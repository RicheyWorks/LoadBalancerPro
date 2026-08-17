# Engineering Evidence Index

This filename is retained for existing links. It is a secondary evidence index, not a work queue, phase gate, or
reason to create documentation-only tasks. The active goal and ordered engineering work are in
[`LOAD_BALANCER_BUILD_OUT.md`](LOAD_BALANCER_BUILD_OUT.md).

New evidence comes from runtime behavior, secure configuration, tests, deployment exercises, benchmarks, and exact
artifact checks. Update this index only when one of those executable surfaces materially changes.

## Load-Balancer Evidence

| Engineering question | Runtime or configuration source | Executable proof | Next qualification gate |
| --- | --- | --- | --- |
| Does the live data plane forward with the selected strategy? | `ReverseProxyService`, `ReverseProxyRoutePlanner`, core routing strategies | proxy integration tests, production Compose smoke, benchmark smoke | Capacity staircase with the forecast route and payload mix |
| Are authentication and TLS enforced? | `ApiSecurityConfiguration`, SSL bundles, `application-proxy-prod.properties`, production Compose | security tests, packaged runtime smoke, TLS-enabled Compose smoke | Staging identity, trust, and certificate-rotation exercise |
| Are upstream targets constrained by the reviewed network policy? | `ProxyBackendUrlClassifier`, `ReverseProxyRoutePlanner`, `ProxyDnsDiscovery` | classifier, routing, reload, DNS discovery, and private-network validation tests | Staging allow-list and resolver-policy proof |
| Are requests, retries, and concurrency bounded? | proxy request limits, admission controls, retry budgets, shedding, cooldown | proxy unit/integration tests, benchmark smoke, one-hour soak | Saturation knee and operating envelope with explicit headroom |
| Do health, failure, reload, drain, and recovery work under traffic? | health runtime, cooldown, slow start, reload generations, graceful shutdown | integration tests, production Compose smoke, benchmark scenarios, one-hour soak | Deployment-equivalent failure and replacement exercises |
| Is the release artifact isolated and runnable? | `pom.xml`, `Dockerfile`, production profile, resolver and artifact scripts | artifact-content tests, packaged-JAR smoke, Docker/Compose smoke | Immutable digest deployed in the selected topology |
| Are code, dependency, and image risks gated? | pinned workflows, dependency configuration, Docker images | CI, dependency review, CodeQL, SBOM generation, application and fixture image scans | All gates green on the exact release artifact |
| Is performance evidence reproducible? | `scripts/bench` and ignored machine-readable output under `target/` | benchmark smoke and scheduled one-hour soak | Repeated capacity staircase and reviewed staging results |

## Engineering Entry Points

- Current phase and promotion gate: [`LOAD_BALANCER_BUILD_OUT.md`](LOAD_BALANCER_BUILD_OUT.md).
- Runtime behavior and configuration: [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md) and
  [`API_SECURITY.md`](API_SECURITY.md).
- Deployment and rollback controls: [`DEPLOYMENT.md`](DEPLOYMENT.md),
  [`DEPLOYMENT_HARDENING_GUIDE.md`](DEPLOYMENT_HARDENING_GUIDE.md), and
  [`OPERATIONS_RUNBOOK.md`](OPERATIONS_RUNBOOK.md).
- Load execution and evidence boundary: [`../scripts/bench/README.md`](../scripts/bench/README.md).
- Exact artifact evidence: required CI, dependency review, CodeQL, generated test reports, SBOMs, image scans, and
  ignored benchmark output for the commit being qualified.

Lab, demo, cloud-management, architecture-history, and campaign-board material is outside the current load
qualification path. Consult it only when a scoped engineering change touches those surfaces.

Editing this index does not close an engineering gate. A gate closes only when its executable proof passes for the
artifact, configuration, workload, and topology being promoted.
