# LoadBalancerPro Safety Invariants

Date: 2026-05-01  
Branch: `codex/safety-invariants`  
Verification command: `mvn -q test`

## Purpose and Scope

This document defines the safety invariants LoadBalancerPro should preserve as an enterprise-demo SRE/control-plane lab. These are the non-negotiable rules that protect the project's core posture: shadow first, replay first, prove first, execute last.

The invariants cover replay/evaluation behavior, LASE shadow behavior, cloud mutation guardrails, API authentication and authorization, telemetry guardrails, input handling, request-size behavior, profile boundaries, and documentation truthfulness.

This is evidence documentation, not formal verification. It does not claim production certification, complete security, or live-cloud assurance.

## How to Read These Invariants

Each invariant has five parts:

- Statement: the rule the project should preserve.
- Why it matters: the safety or security failure the rule prevents.
- Current enforcement mechanism: the code, profile behavior, configuration, or documentation that currently supports the rule.
- Evidence/tests if known: the regression evidence that currently supports the rule.
- Residual risk or limitation: what is not proven, not fully automated, or still owned by deployment.

The strongest invariants are both implemented and tested. Some are implemented but depend on deployment configuration. Others are intentionally documented as future formalization opportunities because the project should not overclaim guarantees beyond the current evidence.

Reference evidence:

- `evidence/THREAT_MODEL.md`
- `evidence/SECURITY_POSTURE.md`
- `evidence/RESIDUAL_RISKS.md`
- `evidence/TEST_EVIDENCE.md`
- `evidence/HARDENING_AUDIT_001.md`

## Core Safety Invariants

### Invariant 1: Replay/evaluation must not mutate live cloud resources.

Statement: Replay and evaluation paths must remain offline/read-only and must not construct, call, or promote through live cloud mutation paths.

Why it matters: Replay inputs may be historical, synthetic, or adversarial. They must not be able to affect AWS resources or production-like cloud state.

Current enforcement mechanism: Replay is exposed as an offline command path with safety messaging, local file input, deterministic report generation, and no API server startup for replay mode.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` records replay/cloud isolation coverage, malformed replay handling, and no AWS credential requirement. `evidence/HARDENING_AUDIT_001.md` records offline/read-only replay behavior and no live cloud execution.

Residual risk or limitation: Default tests use mocks and local files. They do not prove behavior in a live AWS account. If replay internals are changed, explicit mock-construction or no-network tests should be kept close to the change.

### Invariant 2: LASE shadow recommendations must remain advisory-only unless a future explicitly gated execution mode is added.

Statement: LASE shadow recommendations must not execute routing, scaling, or cloud mutations unless a separate future execution mode is intentionally designed, gated, documented, and tested.

Why it matters: Shadow recommendations are useful for observation and evaluation, but automatic execution would create a new control-plane mutation surface.

Current enforcement mechanism: LASE shadow behavior is advisory, records shadow observations, preserves allocation responses, and does not treat recommendations as commands.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` and `evidence/HARDENING_AUDIT_001.md` record LASE shadow-only behavior, allocation-response isolation, no CloudManager construction, and redacted failure output.

Residual risk or limitation: Future LASE execution work must be treated as a new feature with separate auth, cloud, replay, rollback, and safety tests. Current evidence does not approve execution.

### Invariant 3: Cloud mutation must fail closed unless explicit dry-run/live gates, profile gates, credential gates, and resource-prefix gates are satisfied.

Statement: Cloud mutation must remain disabled unless the operator explicitly chooses live behavior and all configured safety gates pass.

Why it matters: Cloud mutation can create, scale, register, or delete infrastructure. Accidental live execution is the highest-impact class of project risk.

