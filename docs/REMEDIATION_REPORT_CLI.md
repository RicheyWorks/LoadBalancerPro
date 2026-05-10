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

For local API or Postman onboarding, the Spring Boot API also exposes read-only evidence training discovery routes:

```text
GET  /api/evidence-training/onboarding
GET  /api/evidence-training/templates
GET  /api/evidence-training/examples
GET  /api/evidence-training/scorecards
GET  /api/evidence-training/scorecards/{name}
GET  /api/evidence-training/scorecards/{name}/answer-template
POST /api/evidence-training/scorecards/grade
```

Those routes help GUI-facing clients and Postman users find templates, examples, scorecards, and answer JSON templates. The API grade route returns deterministic JSON in memory and does not write Markdown/JSON report files. The offline CLI remains available and does not require the API server. See [`POSTMAN_EVIDENCE_TRAINING.md`](POSTMAN_EVIDENCE_TRAINING.md) for collection import steps.

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
| `--diff-inventory <before.json> <after.json>` | Compares two saved JSON evidence inventories. |
| `--diff-format markdown\|json` | Evidence catalog diff output format. Defaults to `markdown`. |
| `--diff-output <path>` | Optional evidence diff output file. If omitted, output is written to stdout. |
| `--fail-on-drift` | Exits non-zero when the diff finds added, removed, changed, status-drifted, or audit-anchor-drifted evidence. |
| `--include-unchanged` | Includes unchanged evidence rows in the diff output. |
| `--policy <path>` | Evaluates the inventory diff against a local evidence handoff policy JSON file. |
| `--policy-template <name>` | Evaluates the inventory diff against a packaged evidence handoff policy template. |
| `--policy-report-format markdown\|json` | Policy report output format. Defaults to `markdown`. |
| `--policy-output <path>` | Optional policy report output file. If omitted, output is written to stdout. |
| `--fail-on-policy-fail` | Exits non-zero when policy evaluation returns `FAIL`. `WARN` exits zero. |
| `--list-policy-templates` | Lists packaged evidence handoff policy templates. |
| `--export-policy-template <name>` | Writes the exact packaged template JSON to `--policy-output` or stdout. |
| `--validate-policy <path>` | Validates a policy file mode, severity, change type, and path-rule schema. |
| `--list-policy-examples` | Lists packaged evidence policy walkthrough examples. |
| `--print-policy-example <name>` | Prints a compact summary for a packaged walkthrough example. |
| `--export-policy-example <name>` | Exports a packaged before/after catalog pair and expected decision metadata. Requires `--example-output-dir`. |
| `--example-output-dir <directory>` | Target directory for exported policy examples or walkthrough exports. |
| `--walkthrough-policy-example <name>` | Exports an example, runs diff and policy evaluation, and emits a deterministic tutorial summary. |
| `--run-policy-training-lab` | Batch-runs every packaged policy walkthrough and compares actual decisions to expected decisions. |
| `--training-lab-format markdown\|json` | Training lab transcript format. Defaults to `markdown`. |
| `--training-lab-output <path>` | Optional training transcript output file. If omitted, output is written to stdout. |
| `--training-lab-export-dir <directory>` | Optional directory where the training lab exports the packaged examples it used. |
| `--include-training-details` | Includes per-change policy details in the training lab JSON transcript. |
| `--fail-on-training-mismatch` | Explicitly exits non-zero when a training lab expected decision mismatch is detected. This is the default behavior. |
| `--no-fail-on-training-mismatch` | Allows the training lab to exit zero even when an expected decision mismatch is detected. |
| `--list-training-scorecards` | Lists packaged operator scorecards for the evidence policy examples. |
| `--print-training-scorecard <name>` | Prints a deterministic scorecard prompt, answer key, remediation note, and scoring model. |
| `--grade-training-scorecard <answers.json>` | Grades operator answers against packaged scorecards. |
| `--scorecard-format markdown\|json` | Scorecard grading report format. Defaults to `markdown`. |
| `--scorecard-output <path>` | Optional scorecard report output file. If omitted, output is written to stdout. |
| `--fail-on-score-below <percent>` | Exits non-zero when the graded score is below the supplied percentage. |
| `--force` | Allows packaged example export, walkthrough export, or training-lab export to overwrite existing example files. |

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

## Evidence Catalog Diff

`--diff-inventory` compares two saved JSON inventory catalogs and emits a deterministic handoff-delta report:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory sender-catalog.json receiver-catalog.json \
  --diff-format markdown \
  --fail-on-drift \
  --diff-output handoff-delta.md
