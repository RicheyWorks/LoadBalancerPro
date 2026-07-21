# LoadBalancerPro — Build Plan: Path to Deployable

**Companion to:** `AUDIT_2026-07-21.md` (defect IDs D1–D16 and phase items referenced below come from that document)
**Goal:** take the reverse proxy from "defensively-written demo, off by default" to a load balancer you can put in front of real traffic.
**Shape:** 5 milestones, ~18 PRs, each PR independently reviewable and shippable. Written to be executed by Codex PR-by-PR; every PR lists scope, touched files, config surface, and acceptance criteria.

Existing config baseline (verified in `application-proxy-demo-*.properties` / `ReverseProxyProperties`): `loadbalancerpro.proxy.{enabled,strategy,request-timeout,max-request-bytes}`, nested `health-check.*`, `retry.*`, `cooldown.*`, `routes.*`, `upstreams[n].*`. All new keys below extend this namespace — no breaking renames in Milestones 1–3.

---

## Milestone 0 — Stop the bleeding (bug fixes, no new features)

Small surgical PRs. All are pure fixes to verified defects; land before feature work so later PRs build on sound ground.

### PR-0.1 Remove per-request shutdown-hook leak (D2)
- **Change:** `ServerMonitor` constructor (`core/ServerMonitor.java:92`) must not register a JVM shutdown hook. Move hook registration into `start()` (register once, keep the `Thread` reference) and deregister in `stop()` via `removeShutdownHook`, guarding `IllegalStateException` during actual shutdown. `AllocatorService`-created balancers never call `start()`, so they get no hook at all.
- **Files:** `core/ServerMonitor.java`, test in `core/ServerMonitorTest`.
- **Accept:** loop 10k `POST /api/allocate/capacity-aware` in a test → `ApplicationShutdownHooks` size stable (assert via reflection or a counter hook); heap stable under `-Xmx64m`.

### PR-0.2 Health eviction → drain + re-admission (D3)
- **Change:** `ServerHealthCoordinator.detectFailedServers` stops deleting servers. Introduce `ServerDegradationState` transitions (the enum already exists): HEALTHY → DEGRADED (threshold breach) → EVICTED only after N consecutive bad cycles; recovered servers (M consecutive good cycles) return to rotation. Manual `setHealthy(false)` = DRAINING, never eviction. Registry removal only via explicit admin call.
- **Files:** `core/ServerHealthCoordinator.java`, `core/LoadBalancer.java:146-163`, `core/Server.java`.
- **Accept:** unit test — one bad sample does not remove; N bad samples evicts; recovery re-admits; drained server is skipped by allocation but present in registry.

### PR-0.3 Per-route strategy instances (D4)
- **Change:** `RoutingStrategyRegistry` returns a **factory** per `RoutingStrategyId`; `ReverseProxyRoutePlanner` instantiates one strategy per route at config build/reload time and stores it on the route object. Stateful strategies (WRR cursor/accumulators, RR cursor) are therefore route-scoped. On reload, carry the old instance over when the route's strategy id and upstream set are unchanged (preserves smooth-WRR state).
- **Files:** `core/RoutingStrategyRegistry.java`, `api/proxy/ReverseProxyRoutePlanner.java`, `api/proxy/ReverseProxyService.java`.
- **Accept:** integration test with two WRR routes (weights 3:1) interleaved → each route's observed split within 5% of 3:1 over 1k requests.

### PR-0.4 Weight-0 = drain (D13) + retry classification fix (interrupted ≠ retriable)
- **Change:** `effectiveWeight`: weight 0 → excluded from candidates (equivalent to `healthy=false` for selection) in WRR/WLC; validation rejects negative weights, allows 0 with documented drain semantics. `forwardOnce` marks `InterruptedException` non-retriable.
- **Files:** `core/WeightedRoundRobinRoutingStrategy.java:100-105`, `core/WeightedLeastConnectionsRoutingStrategy.java:80-85`, `api/proxy/ReverseProxyService.java:405-413`.
- **Accept:** weight-0 upstream receives zero requests; weight 0.01 gets ~1% vs weight 1.0 peer.

### PR-0.5 Auth fail-closed in api-key mode (D9) + actuator lockdown (D15)
- **Change:** `ProdApiKeyFilter` drops `@Profile` gating → active whenever auth-mode=api-key; **startup fails** if api-key mode with empty key unless `loadbalancerpro.api.auth-mode=none` is explicitly set (new explicit "I know it's open" mode for local dev). Default `application.properties` exposure narrowed to `health,info`; `metrics,prometheus` moved behind auth (see PR-3.2).
- **Files:** `api/config/ProdApiKeyFilter.java`, `api/config/ApiSecurityConfiguration.java`, `application.properties`.
- **Accept:** default profile + no key → app refuses to start with clear message; auth-mode=none logs a prominent warning; prod behavior unchanged.

