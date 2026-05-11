# Proxy Strategy Demo Lab

This lab shows strategy-specific reverse proxy behavior with two local loopback backends and the existing optional `/proxy/**` forwarding path. It uses no cloud services, no public internet, no generated reports, and no backend mutation controls.

For the single start-to-finish demo stack path with checked-in profile files, the Java fixture launcher, Windows PowerShell commands, Unix shell commands, status-page verification, and cleanup steps, see [`PROXY_DEMO_STACK.md`](PROXY_DEMO_STACK.md). For the Java launcher details, see [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md).

Start the two local fixture backends with the cross-platform Java launcher:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"
```

Classpath fallback:

```bash
mvn -q -DskipTests compile
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode round-robin
```

Or use the helper script:

```powershell
.\scripts\proxy-demo.ps1 -Mode round-robin
```

The script prints a `mvn spring-boot:run` command for the selected mode. Keep the script terminal open, start LoadBalancerPro in a second terminal with the printed command, then run the curl recipes below.

Open the status page while running any flow:

```text
http://localhost:8080/proxy-status.html
```

The response headers to inspect are:

```text
X-LoadBalancerPro-Upstream
X-LoadBalancerPro-Strategy
```

The status page and `GET /api/proxy/status` should show the selected strategy, last selected upstream, total forwarded count, per-upstream forwarded counters, status-class counters, effective health, and retry/cooldown fields if those optional features are enabled.

## ROUND_ROBIN

Start the fixture:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode round-robin
```

or:

```powershell
.\scripts\proxy-demo.ps1 -Mode round-robin
```

Start LoadBalancerPro with the command printed by the script. The core settings are:

```text
--loadbalancerpro.proxy.enabled=true
--loadbalancerpro.proxy.strategy=ROUND_ROBIN
--loadbalancerpro.proxy.upstreams[0].id=backend-a
--loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:18081
--loadbalancerpro.proxy.upstreams[1].id=backend-b
--loadbalancerpro.proxy.upstreams[1].url=http://127.0.0.1:18082
```

Run four forwarded requests:

```bash
curl -i http://127.0.0.1:8080/proxy/demo?step=1
curl -i http://127.0.0.1:8080/proxy/demo?step=2
curl -i http://127.0.0.1:8080/proxy/demo?step=3
curl -i http://127.0.0.1:8080/proxy/demo?step=4
curl -s http://127.0.0.1:8080/api/proxy/status
```

Expected selected-upstream evidence for the first four requests:

```text
backend-a, backend-b, backend-a, backend-b
```

Each forwarded response should include `X-LoadBalancerPro-Strategy: ROUND_ROBIN`. The status page should show both upstreams with forwarded counters increasing.

## WEIGHTED_ROUND_ROBIN

Start the fixture:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode weighted-round-robin"
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode weighted-round-robin
```

or:

```powershell
.\scripts\proxy-demo.ps1 -Mode weighted-round-robin
```

Start LoadBalancerPro with the command printed by the script. The weighted settings are:

```text
--loadbalancerpro.proxy.strategy=WEIGHTED_ROUND_ROBIN
--loadbalancerpro.proxy.upstreams[0].weight=3.0
--loadbalancerpro.proxy.upstreams[1].weight=1.0
```

Run four forwarded requests:

```bash
curl -i http://127.0.0.1:8080/proxy/weighted?step=1
curl -i http://127.0.0.1:8080/proxy/weighted?step=2
curl -i http://127.0.0.1:8080/proxy/weighted?step=3
curl -i http://127.0.0.1:8080/proxy/weighted?step=4
curl -s http://127.0.0.1:8080/api/proxy/status
```

With the current smooth weighted round-robin implementation and weights `3.0` to `1.0`, the first four selected upstreams are expected to be:

```text
backend-a, backend-a, backend-b, backend-a
```

This is selected-upstream evidence for the configured local demo, not a throughput benchmark. Longer sequences should trend toward the configured weights, but exact long-running distributions depend on request ordering and process-local strategy state.

## Health-Aware Failover

Start the fixture:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode failover"
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode failover
```

or:

```powershell
.\scripts\proxy-demo.ps1 -Mode failover
```

Start LoadBalancerPro with the command printed by the script. The failover flow uses active health checks:

```text
--loadbalancerpro.proxy.strategy=ROUND_ROBIN
--loadbalancerpro.proxy.health-check.enabled=true
--loadbalancerpro.proxy.health-check.path=/health
--loadbalancerpro.proxy.health-check.interval=1s
```

Mark backend-b unhealthy, then send proxy traffic:

```bash
curl http://127.0.0.1:18082/fixture/health/fail
curl -i http://127.0.0.1:8080/proxy/failover?step=1
curl -s http://127.0.0.1:8080/api/proxy/status
curl http://127.0.0.1:18082/fixture/health/ok
curl -i http://127.0.0.1:8080/proxy/failover?step=2
```

Expected evidence while backend-b is failing:

- forwarded response header `X-LoadBalancerPro-Upstream: backend-a`
- forwarded response header `X-LoadBalancerPro-Strategy: ROUND_ROBIN`
- status page shows backend-b `effectiveHealthy=false`
- backend-b forwarded counter remains lower than backend-a while the health probe reports it unavailable

## Troubleshooting

- If `/proxy/**` returns 404, confirm LoadBalancerPro was started with `--loadbalancerpro.proxy.enabled=true`.
- If the same upstream appears repeatedly in the round-robin demo, check whether an earlier request already advanced the process-local strategy state.
- If weighted output differs after extra manual requests, restart LoadBalancerPro and rerun the first four weighted curl commands from a fresh process.
- If failover does not skip backend-b, confirm health checks are enabled and `curl http://127.0.0.1:18082/fixture/health/fail` was run before the proxy request.
- If the status page cannot load, verify `GET /api/proxy/status` from the same host and port.

## Safety Boundaries

- Proxy mode remains optional and disabled by default.
- Demos use loopback backends only.
- No `CloudManager` construction.
- No cloud mutation.
- No external network dependency.
- No backend write, reset, or mutation controls.
- No browser storage.
- No production gateway, benchmark, certification, legal, identity, or security guarantee.
