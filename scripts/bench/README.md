# Proxy Benchmark And Soak Harness

## Reviewed staging qualification

`proxy-staging-qualification.sh` is the live non-production boundary. Structural validation performs no DNS lookup or
traffic and rejects public ranges, production-looking targets, embedded secrets, incomplete workload cases, and
unhashed action adapters:

```bash
bash scripts/bench/staging-validator-contract-test.sh
bash scripts/bench/staging-deployment-contract-test.sh
bash scripts/bench/proxy-staging-qualification.sh --mode validate --profile /path/to/staging-profile.json
```

Run mode additionally requires a reviewed profile tied to the clean checkout and exact prior/candidate registry digests, private DNS
resolution inside its approved RFC1918/ULA ranges, billable/cleanup authority, a secret API-key file, a pinned CA, and
hash-pinned adapters outside the repository. `verify-deployment`, `rollout-candidate`, and `rollback-prior` must emit
the strict JSON snapshot enforced by `validate-staging-deployment.py`; fault/reset adapters remain self-contained.
Supply paths with `LBP_STAGING_API_KEY_FILE`, `LBP_STAGING_CA_FILE`, and `LBP_STAGING_ACTION_DIR`, then run:

```bash
bash scripts/bench/proxy-staging-qualification.sh --mode run --profile /path/to/staging-profile.json
```

The runner pins resolved private addresses, keeps authentication out of process arguments and evidence, verifies the
prior deployment, rolls the candidate digest under traffic, runs the failure matrix, and restores the prior digest
under traffic. Snapshots must prove full replica convergence, reviewed zone/resource/config/ingress identity,
per-replica metrics, drain completion, and transition bounds. A passing run applies only to that staging target and
those hash-pinned adapters; it is not production authorization.

### Kubernetes staging adapters

`prepare-kubernetes-staging-adapters.py` turns one reviewed Kubernetes boundary into the complete deployment-action
and telemetry adapter set required by both staging runners. Copy
`kubernetes-staging-adapter-profile.example.json` outside the repository, then pin the exact kubectl binary, context,
API server, namespace UID/staging label, change ticket, prior/candidate artifact identities, proxy objects, action
payload hashes, fault targets, and TLS secrets. Validation and compilation do not contact the cluster:

```bash
python3 scripts/bench/prepare-kubernetes-staging-adapters.py \
  --mode validate --profile /secure/reviewed-kubernetes-adapter.json
python3 scripts/bench/prepare-kubernetes-staging-adapters.py \
  --mode build --profile /secure/reviewed-kubernetes-adapter.json \
  --output /secure/compiled-lbp-adapters
```

With the reviewed kubectl context selected, run `/secure/compiled-lbp-adapters/inspect.sh`. The read-only result supplies
the observed configuration and ingress hashes for the frozen staging profile. Copy `bindings.json.actions` into that
profile's `hooks`, and bind `capacitySamplerSha256` into the capacity profile. Run mode then uses:

```bash
export LBP_STAGING_ACTION_DIR=/secure/compiled-lbp-adapters/actions
export LBP_STAGING_CAPACITY_SAMPLER=/secure/compiled-lbp-adapters/capacity-sampler.sh
```

Every generated executable rechecks the kubectl hash, current context, API server, namespace UID, staging label,
artifact identity, configuration/ingress fingerprints, and immutable action payload bytes before it observes or
mutates the reviewed namespace. Every referenced configuration ConfigMap or Secret must be Kubernetes-immutable. The
compiler and runtime contracts use a hermetic kubectl fixture and never contact a cluster:

```bash
bash scripts/bench/kubernetes-staging-adapter-contract-test.sh
bash scripts/bench/kubernetes-staging-runtime-contract-test.sh
```

## Deployment-equivalent staging capacity

`proxy-staging-capacity-staircase.sh` extends the reviewed staging boundary into the real capacity lane. It binds a
capacity profile to the exact bytes of the reviewed staging profile, rolls that profile's candidate digest under
traffic, replaces every candidate replica before each repeat, runs equal/slow/failing/draining/recovering cases across
the reviewed rate ladder, locates a reproducible saturation knee, and restores the prior digest under traffic.
Validation performs no DNS lookup or traffic:

```bash
bash scripts/bench/staging-capacity-contract-test.sh
bash scripts/bench/staging-capacity-evaluator-contract-test.sh
bash scripts/bench/proxy-staging-capacity-staircase.sh --mode validate \
  --staging-profile /path/to/staging-profile.json \
  --capacity-profile /path/to/staging-capacity-profile.json
```

Copy `staging-capacity-profile.example.json` outside the repository and set `stagingBinding.stagingProfileSha256` to
the `sha256sum` of the frozen staging profile. Run mode requires both reviews, the staging secret/CA/action environment
from the staging runner, and `LBP_STAGING_CAPACITY_SAMPLER` pointing to an external non-writable executable whose hash
matches `telemetry.samplerSha256`:

```bash
bash scripts/bench/proxy-staging-capacity-staircase.sh --mode run \
  --staging-profile /path/to/staging-profile.json \
  --capacity-profile /path/to/staging-capacity-profile.json
```

