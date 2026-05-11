# Proxy Demo Stack

This is the shortest local path for reviewing the practical reverse proxy mode with two fixture backends, checked-in demo profiles, copyable curl commands, and the browser status page.

The stack is loopback-only. It does not require cloud credentials, does not contact public internet, does not write backend state beyond the fixture health toggle endpoints, and does not prove production gateway readiness or benchmark performance.

## Quick Start

Use one terminal for the two fixture backends and one terminal for LoadBalancerPro.

Recommended cross-platform Java fixture path:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"
```

Classpath fallback:

```bash
mvn -q -DskipTests compile
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode round-robin
```

The Java launcher starts both loopback fixture backends, prints the matching `proxy-demo-*` profile, prints curl recipes, and blocks until `Ctrl+C`. It is the preferred path on both Windows and Unix because it uses Java code from this project instead of shell-specific backend code.

Windows PowerShell:

```powershell
.\scripts\proxy-demo.ps1 -Mode round-robin
```

Unix shell:

```bash
bash scripts/proxy-demo.sh --mode round-robin
```

The scripts also point to the Java launcher path. PowerShell keeps `-LegacyPowerShellFixture` as a local fallback, but the Java fixture launcher is the single cross-platform backend fixture path.

The launcher prints the exact LoadBalancerPro startup command. For the default ports, the round-robin command is:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=proxy-demo-round-robin"
```

Open the read-only browser status page after the app starts:

```text
http://localhost:8080/proxy-status.html
```

Inspect the raw status endpoint:

```bash
curl -s http://127.0.0.1:8080/api/proxy/status
```

## Demo Modes

The scripts support these modes:

```text
round-robin
weighted-round-robin
failover
status
```

Use `status` when the app is already running and you only want the status page and endpoint reminders.

## Java Fixture Launcher

The launcher class is:

```text
com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher
```

Supported modes:

```text
round-robin
weighted-round-robin
failover
```