### PR-0.6 Simulation-core correctness batch (D10, D11, D12 + P3 items)
- Load-shedding priority ordering fixed (CRITICAL never shed before USER at same pressure); redistribution merge bug (`ServerHealthCoordinator.java:74-85`) — use `leastLoaded()`'s merged result, drop `putAllAllocations` overwrite; `consistentHashing` takes the server read-lock and handles empty-ring between checks; `Math.floorMod` in `Server.updateHistory`; validate-before-snapshot in `updateMetrics`; synchronize single-metric setters properly; delete dead `loadQueue`; remove `isCloudServer` global property override.
- **Accept:** existing test suite green + new unit tests per fix.

**Milestone 0 exit:** all P0/P1 defects in the audit closed except D1 (telemetry — Milestone 1) and D5 (streaming — Milestone 2).

---

## Milestone 1 — An honest adaptive proxy (Phase A)

### PR-1.1 Timeout correctness (D7)
- **Config:** `proxy.connect-timeout` (default `1s`) applied via `HttpClient.newBuilder().connectTimeout(...)`; per-route `routes.<name>.request-timeout` overriding the global; existing `health-check.timeout` unchanged.
- **Files:** `api/proxy/ReverseProxyConfiguration.java`, `ReverseProxyProperties.Route`, `ReverseProxyService`.
- **Accept:** upstream that accepts-then-blackholes → request fails in ≈connect/request timeout, not minutes; per-route override observed.

### PR-1.2 Upstream runtime stats (foundation for D1)
- **New class:** `api/proxy/UpstreamRuntimeStats` — per-upstream: `LongAdder inFlight` (incremented before `httpClient.send`, decremented in `finally`), rolling latency (fixed 256-slot ring of recent millis + EWMA; compute p50/p95/p99 on snapshot), error-rate window (sliding 30s success/failure counts), last-updated timestamp. Held in `ConcurrentHashMap<String, UpstreamRuntimeStats>` in `ReverseProxyService`; survives reload for unchanged upstream ids (fixes half of D14).
- **Accept:** concurrent load test → inFlight returns to 0 after quiesce (no drift, including on exceptions); stats visible in status endpoint.

### PR-1.3 Live telemetry → routing (closes D1)
- **Change:** `toCandidate()` builds `ServerStateVector` from `UpstreamRuntimeStats` (in-flight, avg/p95/p99 latency, error rate, queueDepth = inFlight) merged with config (weight, admin healthy). Config telemetry fields on `Upstream` become **seed/fallback values** (deprecation note in javadoc, kept for compatibility one release).
- **Accept:** integration test with one artificially slow upstream under WEIGHTED_LEAST_CONNECTIONS → slow upstream's share drops materially (assert < 30% of requests); same test under TAIL_LATENCY_POWER_OF_TWO shows tail improvement vs ROUND_ROBIN.

### PR-1.4 Background health checking (D6)
- **Change:** dedicated `ScheduledExecutorService` (daemon, name-prefixed threads), one jittered task per upstream per interval; **rise/fall thresholds**: `health-check.healthy-threshold` (default 2), `health-check.unhealthy-threshold` (default 3). Request path reads a volatile `HealthSnapshot`; `statusSnapshot` becomes read-only (no probes, no cooldown mutation). Cooldown failure-counting driven only by real forwarding failures + prober results (single-count).
- **Files:** new `api/proxy/UpstreamHealthProber.java`; `ReverseProxyService` sheds `probeDue`/inline probing.
- **Accept:** zero probe I/O on request threads (assert via instrumentation); flapping upstream (alternating probe results) does not flap state; status GET has no side effects.

### PR-1.5 Forwarding headers (D8)
- **Config:** `proxy.forwarded.mode=strip-and-set|append|off` (default `strip-and-set`), `proxy.forwarded.trusted-proxies=<CIDR list>` (inbound values honored only from these), `routes.<name>.headers.{add,set,remove}` map for static rewrite rules.
- **Change:** inject `X-Forwarded-For` (append-safe), `X-Forwarded-Proto`, `X-Forwarded-Host`, RFC 7239 `Forwarded`; strip inbound spoofables unless from trusted CIDR.
- **Accept:** backend fixture asserts correct XFF chain for direct and chained-proxy cases; spoofed inbound XFF from untrusted source not forwarded.

