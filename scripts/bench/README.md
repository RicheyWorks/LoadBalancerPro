# Proxy Benchmark And Soak Harness

## Reviewed staging qualification

`proxy-staging-qualification.sh` is the live non-production boundary. Structural validation performs no DNS lookup or
traffic and rejects public ranges, production-looking targets, embedded secrets, incomplete workload cases, and
unhashed action adapters:

```bash
bash scripts/bench/staging-validator-contract-test.sh
bash scripts/bench/proxy-staging-qualification.sh --mode validate --profile /path/to/staging-profile.json
```

Run mode additionally requires a reviewed profile tied to the clean checkout and exact image digest, private DNS
resolution inside its approved RFC1918/ULA ranges, billable/cleanup authority, a secret API-key file, a pinned CA, and
hash-pinned, self-contained `verify-artifact`, `slow`, `failure`, `reload`, `drain`, `restart`, `certificate-rotation`,
and `reset` adapters outside the repository. Supply those paths with `LBP_STAGING_API_KEY_FILE`, `LBP_STAGING_CA_FILE`, and
`LBP_STAGING_ACTION_DIR`, then run:

```bash
bash scripts/bench/proxy-staging-qualification.sh --mode run --profile /path/to/staging-profile.json
```

The runner pins resolved private addresses, keeps authentication out of process arguments and evidence, verifies the
deployed artifact before load, and records client/upstream p99, derived proxy overhead, health-check cost, recovery,
and certificate change. Copy `staging-profile.example.json` outside the repository and replace its placeholders. A
passing run is staging evidence for its exact target and artifact; it is not production authorization.

## Active-active topology proof

`proxy-active-active-topology.sh` starts two real proxy processes behind a bounded TLS/authenticated round-robin
ingress fixture. It proves both replicas receive traffic, configuration and generations converge, rollout and rollback
stay available under load, aggregate per-upstream limits remain inside the reviewed budget, per-instance metrics exist,
and traffic survives one replica stopping and recovering within the profile's bounded ingress failure-detection time:

```bash
bash scripts/bench/proxy-active-active-topology.sh --mode validate
bash scripts/bench/proxy-active-active-topology.sh --mode smoke
```

`--mode run --profile /path/to/topology-profile.json` requires a reviewed profile, a clean checkout, and at least
30 seconds per loaded case. CI runs smoke mode and scans the ingress fixture image. This is executable local topology
evidence, not a substitute for the reviewed deployment ingress or a multi-zone run.

## Capacity staircase

`proxy-capacity-staircase.sh` is the separate capacity-qualification lane. It runs equal, slow, failing, draining, and
recovering fixture cases across an ascending rate ladder. Every rate gets warm-up, at least three fresh-proxy repeats,
steady measurements, and cooldown. Raw Vegeta results, Prometheus snapshots, proxy status, container metadata, and
resource samples stay under ignored `target/capacity/`; the final JSON identifies a reproducible saturation step and a
headroom-adjusted operating envelope. A pass must cover the larger of the declared burst rate or the forecast peak
after expected growth and reserved headroom; those inputs are reviewed claim inputs, not annotations.

Copy `capacity-profile.example.json` outside the repository when the workload contains private deployment details,
replace every example value with reviewed forecast inputs, and populate its approval fields. Validation is safe and
does not start Docker:

```bash
bash scripts/bench/proxy-capacity-staircase.sh --mode validate --profile /path/to/capacity-profile.json
```

Execution is loopback-only and refuses an unreviewed profile or dirty checkout:

```bash
bash scripts/bench/proxy-capacity-staircase.sh --mode run --profile /path/to/capacity-profile.json
```

A passing result applies only to the recorded artifact, profile, host, Compose limits, and fixture behavior. It is not
staging, public-ingress, multi-instance, or production-capacity evidence.

## Access-log overhead lane

Run `bash scripts/bench/access-log-overhead.sh` for five fresh-JVM enabled/disabled access-log comparisons. Raw JSON
samples and Maven logs are written under `target/access-log-benchmark/`. Set
`LBP_ACCESS_LOG_BENCHMARK_FORKS=3..20` to change the fork count.

This lane is diagnostic local or hosted-runner evidence. It does not enforce or prove the production `<5%` target,
production throughput or latency, SLOs, production readiness, or production certification. Deterministic access-log
correctness and architecture guards remain in the default Maven suite.

## Single-replica regression smoke and soak

This harness exercises the authenticated TLS `proxy-prod` Compose stack with deterministic local fixture backends. It
uses Vegeta for steady, spike, slow-backend, backend-kill, reload-under-load, and drain-under-load scenarios. It never
accepts a caller-supplied target; the proxy endpoint is fixed to `https://127.0.0.1:<port>`, while upstreams stay on the
private Compose network.

## Prerequisites

- Linux or a compatible Bash environment;
- Docker with Compose v2;
- `curl`, `jq`, and OpenSSL;
- Vegeta 12.13.0 or a separately reviewed compatible version.

Run the bounded smoke scenario set:

```bash
bash scripts/bench/proxy-benchmark-soak.sh --mode smoke
```

Run the full soak gate. Soak mode rejects durations shorter than one hour:

```bash
LBP_BENCH_SOAK_SECONDS=3600 bash scripts/bench/proxy-benchmark-soak.sh --mode soak
```

Outputs are written beneath ignored `target/bench/`. JSON/text reports plus the scenario summary record request counts,
p99 observations, 5xx counts, transport errors, and the configured local thresholds. Soak mode also samples
authenticated Prometheus heap and in-flight metrics. It compares six post-warm-up heap-floor buckets, rejects a
positive projected or observed
floor increase above 64 MiB, requires `lbp_proxy_inflight` to return to zero, enforces local p99 budgets, and requires
zero 5xx responses or transport errors during reload and drain phases.

Environment overrides are intentionally numeric and local: rates, durations, p99 budgets, the heap-growth budget,
the loopback port, project name, and output directory. There is no remote base-URL option. Temporary API-key and TLS
material is generated per run, mounted read-only, never written to evidence, and removed during bounded cleanup.

The ordinary CI workflow runs smoke mode against its already-built local images. The scheduled workflow, plus pull
requests that change the soak workflow, benchmark harness, or its Compose fixtures, run the one-hour soak and upload
the ignored reports. These results are regression evidence for the exact local runner and commit only. They do not
establish production SLOs, production capacity, public-ingress safety, sustained real-world load, p95/p99 guarantees,
high availability, or production certification.