Current enforcement mechanism: CloudManager defaults to dry-run/no-op behavior, requires explicit live-mode configuration, rejects placeholder credentials, checks capacity/account/region/ownership guardrails, and uses resource-prefix constraints for sandbox-style targeting.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` records dry-run/default behavior, placeholder credential failure, guarded live mutation paths, and sandbox prefix guardrails. `evidence/HARDENING_AUDIT_001.md` records mocked AWS boundary checks and fail-closed cloud guardrails.

Residual risk or limitation: Maven tests use mocked AWS clients and do not prove real AWS IAM, account policy, or region constraints. Live validation belongs in disposable sandbox infrastructure.

### Invariant 4: Cloud-sandbox live mutation must only target documented `lbp-sandbox-` resources.

Statement: Cloud-sandbox live mutation must be constrained to resources using the documented `lbp-sandbox-` prefix.

Why it matters: Prefix confinement reduces the chance that a sandbox action touches non-sandbox or production-like resources.

Current enforcement mechanism: Cloud-sandbox defaults use `cloud.resourceNamePrefix=lbp-sandbox-`, README and evidence docs document that prefix, and GUI/CLI configuration can pass the resource-name prefix into cloud configuration.

Evidence/tests if known: `evidence/SECURITY_POSTURE.md` records the documented cloud-sandbox prefix posture. `evidence/TEST_EVIDENCE.md` records sandbox prefix guardrail coverage. `evidence/HARDENING_AUDIT_001.md` records the GUI pass-through fix for `CLOUD_RESOURCE_NAME_PREFIX`.

Residual risk or limitation: This is a safety invariant stronger than mere configuration convenience. If operators can override the prefix, fixed-prefix enforcement and tests should be strengthened before treating this as hostile-operator-proof.

### Invariant 5: API-key and OAuth-protected mutation routes must fail closed when authentication or authorization is missing, malformed, or insufficient.

Statement: Protected mutation routes must reject missing credentials, wrong API keys, missing or invalid bearer tokens, and valid tokens without required roles.

Why it matters: Allocation endpoints are the public control-plane mutation surface. They must not become reachable through auth drift or missing configuration.

Current enforcement mechanism: API-key mode is the default production-compatible auth mode for prod/cloud-sandbox. OAuth2 mode is explicit opt-in, uses Spring Security resource-server validation, and applies observer/operator route checks.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` records API-key missing/wrong/correct coverage, OAuth 401/403 coverage, and operator/observer route behavior. `evidence/SECURITY_POSTURE.md` summarizes current auth/RBAC posture.

Residual risk or limitation: Local/default behavior is intentionally convenient and must not be deployed as hardened production posture. Deployment remains responsible for secret rotation and upstream controls.

### Invariant 6: OAuth role mapping must not silently weaken route protection because of common JWT claim shape drift.

Statement: OAuth authority extraction must consistently normalize supported JWT claim shapes into the route authority convention used by protected routes.

Why it matters: Enterprises commonly use different role claim shapes. Mapping drift can accidentally deny valid users or, worse, grant mutation access incorrectly.

Current enforcement mechanism: OAuth authority extraction handles supported claim shapes and normalizes them for route checks.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` records role-shape coverage for common JWT claim forms. `evidence/HARDENING_AUDIT_001.md` records OAuth2 common JWT role mapping as verified.

Residual risk or limitation: The app still trusts the configured OAuth issuer/JWK provider. Token lifecycle, key rotation, user provisioning, and issuer compromise remain deployment responsibilities.

### Invariant 7: Swagger/OpenAPI must not be publicly exposed in strong-auth mode by default.

Statement: OAuth2 strong-auth mode must gate Swagger/OpenAPI by default.

Why it matters: API documentation can expose route names, schemas, and operational structure that help attackers or unauthorized users.

Current enforcement mechanism: OAuth2 mode gates OpenAPI/Swagger unless explicitly configured otherwise.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` records Swagger/OpenAPI gating in OAuth2 mode. `evidence/THREAT_MODEL.md` identifies public docs in strong-auth mode as a concrete threat scenario.

Residual risk or limitation: API-key mode may intentionally keep docs public for portfolio/demo review. Production deployments should gate or disable docs according to their exposure model.

### Invariant 8: Telemetry export must be disabled by default in prod and cloud-sandbox.

Statement: Production-oriented profiles must not export OTLP metrics or expose Prometheus scrape endpoints by default.

Why it matters: Telemetry can reveal service names, route names, error rates, latency, host/runtime details, and operational patterns.

Current enforcement mechanism: Profile properties keep OTLP export disabled and actuator exposure limited for prod and cloud-sandbox.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` records prod/cloud-sandbox OTLP disabled-by-default and Prometheus disabled-by-default behavior. `evidence/SECURITY_POSTURE.md` summarizes telemetry posture.

Residual risk or limitation: Local/default demo behavior may expose metrics intentionally. Deployments must not expose local/demo settings as production.

### Invariant 9: OTLP telemetry export must require explicit opt-in and safe endpoint guardrails.

Statement: OTLP metrics export must require explicit enablement and endpoint validation before startup.

Why it matters: A public or credential-bearing collector endpoint can leak operational metadata or credentials through configuration.

Current enforcement mechanism: OTLP startup guardrails require an endpoint when enabled, reject blank/malformed endpoints, reject credentials/query/fragment components, and block public endpoints by default unless the private-endpoint requirement is deliberately overridden.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` records OTLP startup validation for missing, malformed, unsafe, credential-bearing, query-bearing, and fragment-bearing endpoint shapes. `evidence/HARDENING_AUDIT_001.md` records lab-grade OTLP guardrails.

