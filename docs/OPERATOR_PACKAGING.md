# Operator Packaging

This page collects the shortest operator commands for the production proxy artifact, the opt-in Lab Tools artifact, local fixtures, and real-backend examples. Default application behavior remains unchanged: `src/main/resources/application.properties` keeps `loadbalancerpro.proxy.enabled=false`.

Start reviewer evidence navigation with [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md). Use [`OPERATOR_RUN_PROFILES.md`](OPERATOR_RUN_PROFILES.md) for the concise profile matrix and copyable startup recipes before drilling into packaging details here.

## Fixture Launcher

The easiest launcher path is Maven exec. It compiles the classes if needed and starts the Java loopback fixture launcher without a manual classpath command:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"
```

Other modes:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode weighted-round-robin"
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode failover"
```

Classpath fallback after compilation:

```bash
mvn -q -DskipTests compile
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode round-robin
```

The Maven exec plugin is declared without lifecycle bindings, so it does not replace the Spring Boot main class and does not change normal `mvn test`, `mvn package`, or `java -jar` behavior.

For a local-only packaged-jar, prod API-key boundary, and proxy-loopback smoke path, see [`DEPLOYMENT_SMOKE_KIT.md`](DEPLOYMENT_SMOKE_KIT.md). For local-only Docker build/run recipes and container-specific safety boundaries, see [`CONTAINER_DEPLOYMENT.md`](CONTAINER_DEPLOYMENT.md). For the release-free production package and status-page helper, see [`OPERATOR_DISTRIBUTION_SMOKE_KIT.md`](OPERATOR_DISTRIBUTION_SMOKE_KIT.md). For local SHA-256, manifest, `jar tf`, and production-boundary inspection, see [`LOCAL_ARTIFACT_VERIFICATION.md`](LOCAL_ARTIFACT_VERIFICATION.md). CI also uploads the same packaged-jar inspection output as the `packaged-artifact-smoke` workflow artifact; [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md) explains how reviewers download that artifact, compare checksum evidence, and inspect the JaCoCo/SBOM workflow artifacts without publishing a release.

When preparing a release-free go/no-go operator packet, use [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md) to cite these packaging commands alongside CI artifacts and demo evidence.

For a side-by-side Windows/Unix install/run matrix covering packaged jar, Maven exec, Spring profiles, proxy demos, status pages, CI artifacts, local verification, and smoke helpers, see [`OPERATOR_INSTALL_RUN_MATRIX.md`](OPERATOR_INSTALL_RUN_MATRIX.md). Before any future release process is intentionally invoked, complete [`RELEASE_INTENT_CHECKLIST.md`](RELEASE_INTENT_CHECKLIST.md).

## Artifact Profiles

The default build produces the deployable proxy artifact. It contains the proxy runtime, health/status APIs, security filters, production configuration, and `proxy-status.html`. It excludes lab, CLI, demo, GUI, Decision Explorer, replay/evidence-training services, `ServerMonitor`, and lab-only AWS, Reactor, and Gson dependencies.

```bash
mvn -B -DskipTests package
```

Build the opt-in Lab Tools artifact when simulation, evidence, demo, or enterprise-lab commands are required:

```bash
mvn -B -P lab -DskipTests package
```

The Lab Tools artifact is named with a `-lab` suffix and starts `com.richmond423.loadbalancerpro.cli.LabToolsApplication`. It is not the release or container artifact.

## Packaged Proxy Jar

Run the default production-boundary artifact:

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
java -jar "$JAR_PATH" --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=local
```

Run a proxy demo profile from the Lab Tools artifact after the fixture launcher is running:

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh --lab)"
java -jar "$JAR_PATH" --spring.profiles.active=proxy-demo-round-robin
```

The resolver uses Maven's effective `project.build.finalName`, requires that exact jar, and does not select stale artifacts by filename or modification time. Without `--lab` (or PowerShell `-Lab`) it resolves the production artifact whose main class is `com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication`. Use Maven exec, the Lab Tools artifact, or the classpath fallback for demo tooling.

## Real-Backend Examples

Example files for local real backends live under `docs/examples/proxy`:

```text
docs/examples/proxy/application-proxy-real-backend-example.properties
docs/examples/proxy/application-proxy-real-backend-round-robin-example.properties
docs/examples/proxy/application-proxy-real-backend-weighted-example.properties
docs/examples/proxy/application-proxy-real-backend-failover-example.properties
docs/examples/proxy/application-proxy-real-backend-resilience-example.properties
```

They are copy/adapt examples, not active default profiles. Each file uses loopback backend placeholders such as `http://localhost:9001` and `http://localhost:9002`, enables proxy mode only inside the example, and includes health-check, retry, and cooldown settings that operators can adjust for their local services.

Use [`REAL_BACKEND_PROXY_EXAMPLES.md`](REAL_BACKEND_PROXY_EXAMPLES.md) for the full round-robin, weighted, health-aware failover, retry/cooldown, header verification, status-page verification, and release-free evidence checklist.

One local run pattern is:

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
java -jar "$JAR_PATH" --spring.config.import=optional:file:docs/examples/proxy/application-proxy-real-backend-example.properties
```

Then verify:

```bash
curl -i http://127.0.0.1:8080/proxy/health
curl -s http://127.0.0.1:8080/api/proxy/status
```

The examples avoid public upstream URLs, secrets, cloud settings, generated runtime files, and backend reset controls.

## JavaFX Desktop UI Retired

The JavaFX desktop simulator and its Maven dependency are removed. The proxy runtime remains in the default artifact; offline report CLIs, the Java fixture launcher, and simulation browser pages remain available from source or the Lab Tools artifact and do not require JavaFX.

See [`JAVAFX_OPTIONAL_UI.md`](JAVAFX_OPTIONAL_UI.md) for the retirement record, the retained JavaFX-free compatibility contract, and the maintained API/proxy/static browser operator paths.

## Naming Note

The active Java package root is `com.richmond423.loadbalancerpro` while the repository is `RicheyWorks/LoadBalancerPro`. See [`PACKAGE_NAMING.md`](PACKAGE_NAMING.md) for the stable legacy namespace decision and why a package rename is deferred.

## Boundaries

- No default proxy enablement.
- No tag, release, or asset creation.
- No generated jar or checksum commits.
- No package rename.
- No cloud services or cloud mutation.
- No `CloudManager` construction from the launcher or example profiles.
- No public internet requirement for tests or demo fixtures.
- No Docker, Python, or Node requirement for the Java launcher path.
- No backend write/reset controls.
- No persistence for demo state.
- No production gateway, benchmark, certification, legal, identity, or security guarantee.
