# Proxy Benchmark And Soak Harness

## Access-log overhead lane

Run `bash scripts/bench/access-log-overhead.sh` for five fresh-JVM enabled/disabled access-log comparisons. Raw JSON
samples and Maven logs are written under `target/access-log-benchmark/`. Set
`LBP_ACCESS_LOG_BENCHMARK_FORKS=3..20` to change the fork count.

This lane is diagnostic local or hosted-runner evidence. It does not enforce or prove the production `<5%` target,
production throughput or latency, SLOs, production readiness, or production certification. Deterministic access-log
correctness and architecture guards remain in the default Maven suite.

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
