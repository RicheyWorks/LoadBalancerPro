# Evidence Audit Campaign Contract

This contract governs the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation/test guidance only. It does not add production hardening, automation, CI/Maven wiring, Dockerfile changes, Compose behavior, runtime behavior, endpoints, secrets, external/cloud/tenant targets, runner services, or production claims.

## Campaign Goal

Audit the current GitHub repository state after the completed 10-PR Goal Mode Trial, repair stale docs/test-only campaign state, and produce reviewer-ready evidence across repository claims, CI, CodeQL, Maven, Docker, Compose, local-lab, runtime configuration, smoke evidence, open PR hygiene, and remaining not-proven boundaries.

## Scope

Allowed default surfaces:

- README.md;
- AGENTS.md;
- BUILD_CONTRACT.md;
- docs/**;
- src/test/java/** documentation guard tests or read-only test-scope audit guards.

Forbidden unless explicitly separately approved:

- src/main/java;
- Maven config;
- CI/workflow files;
- Dockerfile;
- Compose files or Compose behavior;
- app behavior;
- endpoints;
- k6 behavior;
- Bruno behavior;
- Toxiproxy behavior;
- scripts;
- runtime resources;
- runner services;
- automation;
- secrets;
- external/cloud/tenant targets;
- production-looking defaults.

## Campaign Rules

- Work one scoped PR at a time.
- Do not open a later slot before the current slot merges and main CI/CodeQL are green.
- Keep changes docs/test-only by default.
- Add or update a documentation guard test when practical.
- Update SESSION_MANAGER.md after branch creation, local verification, PR creation, merge, and post-merge main verification checkpoints.
- Log every local, remote, scope, or tooling failure in FAILURE_LOG.md before continuing.
- Use focused verification while editing.
- Run full local verification before opening or merging.
- Merge only when latest required checks are green for the current PR head SHA.
- Main CI/CodeQL must be green after merge before the slot counts.

## Verification Levels

Each slot should run:

- `git status`;
- `git diff --name-status origin/main...HEAD`;
- `git diff --stat origin/main...HEAD`;
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"`;
- focused documentation guard for the current slot;
- relevant audit/campaign selector bundle;
- `mvn -q test`;
- `mvn -q "-DskipTests" package`;
- `mvn -B package`;
- `git diff --check`;
- `git diff --check origin/main...HEAD`;
- `git diff --cached --check`;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package`.

Remote verification must include current-head Build/Test/Package/Smoke, Analyze Java / CodeQL, and Dependency Review where applicable. Failed, cancelled, stale, pending, missing, or duplicate-only required checks are not acceptable.

## Twenty PR Slots

1. Prior 10-PR closeout repair.
2. Open PR hygiene audit.
3. Repository evidence map.
4. CI workflow audit.
5. CodeQL and dependency-review audit.
6. Maven/dependency posture audit.
7. Dockerfile runtime audit.
8. Compose/local-lab audit.
9. Runtime configuration audit.
10. Proxy demo fixture audit.
11. CLI mode and app startup audit.
12. Enterprise lab workflow smoke audit.
13. README and Reviewer Trust claim audit.
14. Local-lab test-scope chain audit.
15. k6 / Bruno / Toxiproxy future-only audit.
16. Guard-test fragility and failure-log audit.
17. Branch and PR hygiene audit.
18. Production-readiness gap matrix.
19. Human reviewer packet.
20. Final 20-PR audit closeout.

## Stop Conditions

Stop and report before pushing or merging if:

- scope expands beyond docs/test-only or approved test-scope guards;
- production behavior changes;
- src/main/java changes appear unexpectedly;
- Maven config, CI/workflow, Dockerfile, Compose, scripts, runtime resources, endpoints, secrets, external/cloud/tenant targets, runner services, or automation changes appear unexpectedly;
- safety wording is removed or weakened;
- required checks fail, are stale, are cancelled, or remain pending;
- the campaign board/session state cannot be made factual;
- a requested change conflicts with the README trust contract or not-proven boundaries.

## Not-Proven Boundaries

This audit campaign does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation.
