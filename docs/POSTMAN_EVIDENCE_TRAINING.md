# Postman Evidence Training Onboarding

LoadBalancerPro exposes read-only evidence training onboarding routes for local API demos, GUI-facing clients, and Postman walkthroughs. The routes surface the packaged evidence policy templates, examples, scorecards, and scorecard answer JSON shape without replacing the offline CLI.

Import the existing collection:

```text
postman/LoadBalancerPro.postman_collection.json
```

Set the `baseUrl` collection variable to the local API server. The default is:

```text
http://localhost:8080
```

## Requests

The `Evidence Training Onboarding` folder includes:

```text
GET  {{baseUrl}}/api/evidence-training/onboarding
GET  {{baseUrl}}/api/evidence-training/templates
GET  {{baseUrl}}/api/evidence-training/examples
GET  {{baseUrl}}/api/evidence-training/scorecards
GET  {{baseUrl}}/api/evidence-training/scorecards/receiver-redaction-warn
GET  {{baseUrl}}/api/evidence-training/scorecards/receiver-redaction-warn/answer-template
POST {{baseUrl}}/api/evidence-training/scorecards/grade
```

The `GET` routes are discovery-only. The `POST` grade route evaluates the submitted answer JSON in memory and returns deterministic JSON. It does not write report files or runtime artifacts.

## Demo Walkthrough Folder

The `Evidence Training Demo Walkthrough` folder is ordered for a reviewer demo:

```text
GET  {{baseUrl}}/api/health
GET  {{baseUrl}}/actuator/health/readiness
GET  {{baseUrl}}/api/evidence-training/onboarding
GET  {{baseUrl}}/api/evidence-training/templates
GET  {{baseUrl}}/api/evidence-training/examples
GET  {{baseUrl}}/api/evidence-training/scorecards
GET  {{baseUrl}}/api/evidence-training/scorecards/strict-zero-drift-pass
GET  {{baseUrl}}/api/evidence-training/scorecards/strict-zero-drift-pass/answer-template
POST {{baseUrl}}/api/evidence-training/scorecards/grade
POST {{baseUrl}}/api/evidence-training/scorecards/grade
POST {{baseUrl}}/api/evidence-training/scorecards/grade
```

The three grade requests embed deterministic perfect, partial, and failing demo bodies from these fixtures:

```text
src/test/resources/evidence-training-demo/perfect-scorecard-answers.json
src/test/resources/evidence-training-demo/partial-scorecard-answers.json
src/test/resources/evidence-training-demo/failing-scorecard-answers.json
```

See [`OPERATOR_DEMO_WALKTHROUGH.md`](OPERATOR_DEMO_WALKTHROUGH.md) for the guided start-to-finish demo.

## Sample Answer JSON

```json
{
  "operator": "operator-a",
  "answers": [
    {
      "exerciseName": "receiver-redaction-warn",
      "decision": "WARN",
      "reason": "Receiver redaction changes require review",
      "action": "confirm redaction summary",
      "notes": "Confirm the summary and document the reviewed redacted files."
    }
  ]
}
```

The grade response includes `scorecardVersion`, `totalExercises`, `totalScore`, `maxScore`, `percent`, `passed`, `passingScore`, and per-exercise feedback. No timestamps or random ids are generated.

## CLI Continuity

The API server is optional for CLI workflows. Operators can still use:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --list-policy-templates
java -jar target/LoadBalancerPro-2.4.2.jar --list-policy-examples
java -jar target/LoadBalancerPro-2.4.2.jar --list-training-scorecards
java -jar target/LoadBalancerPro-2.4.2.jar --grade-training-scorecard scorecard-answers.json
```

## GUI Note

The repository currently has a JavaFX operational GUI, but no existing web-static GUI resource pattern for API onboarding pages. This sprint exposes a GUI-facing API surface and Postman workflow instead of adding a new frontend stack.

## Limits

- Local/operator training aid only.
- Not certification.
- Not legal compliance proof.
- Not identity proof.
- Read-only discovery plus deterministic in-memory grading only.
- No cloud mutation.
- No `CloudManager` construction is required.
- API server is required for API/Postman demos but optional for offline CLI workflows.
