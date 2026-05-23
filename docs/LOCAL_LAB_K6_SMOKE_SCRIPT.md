# Local Lab k6 Smoke Script

This document describes the first separately scoped optional local-lab k6 smoke script skeleton: [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js).

The script is local-lab-only tooling. It targets an already-running local app or local-lab-owned endpoint only. It is not CI-gated. It is not Dockerized. It is not a benchmark. It is not a stress test. It is not load testing. It does not prove throughput, p95, p99, production readiness, production certification, live-cloud validation, or real-tenant validation.

The default base URL is `http://127.0.0.1:8080`. The script must not target external hosts by default. Reviewers may set `LOCAL_LAB_BASE_URL` only for local/lab-owned loopback endpoints, such as another `127.0.0.1` or `localhost` port. Non-loopback targets require a separate review and must not be treated as default behavior.

## Relationship To The Design Specs

This smoke skeleton is the first narrow step after the docs/test-only design phase:

- [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md) defines future k6 scenario shapes and now points at this first optional smoke skeleton.
- [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md) remains design-only; no Bruno collection files are added here.
- [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md) remains design-only; no Toxiproxy config is added here.

This sprint does not add Docker, Docker Compose, Bruno collections, Toxiproxy config, CI workflow changes, Maven dependency changes, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage, export, upload, download, PDF, or ZIP behavior.

## Manual Use Boundary

The app or local-lab endpoint must already be running before a reviewer manually invokes k6. Maven tests do not run this script, and CI does not run this script.

Example local-only manual command:

```powershell
k6 run lab/k6/local-lab-smoke.js
```

Example loopback override for a locally owned port:

```powershell
$env:LOCAL_LAB_BASE_URL = "http://127.0.0.1:8080"
k6 run lab/k6/local-lab-smoke.js
```

The script performs a tiny smoke walkthrough against `/actuator/health` and checks only that the endpoint responds with status `200` and a body. The tiny request count is for local smoke confidence only. It is not throughput evidence, not p95 evidence, not p99 evidence, not production proof, not live-cloud proof, and not real-tenant proof.

## Reviewer Stop Conditions

Stop before merge or use if:

- the default target is changed away from `127.0.0.1` or `localhost`;
- an external host appears as a default;
- the script becomes automatic Maven or CI execution;
- Docker or Docker Compose is added;
- Bruno collections or Toxiproxy config are added;
- production endpoint wiring, production listeners, or production runtime behavior changes are required;
- documentation starts claiming benchmark, stress, load, throughput, p95, p99, production readiness, production certification, live-cloud validation, or real-tenant validation evidence.

## Remaining Not-Proven Boundaries

The following remain not proven: production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, Docker/Bruno/Toxiproxy implementation, Docker Compose implementation, expanded k6 scenario implementation, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, p95 evidence, p99 evidence, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, and facility automation.
