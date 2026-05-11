# JavaFX Optional UI

LoadBalancerPro includes JavaFX desktop UI code, but JavaFX is optional. It is not required for the Spring API, reverse proxy mode, static browser pages, proxy demos, CI artifact verification, or release-free operator review paths.

## Recommended Operator Path

Use these paths first for reviewer and operator work:

- Spring API through the packaged jar or `mvn spring-boot:run`.
- Optional `/proxy/**` reverse proxy mode when explicitly enabled.
- `http://localhost:8080/proxy-status.html` for proxy health, counters, retry/cooldown state, and raw status JSON.
- `http://localhost:8080/load-balancing-cockpit.html` for the static browser cockpit.
- `docs/OPERATOR_INSTALL_RUN_MATRIX.md` for Windows and Unix install/run commands.
- `docs/PROXY_DEMO_STACK.md` and `docs/PROXY_DEMO_FIXTURE_LAUNCHER.md` for local no-cloud proxy demos.
- `docs/LOCAL_ARTIFACT_VERIFICATION.md`, `docs/OPERATOR_DISTRIBUTION_SMOKE_KIT.md`, and `docs/CI_ARTIFACT_CONSUMER_GUIDE.md` for release-free artifact evidence.

These paths do not require a desktop display, JavaFX launcher setup, or platform-specific JavaFX runtime configuration.

## What The Desktop UI Is For

The JavaFX UI is a desktop-oriented simulator surface for local server/load-balancer interaction. The source lives under `src/main/java/com/richmond423/loadbalancerpro/gui`, and `LoadBalancerGUI` extends JavaFX `Application`.

Use it when you specifically want to explore the desktop simulator experience. Do not treat it as the required way to run the API, proxy, fixture launcher, CI artifact checks, or operator review packet.

## Current Build Reality

- The Maven project declares `org.openjfx:javafx-controls` using the `javafx.version` property in `pom.xml`.
- The packaged Spring Boot jar main class remains `com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication`.
- The Java fixture launcher remains `com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher`.
- The CLI has an interactive `Launch GUI` menu option that calls `LoadBalancerGUI.launch(...)`.
- No JavaFX-specific Maven plugin is bound to the default lifecycle.

## Launch Guidance

For normal API/proxy work, do not launch JavaFX. Start the API or proxy using the operator docs instead:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=8080
```

If you intentionally want to try the desktop UI, use the CLI entry point and choose the interactive `Launch GUI` option:

```bash
mvn -q exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.cli.LoadBalancerCLI"
```

This requires a JavaFX-capable runtime environment and a desktop display. If the local runtime cannot launch JavaFX, use the API/proxy/static browser paths instead.

## Platform Caveats

- JavaFX desktop launch behavior can vary by operating system, JDK distribution, graphics stack, and module/classpath setup.
- Headless CI, containers, SSH sessions, and servers without a desktop display may not support the JavaFX UI.
- A packaged Spring Boot API jar starting successfully does not prove that a desktop JavaFX session can open on every workstation.
- JavaFX dependency upgrades should be treated as GUI/runtime compatibility work and verified separately from API/proxy checks.

## Troubleshooting

- If a JavaFX class or module is missing, confirm Maven dependency resolution succeeded and the runtime can load JavaFX controls.
- If the window does not open in CI, a container, or an SSH session, retry on a machine with a desktop display.
- If JavaFX launch fails but API/proxy work is the goal, continue with `java -jar`, `mvn spring-boot:run`, `/proxy-status.html`, and `/load-balancing-cockpit.html`.
- If cloud-related GUI controls are explored, keep cloud live-mutation guardrails disabled unless a separate cloud-safety task explicitly approves otherwise.

## Limits

- JavaFX is not required for the Spring API.
- JavaFX is not required for reverse proxy mode.
- JavaFX is not required for `/proxy-status.html`.
- JavaFX is not required for `/load-balancing-cockpit.html`.
- JavaFX is not required for CI artifact verification.
- JavaFX is not required for the operator distribution smoke kit.
- JavaFX is not required for the proxy demo stack.
- JavaFX documentation does not change runtime behavior, default proxy state, packaging, release workflows, package names, or cloud behavior.
- JavaFX is local desktop UI support only, not evidence of production support, benchmark results, certification, legal compliance, identity, or security posture.