The sampler writes one JSON object to stdout per invocation. It must identify the exact candidate, configuration, and
ingress; use immutable runtime IDs for every ready replica; include zone, CPU, memory, connections, and JVM threads per
replica; and provide deployment-wide monotonic request/retry/shed/limit/GC and per-upstream counters. The enforced
shape is the `sample` root in `validate-staging-capacity-sample.py`. Raw samples and Vegeta results remain under ignored
`target/staging-capacity/`, and `evaluate-staging-capacity.py` recomputes the final envelope from the complete matrix.
Only an authorized passing run creates environment-specific capacity evidence.

## Active-active topology proof

`proxy-active-active-topology.sh` starts two real proxy processes behind a bounded TLS/authenticated round-robin
ingress fixture. It proves both replicas receive traffic, configuration and generations converge, an unhealthy image
candidate is rejected before replica two changes, exact content-addressed image IDs promote and roll back one replica
at a time under load, aggregate per-upstream limits remain inside the reviewed budget, per-instance metrics exist, and
traffic survives one replica stopping and recovering within the profile's bounded failure-detection time:

```bash
bash scripts/bench/proxy-active-active-topology.sh --mode validate
bash scripts/bench/topology-validator-contract-test.sh
bash scripts/bench/proxy-active-active-topology.sh --mode smoke
```

`--mode run --profile /path/to/topology-profile.json` requires a reviewed profile, a clean checkout, and at least
30 seconds per loaded case. CI runs smoke mode and scans the application, candidate, backend fixture, and ingress
images. The local candidate changes proof metadata only; its Docker content ID is not a registry manifest digest.

## Live Kubernetes topology proof

`proxy-kubernetes-topology.sh` creates an isolated kind cluster from a digest-pinned Kubernetes node image, loads the
numeric-non-root proxy and fixture images, and deploys two proxy replicas and redundant backends across two workers and
zones. It sends TLS/API-key-protected connection-churn traffic through a loopback-only NodePort, proves both proxy
replicas and both backends served requests, promotes a metadata-only content-distinct candidate under continuous
traffic, samples pod and Service endpoint continuity, proves complete pod-UID and runtime-image identity transition, requires both
candidate replicas and both backends to serve new traffic, then rolls back under a second continuous load window and
proves fresh pod identities, restoration of the initial runtime image identity, and traffic through both restored
replicas. It next creates two independently rooted one-day server identities as versioned immutable Kubernetes TLS
Secrets, rotates the Deployment's TLS Secret reference under continuous close-per-request traffic using a dual-CA
rollover bundle, and restores the baseline Secret under a second load window. Positive and negative single-CA checks
plus repeated served-leaf SHA-256 fingerprint checks prove the identity changed and returned; both directions also
require complete pod turnover, an unchanged runtime image ID, two-zone endpoint continuity, and post-transition traffic
through both replicas and backends. Before the worker-loss phases, it creates immutable A-only, A+B overlap, and B-only
API-key Secrets. Four zero-unavailable rollouts prove baseline-key traffic through overlap, candidate-key traffic through
commit, the reverse rollback overlap, and final restoration of A-only. Positive and negative authentication checks prove
both overlap windows and both key-retirement boundaries; every phase also requires fresh pod UIDs, two ready endpoints,
an unchanged runtime image, and traffic through both replicas and backends. The runtime accepts only the required primary
plus one optional rotation key and does not dynamically reload Secret files. It then drains and stops one worker under load,
tests the one-replica degraded service, requires both recovered replicas and backends to serve new traffic, then
forcibly stops that recovered worker without a drain. After confirming the worker container is down, it applies
Kubernetes' out-of-service `NoExecute` remediation and force-removes the three exact stateless workload pods from the
API, bounds endpoint withdrawal, proves degraded traffic, and requires fresh pod identity, two-zone placement, and traffic
distribution after recovery. The lab cluster pins iptables kube-proxy to immediate EndpointSlice-triggered updates and a
one-second cleanup sync so the Service failover objective is executable and recorded. The abrupt transition and degraded
windows retain bounded 90% and 95% success floors with 5.5-second p99 ceilings for stale conntrack paths; recovered
traffic returns to the normal 99.9% success and 1.5-second p99 objectives:

```bash
bash scripts/bench/proxy-kubernetes-topology.sh --mode validate
bash scripts/bench/kubernetes-topology-contract-test.sh
bash scripts/bench/proxy-kubernetes-topology.sh --mode smoke
```

Smoke mode requires Docker, kind 0.31.0, kubectl 1.34.3, Vegeta, jq, OpenSSL, and curl. TLS private keys and both API keys
live only in a temporary directory; evidence contains only redacted Secret metadata, key-slot names, and generated leaf fingerprints
and is written beneath `target/kubernetes/`. The result proves disposable Kubernetes content-addressed image,
inbound-server TLS Secret transition/rollback, bounded API-key rotation/rollback, and worker-loss mechanics. It does not
prove dynamic Secret reload or an external secret manager, and it does not deploy or test an ingress controller,
external certificate authority, or client trust-distribution system. Because the local candidate
changes immutable proof metadata but not application layers, it does not prove application-layer release compatibility,
registry integrity,
deployment capacity, external ingress behavior, automatic infrastructure-failure detection, or an authorized staging
environment.

## Local capacity staircase

`proxy-capacity-staircase.sh` is the local baseline lane. It runs equal, slow, failing, draining, and
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
