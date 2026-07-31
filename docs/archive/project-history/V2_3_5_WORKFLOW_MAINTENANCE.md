# LoadBalancerPro v2.3.5 Workflow Maintenance

## Scope

`v2.3.5` is a focused GitHub Actions Node.js 24 workflow-maintenance release.

- Updated `actions/checkout` from `4.3.1` to `6.0.2`.
- Updated `actions/setup-java` from `4.8.0` to `5.2.0`.
- Updated `actions/upload-artifact` from `4.6.2` to `7.0.1`.
- No application behavior changes are intended.
- No Docker, Java runtime, JavaFX, Spring Boot, dependency, namespace migration, or other updates are included.

## Workflow Compatibility

- Java version remains `17`.
- Java distribution remains `temurin`.
- Maven cache behavior remains unchanged.
- Release artifact names, upload paths, and retention days remain unchanged.
- `actions/upload-artifact@v7` affects the release artifact upload path, so tag-triggered Release Artifacts verification is required after tagging.

## Verification

Release-prep verification for the `v2.3.5` metadata alignment should pass before tagging:

- `mvn -q test`
- `mvn -q -DskipTests package`
- `java -jar target/LoadBalancerPro-2.3.5.jar --version`
- `java -jar target/LoadBalancerPro-2.3.5.jar --lase-demo=healthy`
- `java -jar target/LoadBalancerPro-2.3.5.jar --lase-demo=overloaded`
- `java -jar target/LoadBalancerPro-2.3.5.jar --lase-demo=invalid-name`
- CycloneDX SBOM generation with `org.cyclonedx:cyclonedx-maven-plugin:2.9.1`
- `git diff --check`

The `invalid-name` LASE smoke check should exit `2` as expected and print valid scenario guidance.

## Safety Notes

- `public/main` remains untouched.
- Namespace migration has not started.
- Existing release tags remain immutable.
