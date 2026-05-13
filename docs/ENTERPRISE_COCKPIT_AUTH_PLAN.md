# Enterprise Cockpit Auth Plan

## Status

- This began as a plan/audit document and now tracks the first prod API-key runtime hardening phase.
- Runtime behavior changed in the prod API-key cockpit hardening sprint: prod/cloud-sandbox API-key mode gates OpenAPI/Swagger with `X-API-Key`, and the cockpit can attach a memory-only operator API key to protected `/api/**` calls.
- The Postman enterprise lab collection now gives operators a placeholder-only way to evaluate local/demo and prod API-key boundaries without adding runtime behavior.
- This is enterprise-demo hardening guidance, not production IAM certification.
- This document describes target operator authentication and OpenAPI gating behavior. It does not implement OAuth2 login, persistent browser token storage, reverse-proxy identity trust, production SSO, or IAM certification.

## Current Model

Local/default mode keeps the app convenient for development, CI smoke tests, and browser demos. The default `loadbalancerpro.auth.mode` is `api-key`, but the prod-only `ProdApiKeyFilter` is not active without the `prod` or `cloud-sandbox` profile. Spring Security permits requests in non-OAuth2 mode, so local users can open `/`, `/load-balancing-cockpit.html`, `/routing-demo.html`, `/evidence-training-demo.html`, Swagger/OpenAPI, `/api/health`, allocation/evaluation routes, routing comparison routes, and local Actuator health/info/metrics/prometheus routes without operator credentials.

Prod and cloud-sandbox API-key mode use `ProdApiKeyFilter` when the active profile is `prod` or `cloud-sandbox` and `loadbalancerpro.auth.mode=api-key`. The filter protects non-OPTIONS `/api/**`, `/proxy` or `/proxy/**`, `/v3/api-docs`, `/v3/api-docs/**`, `/swagger-ui.html`, and `/swagger-ui/**` with the `X-API-Key` header. A missing configured API key fails closed for those protected requests. The explicit public API exceptions are `GET /api/health` and unauthenticated `OPTIONS` preflight requests. Static browser pages remain public by current API-key-mode behavior. Prod and cloud-sandbox profiles expose Actuator health/info only by default and keep metrics/prometheus disabled by profile configuration.

OAuth2 mode is a real Spring Security resource-server foundation, not just documentation. When `loadbalancerpro.auth.mode=oauth2` is active, startup requires either an issuer URI or JWK set URI, JWTs are decoded through Spring Security, and application roles are extracted from dedicated `roles`, `role`, `authorities`, and `realm_access.roles` claims. Ordinary OAuth2 `scope` and `scp` claims are not app roles and do not become `ROLE_operator` or `ROLE_admin`. `/api/health` and CORS preflight remain public. Allocation, routing, proxy, and LASE shadow routes require configured roles. `/v3/api-docs` and Swagger UI are gated by default unless `loadbalancerpro.auth.docs-public=true`. Static browser pages are not an implemented OAuth2 cockpit workflow today; OAuth2 login/session UX is not present in this sprint.

The static cockpit and demo pages are browser-only assets. `/load-balancing-cockpit.html` calls same-origin `/api/health`, Actuator readiness, `POST /api/allocate/capacity-aware`, `POST /api/allocate/evaluate`, and `POST /api/routing/compare`. The cockpit now includes an operator API-key control for API-key mode: it stores the entered value in JavaScript memory only, clears it on refresh/navigation, does not use `localStorage` or `sessionStorage`, does not put keys in URLs or logs, and adds `X-API-Key` to protected `/api/**` calls after the operator configures it. Copyable curl snippets use `<API_KEY>` instead of the entered value. `/routing-demo.html` calls readiness and `POST /api/routing/compare`. `/evidence-training-demo.html` calls read-only onboarding/template/example/scorecard routes and `POST /api/evidence-training/scorecards/grade`. Those other demo pages still send JSON or Accept headers only; they do not collect, store, or send `X-API-Key` or `Authorization` credentials, so they are local/default demo pages unless separately enhanced for prod/cloud-sandbox API-key review.

Actuator exposure is profile scoped. Local/default exposes health, info, metrics, and prometheus. Prod/cloud-sandbox expose health and info only, with Prometheus export disabled by default. Metrics and Prometheus must stay private or disabled unless a future sprint explicitly gates them behind trusted deployment controls.

## Product Mismatch

