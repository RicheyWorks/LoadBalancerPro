# Antivirus-Safe Development

LoadBalancerPro development should stay boring to endpoint protection tools. The default project outputs are source, tests, docs, scripts, container recipes, workflow configuration, and Spring Boot JARs produced by Maven.

This policy does not change production runtime behavior. It sets the default development boundary for future tooling, packaging, live load balancer work, and proxy validation.

## Safe Default Artifacts

Prefer these artifact types for this project:

- Java source.
- Maven tests.
- Spring Boot JARs built by Maven.
- PowerShell scripts checked in as readable source.
- Postman JSON collections and environments with placeholders only.
- Markdown docs.
- Dockerfile and Docker documentation.
- GitHub Actions workflow definitions.

These formats are inspectable in the repository and fit the existing CI, smoke, and reviewer evidence model.

## Avoided Artifacts

Do not create, download, commit, or require these artifact types unless a future task explicitly approves them and documents why they are needed:

- `.exe` files.
- `native-image` outputs.
- `launch4j` wrappers.
- `jpackage` installers.
- Installers.
- Packers.
- Self-extracting archives.
- Vendored third-party binaries.

Do not vendor third-party binary tools into the repository. If a future workflow needs a tool, prefer Maven-managed Java dependencies, readable scripts, or documented operator prerequisites instead of checked-in binaries.

## Why False Positives Happen

Antivirus and endpoint protection tools often score development artifacts by behavior and packaging signals, not only by known malware signatures. Small unsigned binaries, packed files, self-extracting archives, native wrappers, tools that spawn child processes, and files copied from another machine or removable drive can look risky even when the project intent is benign.

False positives are still operational incidents. Treat them as a signal to slow down, identify provenance, and avoid normalizing unknown executables in the repo.

## Safe Response Policy

- Quarantine unknown detections.
- Do not whitelist unknown files.
- Do not restore unknown quarantined files.
- Verify with a second-opinion scan when needed.
- Record the file path, source, hash when available, and why it was expected before considering any exception.
- Prefer rebuilding safe artifacts from source over restoring quarantined local outputs.

If the source or purpose of a detected file is unclear, leave it quarantined and continue with source-only or Maven-built alternatives.

## Repo Hygiene

- Do not copy borrowed-drive artifacts into the repository.
- Do not commit generated binaries, generated archives, local smoke evidence, or local checksum outputs.
- Do not commit secrets, account identifiers that should remain private, tokens, cookies, API keys, private keys, or local credential files.
- Do not mutate `release-downloads/` unless a separate explicit release evidence task approves that path.
- Keep local logs, reports, packaged outputs, and transient evidence under ignored local paths such as `target/` unless a reviewed doc-only summary is requested.

## Future Live Load Balancer And Proxy Work

Live load balancer and proxy work should keep validation deterministic and inspectable. Use Java tests, Maven builds, Spring Boot JARs, PowerShell source scripts, Postman JSON, Docker docs, and GitHub Actions checks before considering any new local tooling shape.

Future proxy or real-backend validation should avoid native helper binaries, downloaded scanners, wrapper executables, installers, packers, and self-extracting archives. Prefer configured localhost or private-network backends, explicit profiles, documented inputs, and repeatable low-risk smoke paths.

Any proposal that needs an avoided artifact type must be separated from normal feature work, reviewed as a tooling exception, and documented before implementation.