```

Use it for before/after evidence inventories, sender/receiver handoffs, or incident-ticket evidence revisions. The diff matches evidence by stable relative path and reports:

- new evidence files;
- removed evidence files;
- changed SHA-256 checksums;
- verification status drift;
- audit log latest-entry hash or entry-count drift;
- report, input, manifest, bundle, redaction-summary, and verification-summary changes.

JSON output is intended for automation. Markdown output is intended for incident tickets and human review. Both formats are sorted deterministically and omit timestamps or random identifiers. `--include-unchanged` can be useful when an operator wants a full handoff table; by default the report focuses on drift.

`--fail-on-drift` exits non-zero when any drift is found, which is useful in scripted handoff checks. A non-zero drift result does not prove malicious activity; it means the two saved inventory catalogs disagree and should be reviewed.

The diff is a local inventory comparison only. It cannot prove files were never changed before inventory, does not provide identity proof, and is not legal chain-of-custody.

## Evidence Handoff Policies

`--policy` evaluates a catalog diff against deterministic pass/warn/fail handoff rules:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory sender-catalog.json receiver-catalog.json \
  --policy handoff-policy.json \
  --policy-report-format markdown \
  --fail-on-policy-fail \
  --policy-output handoff-policy-report.md
```

Policy reports include a `PASS`, `WARN`, or `FAIL` decision, severity counts, matched rules, unclassified changes, and the same local-only limitations as the underlying checksum inventory diff. JSON output is stable for automation; Markdown output is stable for ticket attachment.

Packaged templates can be listed, exported, validated, and used directly:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --list-policy-templates

java -jar target/LoadBalancerPro-2.4.2.jar \
  --export-policy-template regulated-handoff \
  --policy-output regulated-handoff.json

java -jar target/LoadBalancerPro-2.4.2.jar \
  --validate-policy regulated-handoff.json

java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory sender-catalog.json receiver-catalog.json \
  --policy-template regulated-handoff \
  --policy-report-format markdown \
  --fail-on-policy-fail \
  --policy-output handoff-policy-report.md
```

Curated templates:

- `strict-zero-drift`: final equality check where any drift fails.
- `receiver-redaction`: expected receiver-side redaction summaries and reviewed redacted-output changes.
- `audit-append`: expected audit-log anchor drift after receiver verification.
- `regulated-handoff`: strict packaged profile for reviewed handoffs, without legal or identity-proof claims.
- `investigation-working-copy`: permissive active-investigation profile where working notes are expected but missing core evidence still fails.

See [`EVIDENCE_POLICY_TEMPLATES.md`](EVIDENCE_POLICY_TEMPLATES.md) for intended use cases and [`EVIDENCE_POLICY_EXAMPLES.md`](EVIDENCE_POLICY_EXAMPLES.md) for concrete sender/receiver catalog pairs with expected `PASS`, `WARN`, and `FAIL` decisions.

Packaged walkthrough examples let operators practice the full flow without locating test resources:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --list-policy-examples

java -jar target/LoadBalancerPro-2.4.2.jar \
  --export-policy-example receiver-redaction-warn \
  --example-output-dir walkthrough/receiver-redaction

java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory walkthrough/receiver-redaction/before.json \
                   walkthrough/receiver-redaction/after.json \
  --policy-template receiver-redaction \
  --policy-report-format markdown
```

For a single-command tutorial dry-run:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --walkthrough-policy-example receiver-redaction-warn \
  --example-output-dir walkthrough/receiver-redaction \
  --policy-report-format json
```

The walkthrough command exports `before.json`, `after.json`, and `expected-decision.json`, runs the same catalog diff and policy evaluator as production CLI handoff checks, and returns a deterministic Markdown or JSON tutorial summary. It refuses to overwrite existing exported example files unless `--force` is supplied.

Run the full offline training lab when an operator needs to practice every packaged profile in one pass:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --run-policy-training-lab \
  --training-lab-format markdown
```

For a machine-readable transcript:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --run-policy-training-lab \
  --training-lab-format json \
  --training-lab-output evidence-policy-training-lab.json
```

The training lab batch-runs the packaged `PASS`, `WARN`, and `FAIL` walkthrough examples, compares actual decisions to each `expected-decision.json`, summarizes matched and mismatched examples, and exits non-zero if a mismatch appears. Use `--training-lab-export-dir <directory>` to save the exact example files used, `--include-training-details` for per-change JSON detail, and `--force` to overwrite an existing export directory. The transcript is deterministic and omits timestamps or random ids by default.

Training scorecards let operators submit answers for the packaged examples and receive deterministic grading reports:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --list-training-scorecards

java -jar target/LoadBalancerPro-2.4.2.jar \
  --print-training-scorecard audit-append-warn
```

Answers use this JSON shape:

```json
{
  "operator": "operator-a",
  "answers": [
    {
      "exerciseName": "audit-append-warn",
      "decision": "WARN",
      "reason": "Audit anchor advanced after receiver verification",
      "action": "verify audit append",
      "notes": "Verify the appended audit entry and record the review."
    }
  ]
}
```

Grade the answers as Markdown or JSON:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --grade-training-scorecard scorecard-answers.json \
  --scorecard-format markdown \
  --scorecard-output scorecard-report.md

java -jar target/LoadBalancerPro-2.4.2.jar \
  --grade-training-scorecard scorecard-answers.json \
  --scorecard-format json \
  --scorecard-output scorecard-report.json \
  --fail-on-score-below 80