Residual risk or limitation: Private endpoint validation is heuristic. It does not prove collector trust, TLS, IAM, firewall posture, network reachability, or egress enforcement.

### Invariant 10: Telemetry startup summaries and logs must not expose credentials, query strings, fragments, API keys, bearer tokens, or auth headers.

Statement: Telemetry diagnostics must be sanitized before logging and must avoid secret-bearing values.

Why it matters: Logs are often copied to CI, issue trackers, terminals, and observability systems. A startup summary must not become a credential leak.

Current enforcement mechanism: Telemetry startup summary logs only high-level enablement, actuator exposure, Prometheus state, and sanitized OTLP host-only detail.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` records sanitized startup summary coverage for credentials, query strings, fragments, bearer tokens, API keys, and auth headers.

Residual risk or limitation: Redaction is pattern-based across the project and not a full DLP system. Deployment logging pipelines still require access controls and retention policy.

### Invariant 11: API error envelopes must not expose stack traces or sensitive request details.

Statement: API failures should return safe error responses without stack traces, exception-class leakage, credentials, bearer tokens, auth headers, or sensitive request bodies.

Why it matters: Error responses are attacker-visible and can reveal implementation details or secrets.

Current enforcement mechanism: Core API validation, auth failures, request-size failures, malformed JSON handling, and LASE failure storage use safe envelopes or redaction where covered.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` records safe JSON envelope coverage for core input/API paths. `evidence/HARDENING_AUDIT_001.md` records sensitive logging and LASE redaction hardening.

Residual risk or limitation: Not every possible framework-generated 404/405/415 or proxy-level error surface is claimed to have identical project JSON-envelope behavior. Review should continue before hostile exposure.

### Invariant 12: Malformed CSV/JSON/import input must fail safely.

Statement: Malformed or dangerous import input must be rejected, quarantined, or failed without unsafe mutation, parser crashes, stack traces, or spreadsheet formula injection.

Why it matters: Import paths can feed server state, replay/evaluation, and reports. Malformed input must not become an execution or data-corruption path.

