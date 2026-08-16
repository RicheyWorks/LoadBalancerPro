# Local Artifact Verification

This guide verifies the production-boundary LoadBalancerPro proxy jar without publishing a release. It covers local SHA-256 checksum evidence, Spring Boot jar inspection, required proxy resources, forbidden lab content, and local run commands.

This path is release-free: it creates no tags, no GitHub releases, no release assets, and no canonical release evidence. It does not modify release workflows, does not touch `release-downloads/`, and does not commit generated jars, checksums, manifests, or reports.

For a wider install/run matrix covering packaged jar, Maven exec, proxy profiles, status pages, CI artifacts, and smoke helpers, see [`OPERATOR_INSTALL_RUN_MATRIX.md`](OPERATOR_INSTALL_RUN_MATRIX.md). Before any future release process is intentionally invoked, use [`RELEASE_INTENT_CHECKLIST.md`](RELEASE_INTENT_CHECKLIST.md).

## Build The Jar

Build the normal Spring Boot executable jar:

```bash
mvn -B -DskipTests package
```

For full CI-equivalent verification, use:

```bash
mvn -B verify
```

Resolve the expected local artifact from Maven's effective `project.build.finalName` rather than copying a version
literal:

```powershell
$jarPath = .\scripts\resolve-executable-jar.ps1 -ExpectedOnly
```

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh --expected-only)"
```

If Maven dependency resolution fails on a workstation because the local Java trust store cannot validate Maven Central certificates, use GitHub CI as the source of truth for build and package validation.

## Checksum Evidence

Windows PowerShell:

```powershell
$jarPath = .\scripts\resolve-executable-jar.ps1
Get-FileHash -Algorithm SHA256 $jarPath
```

Unix with `sha256sum`:

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
sha256sum "$JAR_PATH"
```

Unix/macOS with `shasum`:

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
shasum -a 256 "$JAR_PATH"
```

These commands produce local integrity evidence for the file on disk. Do not commit the checksum as canonical release evidence unless a separate release process explicitly asks for it.

## Inspect The Jar

List jar entries:

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
jar tf "$JAR_PATH"
```

Verify the manifest and Spring Boot layout:

Windows PowerShell:

```powershell
$jarPath = .\scripts\resolve-executable-jar.ps1
jar tf $jarPath | Select-String "META-INF/MANIFEST.MF"
jar tf $jarPath | Select-String "BOOT-INF/classes/"
```

