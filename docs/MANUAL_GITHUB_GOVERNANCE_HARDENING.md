# Manual GitHub Governance Hardening

## Verdict

This is repo-side governance hardening. It documents and prepares recommended GitHub governance settings, CODEOWNERS coverage, reviewer ownership, and future evidence expectations.

This sprint does not mutate GitHub settings, rulesets, branch protection, repository settings, secrets, environments, or Actions settings. It does not certify LoadBalancerPro as enterprise-production ready.

LoadBalancerPro remains **Enterprise Lab ready**, **not production certified**, and **not enterprise-production ready**.

## Current Observed State

Read-only audit on 2026-05-14 observed:

- Repository: `RicheyWorks/LoadBalancerPro`.
- Default branch: `main`.
- Active repository ruleset: `Protect main`, targeting `refs/heads/main`.
- Required status checks: `Build, Test, Package, Smoke` and `Analyze Java (java-kotlin)`.
- Non-fast-forward updates are blocked by ruleset.
- Pull-request rule allows merge, squash, and rebase methods.
- Required approving review count is `0`.
- CODEOWNERS review is not enforced by the observed ruleset.
- Stale-review dismissal on push is `false`.
- Last-push approval and review-thread resolution are `false`.
- Classic branch-protection endpoint returned `404 Branch not protected`; protection is supplied through repository rulesets rather than classic branch protection.
- Collaborator audit returned `@RicheyWorks` with admin permissions.

Branch deletion protection was not confirmed by this audit.

## Recommended Target State

manual settings change required.

Recommended target:

- Require at least 1 approving review before merge.
- Enable CODEOWNERS review for sensitive paths.
- Keep required CI check: `Build, Test, Package, Smoke`.
- Keep required CodeQL check: `Analyze Java (java-kotlin)`.
- Block force pushes.
- Block branch deletion.
- Decide whether stale-review dismissal applies.
- Decide whether administrators are included.
- Decide bypass actors, if any.
- Decide whether signed commits should be required later.

This is a recommended target state, not applied in this sprint.

## Sensitive Path Ownership Model

| Path | Owner | Why review matters |
| --- | --- | --- |
| `/.github/` | `@RicheyWorks` | Repository automation, dependency policy, and ownership metadata can change review and release behavior. |
| `/.github/workflows/` | `@RicheyWorks` | CI, CodeQL, release-artifact, container-smoke, Trivy, SBOM, and artifact upload behavior live here. |
| `/.github/CODEOWNERS` | `@RicheyWorks` | Ownership changes can alter review routing once CODEOWNERS review is enabled manually. |
| `/src/main/` | `@RicheyWorks` | Production Java code changes affect runtime behavior, auth boundaries, cloud guardrails, and proxy behavior. |
| `/src/test/` | `@RicheyWorks` | Tests and static guardrails protect readiness, security, and no-overclaim boundaries. |
| `/docs/` | `@RicheyWorks` | Readiness, production-candidate, governance, release, container, and operator claims live in public docs. |
| `/evidence/` | `@RicheyWorks` | Evidence posture and residual-risk claims must stay aligned with repo-supported proof. |
| `/Dockerfile` | `@RicheyWorks` | Container defaults, runtime user, exposed ports, healthcheck, and base-image trust live here. |
| `/pom.xml` | `@RicheyWorks` | Build, dependency, plugin, Java, and packaging changes affect CI and supply-chain posture. |
| `/README.md` | `@RicheyWorks` | The top-level reviewer entry point must preserve the Enterprise Lab and not-production-certified boundary. |

## Manual Settings Checklist

- [ ] Verify default branch is `main`.
- [ ] Verify active ruleset target is `refs/heads/main`.
- [ ] Set required approving reviews to `1` or more.
- [ ] Enable CODEOWNERS review.
- [ ] Keep required checks: `Build, Test, Package, Smoke` and `Analyze Java (java-kotlin)`.
- [ ] Confirm stale-review dismissal setting.
- [ ] Confirm force-push protection.
- [ ] Confirm branch-deletion protection.
- [ ] Confirm bypass policy and administrator inclusion.
- [ ] Document final settings in `docs/ENTERPRISE_READINESS_AUDIT.md` after they are actually applied.

## Stale-Review Dismissal Decision

Pros:

- Safer for production code, workflow, CODEOWNERS, dependency, auth, cloud, container, release, and security-sensitive changes.
- Forces re-review when the reviewed diff no longer matches the merge candidate.
- Helps prevent review approval from carrying across meaningful post-review edits.

Cons:

- Can slow AI-assisted iteration when small follow-up commits are pushed.
- Can require re-review after trivial docs-only wording changes.
- Can create review fatigue if the repository uses one broad rule for all paths.

Recommended default:

- Enable stale-review dismissal for production code, workflows, CODEOWNERS, build metadata, Dockerfile, security-sensitive docs, evidence docs, and release/container governance paths.
- Consider more relaxed handling for docs-only changes if GitHub ruleset capabilities support path-specific policy without weakening sensitive-path review.

This is a recommendation only and is not applied by this sprint.

## Governance Evidence Expectations

Future governance evidence should include:

- Screenshot or exported ruleset JSON.
- Current `.github/CODEOWNERS` file.
- Required check list.
- PR showing CODEOWNERS review behavior after settings are manually applied.
- Readiness audit update.
- Date and commit of governance review.

## Non-Goals

- No GitHub settings mutation in this sprint.
- No ruleset mutation in this sprint.
- No branch protection mutation in this sprint.
- No secrets or environments mutation.
- No release, tag, or GitHub Release.
- No registry publish or signing.
- No production certification claim.
- No live cloud, private-network, or real tenant proof.

## Reviewer Checklist

- [ ] CODEOWNERS exists.
- [ ] Sensitive paths are covered.
- [ ] Governance recommendations are documented.
- [ ] Current state is documented.
- [ ] Manual settings checklist is present.
- [ ] No wording claims settings were applied unless verified.
- [ ] Readiness and reviewer docs link this governance packet.
- [ ] Static tests protect against governance overclaims.

## Safety Boundary

LoadBalancerPro remains **Enterprise Lab ready**, **not production certified**, and **not enterprise-production ready**.

Governance hardening is prepared repo-side. It is not applied through GitHub settings by this sprint.
