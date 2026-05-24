# Local Lab Manual Tooling Index

This docs/test-only documentation-only index gives reviewers one bounded place to inspect the current optional local-lab manual tooling. It does not add automated execution, Docker/Compose orchestration, CI-gating, new endpoints, new harness/client/server implementation, runtime app behavior, replay execution, evidence/report generation, storage, or export behavior.

## What Exists Now

| Tool skeleton | Local path | Individual doc | Current boundary |
| --- | --- | --- | --- |
| k6 local-lab smoke script | [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js) | [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md) | optional, manual-only, local-lab-only smoke walkthrough against an already-running loopback/local app endpoint |
| Bruno local-lab collection | [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/) | [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md) | optional, manual-only, local-lab-only request collection against an already-running loopback/local app endpoint |
| Toxiproxy local-lab config | [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json) | [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md) | optional, manual-only, local-lab-only loopback proxy config skeleton that does not start Toxiproxy or the application |

The supporting boundary and design docs remain [`LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md), [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md).

## Shared Boundaries

All three tool skeletons are optional, manual-only, local-lab-only reviewer aids. They use loopback/local defaults and must keep default targets at `127.0.0.1` or `localhost`. They are not CI-gated, not Dockerized, not Docker Compose orchestration, not wired into Maven, not wired into production runtime, and not production traffic automation.

The tools are separate from one another: the k6 script does not run Bruno or Toxiproxy; the Bruno collection does not run k6 or Toxiproxy; the Toxiproxy config does not run k6 or Bruno. None of these files should be treated as automatic Maven execution, CI execution, Docker execution, Docker Compose execution, replay execution, report generation, storage, export, or production runtime behavior.

## Reviewer Checklist

- Confirm each default target is `127.0.0.1` or `localhost`.
- Confirm the reviewer understands the app and any chosen tool must be started manually if they choose to run it.
- Confirm no external endpoint is used by default.
- Confirm no throughput evidence, no p95/p99 evidence, and no load/stress/benchmark evidence is claimed.
- Confirm no production readiness/certification conclusion is drawn.
- Confirm no live-cloud or real-tenant validation is claimed.
- Confirm no runtime enforcement is claimed.
- Confirm no replay execution, evidence/report generation, storage, or export behavior is claimed.

## Suggested Manual Review Order

1. Read [`LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md) and the design docs first.
2. Inspect [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js) and [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).
3. Inspect [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/) and [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
4. Inspect [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json) and [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).
5. Use the existing Maven documentation guard tests and local-lab docs as boundary evidence; actually running k6, Bruno, or Toxiproxy is not required for this review.

## Remaining Not-Proven Boundaries

The following remain not proven:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- Docker/k6/Bruno/Toxiproxy platform implementation;
- replay execution;
- evidence/report generation;
- storage/export behavior;
- load testing;
- stress testing;
- benchmarking;
- throughput evidence;
- p95/p99 evidence;
- autonomous production traffic shifting;
- carbon-aware routing;
- GPU orchestration;
- power/grid control;
- facility automation.