Unix shell:

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
jar tf "$JAR_PATH" | grep 'META-INF/MANIFEST.MF'
jar tf "$JAR_PATH" | grep 'BOOT-INF/classes/'
```

Verify required production entries:

```text
BOOT-INF/classes/static/proxy-status.html
BOOT-INF/classes/application-proxy-prod.properties
BOOT-INF/classes/com/richmond423/loadbalancerpro/api/LoadBalancerApiApplication.class
BOOT-INF/classes/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyService.class
BOOT-INF/classes/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyStatusController.class
```

The verification helpers also reject lab, CLI, demo, and GUI packages; Decision Explorer, replay, and evidence-training APIs; the synthetic `ServerMonitor`; simulation browser pages; and lab-only cloud/reactive/JSON libraries. The real-backend example property files under `docs/examples/proxy` remain source-tree copy/adapt examples and are not packaged.

## Helper Scripts

Windows PowerShell:

```powershell
.\scripts\local-artifact-verify.ps1
.\scripts\local-artifact-verify.ps1 -Build
.\scripts\local-artifact-verify.ps1 -JarPath PATH
```

Unix shell:

```bash
bash scripts/local-artifact-verify.sh
bash scripts/local-artifact-verify.sh --build
bash scripts/local-artifact-verify.sh --jar PATH
```

Without an explicit override, both helpers resolve exactly `target/${project.build.finalName}.jar` from the effective
Maven model and fail when that file is absent. They do not choose another version by lexical order or modification
time. The helpers optionally run `mvn -B -DskipTests package`, compute a SHA-256 checksum for the selected local jar,
inspect required jar entries, and print local run commands. They do not start long-running servers unless the operator
runs the printed commands separately.

## CI Packaged Artifact Smoke

The `Build, Test, Package, Smoke` GitHub Actions workflow also runs a release-free packaged artifact smoke lane after `mvn -B package`.

The workflow uploads a normal GitHub Actions artifact named:

```text
packaged-artifact-smoke
```

That workflow artifact contains:

```text
artifact-smoke-summary.txt
artifact-sha256.txt
jar-resource-list.txt
```

The CI summary verifies the same production entries and absence rules as the local helpers. Its required application entries are:

```text
BOOT-INF/classes/static/proxy-status.html
BOOT-INF/classes/application-proxy-prod.properties
BOOT-INF/classes/com/richmond423/loadbalancerpro/api/LoadBalancerApiApplication.class
BOOT-INF/classes/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyService.class
```

Use the artifact checksum as CI evidence for the jar built in that workflow run. It is not a GitHub Release checksum, is not uploaded as a release asset, and should not be copied into `release-downloads/` for this verification path.

For a reviewer-facing workflow artifact download and local-vs-CI SHA-256 comparison checklist, see [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md). That guide also covers the `jacoco-coverage-report` and `loadbalancerpro-sbom` workflow artifacts.

For a release-free go/no-go packet that records local verification alongside CI artifacts, SBOM files, packaged smoke evidence, proxy status UI review, and demo stack commands, see [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md).

## Local Run Commands

Start the packaged API jar on loopback:

```bash
JAR_PATH="$(bash scripts/resolve-executable-jar.sh)"
java -jar "$JAR_PATH" --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=local
```

Then verify:

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:8080/proxy-status.html
curl -fsS http://127.0.0.1:8080/api/proxy/status
```

Build and resolve the separate Lab Tools artifact when simulation or demo commands are intentionally required:

```bash
mvn -B -P lab -DskipTests package
LAB_JAR_PATH="$(bash scripts/resolve-executable-jar.sh --lab)"
java -jar "$LAB_JAR_PATH" --lase-demo=healthy
```

PowerShell uses the equivalent resolver switch:

```powershell
$labJarPath = .\scripts\resolve-executable-jar.ps1 -Lab
```

The default executable starts `com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication`; the `-P lab` executable starts `com.richmond423.loadbalancerpro.cli.LabToolsApplication`.

## Cleanup

- Stop local Java processes with `Ctrl+C`.
- Delete local `target/` output if you want a clean checkout.
- Do not stage or commit generated jars, checksum output, temporary manifests, or local smoke logs.
- Do not copy generated files into `release-downloads/` for this verification path.

## Troubleshooting

- If the version-derived executable jar is missing, run `mvn -B -DskipTests package`, then rerun the resolver.
- If `jar` is unavailable, confirm the JDK `bin` directory is on `PATH`.
- If `proxy-status.html` is missing from `jar tf`, rebuild and confirm the source file exists under `src/main/resources/static`.
- If a lab command is unavailable, package with `-P lab` and resolve the artifact with `--lab` or `-Lab`; do not add lab classes to the production jar.
- If `/api/proxy/status` reports proxy disabled during local startup, that is expected for default local runs.
- If Maven fails with a PKIX trust-chain error, use the GitHub CI package result as the source of truth until the workstation trust store is fixed.

## Safety Boundaries

- Release-free only.
- No tags, releases, or release assets.
- No release workflow changes.
- No `release-downloads/` modification.
- No generated jars or checksums committed.
- No default proxy enablement.
- No default application behavior changes.
- No cloud services.
- No cloud mutation.
- No `CloudManager` construction from artifact verification paths.
- No Docker, Python, Node, or heavy packaging framework requirement.
- No production gateway, benchmark, certification, legal, identity, or security guarantee.