### PR-1.6 Live load shedding + concurrency limits (wires dormant core code)
- **Config:** `proxy.limits.max-in-flight` (global), `upstreams[n].max-in-flight`, `proxy.shedding.enabled` + pressure thresholds mapping to existing `LoadSheddingConfig`.
- **Change:** `forward()` consults limits using PR-1.2 counters → 503 + `Retry-After` when exceeded; `AdaptiveConcurrencyLimiter` optional mode (`proxy.limits.adaptive=true`) adjusting the global cap from latency feedback.
- **Accept:** saturate a 2-upstream setup beyond cap → excess gets fast 503, upstreams never see > cap concurrent; CRITICAL priority requests (header-mapped) shed last.

### PR-1.7 Consistent-hash strategy + cookie affinity
- **Config:** `routes.<name>.strategy=CONSISTENT_HASH`, `routes.<name>.hash-on=client-ip|header:<name>`; `routes.<name>.affinity.cookie-name` (cookie mode independent of strategy).
- **Change:** register `ConsistentHashRingStrategy` implementing `RoutingStrategy` over the existing ring (port fixes ring locking per PR-0.6); affinity cookie (HMAC of upstream id, key from config) checked before strategy, fallback to strategy when target unhealthy.
- **Accept:** same key always → same healthy upstream; upstream removal remaps only ~1/N keys; cookie pin survives across requests and fails over cleanly.

### PR-1.8 Retry budget + backoff, slow-start
- **Config:** `retry.budget-percent` (default 20), `retry.backoff={base,max}` (exponential, jittered); `proxy.slow-start.duration` (default 0=off) — linear weight ramp for upstreams newly added or exiting cooldown; cooldown expiry no longer resets failure memory to zero (keep half).
- **Accept:** brownout test — retries capped at budget; recovering upstream's traffic ramps rather than steps.

**Milestone 1 exit:** all five strategies honest on live traffic; health, timeouts, shedding, affinity real. This is the "credible single-node L7 LB (buffered)" checkpoint.

---## Milestone 2 — Deployable data path

### PR-2.1 Streaming request path (D5, part 1)
- **Change:** `ReverseProxyController` stops taking `@RequestBody byte[]`; reads `HttpServletRequest.getInputStream()` and forwards via `BodyPublishers.ofInputStream`. Pre-check `Content-Length` against `max-request-bytes` before reading; for chunked inbound, enforce cap with a counting bounded stream (abort with 413 mid-stream). Remove the redundant `clone()`.
- **Accept:** 1GB request rejected instantly on Content-Length; chunked over-limit aborted at the cap with bounded memory (`-Xmx128m` test); normal POSTs byte-identical at backend.

### PR-2.2 Streaming response path (D5, part 2)
- **Change:** `BodyHandlers.ofInputStream()` → stream to `HttpServletResponse` output with a fixed copy buffer; new `proxy.max-response-bytes` (default 0=unlimited, streamed so memory-safe either way); flush strategy compatible with SSE (`text/event-stream` passes through incrementally — do not buffer-and-forward).
- **Note:** retry semantics change — a response can only be retried before first byte is written to the client; encode that rule explicitly in `forward()`.
- **Accept:** 2GB response proxied under `-Xmx128m`; SSE fixture streams events with < 100ms added latency per event; retries still work for connect-phase failures.

### PR-2.3 TLS termination + SNI + hot cert reload
- **Config:** standard Spring `server.ssl.bundle` (SSL bundles give file-watch reload); document keystore/PEM setup in `docs/DEPLOYMENT.md`; optional second connector for cleartext health traffic if needed.
- **Backend TLS:** `proxy.backend-tls.truststore` (custom CA), `upstreams[n].tls.{verify (default true), client-cert}` for mTLS to backends. Never expose a "verify=false" without a loud startup warning.
- **Accept:** TLS termination e2e test with self-signed bundle; cert file swap picked up without restart; backend mTLS fixture handshake verified.

### PR-2.4 Graceful shutdown + draining reload (D14 complete)
- **Change:** `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase` (default 30s) in all profiles; prober/executors implement `SmartLifecycle` stop; reload diff engine — unchanged upstreams keep runtime stats/health/cooldown, removed upstreams enter DRAINING (no new picks, config retained until in-flight drains or timeout), added upstreams start in slow-start.
- **Accept:** SIGTERM under load → zero failed in-flight requests, exit within window; reload that drops an upstream mid-load → no 5xx from that transition.

### PR-2.5 Deployment packaging: proxy on by default in a real prod story
- **Change:** new `application-proxy-prod.properties` profile: proxy enabled, api-key auth enforced, actuator behind auth, health-check + cooldown + limits enabled with sane defaults; Dockerfile gains `HEALTHCHECK` against LB health (not just API health), documented env-var config surface (`LBP_UPSTREAM_0_URL` style via Spring relaxed binding); `docs/DEPLOYMENT.md` with docker-compose example (LB + 2 backends), K8s manifest sketch (readiness = `/api/health`, preStop drain sleep), and config reference table generated from `ReverseProxyProperties`.
- **Accept:** `docker compose up` from the example proxies traffic with TLS, auth, health checks, metrics — no code edits required.

