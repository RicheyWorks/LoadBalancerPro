# Remediation Report CLI

LoadBalancerPro can generate deterministic remediation reports from saved API JSON without starting the API server:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --remediation-report \
  --input saved-evaluation.json \
  --format markdown \
  --output incident-report.md \
  --manifest incident-report.manifest.json \
  --report-id incident-123
```

The same packaged JAR can write structured JSON for automation:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --remediation-report \
  --input saved-replay.json \
  --format json \
  --output incident-report.json
```

It can also create a portable offline incident bundle:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --input saved-evaluation.json \
  --format markdown \
  --bundle incident-bundle.zip \
  --report-id incident-123
```

## Inputs

The CLI accepts:

- a raw `POST /api/allocate/evaluate` response;
- a raw `POST /api/scenarios/replay` response;
- a report request wrapper with either `evaluation` or `replay`.
- a saved remediation report response or structured report payload.

Save evaluation JSON:

```bash
curl -fsS -X POST http://127.0.0.1:8080/api/allocate/evaluate \
  -H "Content-Type: application/json" \
  -d @evaluation-request.json \
  > saved-evaluation.json
```

Save replay JSON:

```bash
curl -fsS -X POST http://127.0.0.1:8080/api/scenarios/replay \
  -H "Content-Type: application/json" \
  -d @scenario-replay.json \
  > saved-replay.json
```

## Options

| Option | Purpose |
| --- | --- |
| `--remediation-report` | Enables offline report mode. |
| `--remediation-report=<path>` | Shorthand for offline report mode with an input file. |
| `--input <path>` | Saved evaluation, replay, or report-request JSON. |
| `--format markdown\|json` | Output format. Defaults to `markdown`. |
| `--output <path>` | Optional output file. If omitted, output is written to stdout. |
| `--report-id <id>` | Optional deterministic report id. |
| `--title <title>` | Optional Markdown report title. |
| `--manifest <path>` | Optional checksum manifest JSON for the generated report bundle. Requires `--output`. |
| `--manifest-extra <path>` | Optional extra file to include in the checksum manifest. May be repeated. |
| `--generated-by <text>` | Optional manifest metadata. Defaults to `LoadBalancerPro offline remediation report CLI`. |
| `--created-at <text>` | Optional caller-supplied manifest timestamp or incident time. Omitted by default for deterministic output. |
| `--verify-manifest <path>` | Offline verification mode for an existing checksum manifest. |
| `--bundle <path>` | Writes an incident ZIP bundle with saved input, generated report, checksum manifest, verification summary, and README. |
| `--verify-bundle <path>` | Offline verification mode for an incident ZIP bundle. |

## Output Semantics

Markdown output is intended for humans and incident tickets. JSON output is the structured report payload for automation. Both formats summarize:

- accepted/allocated load;
- rejected/unallocated load;
- scaling recommendation;
- load-shedding decision;
- ranked remediation actions;
- read-only, advisory-only, and cloud-mutation guarantees;
- warnings and limitations.

The CLI reuses `RemediationReportService`, so output semantics match `POST /api/remediation/report`.

## Checksum Manifests

When `--manifest` is supplied, the CLI writes a deterministic JSON manifest with SHA-256 checksums for:

- the saved input JSON;
- the generated Markdown or JSON report file;
- any `--manifest-extra` files.

Example:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --remediation-report \
  --input saved-replay.json \
  --format json \
  --output incident-report.json \
  --manifest incident-report.manifest.json \
  --report-id incident-123
```

Verify the bundle later without starting the API server:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --verify-manifest incident-report.manifest.json
```

Verification exits `0` when every listed file exists and matches its recorded SHA-256 digest. It exits non-zero when the manifest is invalid, a file is missing, or a digest does not match.

The manifest is checksum-based tamper evidence. It can detect accidental corruption, missing files, and edits after the manifest was created. It is not a cryptographic signature, does not prove who generated the report, does not use private keys, and does not replace release artifact attestations or operator review.

Default manifest output omits timestamps and random identifiers. `reportId`, `generatedBy`, source type, app version, safety flags, and file roles are deterministic. `createdAt` is included only when the caller supplies `--created-at`.

## Incident Bundles

`--bundle` creates a deterministic, offline ZIP for ticket attachment and later verification. The bundle contains:

- `input.json`: the saved evaluation, replay, or report request JSON.
- `report.md` or `report.json`: the generated remediation report.
- `manifest.json`: SHA-256 checksums for bundle files.
- `verification-summary.json`: deterministic safety and verification summary.
- `README.md`: bundle contents and checksum-manifest caveats.

Example:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --input saved-replay.json \
  --format json \
  --bundle incident-bundle.zip \
  --report-id incident-123
```

Verify the ZIP later without extracting files manually:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --verify-bundle incident-bundle.zip
```

Bundle verification opens the ZIP safely, rejects unsafe entry names, requires `manifest.json`, confirms every manifest-listed file exists, and recomputes each SHA-256 digest. It exits `0` when verification passes and non-zero when the bundle is invalid, missing files, or contains changed content.

Bundles use stable entry names, stable entry ordering, and normalized ZIP entry timestamps. With the same input and same options, output is intended to be byte-stable. The guarantee remains checksum-based content integrity, not identity proof.

## Safety

Offline report generation:

- does not start Spring Boot or the API server;
- does not contact networks or external services;
- does not construct or mutate `CloudManager`;
- does not call AWS;
- does not execute remediation actions;
- can write and verify incident ZIP bundles entirely offline;
- does not generate timestamps or random ids unless the caller supplies `--report-id` or manifest `--created-at`;
- generates and verifies checksum manifests locally with Java SHA-256, not external tools or signing keys.

## Limitations

- The CLI formats saved results. It does not recompute allocation, replay, routing, health checks, or remediation.
- Checksum manifests are useful for bundle integrity, not identity proof or non-repudiation.
- Incident bundles are portable evidence containers, not signed attestations.
- Live deployment state should still be verified before taking operator action.
- Invalid JSON or unsupported input shapes exit non-zero and print a safe error without stack traces.
