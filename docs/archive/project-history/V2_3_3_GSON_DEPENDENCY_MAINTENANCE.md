# LoadBalancerPro v2.3.3 Gson Dependency Maintenance

## Scope

`v2.3.3` is a focused dependency-maintenance release for Gson.

- Updated `com.google.code.gson:gson` from `2.10.1` to `2.14.0`.
- No behavior changes are intended.
- No Docker, Java runtime, JavaFX, Spring Boot, Caffeine, GitHub Actions, or namespace migration changes are included.
- Existing release tags remain immutable.

## Verification

Release-prep verification for the `v2.3.3` metadata alignment passed before tagging:

- `mvn -q test`
- `mvn -q -DskipTests package`
- `java -jar target/LoadBalancerPro-2.3.3.jar --version`
- `java -jar target/LoadBalancerPro-2.3.3.jar --lase-demo=healthy`
- `java -jar target/LoadBalancerPro-2.3.3.jar --lase-demo=overloaded`
- `java -jar target/LoadBalancerPro-2.3.3.jar --lase-demo=invalid-name`
- CycloneDX SBOM generation with `org.cyclonedx:cyclonedx-maven-plugin:2.9.1`
- `git diff --check`

The `invalid-name` LASE smoke check exited `2` as expected and printed valid scenario guidance.

## Release Metadata Alignment

Before tagging `v2.3.3`, active Maven, application, README, API fallback, CLI, telemetry, and version-sensitive test metadata should report `2.3.3` so the tag-triggered Release Artifacts workflow can verify tag and Maven version alignment.
