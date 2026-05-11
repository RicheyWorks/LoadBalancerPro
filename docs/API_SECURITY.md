# API Security And Abuse-Resistance Posture

LoadBalancerPro's HTTP API is intended for demos, CI validation, and controlled operator review. The application includes app-level guardrails, but it is not a complete internet-edge security boundary by itself.

## Authentication Posture

The local/default profile keeps API routes convenient for development and automated tests. Production-like deployments should use the `prod` or `cloud-sandbox` profile with API-key mode or OAuth2 mode configured.

API-key mode protects mutation-style API routes with the `X-API-Key` header. It is suitable for demos and compatibility testing, but it is not a full enterprise identity model. OAuth2 mode provides bearer-token validation and role checks when issuer or JWK settings are supplied.

Deployments that expose the service beyond a private test network should place it behind a trusted ingress, API gateway, reverse proxy, or zero-trust access layer.

## Rate-Limit Posture

The application currently uses validation, request-size limits, structured errors, and cloud-safety boundaries as app-level protections. It does not provide a distributed app-native rate limiter, and it does not depend on Redis, a database, or an external queue for throttling.

Apply rate limiting at the edge for shared or public deployments. A reasonable first policy is to limit POST requests to allocation, evaluation, and routing endpoints per client identity or source IP, with stricter limits for repeated `400`, `401`, `413`, and `415` outcomes.

## Abuse-Resistance Guarantees

The API rejects malformed JSON, unsupported content types, oversized API mutation bodies, invalid numeric ranges, unknown strategies, duplicate routing identifiers, and invalid load-shedding metadata with controlled error responses. Error bodies should not include stack traces, exception class names, secrets, or diagnostic internals.

Rejected requests must not construct or mutate `CloudManager`. Read-only evaluation requests must remain recommendation-only and keep `metricsPreview.emitted` set to `false`.

Request-size limits are enforced for `/api/**` POST, PUT, and PATCH requests. The default limit is 16 KiB and can be changed with `loadbalancerpro.api.max-request-bytes`.

Evidence training onboarding routes under `/api/evidence-training/**` expose packaged template, example, scorecard, and answer-template metadata for local operator onboarding and Postman demos. Discovery routes are read-only. `POST /api/evidence-training/scorecards/grade` performs deterministic in-memory grading only; it does not write report files, construct `CloudManager`, or mutate cloud state. The Postman operator demo uses local `{{baseUrl}}` requests and deterministic no-secret grading bodies only; it must not include cloud mutation, admin, release, tag, ruleset, or credential requests. The browser cockpit at `/evidence-training-demo.html` uses plain same-origin HTML/CSS/JavaScript, has no external scripts, styles, fonts, images, CDNs, services, dependencies, secrets, auth fields, browser storage, or admin/release/ruleset/cloud controls, and calls only the evidence training demo routes. Its run-sequence and summary/transcript preview are client-side only; they do not write runtime reports or introduce server-side automation. Treat the scorecard output as a local training aid only, not certification, legal compliance proof, or identity proof.

The routing decision demo at `/routing-demo.html` uses plain same-origin HTML/CSS/JavaScript and the existing `POST /api/routing/compare` route. It loads synthetic request-level routing telemetry, displays selected servers and strategy reasons, and copies curl, payload, raw response, and normalized summary text on the client only. The page and the `Routing Decision Demo` Postman folder must not add external scripts, styles, fonts, images, CDNs, services, dependencies, secrets, credential fields, admin controls, release controls, tag controls, ruleset controls, or cloud mutation controls. The routing demo is local/operator review only, not certification, not benchmark proof, not legal compliance proof, and not identity proof.

The unified load-balancing cockpit at `/load-balancing-cockpit.html` uses plain same-origin HTML/CSS/JavaScript and existing calculation/recommendation routes only: `/api/health`, Actuator readiness, `POST /api/allocate/capacity-aware`, `POST /api/allocate/evaluate`, and `POST /api/routing/compare`. It shows allocation, routing, load-shedding, and advisory remediation-hint output from real API responses. Its operator scenario gallery loads deterministic normal load, overload pressure, all-unhealthy degradation, and recovery/capacity-restored payloads into those same endpoints and compares results client-side. Its operator comparison matrix runs those packaged scenarios sequentially and summarizes real response fields in a client-side table with deterministic copy output. Its operator replay mode replays a selected baseline/comparison pair in memory only, highlights before/after fields client-side, and copies reviewer notes without backend writes or browser storage. Its operator review packet assembles current in-memory cockpit summaries and API contract trace notes into copyable and printable browser text without persistence. Its API contract trace maps visible claims to endpoint paths, request payload sources, raw response sources, displayed raw fields, derived labels, unavailable fields, and mutation/safety notes in memory only. Its explanation drill-down panels add routing, allocation, overload, remediation, and scenario-delta rationale from real response fields and visible scenario inputs; any supporting math is labeled as derived from visible request/response fields when exact internal scores or thresholds are not exposed. It must not add external scripts, styles, fonts, images, CDNs, services, dependencies, secrets, credential fields, browser storage, admin controls, release controls, tag controls, ruleset controls, cloud mutation controls, server-side report writing, benchmark claims, score claims, or fabricated algorithm behavior. The cockpit, `Unified Load-Balancing Cockpit` Postman folder, `Operator Scenario Gallery` Postman folder, and `Operator Explanation Drill-Down` Postman folder are local/operator review only, not certification, not benchmark proof, not legal compliance proof, and not identity proof.

## Out Of Scope

The repository does not currently provide WAF rules, distributed quotas, bot detection, credential rotation, customer identity lifecycle management, TLS certificates, or production incident-response automation. Those controls belong in the deployment platform and should be reviewed separately before public exposure.
