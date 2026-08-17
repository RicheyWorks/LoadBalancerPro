# Production Load Balancer Build-Out

This is the current engineering path from the packaged single-node reverse proxy to a reviewed deployment that can
receive forecast traffic. The older [`BUILD_PLAN_DEPLOYABLE.md`](BUILD_PLAN_DEPLOYABLE.md) records how the proxy was
built. This document starts from the executable system that exists now and defines the remaining capacity, staging,
topology, and rollout work.

The build-out is for the `/proxy/**` data plane implemented in
[`api/proxy`](../src/main/java/com/richmond423/loadbalancerpro/api/proxy). Calculation APIs, browser demos, Lab Tools,
replay, and synthetic allocation scenarios are not load-path evidence.

## Existing Foundation

The repository already has these executable production surfaces:

- a production-only Spring Boot artifact with the proxy runtime and routing strategies;
- streamed request and response forwarding, bounded request size, timeouts, retry budgets, concurrency limits,
  shedding, slow start, health hysteresis, cooldown, drain, and graceful shutdown;
- live per-upstream in-flight, error-rate, EWMA, and exact rolling p50/p95/p99 signals used by adaptive routing;
- TLS termination, verified backend TLS and optional mTLS bundles, API-key protection, and protected Actuator metrics;
- a hardened production Compose definition and a static Kubernetes manifest sketch;
- production artifact, Compose, graceful-shutdown, benchmark-smoke, SBOM, CodeQL, and image-scan CI gates; and
- a one-hour loopback soak covering steady traffic, spikes, slow upstreams, upstream loss, reload, drain, in-flight
  quiescence, p99 budgets, and heap-floor growth.

These establish a tested single-process data plane. They do not establish the capacity of deployment hardware,
public-ingress safety, multi-instance consistency, a production SLO, or behavior against real upstream services.

## Reproduce The Baseline

Run from a clean checkout before changing deployment topology or traffic policy:

```bash
mvn -q test
mvn -q -DskipTests package spring-boot:repackage
bash scripts/smoke/proxy-prod-compose-smoke.sh
bash scripts/bench/proxy-benchmark-soak.sh --mode smoke
LBP_BENCH_SOAK_SECONDS=3600 bash scripts/bench/proxy-benchmark-soak.sh --mode soak
```

The smoke and soak targets are deliberately loopback-only. Keep TLS verification and authentication enabled. Do not
add a generic remote URL to these scripts; staging traffic requires the separate reviewed boundary below.

## Required Workload Contract

Capacity work starts only after the expected load is written down. Record these inputs outside source control if they
contain private topology or customer information:

| Input | Required detail |
| --- | --- |
| Request rate | normal, peak, burst rate, burst duration, and expected growth |
| Concurrency | client connections, HTTP/2 streams, WebSockets, keep-alive behavior, and connection churn |
| Route mix | percentage by route, method, retry eligibility, affinity, and routing strategy |
| Payload | request and response p50/p95/p99 sizes, streaming responses, SSE, and upload behavior |
| Upstreams | count, latency distribution, connection limits, error modes, health endpoint cost, and TLS/mTLS |
| Objectives | success ratio and latency objectives measured at the client and at each upstream |
| Failure model | slow target, refusal, timeout, partial outage, reload, drain, certificate rotation, and restart |
| Topology | instance size, replica count, zones, ingress layer, NAT/egress limits, and observability backend |

Do not substitute the repository's fixture rates or p99 budgets for missing workload inputs.

## Engineering Sequence

### 1. Establish The Capacity Envelope

The dedicated loopback runner is
[`proxy-capacity-staircase.sh`](../scripts/bench/proxy-capacity-staircase.sh). It requires the executable reviewed
workload profile represented by
[`capacity-profile.example.json`](../scripts/bench/capacity-profile.example.json) and remains separate from the
regression soak. Run it to:

- test a reviewed rate ladder with warm-up, steady measurement, and cooldown at every step;
- run each step at least three times from a fresh proxy process;
- capture achieved throughput, client p50/p95/p99, errors and status classes, upstream distribution, retries, sheds,
  in-flight counts, heap, CPU, process/container memory, GC pauses, open connections, and thread counts;
- include equal, slow, failing, draining, and recovering upstream cases;
- identify the first saturation step instead of hiding overload with retries; and
- retain raw machine-readable results beneath ignored `target/` output.

Acceptance is a reproducible saturation knee and a recommended operating envelope with explicit headroom. Stop below
the first step where non-injected failures appear, latency exceeds the workload objective, in-flight work does not
quiesce, memory grows outside budget, or the proxy reaches a configured safety limit.

