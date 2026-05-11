# Release Candidate Review Packet Template

Use this template after completing [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md). Use [`OPERATOR_INSTALL_RUN_MATRIX.md`](OPERATOR_INSTALL_RUN_MATRIX.md) for the install/run path names and [`RELEASE_INTENT_CHECKLIST.md`](RELEASE_INTENT_CHECKLIST.md) for the hard-stop release-intent gate. Replace placeholders with observed evidence only.

```text
Release Candidate Dry-Run Review Packet

Commit hash:
<commit-hash>

CI run URL:
<ci-run-url>

CI status:
<success|failure|not-reviewed>

CodeQL status:
<success|failure|not-reviewed>

Skipped test count:
<count-from-ci>

JaCoCo artifact reviewed:
<yes|no>

Packaged-artifact-smoke artifact reviewed:
<yes|no>

SBOM artifact reviewed:
<yes|no>

Local SHA-256:
<local-sha256-or-not-run>

CI SHA-256:
<ci-sha256-or-not-reviewed>

Checksum comparison note:
<same-artifact-controlled-rebuild-or-not-comparable>

Jar resources verified:
<yes|no>

Proxy demo reviewed:
<yes|no>

Operator install/run path selected:
<matrix-row-or-not-selected>

Release-intent checklist result:
<not-started|remain-release-free|separate-release-approval-needed>

Known limitations:
<observed-limitations>

Go/no-go result:
<go|no-go|defer>

Reviewer notes:
<notes>
```

Do not replace placeholders with sample hashes, sample checksums, guessed artifact names, or copied release evidence from another run.
