# Operator Evidence Training Demo Walkthrough

This walkthrough gives reviewers and operators one local path through health checks, the first-party browser demo, evidence training onboarding, Postman import, packaged policy discovery, answer templates, and deterministic scorecard grading.

The demo is a local/operator training aid only. It is not certification, not legal compliance proof, and not identity proof. The training and onboarding routes do not mutate cloud state and do not require `CloudManager`.

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

## 3. Open The Browser Demo

Open the first-party browser demo page:

```text
http://localhost:8080/evidence-training-demo.html
```

Use the page to check health, check readiness, load onboarding, list templates, list examples, list scorecards, load a selected scorecard, load its answer template, and grade perfect, partial, and failing sample answers. The page uses same-origin API calls by default and contains no external scripts, styles, fonts, CDNs, services, secrets, auth fields, admin controls, release controls, ruleset controls, or cloud mutation controls.

## 4. Open Evidence Training Onboarding

Open the summary route:

```bash
curl -fsS http://127.0.0.1:8080/api/evidence-training/onboarding
```

The response lists packaged policy templates, examples, scorecards, workflow pointers, a sample answer template, and safety notes. Discovery routes are read-only. The scorecard grade route performs deterministic in-memory grading only.

## 5. Import The Postman Collection

Import:

```text
postman/LoadBalancerPro.postman_collection.json
```

Set the collection variable:

```text
baseUrl = http://localhost:8080
```

Run the `Evidence Training Demo Walkthrough` folder from top to bottom. It covers the same health, readiness, onboarding, policy template discovery, example discovery, scorecard discovery, answer template retrieval, and three grading examples as the browser page.

## 6. Inspect Templates, Examples, And Scorecards

Equivalent curl requests:

```bash
curl -fsS http://127.0.0.1:8080/api/evidence-training/templates
curl -fsS http://127.0.0.1:8080/api/evidence-training/examples
curl -fsS http://127.0.0.1:8080/api/evidence-training/scorecards
curl -fsS http://127.0.0.1:8080/api/evidence-training/scorecards/strict-zero-drift-pass
```

Use the packaged scorecards to discuss expected `PASS`, `WARN`, and `FAIL` decisions before asking an operator to submit answers.

## 7. Retrieve An Answer Template

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

## 8. Submit Deterministic Grading Requests

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

## 9. GUI-Facing Surface

The browser page is a tiny static GUI-facing surface served by the existing Spring Boot app at `/evidence-training-demo.html`. It reuses the existing evidence training API endpoints and deterministic embedded sample payloads. It does not add a frontend framework, external dependency, external script, CDN, or controller.

## Troubleshooting

- If `curl` cannot connect, confirm the API is running on `127.0.0.1:8080` and no other process is using the port.
- If the browser page returns `404`, confirm the app is running from a build that includes `src/main/resources/static/evidence-training-demo.html`.
- If Postman requests fail, confirm the collection `baseUrl` variable is `http://localhost:8080`.
- If `POST /api/evidence-training/scorecards/grade` returns `400`, inspect the response message for malformed JSON, missing `answers`, an unknown `exerciseName`, or a decision outside `PASS`, `WARN`, and `FAIL`.
- If a production-like profile requires authentication, keep the local demo on the `local` profile or add the configured demo `X-API-Key` only in Postman variables. Do not commit secrets.
- If local Maven fails with a PKIX or certificate chain error, treat that as a workstation trust-store issue and rely on GitHub CI for Maven verification.

## Safety Boundaries

- Local/operator training aid only.
- Not certification.
- Not legal compliance proof.
- Not identity proof.
- No cloud mutation.
- No `CloudManager` required for the training/onboarding demo.
- API server is required for browser/Postman demo but not for offline CLI workflows.
- No external scripts, styles, fonts, CDNs, services, or dependencies.
- The grading endpoint is deterministic in-memory evaluation only; it does not write generated runtime reports.
