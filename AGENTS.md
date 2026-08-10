# Agent Operating Rules

This is the authoritative repository rule source for Codex and other engineering agents. A user prompt or pull-request
description may define the task contract; [`BUILD_CONTRACT.md`](BUILD_CONTRACT.md) is an optional short template.

## Engineering Priority

- Deliver the smallest complete engineering outcome inside the user's scope.
- Prefer runtime behavior, secure configuration, deterministic tests, infrastructure, deployment capability,
  benchmarks, and reproducible lab evidence over governance prose.
- Preserve existing valid work and unrelated user changes. Never overwrite or abandon an active branch or pull request
  to simplify a new task.
- For implementation campaigns, keep at least 90% of substantive effort in engineering surfaces. Routine documentation
  stays at or below 10%.
- Documentation-only work is appropriate only when explicitly requested or needed to correct a material false claim,
  broken operator/security instruction, or legal/compliance requirement.

## Safety Invariants

- Keep authentication, authorization, TLS verification, secret handling, private-network validation, dependency and
  image vulnerability gates, data integrity, concurrency bounds, failure containment, rollback, and destructive-action
  safeguards intact.
- Do not introduce credentials, customer data, production targets, billable cloud actions, or production-looking
  defaults. Use mocks and loopback fixtures unless the user explicitly authorizes a reviewed external target.
- Do not bypass required checks, branch protection, dependency review, CodeQL, SBOM generation, image scanning, or
  pinned workflow controls.
- Do not claim production readiness, certification, live-cloud or real-tenant proof, capacity, latency, or SLO evidence
  beyond what executable verification actually establishes.
- Stop before an irreversible action when the exact target or recovery path is uncertain.

## Scope And Change Discipline

- Inspect the request, current Git state, active pull requests, and relevant code before editing.
- Keep changes close to the requested behavior. Do not mix unrelated refactors or dependency updates.
- Product behavior may change when the task explicitly scopes it; docs/test-only is not the default.
- Documentation required to explain a behavior, configuration, security posture, operator procedure, or material
  architectural decision may ship in the same pull request as the implementation.
- Treat campaign boards and manifests as operational state, not evidence. Update them only at slot start, PR opening,
  merge completion, or a genuine blocker.
- Record failures in repository prose only when they expose an unresolved defect, persistent-state risk, invalid
  evidence, or a reusable engineering lesson.

## Risk-Proportional Verification

- During development, run the smallest focused tests covering the changed behavior.
- Before a pull request, run affected integration/contract tests. Run the full suite when shared runtime, security,
  build, or broad governance/test surfaces changed.
- Package when packaging/runtime resources changed or when preparing a merge candidate.
- Use required GitHub checks as the exact-head remote source of truth. Never accept stale, failed, cancelled, skipped-only,
  duplicate-only, or pending required evidence.
- Merge normally only when the current pull-request head is reviewed, mergeable, and all applicable required checks are
  green. Verify merge-main CI and CodeQL before calling main green.
- Do not repeat an unchanged expensive local check merely to populate a report field.

See [`docs/agent/VERIFICATION_PROTOCOL.md`](docs/agent/VERIFICATION_PROTOCOL.md) for the compact verification matrix.

## Documentation Policy

- Documentation describes executable evidence; it does not create evidence.
- Change docs when user-facing behavior, configuration, security posture, operator procedure, or a material architecture
  decision changes. Keep prose current, concise, and link to the executable source of truth.
- Do not store transient branches, SHAs, polling, CI narration, or completed campaign history in README or reviewer docs.
- Tests may protect executable safety invariants and essential links, but must not freeze positive boilerplate, historical
  PRs/SHAs, repeated limitations, or cross-link webs.

## Genuine Stop Conditions

Stop and report when scope or authority is insufficient, a security invariant conflicts with the request, a required
check fails without a safe in-scope repair, protected workflow state is ambiguous, data integrity is at risk, or an
irreversible action needs a user decision. Obsolete prose or an exact-wording test is not a stop condition.

## Reporting

Normally report only the outcome, PR, exact final head or merge, important verification, a genuine blocker or remaining
risk, and the next engineering action. Add detail only when the task's risk requires it.
