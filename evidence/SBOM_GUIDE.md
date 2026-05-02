# LoadBalancerPro Manual SBOM Guide

Date: 2026-05-01  
Branch: `codex/cyclonedx-sbom-audit`  
Verification command: `mvn -q test`

## Purpose and Scope

This guide documents a manually invoked CycloneDX Software Bill of Materials (SBOM) generation path for LoadBalancerPro.

It is documentation only. It does not change production code, `pom.xml`, dependencies, Maven plugins, CI workflows, Docker behavior, or release gates. It does not commit generated SBOM files.

This guide is intended to improve repeatable dependency inventory evidence while keeping supply-chain claims conservative.

## What an SBOM Proves

An SBOM generated from this command can provide an inventory of Maven-resolved project components at the time the command is run.

It can help reviewers see:

- Direct and transitive Maven dependencies resolved for the project.
- Component names, versions, package URLs, and related metadata included by the CycloneDX Maven plugin.
- A point-in-time dependency inventory that can be attached to a release review or local audit.

## What an SBOM Does Not Prove

An SBOM is not a vulnerability scan, security certification, provenance guarantee, or deployment attestation.

It does not prove:

- Dependencies are free of vulnerabilities or malicious code.
- Transitive dependencies have been reviewed by humans.
- Runtime container OS packages are fully represented by the Maven SBOM.
- Docker base images are immutable or digest-pinned.
- GitHub Actions are commit-SHA pinned.
- The Maven repository, CI runner, developer workstation, or artifact registry is trusted.
- TLS, IAM, firewalling, rate limiting, egress controls, or secret rotation are correctly deployed.

SBOM inventory should complement GitHub dependency review, Trivy image scanning, future dependency-check tooling, and human dependency triage. It does not replace them.

## Manual CycloneDX Command

Windows Command Prompt:

```cmd
mvn -q org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom ^
  -DoutputFormat=all ^
  -DoutputDirectory=target ^
  -DoutputName=bom ^
  -Dcyclonedx.skipAttach=true
```

Unix/macOS shell:

```sh
mvn -q org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom \
  -DoutputFormat=all \
  -DoutputDirectory=target \
  -DoutputName=bom \
  -Dcyclonedx.skipAttach=true
```

This uses a fully qualified, pinned CycloneDX Maven plugin invocation rather than adding the plugin to `pom.xml`.

## Expected Outputs

The command writes generated SBOM files under `target/`:

- `target/bom.json`
- `target/bom.xml`

These files are generated artifacts. They should be reviewed or attached to release evidence when needed, but they are not source files.

## Why Generated SBOM Files Are Not Committed Yet

`target/` is ignored by `.gitignore`, so generated SBOM files are not committed by default.

Phase 6B intentionally does not commit generated SBOM files because:

- Generated inventory can become stale whenever dependencies, plugins, Maven resolution, or build inputs change.
- Committed SBOM files can imply stronger continuous evidence than the project currently provides.
- The project has not yet defined a release artifact retention policy for generated SBOMs.
- The project has not yet added SBOM generation to CI or release gates.

Future release processes may choose to publish SBOM files as versioned release artifacts instead of committing them to the source tree.

## Network and Build Considerations

The first manual run may need network access so Maven can resolve the CycloneDX plugin and any missing dependencies.

This command:

- Does not change production code.
- Does not add a Maven plugin to `pom.xml`.
- Does not change CI.
- Does not run OWASP dependency-check.
- Does not perform vulnerability analysis.
- Does not contact a vulnerability database by itself.

Generated output may vary when dependency versions, transitive dependencies, Maven repositories, plugin versions, or build inputs change.

## Runtime vs Test Dependency Scope Note

The recommended command uses the CycloneDX Maven plugin defaults. The plugin's default `includeTestScope` behavior is `false`, which is appropriate for a runtime-oriented dependency inventory.

Test libraries still matter to evidence quality and supply-chain review, but runtime SBOM generation and test-scope inventory are separate concerns. If a future audit needs test dependency inventory, document that scope explicitly before changing the command.

## Future Hardening Options

Conservative next steps:

- Capture generated SBOMs as release artifacts outside the source tree.
- Add a documented dependency update cadence and accepted dependency-risk triage process.
- Add a pinned CycloneDX plugin configuration to `pom.xml` only after manual generation proves useful.
- Add CI SBOM generation after deciding retention, review, and failure behavior.
- Consider Docker image SBOM generation separately from Maven dependency SBOMs.
- Consider OWASP dependency-check after a triage and false-positive handling process exists.
- Consider Docker base image digest pinning.
- Consider GitHub Actions commit-SHA pinning where maintainability tradeoffs are acceptable.
