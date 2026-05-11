# Operator Packaging

This page collects the shortest operator commands for local API/proxy review, the Java fixture launcher, real-backend examples, and packaging caveats. Default application behavior remains unchanged: `src/main/resources/application.properties` keeps `loadbalancerpro.proxy.enabled=false`.

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

For a release-free packaged-jar and status-page smoke path that wraps these checks, see [`OPERATOR_DISTRIBUTION_SMOKE_KIT.md`](OPERATOR_DISTRIBUTION_SMOKE_KIT.md). For local SHA-256, manifest, `jar tf`, static page, demo profile, and fixture launcher class inspection, see [`LOCAL_ARTIFACT_VERIFICATION.md`](LOCAL_ARTIFACT_VERIFICATION.md). CI also uploads the same packaged-jar inspection output as the `packaged-artifact-smoke` workflow artifact; [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md) explains how reviewers download that artifact, compare checksum evidence, and inspect the JaCoCo/SBOM workflow artifacts without publishing a release.

When preparing a release-free go/no-go operator packet, use [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md) to cite these packaging commands alongside CI artifacts and demo evidence.

## Packaged API Jar

Build the normal Spring Boot application jar:

```bash
mvn -B -DskipTests package
```

Run the API with the packaged jar:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=8080
```

Run the proxy demo profile with the packaged jar after the fixture launcher is running:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --spring.profiles.active=proxy-demo-round-robin
```

The executable jar main class remains `com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication`. Use Maven exec or the classpath fallback for `com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher`.

## Real-Backend Examples

Example files for local real backends live under `docs/examples/proxy`:

```text
docs/examples/proxy/application-proxy-real-backend-example.properties
docs/examples/proxy/application-proxy-real-backend-weighted-example.properties
docs/examples/proxy/application-proxy-real-backend-failover-example.properties
```

They are copy/adapt examples, not active default profiles. Each file uses loopback backend placeholders such as `http://localhost:9001` and `http://localhost:9002`, enables proxy mode only inside the example, and includes health-check, retry, and cooldown settings that operators can adjust for their local services.

One local run pattern is:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --spring.config.import=optional:file:docs/examples/proxy/application-proxy-real-backend-example.properties
```

Then verify:

```bash
curl -i http://127.0.0.1:8080/proxy/health
curl -s http://127.0.0.1:8080/api/proxy/status
```

The examples avoid public upstream URLs, secrets, cloud settings, generated runtime files, and backend reset controls.

## JavaFX Optional

JavaFX UI support exists in the project, but the API, proxy, CLI, Java fixture launcher, static browser pages, and Maven exec recipes do not require JavaFX. Treat JavaFX as optional desktop UI support with platform-sensitive deployment considerations, not as a required operator path for the Spring API or reverse proxy demos.

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
