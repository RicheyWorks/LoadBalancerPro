# Evidence Policy Templates

LoadBalancerPro ships curated offline evidence handoff policy templates for common sender/receiver review profiles. They are local checksum policy rules for saved evidence inventory diffs. They do not provide identity proof, cryptographic signing, non-repudiation, centralized custody, or legal chain-of-custody.

## CLI Commands

List packaged templates:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --list-policy-templates
```

Export a template:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --export-policy-template regulated-handoff \
  --policy-output regulated-handoff.json
```

Validate a policy file:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --validate-policy regulated-handoff.json
```

Run a catalog diff with a packaged template:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --diff-inventory sender-catalog.json receiver-catalog.json \
  --policy-template regulated-handoff \
  --policy-report-format markdown \
  --fail-on-policy-fail \
  --policy-output handoff-policy-report.md
```

Use `--policy <path>` when a local custom policy file is needed instead of a packaged template.

## Templates

| Template | Mode | Use Case | Expected Behavior |
| --- | --- | --- | --- |
| `strict-zero-drift` | `STRICT` | Final sender/receiver equality checks. | Any drift fails unless a future explicit rule classifies it. |
| `receiver-redaction` | `ALLOWLIST` | Receiver creates redacted evidence before sharing. | Added redaction summaries are `INFO`; changed redacted reports or bundles are `WARN`; removed bundles fail. |
| `audit-append` | `ALLOWLIST` | Receiver appends verification entries to a local audit log. | Audit anchor drift is `WARN`; bundle or manifest removal/change fails. |
| `regulated-handoff` | `ALLOWLIST` | Strict handoff review where only documented summaries are expected. | Removed or changed core evidence fails; audit anchor drift warns; documented summaries are informational. |
| `investigation-working-copy` | `ALLOWLIST` | Active investigation before final evidence handoff. | Working notes are informational; report edits warn; missing core bundle/manifest/input evidence fails. |

## Choosing A Template

Use `strict-zero-drift` when both sides should have exactly the same catalog and any drift should stop the handoff.

Use `receiver-redaction` when the receiver is expected to create redacted copies for sharing. Treat warnings as review items: the policy allows expected redaction-related changes to surface without pretending they are identical evidence.

Use `audit-append` when a receiver verifies a bundle or manifest and appends that verification to the local checksum-chained audit log. Save the latest audit anchor hash/count in the incident ticket when tail-truncation detection matters.

Use `regulated-handoff` for the strictest packaged profile. It is still local checksum evaluation only; the name does not imply legal compliance, identity proof, or custody certification.

Use `investigation-working-copy` while responders are actively adding notes or reviewing reports before final handoff. Move back to `strict-zero-drift` or `regulated-handoff` for the final sender/receiver check.

## Determinism And Limits

Template listing, export, validation, JSON reports, and Markdown reports are deterministic and do not include timestamps or random identifiers by default.

Policy evaluation classifies changes already recorded by `--diff-inventory`. It cannot prove files were never changed before either inventory was created, cannot prove operator identity, and cannot replace a centralized evidence system.

For concrete sender/receiver catalog pairs and expected `PASS`, `WARN`, and `FAIL` outcomes, see [`EVIDENCE_POLICY_EXAMPLES.md`](EVIDENCE_POLICY_EXAMPLES.md).

## Walkthrough Examples

Packaged examples can be listed, exported, and dry-run fully offline:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --list-policy-examples

java -jar target/LoadBalancerPro-2.4.2.jar \
  --export-policy-example regulated-handoff-fail \
  --example-output-dir walkthrough/regulated-fail

java -jar target/LoadBalancerPro-2.4.2.jar \
  --walkthrough-policy-example regulated-handoff-fail \
  --example-output-dir walkthrough/regulated-fail \
  --policy-report-format json \
  --policy-output walkthrough-summary.json
```

Walkthrough output is deterministic and local-only. It demonstrates policy behavior over synthetic catalogs; it is not identity proof, legal chain-of-custody, or a compliance certification.