**Milestone 2 exit:** streaming, TLS, graceful lifecycle, and a documented turnkey deployment. This is the "you can actually put it in front of something" checkpoint.

---

## Milestone 3 — Operable

### PR-3.1 Micrometer metrics
- Port `ReverseProxyMetrics` to `MeterRegistry`: `lbp.proxy.requests` (counter; tags: route, upstream, status_class, outcome), `lbp.proxy.latency` (timer w/ histogram buckets; route, upstream), `lbp.proxy.inflight` (gauge per upstream), `lbp.proxy.retries`, `lbp.proxy.sheds`, `lbp.proxy.health` (gauge 0/1), `lbp.proxy.cooldown.trips`. Keep the JSON status endpoint reading from the same source.
- **Accept:** `/actuator/prometheus` (behind auth, enabled in proxy-prod profile) exposes all series; Grafana-ready; cardinality bounded by config (no per-client tags).

### PR-3.2 Access log
- Structured per-request line (JSON or combined-log configurable): timestamp, client, method, path, route, upstream, status, bytes in/out, duration, retries, shed/cooldown flags. Async appender; `proxy.access-log.{enabled,format,path}`. Sampling knob for high QPS.
- **Accept:** every proxied request (success and failure) produces exactly one line; overhead < 5% at saturation benchmark.

### PR-3.3 Admin API v1 (incremental config)
- `POST /api/proxy/upstreams` (add), `DELETE /api/proxy/upstreams/{id}` (→ DRAINING then remove), `PATCH /api/proxy/upstreams/{id}` (weight/healthy/drain), `GET /api/proxy/config` (redacted effective config + generation). Same auth as reload; every mutation audit-logged with generation bump. Full-config reload remains for bulk changes.
- **Accept:** add/drain/remove cycle under load with zero dropped requests; concurrent mutations serialized (or 409 on generation conflict).

### PR-3.4 Benchmark + soak harness (CI-gated)
- `scripts/bench/` — wrk/vegeta scenario set (steady, spike, slow-backend, backend-kill, reload-under-load, drain-under-load) against the compose stack; nightly soak (1h) asserting: no heap growth trend, inFlight returns to zero, p99 within budget, zero 5xx during drain/reload scenarios. This is the regression net that keeps Milestones 0–2 fixed.

---

## Milestone 4 — Growth (post-deployable, design-doc first)

- **Host/header-based routing rules** and percentage canary splitting (generalize route matching; precedence: host > path-prefix length; `routes.<name>.match.{host,header.<h>}`; `split` groups with percentage weights).
- **DNS service discovery:** `upstreams[n].discovery=dns:<name>:<port>`, periodic re-resolution with per-IP health, respecting TTL floor.
- **HTTP/2 inbound + WebSocket passthrough:** decision point — Tomcat h2 + servlet upgrade handling vs migrating the proxy layer to Netty/reactive. Write `docs/adr/ADR-streaming-stack.md` first (the Milestone 2 servlet-streaming work is compatible with either, but WebSocket forces the choice).
- **Simulation core quarantine:** move `lab/`, DecisionExplorer/Replay/Evidence services, GUI, demo CLIs behind a Maven profile/module (`-P lab`) so the deployable artifact is the ~15% that's real. Retire `ServerMonitor`'s random-walk or fence it behind `demo` profile. This roughly halves the audit surface and jar size, and stops sim code (with its own bug tail) from shipping in prod builds.

---

## Sequencing & dependency graph

```
M0: 0.1 0.2 0.3 0.4 0.5 0.6        (parallel-safe, independent)
M1: 1.1 → 1.2 → 1.3 → 1.6          (1.2 is the spine: stats feed 1.3, 1.6, 3.1)
        1.4 (independent after 0.x)
        1.5 (independent)
        1.7 (after 0.3, 0.6)
        1.8 (after 1.2)
M2: 2.1 → 2.2 → 2.3 → 2.5          (2.4 after 1.4; 2.5 last, integrates all)
M3: 3.1 (after 1.2), 3.2, 3.3 (after 2.4), 3.4 (after 2.5)
```

Suggested PR cadence for Codex: land M0 as a batch of 6 small PRs first (each < ~300 lines diff), then M1 in the arrow order above. Every PR: unit tests + one integration test against the `ProxyDemoFixtureLauncher` backends (extend the fixture with `slow`, `blackhole`, `flaky`, and `sse` modes in PR-1.1 — several acceptance tests above need them). Definition of done for "deployable" = Milestone 2 exit + PR-3.4 soak green.
