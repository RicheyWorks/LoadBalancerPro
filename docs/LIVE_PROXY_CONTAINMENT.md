# Live Proxy Containment

Live/proxy mode is opt-in. LoadBalancerPro must continue to default to local, mock, or explicitly configured behavior, with production and cloud behavior remaining deliberate and security-gated.

This document is a containment baseline for future real-backend proxy work. It does not change the current Spring Boot runtime, proxy defaults, API-key behavior, OAuth2 documentation, smoke behavior, or production/cloud guardrails.

## Default Validation Scope

Default validation should use localhost or private-network backends. Prefer loopback fixture services, configured local ports, checked-in example profiles, and deterministic Postman or Maven tests.

Real-backend tests should be deterministic, documented, and low-risk. They should name the exact backend class, host pattern, ports, profile, expected HTTP status, and cleanup behavior. They should avoid public internet targets unless a future task explicitly approves that scope.

The current automated real-backend validation path is the source-visible Maven test `LocalOnlyRealBackendProxyValidationTest`. It starts JDK `HttpServer` fixtures on `127.0.0.1` using Java-assigned ephemeral loopback ports, opts proxy mode in through test properties, proves status/body/header forwarding through `/proxy/**`, and checks a configured-unavailable backend fails closed without calling the fixture. It does not scan ports, install tools, persist state, store secrets, or call external networks.

For reviewer-readable export, `LocalProxyEvidenceExportTest` uses the same source-visible loopback pattern and writes sanitized Markdown and JSON to `target/proxy-evidence/local-proxy-evidence.md` and `target/proxy-evidence/local-proxy-evidence.json`. The generated evidence shows the loopback backend start policy, proxied `/proxy/**` request, backend receipt, forwarding response status/body/headers, and prod API-key boundary checks with the key redacted. The files stay under ignored Maven build output and are not tracked documentation artifacts.

For the future path beyond loopback, [`PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md`](PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md) defines the design-only private-network profile guardrails: explicit operator-provided backend URLs only, local/private-network allowlisting, no discovery or scanning, no secret persistence, redacted ignored evidence, and no runtime behavior change until a separate reviewed implementation task.

## Containment Rules

- Live/proxy mode is opt-in and must not become the default profile behavior.
- No port scanning.
- No persistence mechanisms for proxy demo state, probe state, operator checklist state, or route-test state.
- No scheduled tasks.
- No service installation.
- No credential storage in the repository.
- No browser `localStorage` for operator secrets by default.
- No browser `sessionStorage`, cookies, generated reports, or URLs for operator secrets by default.
- No suspicious downloads or downloaded helper binaries.
- No hidden background agents.
- No native executable wrappers, installers, packers, or self-extracting archives.
- No release asset generation, release upload, tag changes, ruleset changes, default-branch changes, or `release-downloads/` mutation as part of proxy validation.

## Operator Secret Boundary

Operator secrets should remain runtime-provided and local to the operator environment. Shared docs, Postman files, scripts, and examples should use placeholders such as `<API_KEY>` or `CHANGE_ME_LOCAL_API_KEY`.

Browser operator surfaces should keep entered secrets in memory only unless a future reviewed change explicitly accepts and documents a different risk. Copyable commands and exported evidence should redact or placeholder secrets.

## Production And Cloud Boundary

Production/cloud behavior must remain explicit and security-gated. Prod API-key mode, cloud-sandbox API-key mode, OAuth2 mode, TLS termination, ingress exposure, and rate-limit decisions must stay documented as deployment choices rather than hidden side effects of a smoke script or proxy example.

Proxy validation should not weaken API-key checks, OAuth2 guidance, CSRF posture, actuator exposure guidance, proxy protections, cloud guardrails, or smoke expectations. If a future real-backend test cannot fit these boundaries, split it into a separate reviewed task before implementation.

## Future Test Checklist

Before adding a future real-backend proxy test, confirm:

- The test is opt-in or safely local by default.
- The backend target is localhost or private-network unless explicitly approved.
- The test does not scan ports or discover hosts.
- The test does not install a service, scheduled task, startup entry, or background agent.
- The test stores no credentials in the repo or browser persistent storage.
- The test uses source-visible Java, Maven, PowerShell, Postman, Docker docs, or GitHub Actions patterns.
- The test produces deterministic, sanitized evidence only under ignored local output paths.
