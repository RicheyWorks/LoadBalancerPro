# Branch Hygiene Inventory

Audit timestamp: 2026-05-12 03:55:37 -07:00

Audited remote: `origin`

Audited `origin/main`: `fdde2f48b0fc1db1d82130032e17d8d175915b3e`

This is a read-only governance inventory. No branches were deleted, no rulesets were changed, no default branch settings were changed, and no release or asset operation was performed.

Owner approval required before deletion. Treat this document as a review packet, not an execution script.

## Summary

| Bucket | Count | Rationale |
| --- | ---: | --- |
| KEEP | 1 | `origin/main` is the default branch and must remain untouched. |
| DELETE CANDIDATE | 119 | Branch is `origin/codex/*`, is merged into `origin/main`, has no open PR at audit time, and is not a backup, release, feature, or unknown-purpose branch. |
| UNKNOWN / DO NOT TOUCH | 5 | Branch is backup, release/history, feature, or not merged into `origin/main`; preserve until the owner reviews intent and retention needs. |

Total remote branches audited: 125

Merged into `origin/main`: 124

Not merged into `origin/main`: 1

## KEEP

- `origin/main`

## DELETE CANDIDATE

These branches are safe-looking candidates for a later owner-approved cleanup batch. They were not deleted in this sprint.

- `origin/codex/adaptive-load-shedding`
- `origin/codex/allocation-api-hardening`
- `origin/codex/api-404-envelope-test`
- `origin/codex/api-abuse-hardening`
- `origin/codex/api-contract-openapi`
- `origin/codex/api-key-security-audit`
- `origin/codex/api-observability-coverage`
- `origin/codex/api-requested-strategy-order-test`
- `origin/codex/async-import-export-lifecycle-characterization`
- `origin/codex/browser-demo-polish`
- `origin/codex/browser-demo-run-sequence`
- `origin/codex/browser-evidence-training-demo`
- `origin/codex/ci-artifact-consumer-guide`
- `origin/codex/ci-packaged-artifact-smoke`
- `origin/codex/cloud-sandbox-fail-closed-defaults`
- `origin/codex/cockpit-first-impression`
- `origin/codex/cockpit-visual-polish`
- `origin/codex/codeql-main-only-triggers`
- `origin/codex/codeql-main-trigger`
- `origin/codex/consistent-hash-ring-test-coverage`
- `origin/codex/container-deployment-hardening`
- `origin/codex/core-routing-integration-coverage`
- `origin/codex/csrf-disposition-proof`
- `origin/codex/decomposition-plan-refresh`
- `origin/codex/dependabot-target-main`
- `origin/codex/dependency-ci-hygiene`
- `origin/codex/deployment-smoke-kit`
- `origin/codex/deprecated-shim-doc-refresh`
- `origin/codex/document-csrf-posture`
- `origin/codex/enterprise-proxy-foundation`
- `origin/codex/evidence-catalog-diff`
- `origin/codex/evidence-handoff-policy`
- `origin/codex/evidence-inventory-cli`
- `origin/codex/evidence-policy-example-catalogs`
- `origin/codex/evidence-policy-templates`
- `origin/codex/evidence-policy-walkthrough`
- `origin/codex/extract-cloud-metrics-coordinator`
- `origin/codex/extract-cloud-metrics-coordinator-test-plan`
- `origin/codex/extract-consistent-hash-ring`
- `origin/codex/final-docs-cleanup`
- `origin/codex/fix-dockerfile-maven-comment`
- `origin/codex/future-release-automation`
- `origin/codex/gui-postman-training-onboarding`
- `origin/codex/incident-bundle-export`
- `origin/codex/incident-evidence-redaction`
- `origin/codex/incident-fixture-catalog`
- `origin/codex/javafx-17-0-19-validation`
- `origin/codex/javafx-patch-validation`
- `origin/codex/lightweight-reverse-proxy-mode`
- `origin/codex/loadbalancer-characterization-tests`
- `origin/codex/loadbalancer-decomposition-plan-refresh`
- `origin/codex/loadbalancer-lifecycle-characterization`
- `origin/codex/load-distribution-engine-audit`
- `origin/codex/local-artifact-verification`
- `origin/codex/netty-4.1.133-security-override`
- `origin/codex/oauth2-auth-hardening-tests`
- `origin/codex/observability-hardening`
- `origin/codex/offline-cli-audit-log`
- `origin/codex/offline-evidence-training-lab`
- `origin/codex/offline-evidence-training-scorecards`
- `origin/codex/offline-remediation-report-cli`
- `origin/codex/openapi-api-docs-polish`
- `origin/codex/operator-api-contract-trace`
- `origin/codex/operator-auth-tls-boundary`
- `origin/codex/operator-comparison-matrix`
- `origin/codex/operator-demo-usability`
- `origin/codex/operator-distribution-smoke-kit`
- `origin/codex/operator-explanation-drilldown`
- `origin/codex/operator-guided-walkthrough`
- `origin/codex/operator-install-run-matrix`
- `origin/codex/operator-navigation-readiness`
- `origin/codex/operator-observability-dashboard`
- `origin/codex/operator-remediation-planner`
- `origin/codex/operator-replay-mode`
- `origin/codex/operator-review-packet`
- `origin/codex/operator-run-profiles`
- `origin/codex/operator-scenario-gallery`
- `origin/codex/optional-javafx-documentation`
- `origin/codex/packaged-proxy-demo-launcher`
- `origin/codex/packaging-operator-polish`
- `origin/codex/performance-regression-baseline`
- `origin/codex/phase-1-repo-connection-repair`
- `origin/codex/proxy-config-hot-reload`
- `origin/codex/proxy-demo-stack-usability`
- `origin/codex/proxy-health-metrics-demo`
- `origin/codex/proxy-operator-status-ui`
- `origin/codex/proxy-resilience-retry-cooldown`
- `origin/codex/proxy-strategy-demo-lab`
- `origin/codex/quantified-testing-credibility`
- `origin/codex/read-only-concurrency-evaluation-api`
- `origin/codex/readme-visibility-polish`
- `origin/codex/real-backend-proxy-examples`
- `origin/codex/refresh-auth-hardening-plan`
- `origin/codex/refresh-enterprise-security-docs`
- `origin/codex/release-candidate-dry-run`
- `origin/codex/release-dry-run`
- `origin/codex/remediation-report-export`
- `origin/codex/report-manifest-checksums`
- `origin/codex/request-level-weighted-least-connections`
- `origin/codex/request-level-weighted-round-robin-strategy`
- `origin/codex/resilience-failover-api`
- `origin/codex/reviewer-trust-map`
- `origin/codex/routing-decision-demo`
- `origin/codex/routing-openapi-contract-tests`
- `origin/codex/routing-strategy-audit-refresh`
- `origin/codex/scenario-replay-regression-diff`
- `origin/codex/security-posture-refresh`
- `origin/codex/server-health-coordinator-audit`
- `origin/codex/server-monitor-lifecycle-test-coverage`
- `origin/codex/server-registry-extraction-audit`
- `origin/codex/server-registry-test-coverage`
- `origin/codex/servermonitor-lifecycle-characterization`
- `origin/codex/servermonitor-timing-cleanup`
- `origin/codex/smoke-kit-failure-triage`
- `origin/codex/traffic-replay-simulation`
- `origin/codex/unified-load-balancing-cockpit`
- `origin/codex/update-contributing-main`
- `origin/codex/update-decomposition-plan`
- `origin/codex/warning-cleanup-audit`