- Backend `/api/**` routes require `X-API-Key` by default in prod/cloud-sandbox API-key mode except `GET /api/health` and unauthenticated `OPTIONS` preflight requests, and the cockpit now has a memory-only operator token path for those API calls. Other demo pages still behave like local/demo UIs unless separately enhanced or gated.
- The app has a stronger OAuth2 resource-server foundation, but the cockpit has no login/session flow and no browser-side `Authorization` header workflow.
- Public `/v3/api-docs` and Swagger UI in prod-like API-key mode were the previous behavior. The first runtime phase gates them with `X-API-Key` in prod/cloud-sandbox API-key mode while preserving public local/default developer usability.
- Enterprise cockpit use needs a clear operator auth story: who the operator is, how the browser receives auth, which routes observer versus operator roles can call, and where secrets are allowed to live.

## Endpoint Exposure Matrix

| Route / route group | Local/demo policy | Prod API-key policy | OAuth2 target policy | UI/cockpit impact | Notes |
| --- | --- | --- | --- | --- | --- |
| `/` | Public static landing page. | Currently public static landing page. Target: keep only for local/private review or gate with the selected operator auth mode. | Authenticated viewer/operator landing page or disabled static landing outside demo mode. | Entry point can link to cockpit only when the cockpit mode is safe for the active profile. | Root page is discoverability, not an auth boundary. |
| `/load-balancing-cockpit.html` | Public local cockpit. | Public static page; protected `/api/**` calls include `X-API-Key` only after the operator enters a memory-only key. | Authenticated operator cockpit after login/session or disabled until OAuth2 UI exists. | First API-key mismatch is closed for cockpit API calls; OAuth2 and static-page gating remain future work. | Key is not stored in `localStorage`/`sessionStorage`, URLs, logs, or copyable curl output. |
| `/routing-demo.html` | Public local routing demo. | Currently public static page; `POST /api/routing/compare` requires `X-API-Key`. | Authenticated operator or local/demo only. | Demo can show form state but prod-like compare calls fail without auth. | Keep as local/operator review unless gated. |
| `/evidence-demo.html` | Not the current checked-in route; current page is `/evidence-training-demo.html`. | Treat any future alias as local/demo only unless gated. | Authenticated viewer for read routes and operator for grading if policy requires. | Avoid adding a new public alias without explicit route policy. | Plan row included to reserve the expected public-name decision. |
| `/evidence-training-demo.html` | Public local evidence training UI. | Currently public static page; evidence training `/api/**` calls require `X-API-Key` in prod/cloud-sandbox API-key mode. | Authenticated viewer for discovery and operator for grading, or disabled until OAuth2 UI exists. | Current demo can render local/static state, but prod-like API calls need auth. | No credential fields or browser storage today. |
| `/api/health` | Public. | Public. | Public. | Cockpit health badge can stay unauthenticated. | Health is the low-detail app readiness signal. |
| `/api/routing/compare` | Public local POST. | Protected by `X-API-Key`. | Requires operator role. | Cockpit/routing demo must attach auth only after operator provides it. | CORS allows `Authorization` and `X-API-Key` headers when origins are configured. |
| Allocation/evaluation endpoints (`/api/allocate/capacity-aware`, `/api/allocate/predictive`, `/api/allocate/evaluate`) | Public local POSTs. | Protected by `X-API-Key`. | Requires operator role. | Cockpit allocation, evaluation, comparison, replay-style panels need operator auth in prod-like modes. | Evaluation is read-only/advisory, but it is still protected by the prod/cloud-sandbox `/api/**` default. |
| Scenario replay endpoints (`/api/scenarios/replay`) | Public local POST. | Protected by `X-API-Key` as `POST /api/**`. | Target: operator by default; consider observer only after a route-by-route data exposure review. | Future cockpit replay calls need auth before sending protected requests. | Replay remains read-only/advisory and must not imply cloud mutation. |
| Remediation report endpoints (`/api/remediation/report`) | Public local POST. | Protected by `X-API-Key` as `POST /api/**`. | Target: operator by default; consider observer only for sanitized report export after review. | Report export should not be available from a public prod browser page. | Reports are deterministic/advisory and should not reveal secrets. |
| Proxy status/reload endpoints (`GET /api/proxy/status`, `POST /api/proxy/reload`) | Status is demo-friendly; reload requires API key outside OAuth2 even in local/default mode. | Status and reload are protected by `X-API-Key`; reload also validates API key in the controller when not in OAuth2 mode. | Requires operator role. | `/proxy-status.html` cannot read prod status unless the selected auth mode supplies credentials. | Do not make proxy status public in prod-like modes. |
| `/proxy` and `/proxy/**` | Demo-friendly only when proxy mode is explicitly enabled and loopback/private. | Protected by `X-API-Key`. | Requires operator role. | Cockpit should not add forwarding controls without auth and route ownership review. | Proxy mode is disabled by default and is not a production gateway. |
| `/v3/api-docs` | Public local generated OpenAPI. | Protected by `X-API-Key` in prod/cloud-sandbox API-key mode. | Require authenticated viewer/operator/developer role unless `docs-public=true` is intentionally set for demo/private review. | Swagger-generated schema is no longer public by default in prod-like API-key mode. | Missing/wrong key returns HTTP 401; correct key can retrieve the JSON contract. |
| Swagger UI (`/swagger-ui.html`, `/swagger-ui/**`) | Public local UI. | Protected by `X-API-Key` in prod/cloud-sandbox API-key mode. | Require authenticated viewer/operator/developer role unless explicitly made public for demo/private review. | Browser docs align with the same auth mode as generated docs. | API-key mode is best reviewed by curl/Postman unless a trusted proxy injects auth for browser assets. |
| `/actuator/health` | Public local Actuator health. | Exposed by prod/cloud-sandbox profile defaults; keep behind private network or deployment auth outside local review. | Require authenticated read role when OAuth2 mode is active, unless deployment chooses a separate private health boundary. | Cockpit can show readiness only if allowed by the active profile. | Keep low-detail and private-network scoped outside demos. |
| `/actuator/info` | Public local Actuator info. | Exposed by prod/cloud-sandbox profile defaults; keep behind private network or deployment auth outside local review. | Require authenticated read role when OAuth2 mode is active. | UI should not rely on public info outside local/private review. | Avoid leaking build/runtime metadata publicly. |
| `/actuator/metrics` | Public local metrics endpoint. | Not exposed by prod/cloud-sandbox profile defaults. | Disabled/private unless explicitly enabled and gated. | Cockpit should not depend on public metrics. | Metrics can reveal operational details. |
| `/actuator/prometheus` | Public local Prometheus scrape endpoint. | Not exposed by prod/cloud-sandbox profile defaults; Prometheus export disabled by default. | Disabled/private unless explicitly enabled and gated. | Never use public Prometheus as a cockpit prerequisite. | Do not make Prometheus public in this auth work. |

