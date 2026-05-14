# Postman Enterprise Lab Collection

LoadBalancerPro includes a deterministic Postman lab for localhost evaluation across local/demo and prod API-key modes.

For repository tooling containment, see [`ANTIVIRUS_SAFE_DEVELOPMENT.md`](ANTIVIRUS_SAFE_DEVELOPMENT.md). For live/proxy validation containment, see [`LIVE_PROXY_CONTAINMENT.md`](LIVE_PROXY_CONTAINMENT.md); Postman examples should remain localhost/private-network oriented, placeholder-only, and opt-in for proxy mode.

Files:

- `docs/postman/LoadBalancerPro Enterprise Lab.postman_collection.json`
- `docs/postman/LoadBalancerPro Local.postman_environment.json`

## Import

1. In Postman, import the collection JSON.
2. Import the local environment JSON.
3. Select the `LoadBalancerPro Local` environment.
4. Keep `baseUrl` set to `http://localhost:8080` unless your local app is bound to another local port.
5. For prod or cloud-sandbox API-key testing, replace the `apiKey` placeholder with your local test value in Postman only. Do not commit real keys.

The environment contains placeholders only. It has no generated export timestamp, no current secret value, no bearer token, and no live public endpoint.

## Safe Run Order

- Use `Local / Demo Health` for the default/local profile. `/v3/api-docs` and Swagger UI are expected to remain public there.
- Use `Prod API-Key Boundary` after starting the app in prod or cloud-sandbox API-key mode. Missing-key OpenAPI, Swagger, and `/api/**` requests should return HTTP 401 except for `GET /api/health` and unauthenticated `OPTIONS` preflight requests. With-key requests send `X-API-Key: {{apiKey}}`.
- Use `Routing Lab`, `Allocation / Evaluation Lab`, `Scenario Replay Lab`, and `Remediation Report Lab` for cockpit-equivalent API exploration. These requests are deterministic, synthetic, and advisory.
- Use the Enterprise Lab API checks for `GET /api/lab/scenarios` and `POST /api/lab/runs` when reviewing deterministic scenario catalog, baseline/shadow/opt-in influence comparison, scorecards, and lab-grade evidence output.
- Use `Proxy Operator Lab` carefully. `GET /api/proxy/status` is read-only. The reload example is process-local and operator-controlled; run it only when proxy mode is enabled and local reload is intended.
- Use `Evidence / Offline Tooling Pointers` for packaged evidence training APIs. Offline CLI evidence workflows remain documented separately and are not faked as HTTP endpoints.

## Local Smoke Harness

The source-visible local-only Postman-aligned smoke harness validates the collection's critical 200/401 expectations without requiring Newman, native tools, or additional project dependencies. Running the script without `-Package`, or with `-DryRun`, performs a dry run only.

Dry-run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\postman-enterprise-lab-safe-smoke.ps1 -DryRun
```

Live packaged smoke:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\postman-enterprise-lab-safe-smoke.ps1 -Package
```

Optional sanitized evidence export:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\postman-enterprise-lab-safe-smoke.ps1 -Package -EvidenceDir target\postman-enterprise-lab-smoke
```

The live harness starts or reuses loopback local/demo and prod API-key profiles, verifies local root/health/OpenAPI/Swagger HTTP 200 behavior, verifies local Enterprise Lab scenario/run behavior, verifies prod missing-key and wrong-key HTTP 401 behavior for OpenAPI/Swagger and representative `/api/**` routes, verifies prod Enterprise Lab requests require and accept the configured test `X-API-Key`, verifies the configured test `X-API-Key` succeeds, and checks that Actuator metrics and Prometheus are not public in prod. Evidence files are sanitized Markdown and sanitized JSON summaries. The configured API key is always rendered as `<REDACTED>` in output and evidence.

## Security Notes

PR #98 gated prod/cloud-sandbox `/v3/api-docs` and Swagger UI behind `X-API-Key` while preserving public local/demo OpenAPI usability. This collection demonstrates that boundary without weakening it.

Protected prod-like requests use `X-API-Key: {{apiKey}}`. The collection does not use `Authorization` bearer tokens because OAuth2 cockpit and Postman auth flows are deferred to a future sprint.

No real secrets belong in the collection, environment, docs, or shared examples. Do not paste real secrets into committed files. Do not store API keys in browser `localStorage` or `sessionStorage`. Keep Postman environment values local to your workstation, and use `<API_KEY>` in shared examples.

Included requests use localhost or loopback placeholders only. They do not call public services, create releases, create tags, change rulesets, delete branches, mutate cloud state, construct cloud control paths, or make Actuator metrics/Prometheus public.

## Troubleshooting

- `401` in prod/cloud-sandbox API-key mode usually means `apiKey` is missing or wrong.
- `404` for Swagger UI may mean your local SpringDoc route differs; try `/swagger-ui.html` or inspect `/v3/api-docs` with the configured API key.
- `409` from `POST /api/proxy/reload` means proxy mode was not enabled at startup.
- Smoke harness failures print the checked route, expected status, actual status, and child process log paths. They never print the configured API key.
- If a protected request works without `X-API-Key` in prod/cloud-sandbox API-key mode, stop and re-check the active profile and `loadbalancerpro.auth.mode`.

This lab and its smoke harness are enterprise-demo hardening support, not production IAM, SSO, certification, compliance proof, OAuth2 validation, or public exposure approval.
