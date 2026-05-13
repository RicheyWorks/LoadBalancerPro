# Dependency And SAST Risk Workflow

Use this workflow when CodeQL, Dependency Review, Trivy, Dependabot, SBOM review, or manual dependency review reports a finding that needs a release decision. It is an operator/reviewer workflow, not an alert-dismissal command.

This page does not dismiss GitHub alerts, does not change rulesets, does not publish releases, does not publish containers, and does not mutate `release-downloads/`. It defines how a finding is owned, triaged, documented, and either fixed or explicitly accepted for a bounded period.

## Ownership

Default owners:

- Repository owner: `@RicheyWorks`.
- Code owner: the matching `.github/CODEOWNERS` entry for the affected path.
- CodeQL/SAST owner: the repository owner plus the code owner for the affected source, workflow, Dockerfile, or documentation path.
- Dependency Review owner: the repository owner plus the code owner for `pom.xml`, `.github/dependabot.yml`, Dockerfile base images, or workflow action changes.
- Trivy/container owner: the repository owner plus the Dockerfile or container-runtime code owner.

The owner of record must appear in the PR, issue, residual-risk note, or release review packet before a non-fixed finding is treated as accepted. If ownership is unclear, assign `@RicheyWorks` as temporary owner and keep the finding open until a narrower owner is identified.

## Severity Handling

Treat tool severity as the starting point, not the final decision. Record the tool, finding id, package or path, affected version or code location, reachability, exploitability notes, and deployment exposure.

Severity policy:

- Critical: block release-candidate, production-candidate, and release-ready claims until fixed or explicitly accepted by the repository owner with an expiry date.
- High: block production-candidate and release-ready claims unless fixed or explicitly accepted by the repository owner with an expiry date and follow-up issue.
- Medium: may proceed only with owner rationale, planned remediation, and review date.
- Low or informational: may proceed with owner review, but still needs a rationale when dismissed, ignored, or deferred.
- Unknown severity: treat as Medium until the owner records enough evidence to lower or raise it.

Do not silently dismiss, ignore, or allowlist high or critical findings. A high or critical exception must include owner, severity, rationale, expiry or review date, compensating controls, and a remediation follow-up.

## Triage Steps

1. Capture evidence: PR number or workflow run, tool name, finding id, affected package/path, severity, and links to CI, CodeQL, Dependency Review, Trivy, or SBOM evidence.
2. Classify the finding: vulnerable dependency, vulnerable transitive dependency, vulnerable container layer, SAST finding, license/policy issue, false positive, unreachable code path, or tool metadata issue.
3. Assign owner: use `.github/CODEOWNERS` and the default ownership rules above.
4. Decide action: fix now, update dependency, change code, add test, accept temporarily, dismiss as false positive, or defer with a bounded follow-up.
5. Record rationale: use the accepted-risk or false-positive template below.
6. Verify: rerun focused tests, `mvn -q clean test`, `mvn -q verify`, `mvn -q -DskipTests package`, affected smoke dry-runs, and required PR checks when the fix touches code, build, Docker, or workflows.
7. Update evidence: link the PR, issue, release review packet, residual-risk note, or versioned release evidence document.

## Accepted-Risk Rationale Template

Use this format when a finding is real but cannot be fixed in the current PR:

```text
Accepted risk:
Finding/tool:
Severity:
Affected package/path:
Owner:
Reason for temporary acceptance:
Reachability/exposure:
Compensating controls:
Expiry or review date:
Follow-up issue or PR:
Evidence links:
```

Acceptance rules:

- Critical and High findings require repository-owner approval.
- Medium findings require owner approval and a review date.
- Acceptance must be temporary unless a future owner explicitly moves the item into the residual-risk register.
- `.trivyignore`, dependency allowlists, or CodeQL dismissals must not be used as a substitute for the template.

## False-Positive Rationale Template

Use this format when a finding does not apply:

```text
False positive:
Finding/tool:
Severity:
Affected package/path:
Owner:
Why the finding does not apply:
Evidence reviewed:
Tests or checks run:
Review date:
Reopen condition:
Evidence links:
```

False-positive rules:

- The rationale must name the exact code path, dependency scope, package version, container layer, or tool metadata that makes the finding non-applicable.
- A false-positive decision should include a reopen condition such as dependency upgrade, code-path exposure, container-base update, or tool-rule update.
- High and Critical false positives still require repository-owner review.

## Remediation SLA Categories

These are review targets for this repository's production-candidate posture, not legal commitments:

- Critical reachable runtime finding: fix or owner-approved temporary acceptance before any release-ready claim; target fix within 7 days.
- High reachable runtime finding: fix or owner-approved temporary acceptance before production-candidate or release-ready claim; target fix within 14 days.
- Medium reachable runtime finding: target fix or documented plan within 30 days.
- Low or informational finding: review during the next dependency/security maintenance window.
- Build-only, test-only, or unreachable finding: document scope and review during the next maintenance window unless exploitability changes.
- License/policy blocker from Dependency Review: resolve or owner-accept before merge when it affects distributed artifacts.

If a target cannot be met, keep the risk visible in the PR, issue, release review packet, or residual-risk register and include the next review date.

## Evidence Locations

Primary evidence locations:

- PR checks: CI, Dependency Review, CodeQL, and Trivy check output.
- CI artifacts: `loadbalancerpro-sbom`, `packaged-artifact-smoke`, and `jacoco-coverage-report`.
- Release review: `docs/PRODUCTION_CANDIDATE_EVIDENCE_GATE.md` and `docs/RELEASE_CANDIDATE_DRY_RUN.md`.
- Supply-chain posture: `evidence/SUPPLY_CHAIN_EVIDENCE.md`.
- Security posture: `evidence/SECURITY_POSTURE.md`.
- Residual risks: `evidence/RESIDUAL_RISKS.md`.
- Container signing/publication decisions: `docs/CONTAINER_SIGNING_DECISION_RECORD.md`.

Keep detailed tool output in GitHub checks, GitHub alerts, PR comments, issues, or ignored `target/` evidence. Do not paste secrets, private tenant IDs, real tokens, internal hostnames, or sensitive customer data into tracked documentation.

## Release Gate Use

Before calling a revision production-candidate or release-ready:

- CodeQL must pass or every finding must have a recorded owner decision.
- Dependency Review must pass or every non-blocking finding must have a recorded owner decision.
- Trivy must pass for fixed high/critical findings or every exception must have a recorded owner decision.
- SBOM review must not show unowned dependency drift in sensitive areas such as Spring Security/OAuth2, AWS SDK, Micrometer/telemetry, Log4j, JSON/CSV parsing, build plugins, or GitHub Actions.
- Accepted high or critical findings must appear in the release review packet or residual-risk register with owner, severity, expiry, and follow-up.

Do not treat a green tool result as proof that dependencies are vulnerability-free or that SAST has found every security issue. The gate proves review discipline and current automated evidence, not complete security.
