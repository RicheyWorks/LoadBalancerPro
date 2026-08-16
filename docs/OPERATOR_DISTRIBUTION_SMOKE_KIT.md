# Operator Distribution Smoke Kit

This smoke kit validates the production proxy distribution path without creating a release. It checks the packaged-jar path, real-backend example configs, the packaged proxy status resource, and proxy status endpoints. Simulation and demo tooling belongs to the separate opt-in Lab Tools artifact.

No tags, releases, or assets are created. The kit does not upload artifacts, does not modify release workflows, and does not touch local release evidence.

For focused local checksum, manifest, production resource, and lab-content absence verification after packaging, see [`LOCAL_ARTIFACT_VERIFICATION.md`](LOCAL_ARTIFACT_VERIFICATION.md).

GitHub CI now runs the same release-free packaged artifact inspection and uploads the `packaged-artifact-smoke` workflow artifact with `artifact-smoke-summary.txt`, `artifact-sha256.txt`, and `jar-resource-list.txt`. This is workflow evidence only; it does not create tags, GitHub releases, release assets, or `release-downloads/` files.

For one reviewer guide covering CI artifact download, JaCoCo coverage, SBOM files, packaged smoke files, and local-vs-CI SHA-256 comparison, use [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md).

For the combined release-free go/no-go review packet, use [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md).

For one command matrix that compares packaged jar, Maven exec, Spring profile, proxy demo stack, status page, CI artifact, local artifact, and distribution smoke paths, use [`OPERATOR_INSTALL_RUN_MATRIX.md`](OPERATOR_INSTALL_RUN_MATRIX.md). For the hard stop before any future release publication step, use [`RELEASE_INTENT_CHECKLIST.md`](RELEASE_INTENT_CHECKLIST.md).

## Prerequisites

- Java 17
- Maven
- curl for the optional HTTP smoke commands
- A clean local checkout with normal Maven dependency access

If Maven dependency resolution fails because a workstation trust store cannot validate Maven Central certificates, use GitHub CI as the source of truth for build and package validation.

## Static Smoke Check

Windows PowerShell:

```powershell
.\scripts\operator-distribution-smoke.ps1
```

Unix shell:

```bash
bash scripts/operator-distribution-smoke.sh
```

The default mode checks that these files are present and prints the operator commands:

- `pom.xml`
- `src/main/resources/static/proxy-status.html`
- `src/main/resources/application-proxy-prod.properties`
- `docs/examples/proxy/application-proxy-real-backend-example.properties`
- `docs/examples/proxy/application-proxy-real-backend-weighted-example.properties`
- `docs/examples/proxy/application-proxy-real-backend-failover-example.properties`

## Package Smoke

Build the normal Spring Boot jar without a release:

```bash
mvn -B -DskipTests package
```

Resolve the expected executable jar path from Maven's effective `project.build.finalName`:

```powershell
$jarPath = .\scripts\resolve-executable-jar.ps1
```

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
```

Both resolvers require the exact current-project artifact and ignore stale jars for other versions.

Windows helper:

```powershell
.\scripts\operator-distribution-smoke.ps1 -Package
```

Unix helper:

```bash
bash scripts/operator-distribution-smoke.sh --package
```

## Packaged Jar Startup Smoke

Start the packaged jar on a loopback-only port:

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
java -jar "$JAR_PATH" --server.address=127.0.0.1 --server.port=18080 --spring.profiles.active=local
```

Then check:

```bash
curl -fsS http://127.0.0.1:18080/actuator/health
curl -fsS http://127.0.0.1:18080/proxy-status.html
curl -fsS http://127.0.0.1:18080/api/proxy/status
```

Windows helper with package and HTTP smoke:

```powershell
.\scripts\operator-distribution-smoke.ps1 -Package -RunJarSmoke
```

Unix helper with package and HTTP smoke:

```bash
bash scripts/operator-distribution-smoke.sh --package --run-jar-smoke
```

