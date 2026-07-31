# JavaFX Desktop UI Retirement

LoadBalancerPro no longer includes or distributes the JavaFX desktop simulator. The unsupported
`LoadBalancerGUI`, its JavaFX table model, desktop-only configuration and command helpers, message bundle, and
`org.openjfx:javafx-controls` Maven dependency were removed in campaign slot L-4.2.

## Supported Operator Paths

Use the maintained local reviewer/operator surfaces:

- start the Spring API from the packaged JAR or `mvn spring-boot:run`;
- enable the optional `/proxy/**` reverse proxy only with explicit bounded configuration;
- use `http://localhost:8080/proxy-status.html` for proxy status, health, counters, and raw status JSON;
- use `http://localhost:8080/load-balancing-cockpit.html` for the static browser cockpit;
- follow [`OPERATOR_INSTALL_RUN_MATRIX.md`](OPERATOR_INSTALL_RUN_MATRIX.md) for Windows and Unix run commands;
- follow [`PROXY_DEMO_STACK.md`](PROXY_DEMO_STACK.md) and
  [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md) for local no-cloud proxy demos; and
- use [`LOCAL_ARTIFACT_VERIFICATION.md`](LOCAL_ARTIFACT_VERIFICATION.md),
  [`OPERATOR_DISTRIBUTION_SMOKE_KIT.md`](OPERATOR_DISTRIBUTION_SMOKE_KIT.md), and
  [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md) for release-free artifact evidence.

None of these paths requires JavaFX, a desktop display, a JavaFX launcher, or platform-specific JavaFX runtime
configuration.

## Current Build Reality

- `pom.xml` has no `javafx.version`, `org.openjfx`, or `javafx-controls` declaration.
- Production source has no JavaFX imports or JavaFX `Application` entry point.
- The packaged Spring Boot JAR main class remains
  `com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication`.
- The Java fixture launcher remains
  `com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher`.
- CI continues to reject OpenJFX libraries in the server JAR as a regression guard.
- `com.richmond423.loadbalancerpro.gui.Command` remains only as a JavaFX-free compatibility contract used by
  `CloudManager`; moving that public type is outside this deletion slot.

## Migration Note

There is no supported JavaFX launch command. Do not restore the retired interactive CLI menu or construct
`CloudManager` merely to recreate the desktop simulator. Use the API, proxy status page, browser cockpit, or local
fixture launcher instead.

Historical release and audit records may still mention the former JavaFX source or version because they describe
the repository at an earlier exact commit. They are not current launch guidance.

## Safety And Evidence Boundaries

Removing the unsupported desktop simulator does not change the API, proxy, cloud guardrails, authentication,
configuration defaults, Docker behavior, or HTML cockpit behavior. It also does not prove production readiness,
production certification, live-cloud validation, real-tenant validation, runtime enforcement, performance, legal
compliance, identity, or security posture.
