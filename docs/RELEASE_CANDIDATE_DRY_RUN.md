# Release Candidate Dry Run

This guide rehearses a LoadBalancerPro release candidate review without publishing anything. It ties together CI artifacts, local artifact verification, SBOM review, packaged jar smoke evidence, proxy status UI checks, and demo stack commands into one go/no-go operator packet.

This dry run does not publish a release.

## Non-Goals

- No tags.
- No GitHub Releases.
- No release assets.
- No `release-downloads/` evidence.
- No generated jars or checksums committed.
- No release workflow changes.
- No default application behavior changes.
- No default proxy enablement.
- No cloud mutation.
- No production certification.

## Prerequisite State

Confirm the repository state before starting the packet:

- Default branch is `main`.
- Latest `origin/main` is the commit under review.
- Open PR count is checked.
- Latest `main` CI is successful.
- Latest `main` CodeQL is successful.
- The review uses one specific CI workflow run for artifact evidence.

Suggested read-only checks:

```bash
git rev-parse origin/main
gh pr list --repo RicheyWorks/LoadBalancerPro --state open --limit 20
gh run list --repo RicheyWorks/LoadBalancerPro --branch main --limit 10
```

## CI Artifact Checklist

Use [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md) to download and inspect workflow artifacts from the selected CI run.

Required workflow artifacts:

- `jacoco-coverage-report`
- `packaged-artifact-smoke`
- `loadbalancerpro-sbom`

Record the CI run URL, commit hash, artifact names, and whether each artifact downloaded successfully in the reviewer packet template.

## Packaged Artifact Smoke Checklist

Inspect the `packaged-artifact-smoke` artifact.

Required files:

- `artifact-smoke-summary.txt`
- `artifact-sha256.txt`
- `jar-resource-list.txt`

Required jar evidence:

- `BOOT-INF/classes/static/proxy-status.html`
- `BOOT-INF/classes/static/load-balancing-cockpit.html`
- `BOOT-INF/classes/application-proxy-demo-round-robin.properties`
- `BOOT-INF/classes/application-proxy-demo-weighted-round-robin.properties`
- `BOOT-INF/classes/application-proxy-demo-failover.properties`
- `BOOT-INF/classes/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.class`

Treat these as workflow artifact checks, not release asset publication.

## Local Artifact Verification Checklist

Use [`LOCAL_ARTIFACT_VERIFICATION.md`](LOCAL_ARTIFACT_VERIFICATION.md) when local Maven dependency resolution works.

Checklist:

- Local jar was built or an operator explicitly cited CI as source of truth because local Maven is blocked.
- Local SHA-256 was generated for `target/LoadBalancerPro-2.4.2.jar` when a local jar exists.
- `jar tf` output was inspected for required static pages, demo profiles, and `ProxyDemoFixtureLauncher.class`.
- Local SHA-256 was compared with CI SHA-256 only for the same artifact or a controlled rebuild context.
- Any checksum mismatch was documented with build context notes rather than treated as automatic failure.

## Testing Evidence Checklist

Use the selected CI run and [`TESTING_COVERAGE.md`](TESTING_COVERAGE.md).

Checklist:

- CI passed.
- CodeQL passed.
- CI reported zero skipped tests.
- `jacoco-coverage-report` was available.
- Coverage percentages were reviewed from CI logs or JaCoCo files.
- Coverage numbers were not inflated, rounded into unsupported claims, or described as complete behavioral proof.

## SBOM Checklist

Use the `loadbalancerpro-sbom` workflow artifact when present in the selected CI run.

Checklist:

- `bom.json` is present.
- `bom.xml` is present.
- SBOM files are treated as workflow evidence for dependency/component inventory.
- SBOM files are not described as signed release assets.
- SBOM files are not described as vulnerability-free proof.

## Operator Demo Checklist

Use [`OPERATOR_DISTRIBUTION_SMOKE_KIT.md`](OPERATOR_DISTRIBUTION_SMOKE_KIT.md), [`OPERATOR_PACKAGING.md`](OPERATOR_PACKAGING.md), and [`PROXY_DEMO_STACK.md`](PROXY_DEMO_STACK.md).

Checklist:

- Packaged jar startup command is available.
- Maven exec fixture launcher command is available.
- Proxy demo profiles are available.
- `/proxy-status.html` opens during demo review when the app is running.
- `/api/proxy/status` is available as the status source when the app is running.
- Demo stack commands are available for Windows PowerShell and Unix shell.
- Proxy mode remains explicitly opt-in.

## Go/No-Go Checklist

Copy this table into the reviewer packet and fill only with observed evidence.

| Evidence item | Source | Expected result | Status | Notes |
| --- | --- | --- | --- | --- |
| Commit under review | Git / CI run | Commit hash recorded |  |  |
| Open PR count | GitHub PR list | No unexpected open PRs |  |  |
| Main CI | GitHub Actions | Success |  |  |
| CodeQL | GitHub Actions | Success |  |  |
| Skipped tests | CI logs | Zero skipped tests |  |  |
| JaCoCo artifact | `jacoco-coverage-report` | Artifact downloaded and inspected |  |  |
| Packaged smoke artifact | `packaged-artifact-smoke` | Artifact downloaded and inspected |  |  |
| Smoke summary | `artifact-smoke-summary.txt` | Required entries pass |  |  |
| CI SHA-256 | `artifact-sha256.txt` | Digest recorded |  |  |
| Jar resource list | `jar-resource-list.txt` | Required jar entries present |  |  |
| SBOM artifact | `loadbalancerpro-sbom` | `bom.json` and `bom.xml` present |  |  |
| Local artifact verification | Local commands or CI source-of-truth note | Result documented |  |  |
| Proxy status UI | `/proxy-status.html` | Page available during demo review |  |  |
| Proxy status source | `/api/proxy/status` | Endpoint available during demo review |  |  |
| Demo stack | `PROXY_DEMO_STACK.md` | Commands reviewed or run |  |  |
| Final dry-run result | Reviewer judgment | Go or no-go recorded |  |  |

## Reviewer Packet

Use [`RELEASE_CANDIDATE_REVIEW_PACKET_TEMPLATE.md`](RELEASE_CANDIDATE_REVIEW_PACKET_TEMPLATE.md). Keep placeholders until real observed values are available. Do not add sample hashes, sample checksums, or invented evidence.

## Limits

- This is a rehearsal and evidence collection path only.
- Workflow artifacts are not GitHub Release assets.
- Local checksums are local evidence unless tied to the same artifact or controlled rebuild context.
- SBOM files are component inventory evidence, not a signed release claim.
- Proxy demo checks prove local operator usability only.
- This dry run does not publish a release.
