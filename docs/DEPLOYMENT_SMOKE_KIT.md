# Deployment Smoke Kit

Use this smoke kit after choosing a run mode in [`OPERATOR_RUN_PROFILES.md`](OPERATOR_RUN_PROFILES.md). It gives operators and reviewers one local-only path for checking the packaged jar, the prod API-key boundary, and the proxy-loopback recipe without external services.

This is a local deployment confidence check. It is not production certification, a benchmark, public exposure approval, release evidence, or a security guarantee.

## What It Proves

- The documented packaged-jar command can start the app on `127.0.0.1`.
- The root landing page and `/api/health` are reachable in local demo mode.
- Prod API-key mode rejects `GET /api/proxy/status` without `X-API-Key`.
- Prod API-key mode accepts `GET /api/proxy/status` with the configured placeholder `X-API-Key`.
- The proxy-loopback example can forward a request from `/proxy/api/smoke` to one of two loopback HTTP backends.
- Child app and backend processes are cleaned up on success or failure.

## What It Does Not Prove

- It does not prove public ingress safety, managed gateway behavior, identity lifecycle, TLS, rate limits, high availability, or operational readiness.
- It does not create tags, GitHub Releases, release assets, generated reports, checksums, or release evidence files.
- It does not require cloud credentials, call cloud APIs, or mutate cloud state.
- It does not replace CI, manual review, or environment-specific deployment controls.

## Prerequisites

- Java 17
- Maven if you run the live package path
- PowerShell 7 (`pwsh`) or Windows PowerShell
- A local checkout of this repository

If local Maven dependency resolution fails because the workstation trust store cannot validate Maven Central certificates, including PKIX trust-chain errors, treat GitHub CI as the build/package source of truth and run the smoke kit later from a workstation with a working trust chain.

## Commands

Dry-run mode checks required files and prints the live command without starting Java or Maven:

```powershell
pwsh ./scripts/smoke/operator-run-profiles-smoke.ps1 -DryRun
```

Windows PowerShell fallback:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke\operator-run-profiles-smoke.ps1 -DryRun
```

Live mode builds the packaged jar if needed, starts each profile on loopback, verifies the API-key boundary, starts two loopback backend fixtures, verifies proxy forwarding, and cleans up:

```powershell
pwsh ./scripts/smoke/operator-run-profiles-smoke.ps1 -Package
```

If you already built the jar and want to avoid Maven:

```powershell
pwsh ./scripts/smoke/operator-run-profiles-smoke.ps1 -SkipPackage
```

Expected output includes `PASS:`, `WARN:`, and `FAIL:` lines. A clear smoke failure exits nonzero.

## Checks

### Local Demo

The script starts:

```text
java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=18080 --spring.profiles.active=local
```

It verifies:

```text
http://127.0.0.1:18080/api/health
http://127.0.0.1:18080/
```

### Prod API-Key Boundary

The script starts prod profile mode with the placeholder key `CHANGE_ME_LOCAL_API_KEY` on loopback. It verifies:

```text
GET http://127.0.0.1:18081/api/proxy/status -> HTTP 401 without X-API-Key
GET http://127.0.0.1:18081/api/proxy/status -> HTTP 200 with X-API-Key
```

This confirms the documented prod API-key boundary for the proxy status surface. It does not make the local profile a public security boundary.

### Proxy-Loopback

The script starts two loopback-only helper backends:

```text
http://127.0.0.1:18181
http://127.0.0.1:18182
```

It imports:

```text
docs/examples/operator-run-profiles/proxy-loopback.properties
```

Then it verifies:

```text
GET http://127.0.0.1:18082/proxy/api/smoke?step=1 -> HTTP 200 from a loopback backend
GET http://127.0.0.1:18082/api/proxy/status -> HTTP 200
```

Proxy mode remains disabled by default in `src/main/resources/application.properties`; the smoke kit enables it only through the explicit proxy-loopback example.

## Cleanup

The script wraps app processes and backend jobs in `finally` cleanup logic. On success or failure it stops child Java processes and loopback backend jobs before exiting.

If a local terminal is interrupted, verify no leftover Java process is still bound to the selected ports before rerunning:

```powershell
Get-NetTCPConnection -LocalPort 18080,18081,18082,18181,18182 -ErrorAction SilentlyContinue
```

## Troubleshooting Live Startup

The live smoke script waits for startup by retrying the exact loopback endpoint it is checking. During startup it prints the checked URL, attempt count, request timeout, and retry delay. If a profile never becomes reachable, the script prints the app process exit code when available, the stdout/stderr log paths under the user temp directory, and the last log lines before cleanup.

If the first local-demo check reports connection failures, inspect the printed log tail before assuming the packaged jar is broken. Common local causes are a slow first Spring Boot startup, a selected port already in use, local security software delaying loopback connections, or an app-side startup error visible in the temp logs.

## Safety Boundaries

- Local-only network targets: `127.0.0.1` and `localhost`.
- Placeholder secrets only.
- No cloud credentials.
- No cloud mutation.
- No release workflow changes.
- No tag, release, or asset creation.
- No generated artifacts committed.
- No default behavior changes.
- No proxy enablement outside the explicit proxy-loopback smoke step.
- No production gateway, benchmark result, certification outcome, legal proof, identity proof, or TLS implementation claim.

## Related Guides

- [`OPERATOR_RUN_PROFILES.md`](OPERATOR_RUN_PROFILES.md) for the run-mode matrix and copyable recipes.
- [`OPERATOR_PACKAGING.md`](OPERATOR_PACKAGING.md) for packaged jar and fixture launcher details.
- [`OPERATOR_DISTRIBUTION_SMOKE_KIT.md`](OPERATOR_DISTRIBUTION_SMOKE_KIT.md) for the older release-free distribution smoke helpers.
- [`API_SECURITY.md`](API_SECURITY.md) for API-key and OAuth2 boundaries.
- [`REVERSE_PROXY_MODE.md`](REVERSE_PROXY_MODE.md) for `/proxy/**` behavior.
- [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md) for the top-level evidence map.
