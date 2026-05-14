# Release Intent Checklist

Use this checklist before anyone intentionally invokes a future release process for LoadBalancerPro. This checklist does not create a release. It is a decision gate for review readiness, not a publishing procedure.

For the prepared `v2.5.0` JAR/docs-first release intent, complete the exact-version gate in [`V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md`](V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md) after reviewing [`RELEASE_NOTES_v2.5.0.md`](RELEASE_NOTES_v2.5.0.md) and the generated dry-run evidence.

## Preconditions

Complete and record these checks before considering release publication:

- Latest `main` CI is successful.
- Latest `main` CodeQL is successful.
- Open PR count has been checked.
- [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md) has been completed for the commit under review.
- [`OPERATOR_INSTALL_RUN_MATRIX.md`](OPERATOR_INSTALL_RUN_MATRIX.md) has been reviewed for the intended install/run paths.
- `jacoco-coverage-report` has been downloaded or reviewed from the selected CI run.
- `packaged-artifact-smoke` has been downloaded or reviewed from the selected CI run.
- `loadbalancerpro-sbom` has been downloaded or reviewed from the selected CI run.
- `artifact-smoke-summary.txt`, `artifact-sha256.txt`, and `jar-resource-list.txt` have been inspected.
- CI reported zero skipped tests.
- Local artifact verification has passed or CI has been explicitly recorded as source of truth because local Maven is blocked.
- SBOM files `bom.json` and `bom.xml` have been reviewed as workflow artifact evidence.
- Packaged jar smoke evidence has been reviewed.
- Real-backend proxy example evidence from [`REAL_BACKEND_PROXY_EXAMPLES.md`](REAL_BACKEND_PROXY_EXAMPLES.md) has been reviewed if those examples are part of the release candidate packet.
- Known limitations and proxy demo boundaries have been acknowledged.
- Documentation links for install/run, artifact verification, CI artifact consumption, and release candidate dry run have been reviewed.

## Intent Questions

Answer these before any release publication command is considered:

- Is this actually intended to become a release, rather than a release-free review?
- Has the release version or tag name been chosen?
- Has release asset provenance been decided?
- Has a human operator given explicit approval for a real release in a separate prompt or approval step?
- Has rollback or cleanup been considered for a failed or partially completed release?
- Has the reviewer confirmed that workflow artifacts are not GitHub Release assets?
- Has the reviewer confirmed that no generated local jar, checksum, or smoke output should be committed?
- Has the reviewer confirmed that `release-downloads/` should remain untouched unless a separate approved verification task requires it?

## Hard Stops

- Do not run `gh release` commands unless the user explicitly approves a real release in a separate prompt.
- Do not run `git tag` commands unless the user explicitly approves a real release in a separate prompt.
- Do not run release upload commands unless the user explicitly approves a real release in a separate prompt.
- Do not modify `release-downloads/` unless the user explicitly approves that evidence task.
- Do not change the default branch.
- Do not change repository rulesets.
- Do not enable proxy mode by default.
- Do not change default application behavior.
- Do not describe workflow artifacts, SBOM files, or checksums as signed release assets unless a future release process actually implements that.

## Release-Intent Decision

Record the decision before leaving release-free review mode:

| Question | Expected release-intent answer | Status | Notes |
| --- | --- | --- | --- |
| CI success confirmed? | Yes |  |  |
| CodeQL success confirmed? | Yes |  |  |
| Zero skipped tests confirmed? | Yes |  |  |
| CI artifacts reviewed? | Yes |  |  |
| SBOM reviewed? | Yes |  |  |
| Local or CI artifact verification recorded? | Yes |  |  |
| Operator install/run path selected? | Yes |  |  |
| Known limitations accepted? | Yes |  |  |
| Explicit separate approval for release publication? | Required before release commands |  |  |
| Release version/tag chosen? | Required before release commands |  |  |
| Default branch/ruleset changes avoided? | Yes |  |  |
| `release-downloads/` unchanged unless separately approved? | Yes |  |  |

If any required item is missing, remain in release-free review mode and do not invoke a release process.
