# Live Proxy Containment

Live/proxy mode is opt-in. LoadBalancerPro must continue to default to local, mock, or explicitly configured behavior, with production and cloud behavior remaining deliberate and security-gated.

This document is a containment baseline for future real-backend proxy work. It does not change the current Spring Boot runtime, proxy defaults, API-key behavior, OAuth2 documentation, smoke behavior, or production/cloud guardrails.

## Default Validation Scope

Default validation should use localhost or private-network backends. Prefer loopback fixture services, configured local ports, checked-in example profiles, and deterministic Postman or Maven tests.

Real-backend tests should be deterministic, documented, and low-risk. They should name the exact backend class, host pattern, ports, profile, expected HTTP status, and cleanup behavior. They should avoid public internet targets unless a future task explicitly approves that scope.

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
