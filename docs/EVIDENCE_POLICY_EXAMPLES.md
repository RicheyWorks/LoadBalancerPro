# Evidence Policy Example Catalogs

LoadBalancerPro includes tiny synthetic sender/receiver catalog pairs that show how each packaged evidence handoff policy template classifies drift. The examples are deterministic test fixtures, not generated operational reports, and they do not provide identity proof, cryptographic signing, legal chain-of-custody, or compliance certification.

Packaged walkthrough examples are available through the offline CLI. Source fixtures live under:

```text
src/test/resources/evidence-policy-examples/
src/main/resources/evidence-policies/examples/
src/main/resources/evidence-policies/scorecards/
```

Each packaged example exports `before.json`, `after.json`, and an `expected-decision.json` descriptor. The older test fixture folders retain focused files such as `after-drift.json` and `expected-fail.json`, but the CLI export normalizes each walkthrough to the three deterministic filenames.

## Example Matrix

| Template | Packaged Example | Expected Decision | What It Shows |
| --- | --- | --- | --- |
| `strict-zero-drift` | `strict-zero-drift-pass` | `PASS` | Sender and receiver catalogs are identical. |
| `strict-zero-drift` | `strict-zero-drift-fail` | `FAIL` | Any checksum drift fails a strict final handoff. |
| `receiver-redaction` | `receiver-redaction-warn` | `WARN` | Redaction summary is expected, while redacted report/bundle changes remain review items. |
| `audit-append` | `audit-append-warn` | `WARN` | Receiver-side audit anchor advancement is expected but should be reviewed. |
| `regulated-handoff` | `regulated-handoff-pass` | `PASS` | Strict packaged review profile with no drift. |
| `regulated-handoff` | `regulated-handoff-fail` | `FAIL` | Missing core bundle evidence fails. |
| `investigation-working-copy` | `investigation-working-copy-warn` | `WARN` | Working notes are informational, while report edits warn before final handoff. |

## Walkthrough CLI

List packaged examples:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar --list-policy-examples
```

Print a compact summary:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar --print-policy-example receiver-redaction-warn
```

Export a training pair:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --export-policy-example receiver-redaction-warn \
  --example-output-dir walkthrough/receiver-redaction
```

The export writes `before.json`, `after.json`, and `expected-decision.json`. Existing files are not overwritten unless `--force` is supplied.

Run the full dry-run walkthrough:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --walkthrough-policy-example receiver-redaction-warn \
  --example-output-dir walkthrough/receiver-redaction \
  --policy-report-format markdown
```

The walkthrough exports the packaged example, runs the evidence catalog diff, evaluates the packaged policy template, and emits a deterministic Markdown or JSON tutorial summary. For JSON tutorial output, use `--policy-report-format json`. For a file output, add `--policy-output walkthrough-summary.json`.

Run every packaged walkthrough as a one-command offline training lab:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --run-policy-training-lab \
  --training-lab-format markdown
```

For automation or operator onboarding records, emit JSON or write the transcript to a file:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --run-policy-training-lab \
  --training-lab-format json \
  --training-lab-output policy-training-lab.json
```

The training lab loads all packaged examples, runs the same diff and policy evaluator used by `--walkthrough-policy-example`, compares actual decisions to `expected-decision.json`, and exits non-zero if any expected decision does not match. Add `--training-lab-export-dir walkthrough/all-examples` to export the example files used by the lab, `--include-training-details` to include per-change detail in the transcript, and `--force` to replace an existing export directory.

## Training Scorecards

Scorecards grade operator-submitted decisions against the same packaged examples without starting the API server:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar --list-training-scorecards

java -jar target/LoadBalancerPro-2.5.0.jar \
  --print-training-scorecard receiver-redaction-warn
```

Use this answers shape for offline practice:

```json
{
  "operator": "operator-a",
  "answers": [
    {
      "exerciseName": "receiver-redaction-warn",
      "decision": "WARN",
      "reason": "Receiver redaction changes require review",
      "action": "confirm redaction summary",
      "notes": "Confirm the summary and document the reviewed redacted files."
    }
  ]
}
```

Grade answers as Markdown or JSON:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --grade-training-scorecard scorecard-answers.json \
  --scorecard-format markdown \
  --scorecard-output scorecard-report.md \
  --fail-on-score-below 80

java -jar target/LoadBalancerPro-2.5.0.jar \
  --grade-training-scorecard scorecard-answers.json \
  --scorecard-format json \
  --scorecard-output scorecard-report.json
```

