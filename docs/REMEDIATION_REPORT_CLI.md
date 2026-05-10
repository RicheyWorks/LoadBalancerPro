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
  --report-id incident-123 \
  --redact internal-host-01 \
  --redact-file incident-redactions.txt
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
| `--redact <literal>` | Literal value to replace in saved input evidence and generated report output. May be repeated. |
| `--redact-file <path>` | Newline-delimited literal values or a JSON string array of values to redact. May be repeated. |
| `--redaction-label <label>` | Replacement label. Defaults to `[REDACTED]`. |
| `--redact-output-summary <path>` | Optional redaction summary JSON for standalone report output. Bundles include `redaction-summary.json` automatically when redaction is used. |
| `--audit-log <path>` | Appends a local checksum-chained audit entry after a successful offline CLI action. |
| `--verify-audit-log <path>` | Verifies a local offline CLI audit log checksum chain. |
| `--audit-actor <label>` | Optional operator-supplied actor label for audit entries. |
| `--audit-action-id <id>` | Optional deterministic action or ticket id for audit entries. |
| `--audit-note <text>` | Optional short operator note for audit entries. |
| `--inventory <directory>` | Indexes local incident bundles, manifests, audit logs, redaction summaries, reports, and inputs. |
| `--inventory-format markdown\|json` | Evidence inventory output format. Defaults to `markdown`. |
| `--inventory-output <path>` | Optional evidence catalog output file. If omitted, output is written to stdout. |
| `--verify-inventory` | Verifies bundles, manifests, and audit logs while inventorying. |
| `--include-hashes` | Includes SHA-256 checksums for inventoried files. |
| `--fail-on-invalid` | Exits non-zero when verification finds missing, tampered, or malformed evidence. |

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

When redaction is used with standalone `--manifest`, the CLI writes a companion redacted input file next to the report, named from the report output such as `incident-report.input.redacted.json`. The manifest hashes that redacted input file and the redacted report file, so later verification checks the evidence that was actually shared.

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

When redaction is used with `--bundle`, the ZIP includes redacted `input.json`, redacted `report.md` or `report.json`, and `redaction-summary.json`. The manifest hashes the redacted files, so `--verify-bundle` confirms the redacted bundle content has not changed after export.

## Redaction

Redaction is deterministic literal string replacement. It is designed for operator-controlled removal of known sensitive strings before attaching evidence to a ticket:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --input saved-replay.json \
  --format markdown \
  --bundle incident-bundle.zip \
  --redact internal-host-01 \
  --redact server-blue-prod \
  --redact-file redactions.txt \
  --redaction-label "[REDACTED]"
```

`redactions.txt` is newline-delimited:

```text
internal-host-01
server-blue-prod
operator-note-42
```

The same option can read a JSON string array:

```json
["internal-host-01", "server-blue-prod", "operator-note-42"]
```

Redaction applies before report, manifest, and bundle hashing. It covers:

- the saved input copy inside a bundle;
- the generated Markdown or JSON report;
- manifest metadata that would otherwise echo redacted report fields;
- redaction summary output when requested.

The redaction summary records:

- summary version;
- replacement label;
- configured token count;
- total replacement count;
- SHA-256 digest of each configured redaction token;
- replacement counts by token digest and file name.

It intentionally does not include the original sensitive values. Token hashes are included so operators can compare summaries without storing the raw values in the incident evidence.

Redaction limitations:

- It is literal replacement, not legal anonymization or a privacy compliance guarantee.
- It only removes exact strings supplied by the operator.
- It does not infer every possible hostname, IP address, identity, or secret shape.
- Checksum manifests prove files did not change after redaction; they do not prove the original data was safe, complete, or fully redacted.

## Local Audit Log

`--audit-log` appends a JSON Lines audit entry after successful offline CLI actions:

- Markdown or JSON report generation;
- checksum manifest generation;
- checksum manifest verification;
- incident bundle generation;
- incident bundle verification;
- redacted report or bundle generation.

Example:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --input saved-replay.json \
  --format markdown \
  --bundle incident-bundle.zip \
  --redact-file redactions.txt \
  --audit-log incident-audit.jsonl \
  --audit-action-id incident-123 \
  --audit-actor operator-a \
  --audit-note "redacted bundle for ticket attachment"
```