## Recommended Auth Modes

1. local-demo mode

- Keep easy browser access to `/`, cockpit, routing demo, evidence training demo, local Swagger/OpenAPI, and local Actuator health/info/metrics/prometheus.
- Use no real secrets and no production identity provider values.
- Assume loopback or trusted developer workstations, not public internet exposure.
- Add clear warning banners later if a cockpit page can detect a prod-like profile without a configured operator auth mode.

2. prod-api-key mode

- Protected operator APIs require `X-API-Key`.
- The cockpit supports a memory-only operator token entry path for protected `/api/**` calls.
- Do not persist API keys in `localStorage` or `sessionStorage` unless that risk is explicitly accepted and documented in a future change.
- Prefer memory-only token entry for a lightweight app-native path or reverse-proxy injected auth for serious deployment.
- Keep API-key values out of logs, static HTML, committed docs, generated artifacts, query strings, and copyable public examples.

3. oauth2 mode

- Use bearer JWT/OIDC validation through Spring Security.
- Keep an observer/operator role split: observer can read limited review/status data, operator can invoke protected allocation, routing, proxy, replay, and report actions.
- Configure the enterprise IdP to emit application roles in `roles`, `role`, `authorities`, or `realm_access.roles`; do not rely on `scope` or `scp` values such as `operator` or `admin` for app authorization.
- Fail closed when issuer or JWK config is missing.
- The cockpit should use an `Authorization` header after a real login/session flow exists.
- Swagger/OpenAPI should be gated or disabled unless the caller is authenticated with the appropriate viewer/operator/developer role.

4. reverse-proxy auth mode

- Let enterprise ingress handle SSO and session cookies outside the app.
- The app may trust forwarded identity only when explicitly configured for a trusted proxy boundary.
- Document the risks: spoofed identity headers, mixed public/private ingress, incorrect forward-header trust, missing TLS termination, and weak route-level role mapping.
- Require deployment evidence that direct public traffic cannot reach the app without the trusted proxy.

## Swagger/OpenAPI Gating Plan

- local/demo: public Swagger/OpenAPI is acceptable for fast review and generated-client inspection.
- prod-api-key: `/v3/api-docs`, `/v3/api-docs/**`, `/swagger-ui.html`, and `/swagger-ui/**` are gated by `X-API-Key` by default in prod/cloud-sandbox API-key mode. If a deployment keeps docs public in a future change, it should be an explicit local/private review override.
- oauth2: require an authenticated viewer/operator/developer role by default. Keep `loadbalancerpro.auth.docs-public=true` as an intentional demo/private-network exception only.
- reverse-proxy auth: gate docs at the trusted ingress or map forwarded identity to an app-recognized docs role only after the proxy trust boundary is explicit.
- Actuator metrics and Prometheus remain disabled/private unless explicitly enabled and gated. This plan must not make `/actuator/metrics` or `/actuator/prometheus` public in prod-like modes.

