# Operator Evidence Training Demo Walkthrough

This walkthrough gives reviewers and operators one local path through health checks, evidence training onboarding, Postman import, packaged policy discovery, answer templates, and deterministic scorecard grading.

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

## 3. Open Evidence Training Onboarding

Open the summary route:

```bash
curl -fsS http://127.0.0.1:8080/api/evidence-training/onboarding
```

The response lists packaged policy templates, examples, scorecards, workflow pointers, a sample answer template, and safety notes. Discovery routes are read-only. The scorecard grade route performs deterministic in-memory grading only.

## 4. Import The Postman Collection

Import:

```text
postman/LoadBalancerPro.postman_collection.json
```

Set the collection variable:

```text
baseUrl = http://localhost:8080
```

Run the `Evidence Training Demo Walkthrough` folder from top to bottom. It covers health, readiness, onboarding, policy template discovery, example discovery, scorecard discovery, answer template retrieval, and three grading examples.

## 5. Inspect Templates, Examples, And Scorecards

Equivalent curl requests:

```bash
curl -fsS http://127.0.0.1:8080/api/evidence-training/templates
curl -fsS http://127.0.0.1:8080/api/evidence-training/examples
curl -fsS http://127.0.0.1:8080/api/evidence-training/scorecards
curl -fsS http://127.0.0.1:8080/api/evidence-training/scorecards/strict-zero-drift-pass
```

Use the packaged scorecards to discuss expected `PASS`, `WARN`, and `FAIL` decisions before asking an operator to submit answers.

## 6. Retrieve An Answer Template

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

## 7. Submit Deterministic Grading Requests

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

## 8. GUI-Facing Surface

The repository has an operational JavaFX GUI, but it does not currently have a web-static resource pattern for onboarding pages. This demo therefore uses the existing GUI-facing API, OpenAPI output, Postman collection, and docs instead of adding a frontend framework or static web stack.

## Troubleshooting

- If `curl` cannot connect, confirm the API is running on `127.0.0.1:8080` and no other process is using the port.
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
- API server is required for API/Postman demo but not for offline CLI workflows.
- The grading endpoint is deterministic in-memory evaluation only; it does not write generated runtime reports.
