# Operator Evidence Training Demo Walkthrough

This walkthrough gives reviewers and operators one local path through health checks, the first-party browser demos, evidence training onboarding, routing decision comparison, unified load-balancing cockpit review, Postman import, packaged policy discovery, answer templates, and deterministic scorecard grading.

The demo is a local/operator training and review aid only. It is not certification, not benchmark proof, not legal compliance proof, and not identity proof. The training, onboarding, routing comparison, allocation preview, and read-only evaluation routes do not mutate cloud state and do not require `CloudManager`.

## 1. Start The API

Build the JAR if needed:

```bash
mvn -B -DskipTests package
```

Start the local API on the Postman default port:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=local
```

During development, the existing Maven startup path is also available:

```bash
mvn spring-boot:run
```

If local Maven dependency resolution is blocked by workstation certificate trust, use GitHub CI as the verification source of truth and start from a previously built JAR.

## 2. Check Health And Readiness

Verify the app health endpoint:

```bash
curl -fsS http://127.0.0.1:8080/api/health
```

Verify Actuator health and readiness where the local profile exposes them:

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
```

If a readiness route is unavailable in a custom profile, confirm `/api/health` and `/actuator/health` first, then review the active profile's Actuator exposure.

## 3. Open The Browser Cockpit

Open the first-party browser cockpit:

```text
http://localhost:8080/evidence-training-demo.html
```

Click `Run demo sequence` to execute the local browser flow in order, or use the page checklist manually from top to bottom:

1. Check health.
2. Check readiness.
3. Load onboarding.
4. List templates.
5. List examples.
6. List scorecards.
7. Load scorecard detail.
8. Load the answer template.
9. Grade the perfect sample.
10. Grade the partial sample.
11. Grade the failing sample.

Use `Stop demo` to halt any remaining queued steps and `Reset demo` to clear client-side statuses, responses, grading summaries, and the summary preview. The `Copy demo summary` control copies a deterministic client-side transcript preview with step outcomes and grading summaries; nothing is written to disk or posted back as a runtime report.

The cockpit shows deterministic step statuses (`Not run`, `Passed`, `Warning`, and `Failed`), summary counters, copyable curl snippets, copyable response blocks, Postman parity notes, copy/export sample answer payloads, and PASS/WARN/FAIL grading summaries while preserving raw JSON responses. The page uses same-origin API calls by default and contains no external scripts, styles, fonts, images, CDNs, services, secrets, auth fields, browser storage, admin controls, release controls, ruleset controls, or cloud mutation controls.

## 4. Open The Routing Decision Demo

Open the routing decision browser demo:

```text
http://localhost:8080/routing-demo.html
```

Use the page from top to bottom:

1. Check health.
2. Check readiness.
3. Load the sample routing scenario.
4. Compare strategies.
5. Inspect selected servers and `Why this server?` explanations.
6. Copy the curl snippet, request payload, raw response, or normalized summary.
7. Reset the demo before replaying it.

The page calls the existing `POST /api/routing/compare` endpoint with synthetic server telemetry. It shows the registered request-level strategies only: `TAIL_LATENCY_POWER_OF_TWO`, `WEIGHTED_LEAST_LOAD`, `WEIGHTED_LEAST_CONNECTIONS`, `WEIGHTED_ROUND_ROBIN`, and `ROUND_ROBIN`. The explanation panel is based on real response fields: `chosenServerId`, `reason`, `candidateServersConsidered`, and `scores`.

The same flow is available in Postman through the `Routing Decision Demo` folder. See [`POSTMAN_ROUTING_DEMO.md`](POSTMAN_ROUTING_DEMO.md) for the sample body, expected response shape, and explanation limits.

## 5. Open The Unified Load-Balancing Cockpit

Open the unified cockpit browser page:

```text
http://localhost:8080/load-balancing-cockpit.html
```

Use the page from top to bottom:

1. Use `Cockpit Navigation & Readiness` to jump between dense cockpit panels and refresh current-session readiness badges.
2. Click `Start walkthrough` in `Operator Guided Walkthrough` if you want the cockpit to step through the review order.
3. Check health and readiness.
4. Pick a scenario from `Scenario Gallery`, or load the default safe local scenario.
5. Run allocation preview against `POST /api/allocate/capacity-aware`.
6. Run routing comparison against `POST /api/routing/compare`.
7. Run load-shedding preview and remediation hints against `POST /api/allocate/evaluate`.
8. Compare allocation, routing, overload, and remediation-hint output side by side.
9. Run another scenario and click `Compare with previous scenario` to see what changed.
10. Click `Run all scenarios` in `Operator Comparison Matrix` to summarize every packaged scenario in one table.
11. Select a baseline and comparison in `Operator Replay Mode`, then click `Replay selected pair` to highlight before/after differences.
12. Review `Explanation Drill-Down` for routing, allocation, overload, remediation, and scenario-delta rationale.
13. Click `Generate trace` in `API Contract Trace` to map visible claims to endpoints, payloads, raw fields, derived labels, and unavailable fields.
14. Use the guided walkthrough's raw-vs-derived verification step before trusting derived summaries.
15. Click `Generate review packet`, then copy or print the packet for a deterministic operator handoff.
16. Copy curl snippets, the selected scenario payload, raw responses, cockpit summary, scenario summary, matrix summary, replay reviewer note, API contract trace, walkthrough summary, readiness summary, review packet, drill-down summary, explanation curl snippets, or operator rationale.
17. Reset the cockpit before replaying it.

The packaged scenario gallery includes `Normal Load`, `Overload Pressure`, `All-Unhealthy Degradation`, and `Recovery / Capacity Restored`. Each scenario shows expected outcome hints before execution, but actual allocation, routing, load-shedding, and remediation output comes from existing endpoints only.

The cockpit does not add a new API contract. It reuses existing calculation and recommendation routes, marks unsupported sections as `Not available in current API`, and does not fabricate allocation, routing, load-shedding, or remediation behavior. The cockpit navigation/readiness layer keeps section orientation and readiness badges in memory only, with deterministic copy output and no backend writes or browser storage. The operator guided walkthrough keeps deterministic checklist state in memory only, highlights existing panels, and copies a Markdown-style checklist summary without backend writes or browser storage. The operator comparison matrix runs packaged scenarios in deterministic order and summarizes real endpoint outputs without benchmark or score claims. The operator replay mode replays a selected baseline/comparison pair in memory only and copies deterministic reviewer notes without backend writes or browser storage. The operator review packet assembles current in-memory scenario, matrix, replay, explanation, API contract trace, endpoint, payload, raw JSON reference, and safety text into a copyable/printable browser handoff. The API contract trace maps cockpit claims to endpoint paths, request payload sources, raw response sources, displayed raw fields, derived labels, unavailable fields, and safety notes. The explanation drill-down labels client-side supporting math as derived from visible request/response fields when exact internal scores or thresholds are not exposed by the current API. The matching Postman flows are the `Unified Load-Balancing Cockpit`, `Operator Scenario Gallery`, and `Operator Explanation Drill-Down` folders. See [`POSTMAN_LOAD_BALANCING_COCKPIT.md`](POSTMAN_LOAD_BALANCING_COCKPIT.md), [`OPERATOR_SCENARIO_GALLERY.md`](OPERATOR_SCENARIO_GALLERY.md), [`OPERATOR_EXPLANATION_DRILLDOWN.md`](OPERATOR_EXPLANATION_DRILLDOWN.md), [`OPERATOR_COMPARISON_MATRIX.md`](OPERATOR_COMPARISON_MATRIX.md), [`OPERATOR_REPLAY_MODE.md`](OPERATOR_REPLAY_MODE.md), [`OPERATOR_REVIEW_PACKET.md`](OPERATOR_REVIEW_PACKET.md), [`OPERATOR_API_CONTRACT_TRACE.md`](OPERATOR_API_CONTRACT_TRACE.md), [`OPERATOR_GUIDED_WALKTHROUGH.md`](OPERATOR_GUIDED_WALKTHROUGH.md), and [`OPERATOR_NAVIGATION_READINESS.md`](OPERATOR_NAVIGATION_READINESS.md) for request order, sample bodies, expected response shape, scenario comparison, drill-down behavior, matrix behavior, replay behavior, review packet behavior, API contract trace behavior, guided walkthrough behavior, navigation/readiness behavior, and safety limits.

