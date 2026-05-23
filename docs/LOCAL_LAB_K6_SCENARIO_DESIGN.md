# Local Lab k6 Scenario Design

This document is a docs/test-only future k6 scenario design spec. It converts the k6 lane from the local-lab boundary plan into concrete future scenario shapes without adding tool files.

No k6 scripts are added in this PR. No load/stress/benchmarking is implemented. No throughput/p95/p99 evidence is produced. No Docker Compose files are added in this PR. No actual tool execution occurs in this PR.

Future k6 scripts must target local lab endpoints first. Future k6 scripts must not target production. Future k6 work must be separately scoped. Future k6 results must be labeled local/test-scope unless separately validated. Future tool work must be separately scoped. Future tool work must target local/lab endpoints first.

## Relationship To Current Local Lab

The current local lab already has deterministic test-scope loopback harnesses, smoke client coverage, traffic matrix coverage, bounded burst coverage, in-memory summaries, reviewer checklist mappings, and handoff docs. This design only names future k6 scenario shapes that could reuse those bounded local-lab concepts after a separate implementation review.

This design does not add k6 scripts, Docker/Docker Compose files, scripts, CI jobs, Maven dependencies, production endpoints, production listeners, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence.

## Future Scenario Shapes

| Future scenario shape | Local-lab source concept | Design-only expectation |
| --- | --- | --- |
| healthy baseline smoke scenario | `HEALTHY_FAST` fixture and loopback smoke coverage | would confirm a future local-only k6 path can issue bounded healthy requests without production targets |
| slow/tail-latency scenario | `SLOW_TAIL_LATENCY` fixture and matrix case | would model slow response labels without making latency benchmark claims |
| partial degradation scenario | `PARTIAL_DEGRADATION` fixture and matrix case | would keep degraded backend labels local/test-scope only |
| error-prone backend scenario | `ERROR_PRONE` fixture and matrix case | would exercise expected error labels without production validation language |
| queue-pressure/overloaded scenario | `OVERLOADED_QUEUE_PRESSURE` fixture and matrix case | would keep queue-pressure labels deterministic and not claim throughput evidence |
| all-unhealthy/no-good-choice scenario | `ALL_UNHEALTHY_OR_NO_GOOD_CHOICE` fixture and boundary response | would preserve reviewer boundary language for no-good-choice outcomes |
| recovery scenario | `RECOVERY` fixture and matrix case | would model local recovery labels without runtime enforcement proof |
| bounded burst scenario | bounded request burst smoke coverage | would remain fixed-count and local/test-scope first, not load/stress/benchmark testing |
| matrix scenario | deterministic traffic matrix coverage | would preserve deterministic scenario ordering and fixture/boundary matching |

## Future k6 Boundaries

- future k6 scripts must target local lab endpoints first;
- future k6 scripts must not target production;
- future k6 work must be separately scoped;
- future k6 results must be labeled local/test-scope unless separately validated;
- future k6 work must not claim load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence unless a later lane explicitly implements and validates that scope;
- future k6 work must stop if it needs production endpoint wiring, non-loopback targets, fixed ports, Maven dependency changes, scripts outside the approved lane, CI changes, replay execution, evidence/report generation, storage, export, or production-validation language.

## Shared Reviewer Boundaries

These scenario shapes are design-only. They provide no actual tool execution, no production proof, no live-cloud proof, no real-tenant proof, no runtime enforcement proof, no replay execution, no evidence/report generation, no storage/export behavior, no production traffic, and no autonomous traffic shifting.

The following remain not proven: production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, Docker/k6/Bruno/Toxiproxy implementation, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, p95/p99 evidence, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, and facility automation.