Verify the audit log later:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --verify-audit-log incident-audit.jsonl
```

Each audit entry includes:

- schema version;
- monotonic sequence number;
- action name;
- optional action id, actor, and note;
- sanitized path fields where possible;
- SHA-256 file hashes for relevant input, report, manifest, or bundle files;
- `previousEntryHash`;
- `entryHash`;
- `advisoryOnly=true`;
- `cloudMutation=false`;
- `redactionApplied` when the action generated redacted output.

`entryHash` is SHA-256 over canonical entry content excluding `entryHash` itself. `previousEntryHash` links each entry to the prior entry, with the first entry using an all-zero previous hash. Verification recomputes each entry hash and link, and exits non-zero for malformed JSON lines, changed entry content, deleted middle entries, reordered entries, sequence gaps, or bad previous-entry hashes.

This is local checksum chaining for tamper evidence. It is not cryptographic signing, does not use private keys, does not prove operator identity, does not provide non-repudiation, and cannot by itself prove that a tail entry was not truncated unless the latest hash or entry count was saved elsewhere.

## Evidence Inventory

`--inventory` scans a local evidence directory and emits a deterministic Markdown or JSON catalog:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --inventory incident-evidence \
  --inventory-format json \
  --verify-inventory \
  --include-hashes \
  --fail-on-invalid \
  --inventory-output evidence-catalog.json
```

The catalog detects:

- incident bundle ZIPs;
- checksum manifest JSON files;
- offline CLI audit log JSONL files;
- redaction summaries;
- report files;
- saved input JSON files;
- verification summaries.

When `--verify-inventory` is present, bundle verification reuses `--verify-bundle` logic, manifest verification reuses `--verify-manifest` logic, and audit-log verification reuses `--verify-audit-log` logic. Loose report and input files can be hashed with `--include-hashes`, but they are not independently verified unless covered by a manifest or bundle.

Catalog summaries include bundle, manifest, audit-log, redaction-summary, and report counts; verification status; warning and failure counts; and the latest audit log entry hash/count when an audit log verifies. Inventory output is sorted by relative path and omits timestamps or random identifiers by default.

The evidence inventory is a local checksum catalog. It does not prove operator identity, provide legal chain-of-custody, or replace centralized evidence storage.

## Safety

Offline report generation:

- does not start Spring Boot or the API server;
- does not contact networks or external services;
- does not construct or mutate `CloudManager`;
- does not call AWS;
- does not execute remediation actions;
- can write and verify incident ZIP bundles entirely offline;
- does not generate timestamps or random ids unless the caller supplies `--report-id` or manifest `--created-at`;
- can redact configured literal values before manifest and bundle checksums are calculated;
- can append and verify local checksum-chained audit logs without an external service;
- can inventory local evidence directories without starting the API server;
- generates and verifies checksum manifests locally with Java SHA-256, not external tools or signing keys.

## Limitations

- The CLI formats saved results. It does not recompute allocation, replay, routing, health checks, or remediation.
- Checksum manifests are useful for bundle integrity, not identity proof or non-repudiation.
- Incident bundles are portable evidence containers, not signed attestations.
- Redaction is exact string replacement and should be reviewed by an operator before evidence is shared externally.
- Audit logs are local checksum chains, not centralized append-only storage, cryptographic signatures, identity proof, or non-repudiation.
- Evidence inventories are local checksum catalogs, not legal chain-of-custody records or identity proof.
- Live deployment state should still be verified before taking operator action.
- Invalid JSON or unsupported input shapes exit non-zero and print a safe error without stack traces.