The HTTP smoke starts the jar locally, waits for `/actuator/health`, checks `/proxy-status.html`, checks `/api/proxy/status`, and stops the process.

## Optional Lab Tools Smoke

Demo commands and resources are intentionally absent from the production jar. Package and resolve the opt-in artifact first:

```bash
mvn -B -P lab -DskipTests package
LAB_JAR_PATH="$(bash scripts/resolve-executable-jar.sh --lab)"
```

The Java loopback fixture launcher can also be started directly from the compiled source tree:

```bash
mvn -q -P lab -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"
```

The fixture launcher binds to loopback by default and blocks until stopped by the operator.

## Proxy Demo Profile Smoke

After starting fixture backends, run LoadBalancerPro with one of the explicit demo profiles:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=proxy-demo-round-robin"
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=proxy-demo-weighted-round-robin"
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=proxy-demo-failover"
```

Or with the Lab Tools jar:

```bash
java -jar "$LAB_JAR_PATH" --spring.profiles.active=proxy-demo-round-robin
```

Then verify:

```bash
curl -i http://127.0.0.1:8080/proxy/demo?step=1
curl -s http://127.0.0.1:8080/api/proxy/status
```

Expected response headers for forwarded traffic:

```text
X-LoadBalancerPro-Upstream
X-LoadBalancerPro-Strategy
```

## Real-Backend Example Config Smoke

The real-backend examples are copy/adapt configs under `docs/examples/proxy`. They are not loaded by default.

```text
docs/examples/proxy/application-proxy-real-backend-example.properties
docs/examples/proxy/application-proxy-real-backend-weighted-example.properties
docs/examples/proxy/application-proxy-real-backend-failover-example.properties
```

Example import command:

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
java -jar "$JAR_PATH" --spring.config.import=optional:file:docs/examples/proxy/application-proxy-real-backend-example.properties
```

The example upstreams are loopback placeholders:

```text
http://localhost:9001
http://localhost:9002
```

They contain no secrets, no cloud config, and no public upstream URLs.

## Packaged Static Resource Checks

After packaging, verify static resources are inside the jar:

Windows PowerShell:

```powershell
$jarPath = .\scripts\resolve-executable-jar.ps1
jar tf $jarPath | Select-String "BOOT-INF/classes/static/proxy-status.html"
```

Unix shell:

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
jar tf "$JAR_PATH" | grep 'BOOT-INF/classes/static/proxy-status.html'
```

The proxy operator status page uses `GET /api/proxy/status` as its status source.

For a reusable helper that checks these entries and rejects lab-only content, run:

```powershell
.\scripts\local-artifact-verify.ps1
```

or:

```bash
bash scripts/local-artifact-verify.sh
```

## Cleanup

- Stop the packaged jar with `Ctrl+C`, or let the helper scripts stop it automatically.
- Stop fixture launchers with `Ctrl+C`.
- Remove local `target/` output if you want a clean working tree.
- Do not create release artifacts for this smoke path.

## Troubleshooting

- If Maven cannot download dependencies locally, verify the same branch in GitHub CI.
- If `/proxy-status.html` is missing, rerun `mvn -B -DskipTests package` and inspect the jar resource list.
- If `/api/proxy/status` reports proxy disabled, that is expected for default local startup.
- If `/proxy/**` returns 404, import a production proxy config with explicit loopback/private upstreams, or use the Lab Tools artifact for an explicit `proxy-demo-*` profile.
- If the fixture launcher command exits immediately, rerun it with `--help` to inspect supported modes.

## Safety Boundaries

- Release-free only.
- No tags, releases, or assets.
- No release workflow changes.
- No release evidence changes.
- No default proxy enablement.
- No default application behavior changes.
- No cloud services.
- No cloud mutation.
- No `CloudManager` construction from smoke paths.
- No Docker, Python, Node, or heavy packaging framework requirement.
- No generated runtime reports committed.
- No production gateway, benchmark, certification, legal, identity, or security guarantee.
