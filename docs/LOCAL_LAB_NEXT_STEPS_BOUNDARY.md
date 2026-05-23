# Local Lab Next Steps Boundary

This document is an end-of-day checkpoint for the local-lab work. It is reviewer handoff text only. It does not add runtime behavior, harness behavior, clients, servers, listeners, tools, storage, export, replay, or report generation.

## Current Local-Lab Stack

The current local-lab stack is:

1. ADR-0009 local lab plan.
2. Local lab scenario matrix.
3. Passive fake backend scenario model/catalog.
4. Passive response fixtures.
5. Passive request/response transcript fixtures.
6. Passive transcript summary renderer.
7. Passive reviewer checklist mapping.
8. Implementation readiness gate.
9. Test-scope in-memory fake backend handler.
10. Handler/transcript consistency tests.
11. Test-scope loopback fake backend server harness.
12. Loopback lifecycle hardening.
13. Test-scope multi-backend loopback harness.
14. Multi-backend transcript alignment tests.
15. Deterministic loopback traffic smoke client.
16. In-memory traffic smoke summary renderer.
17. Traffic smoke reviewer checklist mapping.
18. Deterministic traffic matrix tests.
19. Traffic matrix summary renderer.
20. Traffic matrix reviewer checklist mapping.
21. Local-lab progress handoff docs.

## What Is Actually Proven Today

- deterministic test-scope local-lab models exist;
- loopback-only fake backend servers can run in tests;
- multi-backend loopback harness can run with OS-assigned ephemeral ports;
- smoke client can call only `127.0.0.1` loopback harness URLs;
- traffic matrix can cover the required local-lab profiles;
- reviewer checklist and handoff docs explain evidence boundaries;
- all current evidence is local/test-scope only.

## What Remains Not Proven

- not production readiness;
- not production certification;
- not live-cloud validation;
- not real-tenant validation;
- not runtime enforcement;
- not production traffic behavior;
- not Docker/k6/Bruno/Toxiproxy execution;
- not replay execution;
- not evidence/report generation;
- not storage/export behavior;
- not autonomous production traffic shifting;
- not carbon-aware routing;
- not GPU orchestration;
- not power/grid control;
- not facility automation.

## Next Safe Implementation Lanes

- Lane A: docs-only k6/Bruno/Toxiproxy implementation plan.
- Lane B: test-scope bounded request burst smoke test, still loopback-only.
- Lane C: test-scope fault-style fixture expansion, no Toxiproxy yet.
- Lane D: docs-only Docker Compose design boundary, no compose file yet.
- Lane E: future actual Docker/k6/Bruno PR only after a separate boundary plan.

Each lane must be separately scoped and reviewed. A future lane should stop before merge if it needs production code, production endpoint wiring, Maven dependencies, fixed ports, non-loopback hosts, Docker/k6/Bruno/Toxiproxy implementation, replay execution, evidence/report generation, file writing, storage, export, or production-validation language.

## Stop Conditions / Safety Gates

- stop if a non-loopback host appears;
- stop if a fixed port appears;
- stop if `src/main/java` changes are needed;
- stop if production endpoint wiring appears;
- stop if Maven dependencies are required;
- stop if Docker/k6/Bruno/Toxiproxy implementation sneaks in;
- stop if docs start claiming production validation.

## Handoff Boundary

Passing the current local-lab checks means the test-scope local-lab evidence chain is coherent enough for separately scoped next steps. It does not mean production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, production traffic behavior, replay execution, evidence/report generation, storage/export behavior, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, or facility automation.