Current enforcement mechanism: CSV/JSON utilities validate schemas, reject malformed documents and rows, handle trailing JSON data, reject non-finite numeric values, and neutralize CSV formula injection.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` records malformed CSV/JSON, unexpected fields, non-finite values, empty input, trailing JSON, and CSV formula-injection tests.

Residual risk or limitation: Very large hostile files, parser implementation vulnerabilities, and future schema changes remain areas for periodic review.

### Invariant 13: Request-size/auth ordering must not allow unauthenticated callers to bypass protected route behavior.

Statement: Protected routes should authenticate or authorize unauthenticated callers before oversized-body rejection changes the observable behavior.

Why it matters: Filter-order drift can reveal protected route behavior to unauthenticated callers or create inconsistent failure semantics.

Current enforcement mechanism: API-key filter ordering and OAuth2 security/request-size ordering are configured to preserve protected-route posture.

Evidence/tests if known: `evidence/HARDENING_AUDIT_001.md` records OAuth authenticates before request-size rejection. `evidence/TEST_EVIDENCE.md` records auth-before-size behavior for protected paths.

Residual risk or limitation: Filter ordering is easy to regress during security or servlet changes. Keep route-specific oversized unauthenticated tests near any future filter changes.

### Invariant 14: Local/demo convenience must not silently change prod/cloud-sandbox safety posture.

Statement: Local/default conveniences must stay profile-scoped and must not weaken prod or cloud-sandbox auth, telemetry, actuator, or cloud guardrails.

Why it matters: Demo defaults are useful for development, but dangerous if they bleed into hardened profiles.

Current enforcement mechanism: Profiles separate local/default behavior from prod/cloud-sandbox behavior for auth, actuator exposure, Prometheus/OTLP settings, and cloud safety defaults.

Evidence/tests if known: `evidence/TEST_EVIDENCE.md` records local/default convenience alongside prod/cloud-sandbox hardened defaults. `evidence/SECURITY_POSTURE.md` describes prod/cloud-sandbox as hardened lab baselines rather than complete production systems.

Residual risk or limitation: Deployment automation must choose the correct profile and environment. The repository cannot prove external platform configuration.

### Invariant 15: Documentation must not claim stronger guarantees than the tests/evidence support.

Statement: README and evidence documents must distinguish implemented behavior, tested behavior, residual risks, and deployment responsibilities.

Why it matters: Overclaiming security is itself a safety failure because operators and reviewers may rely on unproven guarantees.

Current enforcement mechanism: Evidence documents explicitly label residual risks, deployment responsibilities, and portfolio/lab scope. README links to evidence rather than embedding brittle exact test-count claims.

Evidence/tests if known: `evidence/THREAT_MODEL.md`, `evidence/SECURITY_POSTURE.md`, `evidence/RESIDUAL_RISKS.md`, `evidence/TEST_EVIDENCE.md`, and `evidence/HARDENING_AUDIT_001.md` together define the current evidence set.

Residual risk or limitation: Documentation can drift from implementation. New features, profile changes, tests, or hardening work should update evidence in the same review cycle.

## Evidence and Test Mapping

| Area | Main invariant coverage | Evidence |
| --- | --- | --- |
| Replay/evaluation | Invariant 1 | `evidence/TEST_EVIDENCE.md`, `evidence/HARDENING_AUDIT_001.md`, `evidence/THREAT_MODEL.md` |
| LASE shadow | Invariant 2 | `evidence/TEST_EVIDENCE.md`, `evidence/HARDENING_AUDIT_001.md`, `evidence/SECURITY_POSTURE.md` |
| Cloud safety | Invariants 3 and 4 | `evidence/SECURITY_POSTURE.md`, `evidence/TEST_EVIDENCE.md`, `evidence/RESIDUAL_RISKS.md` |
| Auth/RBAC | Invariants 5, 6, and 7 | `evidence/SECURITY_POSTURE.md`, `evidence/TEST_EVIDENCE.md`, `evidence/THREAT_MODEL.md` |
| Telemetry | Invariants 8, 9, and 10 | `evidence/SECURITY_POSTURE.md`, `evidence/TEST_EVIDENCE.md`, `evidence/HARDENING_AUDIT_001.md` |
| Input/API hardening | Invariants 11 and 12 | `evidence/TEST_EVIDENCE.md`, `evidence/HARDENING_AUDIT_001.md`, `evidence/RESIDUAL_RISKS.md` |
| Filter/profile boundaries | Invariants 13 and 14 | `evidence/TEST_EVIDENCE.md`, `evidence/HARDENING_AUDIT_001.md`, `evidence/SECURITY_POSTURE.md` |
| Documentation truthfulness | Invariant 15 | `evidence/THREAT_MODEL.md`, `evidence/RESIDUAL_RISKS.md`, `evidence/HARDENING_AUDIT_001.md` |

## Residual Risks

The standing residual-risk register is `evidence/RESIDUAL_RISKS.md`. The most relevant residuals and recently mitigated items for these invariants are:

- Real AWS validation is outside default CI and must run only in disposable, explicitly guarded sandbox infrastructure.
- Production TLS, IAM, firewalling, external rate limiting, secret rotation, deployment identity, log retention, and collector access controls remain deployment responsibilities.
- OTLP private endpoint validation is heuristic and does not prove collector trust or network security.
- Redaction is pattern-based and not full DLP.
- Allocation/evaluation request DTOs reject omitted enterprise-required fields instead of silently defaulting JSON omissions to `0`, `0.0`, or `false`.
- Cloud-sandbox fixed-prefix enforcement should be strengthened if operators can override prefixes in an untrusted environment.
- Not every possible framework-generated error surface is claimed to have identical safe-envelope behavior.

## Future Formalization Opportunities

- Convert this document into machine-checkable policy tests for profile defaults, endpoint exposure, and cloud mutation gates.
- Add explicit tests for every documented API-key mode alias if multiple spellings are supported externally.
- Strengthen cloud-sandbox fixed-prefix enforcement around the documented `lbp-sandbox-` namespace.
- Add mutation testing or architecture tests that forbid replay/evaluation packages from depending on CloudManager or AWS clients.
- Add API contract tests for less-common framework-generated errors such as all 404/405/415 variants.
- Add deployment evidence templates for TLS, IAM, firewall, rate limiting, secret rotation, OAuth issuer operations, and collector security.
- Keep nullable validated DTO fields for required request input, and apply the same omission-review rule to future API request DTOs.