## Cockpit Auth UX Options

| Option | Strengths | Risks | Recommended use |
| --- | --- | --- | --- |
| Disable cockpit in prod unless auth mode configured | Smallest runtime exposure and easiest to reason about. | Less convenient for demos that use prod-like API-key mode. | Default safety posture for prod/cloud-sandbox until cockpit auth ships. |
| Memory-only API-key/operator token prompt | Works with current API-key boundary and avoids persistent browser storage. | Operator can still paste secrets into an untrusted page; refresh loses token; must avoid logs/copy output. | Best first app-native cockpit improvement for API-key mode. |
| Session cookie behind reverse proxy | Aligns with enterprise SSO and keeps tokens out of JavaScript. | Requires trusted edge controls and CSRF review before browser ambient credentials reach mutation routes. | Strong option for serious deployments when ingress owns auth. |
| OAuth2 login flow | Best long-term app-native identity story with roles and bearer-token route checks. | Requires login/session UX, callback handling, token refresh strategy, and careful browser storage policy. | Target long-term cockpit workflow after API-key/docs gating is closed. |
| Postman/curl-only prod operations | Avoids browser secret handling. | Lower operator ergonomics; cockpit remains demo-only. | Acceptable interim posture for prod-like validation. |

Recommended phases:

- Phase 1: docs + route matrix. Completed in the enterprise cockpit auth plan sprint.
- Phase 2: gate Swagger in prod API-key mode. Completed for prod/cloud-sandbox API-key mode.
- Phase 3: add cockpit memory-only operator token support for API-key mode. Completed for cockpit protected `/api/**` calls.
- Phase 4: add deterministic Postman enterprise lab coverage for local/demo and prod API-key evaluation. Completed with placeholder-only collection/environment docs.
- Phase 5: add OAuth2/OIDC cockpit workflow.
- Phase 6: document a reverse-proxy SSO reference deployment.

## Required Tests

- Local/default cockpit still works.
- Prod missing API key returns 401 for protected APIs.
- Prod wrong API key returns 401.
- Prod correct API key succeeds.
- Prod `/v3/api-docs` is gated or disabled according to the target policy.
- Swagger UI is gated or disabled according to the target policy.
- Cockpit protected API calls include the configured auth header only after the operator supplies a token.
- No token or API key is logged.
- No token or API key is stored in `localStorage` or `sessionStorage` unless explicitly chosen and documented in a future reviewed change.
- OAuth2 mode missing issuer/JWK fails closed.
- OAuth2 invalid token returns 401.
- OAuth2 insufficient role returns 403.
- Observer versus operator route behavior is covered.
- Actuator metrics and Prometheus are not public in prod-like modes.

## Non-Goals

- No real IAM certification.
- No production SSO claim yet.
- No blockchain/catastrophic recovery in this sprint.
- No release provenance work in this sprint.
- No GitHub ruleset changes in this sprint.
- No OAuth2 implementation in this sprint.
- No persistent cockpit token storage implementation in this sprint.
- No release, tag, asset, branch-deletion, or workflow changes in this sprint.

## Implementation Risks

- Accidentally bypassing `ProdApiKeyFilter` by adding a second, inconsistent auth path.
- Duplicate auth systems where API-key, OAuth2, and reverse-proxy identity disagree about protected routes.
- Breaking local demos by gating static pages or Swagger without a profile-aware fallback.
- Insecure browser token storage, especially persistent API keys in `localStorage` or copied report output.
- Leaving Swagger/OpenAPI public in prod-like modes by accident after claiming enterprise gating.
- Overclaiming OAuth2 readiness before login/session, issuer operation, key rotation, and role lifecycle are implemented.
- Introducing cookie/session auth without revisiting CSRF, CORS credentials, and same-site behavior.
- Making Actuator metrics or Prometheus public while trying to improve cockpit ergonomics.

## Follow-up Sprint Recommendation

Run one aggressive implementation sprint: add a real OAuth2/OIDC cockpit workflow behind the existing resource-server foundation, with observer/operator route behavior, fail-closed issuer/JWK validation, authenticated Swagger/OpenAPI access, and no persistent browser token storage unless a separately reviewed policy explicitly accepts that risk.