## UNKNOWN / DO NOT TOUCH

These branches are intentionally excluded from cleanup candidates.

- `origin/backup/pre-normalization-main-2026-05-09` - backup branch and the only audited branch not merged into `origin/main`.
- `origin/feature/v2.4.0-package-namespace-migration` - feature/history branch; merged status alone is not enough to delete without owner review.
- `origin/release/v1.1.0-hardening-review` - release/history branch.
- `origin/release/v1.1.1-version-alignment` - release/history branch.
- `origin/release/v1.2.0-routing-engine` - release/history branch.

## Future Cleanup Template

Use a separate owner-approved execution sprint. Do not run cleanup from this document.

- Suggested batch size: 20 to 30 branches per batch.
- Before each batch, re-run `git fetch origin --prune --tags`, `git branch -r --merged origin/main`, `git branch -r --no-merged origin/main`, and `gh pr list --repo RicheyWorks/LoadBalancerPro --state open --limit 50`.
- Template for a single approved deletion: `# git push origin --delete <branch-name>`
- After each batch, verify `git fetch origin --prune --tags`, `git branch -r --sort=-committerdate --format="%(committerdate:short) %(refname:short)"`, main CI, main CodeQL, and open PR count.

## Future Cleanup Rules

- Do not delete `origin/main`.
- Do not delete backup branches without explicit owner approval.
- Do not delete release/history branches without explicit owner approval.
- Do not delete unmerged branches without explicit owner approval.
- Do not use an executable mass-delete script.
- Do not change rulesets, branch protection, default branch settings, workflows, or release posture as part of branch cleanup.