## 6. Open Evidence Training Onboarding

Open the summary route:

```bash
curl -fsS http://127.0.0.1:8080/api/evidence-training/onboarding
```

The response lists packaged policy templates, examples, scorecards, workflow pointers, a sample answer template, and safety notes. Discovery routes are read-only. The scorecard grade route performs deterministic in-memory grading only.

## 7. Import The Postman Collection

Import:

```text
postman/LoadBalancerPro.postman_collection.json
```

Set the collection variable:

```text
baseUrl = http://localhost:8080
```

Run the `Evidence Training Demo Walkthrough` folder from top to bottom. It covers the same health, readiness, onboarding, policy template discovery, example discovery, scorecard discovery, answer template retrieval, and three grading examples as the evidence browser page. Run the `Routing Decision Demo` folder to cover the routing browser page's health, readiness, sample strategy comparison, weighted sample, least-connections sample, and tail-latency sample. Run the `Unified Load-Balancing Cockpit` folder to cover health, readiness, routing comparison, capacity-aware allocation, predictive allocation, and read-only load-shedding/remediation evaluation. Run the `Operator Scenario Gallery` folder to replay normal load, overload pressure, all-unhealthy degradation, and recovery/capacity-restored scenarios through the same real endpoint set. Run the `Operator Explanation Drill-Down` folder when you want request-by-request parity for the cockpit explanation panels.

## 8. Inspect Templates, Examples, And Scorecards

Equivalent curl requests:

```bash
curl -fsS http://127.0.0.1:8080/api/evidence-training/templates
curl -fsS http://127.0.0.1:8080/api/evidence-training/examples
curl -fsS http://127.0.0.1:8080/api/evidence-training/scorecards
curl -fsS http://127.0.0.1:8080/api/evidence-training/scorecards/strict-zero-drift-pass
```

Use the packaged scorecards to discuss expected `PASS`, `WARN`, and `FAIL` decisions before asking an operator to submit answers.

## 9. Retrieve An Answer Template

Fetch a deterministic answer template:

```bash
curl -fsS http://127.0.0.1:8080/api/evidence-training/scorecards/strict-zero-drift-pass/answer-template
```

The template shape is:

```json
{
  "operator": "operator-label",
  "answers": [
    {
      "exerciseName": "strict-zero-drift-pass",
      "decision": "PASS",
      "reason": "No drift detected",
      "action": "continue handoff",
      "notes": "No remediation required; record the zero-drift result and continue the handoff."
    }
  ]
}
```

## 10. Submit Deterministic Grading Requests

The repo includes small deterministic demo fixtures:

```text
src/test/resources/evidence-training-demo/perfect-scorecard-answers.json
src/test/resources/evidence-training-demo/partial-scorecard-answers.json
src/test/resources/evidence-training-demo/failing-scorecard-answers.json
```

Grade the perfect sample:

```bash
curl -fsS -X POST http://127.0.0.1:8080/api/evidence-training/scorecards/grade \
  -H "Content-Type: application/json" \
  -d @src/test/resources/evidence-training-demo/perfect-scorecard-answers.json
```

Grade the partial sample:

