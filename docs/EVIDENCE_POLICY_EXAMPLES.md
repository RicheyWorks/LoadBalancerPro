# Evidence Policy Example Catalogs

LoadBalancerPro includes tiny synthetic sender/receiver catalog pairs that show how each packaged evidence handoff policy template classifies drift. The examples are deterministic test fixtures, not generated operational reports, and they do not provide identity proof, cryptographic signing, legal chain-of-custody, or compliance certification.

Example fixtures live under:

```text
src/test/resources/evidence-policy-examples/
```

Each profile includes `before.json`, `after.json`, and an `expected-decision.json` descriptor. Some profiles also include a focused failure descriptor such as `expected-fail.json`.

## Example Matrix

| Template | Example | Expected Decision | What It Shows |
| --- | --- | --- | --- |
| `strict-zero-drift` | `strict-zero-drift/before.json` -> `strict-zero-drift/after.json` | `PASS` | Sender and receiver catalogs are identical. |
| `strict-zero-drift` | `strict-zero-drift/before.json` -> `strict-zero-drift/after-drift.json` | `FAIL` | Any checksum drift fails a strict final handoff. |
| `receiver-redaction` | `receiver-redaction/before.json` -> `receiver-redaction/after.json` | `WARN` | Redaction summary is expected, while redacted report/bundle changes remain review items. |
| `audit-append` | `audit-append/before.json` -> `audit-append/after.json` | `WARN` | Receiver-side audit anchor advancement is expected but should be reviewed. |
| `regulated-handoff` | `regulated-handoff/before.json` -> `regulated-handoff/after.json` | `PASS` | Strict packaged review profile with no drift. |
| `regulated-handoff` | `regulated-handoff/before.json` -> `regulated-handoff/after-missing-bundle.json` | `FAIL` | Missing core bundle evidence fails. |
| `investigation-working-copy` | `investigation-working-copy/before.json` -> `investigation-working-copy/after.json` | `WARN` | Working notes are informational, while report edits warn before final handoff. |

## CLI Examples

Strict zero-drift final check:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/strict-zero-drift/before.json \
                   src/test/resources/evidence-policy-examples/strict-zero-drift/after.json \
  --policy-template strict-zero-drift \
  --policy-report-format markdown
```

Expected decision: `PASS`.

Strict zero-drift with checksum drift:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/strict-zero-drift/before.json \
                   src/test/resources/evidence-policy-examples/strict-zero-drift/after-drift.json \
  --policy-template strict-zero-drift \
  --policy-report-format markdown \
  --fail-on-policy-fail
```

Expected decision: `FAIL`; the command exits non-zero when `--fail-on-policy-fail` is present.

Receiver redaction handoff:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/receiver-redaction/before.json \
                   src/test/resources/evidence-policy-examples/receiver-redaction/after.json \
  --policy-template receiver-redaction \
  --policy-report-format markdown
```

Expected decision: `WARN`.

Audit append handoff:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/audit-append/before.json \
                   src/test/resources/evidence-policy-examples/audit-append/after.json \
  --policy-template audit-append \
  --policy-report-format markdown
```

Expected decision: `WARN`.

Regulated handoff clean path:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/regulated-handoff/before.json \
                   src/test/resources/evidence-policy-examples/regulated-handoff/after.json \
  --policy-template regulated-handoff \
  --policy-report-format markdown \
  --fail-on-policy-fail
```

Expected decision: `PASS`.

Regulated handoff with missing bundle:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/regulated-handoff/before.json \
                   src/test/resources/evidence-policy-examples/regulated-handoff/after-missing-bundle.json \
  --policy-template regulated-handoff \
  --policy-report-format markdown \
  --fail-on-policy-fail
```

Expected decision: `FAIL`; missing core evidence should stop the handoff.

Investigation working-copy handoff:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory src/test/resources/evidence-policy-examples/investigation-working-copy/before.json \
                   src/test/resources/evidence-policy-examples/investigation-working-copy/after.json \
  --policy-template investigation-working-copy \
  --policy-report-format markdown
```

Expected decision: `WARN`.

## How To Interpret The Examples

Use the examples as starting points for operator training and scripted regression checks. `PASS` means the packaged policy found no failing or warning drift. `WARN` means the policy found expected or reviewable drift that should be documented. `FAIL` means the policy found drift that should stop the handoff unless the operator intentionally changes the policy.

These examples compare saved catalog records only. They cannot prove files were never changed before inventory, cannot prove who transferred evidence, and cannot replace a centralized evidence system.
