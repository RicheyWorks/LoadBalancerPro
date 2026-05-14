# LoadBalancerPro Test Evidence

Default verification command:

```bash
mvn -q test
```

## OAuth/API-Key Tests

- Prod and cloud-sandbox API-key mode reject missing or wrong API keys and allow correct keys for protected endpoints.
- OAuth2 mode rejects missing or invalid bearer tokens with 401 and valid tokens with insufficient role with 403.
- OAuth2 mode allows observer access to LASE shadow observability and operator access to allocation mutations.
- OAuth2 mode gates Swagger/OpenAPI by default while keeping `/api/health` public.
- CORS preflight coverage verifies configured origins and auth-related headers.

## Telemetry Guardrail Tests

- OTLP metrics export is disabled by default.
- Prod and cloud-sandbox profiles keep OTLP disabled and Prometheus endpoint exposure disabled by default.
- OTLP startup validation covers missing, malformed, unsafe, credential-bearing, query-bearing, and fragment-bearing endpoint shapes.
- Startup summaries are sanitized and do not include credentials, query strings, fragments, bearer tokens, API keys, or auth headers.

## LASE Redaction/Shadow Tests

- LASE shadow mode remains advisory and does not construct or call `CloudManager`.
- Shadow recommendations do not alter allocation responses.
- Evaluator failures are captured as fail-safe shadow events.
- Sensitive-looking failure text is redacted while useful non-sensitive context is preserved.
- Control characters are neutralized in stored failure reasons.
- Adaptive-routing experiment tests cover deterministic fixtures, shadow-only default behavior, recommend/active-experiment comparison output, stale-signal guardrails, all-unhealthy degradation, CLI mode selection, rollback reasons, policy decisions, and ignored `target/adaptive-routing-experiments/` evidence generation safety.
- Enterprise Lab workflow tests cover stable scenario catalog metadata, unique scenario ids, deterministic run output, bounded process-local in-memory run retention, bounded audit-event retention, scorecard counts, prod API-key protection for `/api/lab/**`, source-visible evidence export safety under ignored `target/enterprise-lab-runs/`, browser lab page content, and smoke-script publish-command exclusions.
- Controlled active LASE policy tests cover `off`, `shadow`, `recommend`, and `active-experiment` modes; safe default and invalid-config fail-closed behavior; health, eligibility, capacity, freshness, conflict, all-unhealthy, rollback, and bounded-context gates; audit-event redaction and bounds; `/api/lab/policy` and `/api/lab/audit-events` auth boundaries; and ignored `target/controlled-adaptive-routing/` smoke evidence safety.

## Replay/Cloud Isolation Tests

- Replay mode is offline and does not require AWS credentials.
- Replay input errors fail safely without raw content exposure.
- CloudManager dry-run mode avoids AWS mutation calls.
- Live mutation paths are guarded by operator intent, capacity limits, account/region checks, and ownership checks.
- Cloud-sandbox behavior is constrained by documented sandbox resource-name prefix guardrails.

## Input/API Hardening Tests

- Allocation API validation rejects malformed JSON, invalid server fields, negative load, empty server lists, and oversized request bodies with safe JSON envelopes.
- Safe error envelopes avoid stack traces and exception-class leakage.
- CSV/JSON import tests cover malformed rows, unexpected fields, non-finite values, empty input, trailing JSON data, and CSV formula-injection handling.
- Allocation/evaluation DTO omission coverage verifies omitted `requestedLoad`, server telemetry, capacity, weight, and health fields fail validation instead of defaulting to `0`, `0.0`, or `false`.

## Current Evidence Set

- [Hardening Audit 001](HARDENING_AUDIT_001.md)
- [Security Posture](SECURITY_POSTURE.md)
- [Residual Risks](RESIDUAL_RISKS.md)
- [Production-Candidate Evidence Gate](../docs/PRODUCTION_CANDIDATE_EVIDENCE_GATE.md)
- [v2.5.0 Post-Release Verification](../docs/V2_5_0_POST_RELEASE_VERIFICATION.md)

## v2.5.0 Release Verification Evidence

- Exact release tag `v2.5.0` points to commit `4cc03750be5479d9f8f88f8ef8014e05a8dc587a`.
- Release Artifacts workflow run `25838247936` completed successfully.
- Expected assets were verified: `LoadBalancerPro-2.5.0.jar`, `LoadBalancerPro-2.5.0-bom.json`, `LoadBalancerPro-2.5.0-bom.xml`, and `LoadBalancerPro-2.5.0-SHA256SUMS.txt`.
- SHA-256 checksum verification passed for the downloaded JAR and SBOM assets.
- SBOM JSON/XML assets are present.
- GitHub artifact attestation verification passed for the release JAR provenance, and the release workflow's JAR/SBOM attestation step completed successfully.
- Container publication and container signing remain deferred.
