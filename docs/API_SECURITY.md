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

## Out Of Scope

The repository does not currently provide WAF rules, distributed quotas, bot detection, credential rotation, customer identity lifecycle management, TLS certificates, or production incident-response automation. Those controls belong in the deployment platform and should be reviewed separately before public exposure.
