# Proxy Demo Fixture Launcher

`ProxyDemoFixtureLauncher` is a small Java-only loopback fixture for local reverse proxy demos. It starts two backend HTTP servers from this project so Windows and Unix reviewers can use the same backend behavior without Python, Node, Docker, cloud services, or public internet.

It is optional demo tooling. It does not change default application behavior, does not enable proxy mode by default, does not persist state, and is not a production gateway or benchmark proof.

## Build The Launcher

From the repository root:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"
```

That Maven exec recipe is the easiest one-command launcher path. The `exec-maven-plugin` is not bound to the default Maven lifecycle, so normal tests, packaging, and Spring Boot startup behavior are unchanged.

If you prefer a separate compile and classpath command:

```bash
mvn -q -DskipTests compile
```

The launcher uses JDK `HttpServer`, so `target/classes` is enough for the fixture process.

## Start Fixture Backends

ROUND_ROBIN:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode round-robin
```

WEIGHTED_ROUND_ROBIN:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode weighted-round-robin"
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode weighted-round-robin
```

Failover:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode failover"
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode failover
```

Optional port overrides:

```bash
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode round-robin --backend-a-port 18091 --backend-b-port 18092
```

The launcher binds only to `127.0.0.1` by default. `--host localhost` is also accepted.

## Fixture Behavior

The launcher starts:

- `backend-a` on `http://127.0.0.1:18081`
- `backend-b` on `http://127.0.0.1:18082`

Each backend exposes:

- `/health`: returns `200` when healthy and `503` when unhealthy
- `/fixture/health/fail`: marks that fixture unhealthy
- `/fixture/health/ok`: marks that fixture healthy
- any other path: echoes backend id, HTTP method, path, query string, and request body length

Responses include:

```text
X-Fixture-Upstream: backend-a
X-Fixture-Upstream: backend-b
```

Failover mode starts `backend-b` with failing health so active proxy health checks can skip it immediately.

## Start LoadBalancerPro

The launcher prints the matching profile and startup command. For the default round-robin demo:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=proxy-demo-round-robin"
```

Profile mapping:

- `round-robin` -> `proxy-demo-round-robin`
- `weighted-round-robin` -> `proxy-demo-weighted-round-robin`
- `failover` -> `proxy-demo-failover`

If custom fixture ports are used, the launcher prints the needed upstream URL overrides.

## Verify Through The Proxy

Round-robin:

```bash
curl -i http://127.0.0.1:8080/proxy/demo?step=1
curl -i http://127.0.0.1:8080/proxy/demo?step=2
curl -i http://127.0.0.1:8080/proxy/demo?step=3
curl -i http://127.0.0.1:8080/proxy/demo?step=4
```

Weighted round-robin:

```bash
curl -i http://127.0.0.1:8080/proxy/weighted?step=1
curl -i http://127.0.0.1:8080/proxy/weighted?step=2
curl -i http://127.0.0.1:8080/proxy/weighted?step=3
curl -i http://127.0.0.1:8080/proxy/weighted?step=4
```

Failover:

```bash
curl -i http://127.0.0.1:8080/proxy/failover?step=1
curl -s http://127.0.0.1:8080/api/proxy/status
curl http://127.0.0.1:18082/fixture/health/ok
curl -i http://127.0.0.1:8080/proxy/failover?step=2
```

Expected proxy evidence headers:

```text
X-LoadBalancerPro-Upstream
X-LoadBalancerPro-Strategy
```

Open the read-only status page:

```text
http://localhost:8080/proxy-status.html
```

Inspect raw status JSON:

```bash
curl -s http://127.0.0.1:8080/api/proxy/status
```

## Helper Scripts

The helper scripts now point to the Java launcher path:

```powershell
.\scripts\proxy-demo.ps1 -Mode round-robin
```

```bash
bash scripts/proxy-demo.sh --mode round-robin
```

PowerShell also keeps a legacy fixture fallback:

```powershell
.\scripts\proxy-demo.ps1 -Mode round-robin -LegacyPowerShellFixture
```

## Cleanup

Stop the launcher with `Ctrl+C`. The fixture state is process-local and disappears when the launcher exits.

## Limits

- Loopback demo only.
- No cloud services or cloud mutation.
- No `CloudManager` construction.
- No Python, Node, or Docker requirement for the Java launcher path.
- No persistent demo state.
- No default proxy behavior change.
- No production gateway, benchmark, certification, legal, identity, or security guarantee.