```bash
curl -fsS -X POST http://127.0.0.1:8080/api/evidence-training/scorecards/grade \
  -H "Content-Type: application/json" \
  -d @src/test/resources/evidence-training-demo/partial-scorecard-answers.json
```

Grade the failing sample:

```bash
curl -fsS -X POST http://127.0.0.1:8080/api/evidence-training/scorecards/grade \
  -H "Content-Type: application/json" \
  -d @src/test/resources/evidence-training-demo/failing-scorecard-answers.json
```

Expected deterministic result shape:

```json
{
  "scorecardVersion": "1",
  "operator": "operator-perfect-demo",
  "totalExercises": 7,
  "totalScore": 70,
  "maxScore": 70,
  "percent": 100.0,
  "passed": true,
  "passingScore": 80.0,
  "perExercise": [
    {
      "exerciseName": "audit-append-warn",
      "expectedDecision": "WARN",
      "actualDecision": "WARN",
      "decisionCorrect": true,
      "reasonMatched": true,
      "reasonCredit": 3,
      "actionMatched": true,
      "actionCredit": 2,
      "score": 10,
      "maxScore": 10,
      "feedback": "Decision matched expected WARN; Reason matched expected primary reason; Action matched acceptable action; Expected note: Verify that the appended audit entry is expected receiver-side verification activity and record the review."
    }
  ]
}
```

No timestamps or random identifiers are generated by default.

## 11. GUI-Facing Surface

The evidence browser cockpit is a tiny static GUI-facing surface served by the existing Spring Boot app at `/evidence-training-demo.html`. It reuses the existing evidence training API endpoints and deterministic embedded sample payloads. The routing decision demo at `/routing-demo.html` is the matching static GUI-facing surface for the existing request-level routing comparison API. The unified load-balancing cockpit at `/load-balancing-cockpit.html` ties allocation, routing, overload/load-shedding, remediation-hint, scenario-gallery, comparison-matrix, replay-mode, review-packet, API-contract-trace, guided-walkthrough, navigation/readiness, and explanation drill-down views together with existing API responses. These pages add no frontend framework, external dependency, external script, CDN, browser automation dependency, or controller.

## Troubleshooting

- If `curl` cannot connect, confirm the API is running on `127.0.0.1:8080` and no other process is using the port.
- If the browser page returns `404`, confirm the app is running from a build that includes `src/main/resources/static/evidence-training-demo.html`, `src/main/resources/static/routing-demo.html`, and `src/main/resources/static/load-balancing-cockpit.html`.
- If copy buttons do not access the system clipboard, select the visible curl or payload block and copy it manually.
- If Postman requests fail, confirm the collection `baseUrl` variable is `http://localhost:8080`.
- If `POST /api/evidence-training/scorecards/grade` returns `400`, inspect the response message for malformed JSON, missing `answers`, an unknown `exerciseName`, or a decision outside `PASS`, `WARN`, and `FAIL`.
- If a production-like profile requires authentication, keep the local demo on the `local` profile or add the configured demo `X-API-Key` only in Postman variables. Do not commit secrets.
- If local Maven fails with a PKIX or certificate chain error, treat that as a workstation trust-store issue and rely on GitHub CI for Maven verification.

## Safety Boundaries

- Local/operator training aid only.
- Not certification.
- Not benchmark proof.
- Not legal compliance proof.
- Not identity proof.
- No cloud mutation.
- No `CloudManager` required for the training/onboarding demo.
- No `CloudManager` required for the routing decision demo.
- No `CloudManager` required for the unified load-balancing cockpit, scenario gallery, replay mode, or explanation drill-down.
- API server is required for browser/Postman demo but not for offline CLI workflows.
- No external scripts, styles, fonts, images, CDNs, services, or dependencies.
- No browser `localStorage` or `sessionStorage` is used.
- The browser summary/transcript preview is client-side copyable text only; it does not write runtime reports.
- The grading endpoint is deterministic in-memory evaluation only; it does not write generated runtime reports.