The passing envelope must cover the larger of the reviewed burst rate or the forecast peak after declared growth and
reserved headroom.

Implementing the runner does not close this gate. Closure requires a reviewed forecast profile and a passing run on
deployment-equivalent resources for the exact release artifact.

### 2. Prove A Reviewed Staging Boundary

The staging runner is [`proxy-staging-qualification.sh`](../scripts/bench/proxy-staging-qualification.sh), backed by
[`validate-staging-target.py`](../scripts/bench/validate-staging-target.py) and the reviewed-input template
[`staging-profile.example.json`](../scripts/bench/staging-profile.example.json). Validation is traffic-free. Run mode
fails closed until the target allow-list, exact artifact, credentials, billable impact, cleanup authority, CA, and
hash-pinned action adapters are reviewed. Every resolved address must remain inside the declared RFC1918/ULA ranges;
production-looking targets and embedded secrets are rejected.

Use the forecast route/payload mix against staging upstreams. Compare client latency with upstream latency to measure
proxy overhead, verify health-check cost, exercise certificate rotation, and repeat slow, failure, reload, drain, and
restart cases. Store secrets in the deployment secret system, not command lines, evidence files, or the repository.

The runner implementation and its CI boundary tests do not close this gate. Closure requires its passing result from
the reviewed non-production environment for the exact release artifact.

### 3. Choose And Prove The Runtime Topology

Runtime stats, health state, cooldowns, retry budgets, recent decisions, and configuration generations are
process-local. Choose one of these deliberately:

- **Single active instance:** place it behind a reviewed ingress or failover mechanism and prove replacement time,
  connection draining, and recovery from total process loss.
- **Multiple active instances:** prove consistent route/upstream configuration, bounded generation skew, readiness
  during rollout, aggregate upstream connection limits, per-instance observability, and any required affinity behavior.

A static Kubernetes manifest is not multi-instance proof. If active-active consistency needs a shared control plane,
that is a separate implementation and failure-containment project.

The executable local active-active proof is
[`proxy-active-active-topology.sh`](../scripts/bench/proxy-active-active-topology.sh) with
[`docker-compose.active-active.yml`](../deploy/topology/docker-compose.active-active.yml). CI starts two actual proxy
processes behind a bounded TLS/authenticated ingress fixture and gates distribution, configuration convergence,
rollout, rollback, aggregate upstream limits, per-instance metrics, replica loss, and recovery under load. This closes
the local mechanics proof only. A reviewed deployment ingress and multi-zone proof remain required when the selected
deployment topology uses them.

### 4. Stage The Rollout And Rollback

The active-active runner proves rolling configuration change and reversal; it does not claim immutable-image rollout.
The next deployment slice is to apply the same availability, generation-skew, metric, and recovery assertions while
replacing proxy replicas by exact image digest behind the reviewed staging ingress.

Use an immutable image digest and begin with a small, explicitly approved traffic slice. During every step, compare
client success/latency, upstream health, proxy p95/p99, in-flight work, retries, sheds, cooldown trips, CPU, memory, GC,
and connection counts with the prior step.

Abort the step when an agreed objective is exceeded, a metric disappears, configuration diverges, an upstream is
overloaded, or rollback cannot be completed inside its window. Rollback removes new traffic, drains accepted work,
restores the prior immutable image/configuration, and verifies health before another attempt.

## Ready-For-Forecast-Load Gate

Traffic promotion requires all of the following on the exact release artifact:

- required CI, dependency review, CodeQL, SBOM, and application/fixture image scans are green;
- the production artifact starts with TLS verification, authentication, bounded concurrency, and the reviewed
  upstream-address policy intact;
- regression smoke and one-hour soak pass without non-injected reload/drain failures or heap-growth violations;
- the capacity staircase passes at forecast peak plus agreed headroom on deployment-equivalent resources;
- the reviewed staging mix passes steady, burst, slow, failure, reload, drain, restart, and certificate-rotation cases;
- dashboards and alerts cover traffic, p95/p99, failures, retries, sheds, in-flight work, health, cooldown, resources,
  and scrape failure without unbounded labels;
- an operator has executed the drain and rollback procedure within the agreed recovery window; and
- the remaining single-instance or multi-instance limitations are explicitly accepted by the deployment owner.

Evidence is valid only for the tested artifact, configuration, topology, workload, upstream behavior, and time window.
Any material change to those inputs requires proportionate re-verification.