The report includes `scorecardVersion`, total/max score, percent, pass/fail status, and per-exercise decision, reason, action, score, and feedback fields. Each exercise is worth 10 points by default: 5 for the `PASS`/`WARN`/`FAIL` decision, 3 for the expected primary reason, and 2 for an acceptable action. Reason and action scoring can award deterministic partial credit for token overlap. Output omits timestamps and random ids by default.

Scorecards are a local onboarding aid only. They are not certification, not legal compliance proof, not identity proof, and not a substitute for real incident review.

## API And Postman Onboarding

When the Spring Boot API is running for a local demo, operators can discover the same packaged examples and scorecards through read-only onboarding routes:

```text
GET  /api/evidence-training/onboarding
GET  /api/evidence-training/examples
GET  /api/evidence-training/scorecards
GET  /api/evidence-training/scorecards/{name}
GET  /api/evidence-training/scorecards/{name}/answer-template
POST /api/evidence-training/scorecards/grade
```

The grade route evaluates submitted answer JSON in memory and returns deterministic JSON; it does not write runtime reports or mutate cloud state. Import `postman/LoadBalancerPro.postman_collection.json` and use the `Evidence Training Onboarding` folder for local Postman walkthroughs. See [`POSTMAN_EVIDENCE_TRAINING.md`](POSTMAN_EVIDENCE_TRAINING.md) for the `baseUrl` variable, sample answer JSON, and limitations.

After exporting, operators can also run the underlying commands directly:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --diff-inventory walkthrough/receiver-redaction/before.json \
                   walkthrough/receiver-redaction/after.json \
  --diff-format markdown

java -jar target/LoadBalancerPro-2.5.0.jar \
  --diff-inventory walkthrough/receiver-redaction/before.json \
                   walkthrough/receiver-redaction/after.json \
  --policy-template receiver-redaction \
  --policy-report-format markdown
```

## CLI Examples

Strict zero-drift final check:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/strict-zero-drift/before.json \
                   src/test/resources/evidence-policy-examples/strict-zero-drift/after.json \
  --policy-template strict-zero-drift \
  --policy-report-format markdown
```

Expected decision: `PASS`.

Strict zero-drift with checksum drift:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/strict-zero-drift/before.json \
                   src/test/resources/evidence-policy-examples/strict-zero-drift/after-drift.json \
  --policy-template strict-zero-drift \
  --policy-report-format markdown \
  --fail-on-policy-fail
```

Expected decision: `FAIL`; the command exits non-zero when `--fail-on-policy-fail` is present.

Receiver redaction handoff:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/receiver-redaction/before.json \
                   src/test/resources/evidence-policy-examples/receiver-redaction/after.json \
  --policy-template receiver-redaction \
  --policy-report-format markdown
```

Expected decision: `WARN`.

Audit append handoff:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/audit-append/before.json \
                   src/test/resources/evidence-policy-examples/audit-append/after.json \
  --policy-template audit-append \
  --policy-report-format markdown
```

Expected decision: `WARN`.

Regulated handoff clean path:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/regulated-handoff/before.json \
                   src/test/resources/evidence-policy-examples/regulated-handoff/after.json \
  --policy-template regulated-handoff \
  --policy-report-format markdown \
  --fail-on-policy-fail
```

Expected decision: `PASS`.

Regulated handoff with missing bundle:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/regulated-handoff/before.json \
                   src/test/resources/evidence-policy-examples/regulated-handoff/after-missing-bundle.json \
  --policy-template regulated-handoff \
  --policy-report-format markdown \
  --fail-on-policy-fail
```

Expected decision: `FAIL`; missing core evidence should stop the handoff.

Investigation working-copy handoff:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/investigation-working-copy/before.json \
                   src/test/resources/evidence-policy-examples/investigation-working-copy/after.json \
  --policy-template investigation-working-copy \
  --policy-report-format markdown
```

Expected decision: `WARN`.

## How To Interpret The Examples

Use the examples as starting points for operator training and scripted regression checks. `PASS` means the packaged policy found no failing or warning drift. `WARN` means the policy found expected or reviewable drift that should be documented. `FAIL` means the policy found drift that should stop the handoff unless the operator intentionally changes the policy.

These examples compare saved catalog records only. They cannot prove files were never changed before inventory, cannot prove who transferred evidence, and cannot replace a centralized evidence system.