```

Markdown reports summarize score, max score, percentage, pass status, and a per-exercise table:

```markdown
# LoadBalancerPro Evidence Training Scorecard

- Scorecard version: 1
- Operator: operator-a
- Total exercises: 7
- Total score: 70
- Max score: 70
- Percent: 100.0
- Passing score: 80.0
- Passed: true
```

JSON reports expose the same deterministic fields for automation:

```json
{
  "scorecardVersion": "1",
  "operator": "operator-a",
  "totalExercises": 7,
  "totalScore": 70,
  "maxScore": 70,
  "percent": 100.0,
  "passed": true,
  "passingScore": 80.0,
  "perExercise": [
    {
      "exerciseName": "audit-append-warn",
      "expectedDecision": "WARN",
      "actualDecision": "WARN",
      "decisionCorrect": true,
      "reasonMatched": true,
      "reasonCredit": 3,
      "actionMatched": true,
      "actionCredit": 2,
      "score": 10,
      "maxScore": 10,
      "feedback": "Decision matched expected WARN; Reason matched expected primary reason; Action matched acceptable action."
    }
  ]
}
```

Each scorecard is worth 10 points: 5 decision points, 3 reason points, and 2 action points. Exact matches receive full credit; reason and action text can earn deterministic partial credit from token overlap. The scorecard path is an offline training aid only. It does not start the API server, does not construct `CloudManager`, does not mutate cloud state, and is not certification, legal compliance proof, identity proof, or legal chain-of-custody.

Policy JSON format:

```json
{
  "policyVersion": "1",
  "mode": "ALLOWLIST",
  "defaultSeverity": "FAIL",
  "rules": [
    {
      "changeType": "ADDED",
      "pathPattern": "redaction-summary.json",
      "severity": "INFO",
      "reason": "receiver generated redaction evidence"
    },
    {
      "changeType": "AUDIT_ANCHOR_CHANGED",
      "pathPattern": "offline-cli-audit.jsonl",
      "severity": "WARN",
      "reason": "receiver appended expected audit entries"
    }
  ]
}
```

Modes:

- `STRICT`: any drift fails unless a rule explicitly classifies it as `WARN`, `INFO`, or `IGNORE`.
- `ALLOWLIST`: unmatched drift uses `defaultSeverity`.

Severities:

- `FAIL`: makes the policy decision `FAIL`.
- `WARN`: makes the decision `WARN` unless any `FAIL` exists.
- `INFO`: records expected drift without changing a passing decision.
- `IGNORE`: records the match without affecting the decision.

Rules match a `changeType` plus a catalog-relative `pathPattern`. Exact paths are supported, a trailing `/**` matches a subtree, and `*` / `**` wildcard patterns are supported for simple handoff allowlists. Matching remains local and deterministic; no external service is consulted.

Recommended handoff workflow:

1. Sender creates `sender-catalog.json` with `--inventory --verify-inventory --include-hashes`.
2. Receiver creates `receiver-catalog.json` the same way after transfer.
3. Operator runs `--diff-inventory ... --policy-template ... --fail-on-policy-fail`, or uses `--policy <path>` for a custom exported policy.
4. Attach the policy report to the incident ticket with the inventories and bundle evidence.

`--fail-on-policy-fail` exits non-zero only for a `FAIL` decision. A `WARN` decision exits zero so scripted handoffs can proceed while still surfacing review items.

Policy evaluation is local checksum/policy evaluation only. It is not identity proof, not a cryptographic signature, not centralized evidence storage, and not legal chain-of-custody.

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
- can diff saved evidence inventory catalogs without starting the API server;
- can evaluate saved evidence inventory diffs against local handoff policies without starting the API server;
- can list, export, validate, and apply packaged evidence policy templates without starting the API server;
- can list, export, and dry-run packaged evidence policy examples without starting the API server;
- can batch-run packaged policy training examples without starting the API server;
- can grade packaged evidence training scorecard answers without starting the API server;
- generates and verifies checksum manifests locally with Java SHA-256, not external tools or signing keys.

## Limitations

- The CLI formats saved results. It does not recompute allocation, replay, routing, health checks, or remediation.
- Checksum manifests are useful for bundle integrity, not identity proof or non-repudiation.
- Incident bundles are portable evidence containers, not signed attestations.
- Redaction is exact string replacement and should be reviewed by an operator before evidence is shared externally.
- Audit logs are local checksum chains, not centralized append-only storage, cryptographic signatures, identity proof, or non-repudiation.
- Evidence inventories are local checksum catalogs, not legal chain-of-custody records or identity proof.
- Evidence catalog diffs compare saved inventory records only; they cannot prove what happened before either inventory was created.
- Evidence handoff policies classify catalog drift only; they do not prove operator identity, intent, or legal custody.
- Evidence policy templates are reusable local rule profiles, not compliance certifications or legal custody controls.
- Evidence policy training labs use packaged synthetic examples only; they are not a substitute for real incident review.
- Live deployment state should still be verified before taking operator action.
- Invalid JSON or unsupported input shapes exit non-zero and print a safe error without stack traces.