Optional port overrides:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin --backend-a-port 18091 --backend-b-port 18092"
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode round-robin --backend-a-port 18091 --backend-b-port 18092
```

Fixture endpoints:

- `GET /health`: `200` when healthy, `503` when unhealthy
- `GET /fixture/health/fail`: marks the fixture unhealthy
- `GET /fixture/health/ok`: marks the fixture healthy
- any other path: echoes backend id, method, path, query string, and body length

Failover mode starts `backend-b` with failing health so the active health-check profile has immediate skip evidence.

## Checked-In Demo Profiles

The demo profiles live under `src/main/resources` and are inactive unless explicitly selected:

```text
src/main/resources/application-proxy-demo-round-robin.properties
src/main/resources/application-proxy-demo-weighted-round-robin.properties
src/main/resources/application-proxy-demo-failover.properties
```

All three profiles bind LoadBalancerPro to `127.0.0.1:8080`, enable `/proxy/**`, use loopback upstreams only, and keep retry/cooldown disabled unless an operator adds extra arguments for a resilience demo.

Default application behavior is unchanged: `src/main/resources/application.properties` keeps `loadbalancerpro.proxy.enabled=false`.

## Real-Backend Example Profiles

For local services beyond the fixture backends, copy/adapt the example files under `docs/examples/proxy`:

```text
docs/examples/proxy/application-proxy-real-backend-example.properties
docs/examples/proxy/application-proxy-real-backend-weighted-example.properties
docs/examples/proxy/application-proxy-real-backend-failover-example.properties
```

They use loopback placeholders `http://localhost:9001` and `http://localhost:9002`, include explicit strategy/health/retry/cooldown settings, contain no secrets or public upstream URLs, and are not loaded unless an operator imports or copies them intentionally.

## ROUND_ROBIN Path

Start fixture backends:

```bash
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode round-robin
```

or:

```powershell
.\scripts\proxy-demo.ps1 -Mode round-robin
```

or:

```bash
bash scripts/proxy-demo.sh --mode round-robin
```

Start LoadBalancerPro in a second terminal:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=proxy-demo-round-robin"
```

Run four forwarded requests:

```bash
curl -i http://127.0.0.1:8080/proxy/demo?step=1
curl -i http://127.0.0.1:8080/proxy/demo?step=2
curl -i http://127.0.0.1:8080/proxy/demo?step=3
curl -i http://127.0.0.1:8080/proxy/demo?step=4
curl -s http://127.0.0.1:8080/api/proxy/status
```

Expected first four selected upstream headers:

```text
X-LoadBalancerPro-Upstream: backend-a
X-LoadBalancerPro-Upstream: backend-b
X-LoadBalancerPro-Upstream: backend-a
X-LoadBalancerPro-Upstream: backend-b
X-LoadBalancerPro-Strategy: ROUND_ROBIN
```

Verify `/proxy-status.html` shows both upstreams and increasing forwarded counters.

## WEIGHTED_ROUND_ROBIN Path

Start fixture backends:

```bash
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode weighted-round-robin
```

or:

```powershell
.\scripts\proxy-demo.ps1 -Mode weighted-round-robin
```

or:

```bash
bash scripts/proxy-demo.sh --mode weighted-round-robin
```

Start LoadBalancerPro in a second terminal:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=proxy-demo-weighted-round-robin"
```

The demo profile configures `backend-a` with weight `3.0` and `backend-b` with weight `1.0`.

Run four forwarded requests:

```bash
curl -i http://127.0.0.1:8080/proxy/weighted?step=1
curl -i http://127.0.0.1:8080/proxy/weighted?step=2
curl -i http://127.0.0.1:8080/proxy/weighted?step=3
curl -i http://127.0.0.1:8080/proxy/weighted?step=4
curl -s http://127.0.0.1:8080/api/proxy/status
```

Expected first four selected upstream headers for this fresh local process:

```text
X-LoadBalancerPro-Upstream: backend-a
X-LoadBalancerPro-Upstream: backend-a
X-LoadBalancerPro-Upstream: backend-b
X-LoadBalancerPro-Upstream: backend-a
X-LoadBalancerPro-Strategy: WEIGHTED_ROUND_ROBIN
```

This is selected-upstream evidence for the checked-in local fixture profile. It is not benchmark evidence. Extra manual requests advance the process-local strategy state, so restart LoadBalancerPro before repeating the exact first-four sequence.

## Failover Path

Start fixture backends:

```bash
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode failover
```

or:

```powershell
.\scripts\proxy-demo.ps1 -Mode failover
```

or:

```bash
bash scripts/proxy-demo.sh --mode failover
```

Start LoadBalancerPro in a second terminal:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=proxy-demo-failover"
```

Mark `backend-b` unhealthy, send proxy traffic, then restore it:

```bash
curl http://127.0.0.1:18082/fixture/health/fail
curl -i http://127.0.0.1:8080/proxy/failover?step=1
curl -s http://127.0.0.1:8080/api/proxy/status
curl http://127.0.0.1:18082/fixture/health/ok
curl -i http://127.0.0.1:8080/proxy/failover?step=2
```

Expected evidence while `backend-b` is failing:

- `X-LoadBalancerPro-Upstream: backend-a`
- `X-LoadBalancerPro-Strategy: ROUND_ROBIN`
- `/proxy-status.html` shows `backend-b` as not effectively healthy after the active probe
- `/api/proxy/status` shows health and forwarded counters without reset or mutation controls

## Status Page Verification

Use the browser page:

```text
http://localhost:8080/proxy-status.html
```

or the JSON endpoint:

```bash
curl -s http://127.0.0.1:8080/api/proxy/status
```

Check:

- proxy enabled flag
- configured strategy
- upstream effective health
- last selected upstream
- total and per-upstream forwarded counters
- failure counters
- retry/cooldown fields when optional resilience settings are enabled
- status-class counters

## Cleanup

Stop LoadBalancerPro with `Ctrl+C` in the app terminal.

Stop fixture backends:

- PowerShell script: press Enter in the fixture terminal.
- Unix shell script: press Enter in the fixture terminal, or use `Ctrl+C`.

The scripts use loopback processes only. They do not write generated runtime reports into the repository.

## Troubleshooting

- If `/proxy/**` returns 404, confirm the app was started with one of the `proxy-demo-*` profiles.
- If `/proxy-status.html` loads but reports proxy disabled, confirm `--spring.profiles.active=proxy-demo-round-robin`, `proxy-demo-weighted-round-robin`, or `proxy-demo-failover` was passed to the app.
- If fixture startup fails, check whether ports `18081` or `18082` are already in use.
- If the weighted sequence differs, restart LoadBalancerPro and rerun the first four weighted curl commands before sending any other proxy traffic.
- If failover does not skip `backend-b`, run `curl http://127.0.0.1:18082/fixture/health/fail` before the proxy request and refresh `/proxy-status.html`.
- If Maven cannot compile locally, use CI as the source of truth for tests and compile output; the Java fixture path itself does not require Python, Node, Docker, or public internet.

## Safety Boundaries

- Demo configs are explicit profiles only.
- Default proxy mode remains disabled.
- Demos use loopback upstreams only.
- No cloud services.
- No cloud mutation.
- No `CloudManager` construction.
- No public internet requirement.
- No Python, Node, or Docker requirement for the Java fixture path.
- No secrets.
- No backend reset, metrics reset, or cooldown reset controls.
- No browser storage.
- No generated runtime reports.
- No production gateway, benchmark, certification, legal, identity, or security guarantee.
