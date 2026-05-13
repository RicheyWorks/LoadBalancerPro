# Private-Network Proxy Profile Plan

This is a design and rollout plan for a future controlled private-network proxy validation profile. It now includes an opt-in configuration-validation primitive, but it does not add private-network live execution, does not change proxy request routing, and does not change the current local-only evidence path.

Use this plan after reviewing [`LIVE_PROXY_CONTAINMENT.md`](LIVE_PROXY_CONTAINMENT.md), [`PRIVATE_NETWORK_PROXY_DRY_RUN.md`](PRIVATE_NETWORK_PROXY_DRY_RUN.md), [`PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md`](PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md), [`REAL_BACKEND_PROXY_EXAMPLES.md`](REAL_BACKEND_PROXY_EXAMPLES.md), and [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md).

## Intended Use Case

The future profile should let an operator prove that optional `/proxy/**` mode can forward to a small set of explicit local or private HTTP backends they control. It should be useful for lab VLANs, local container networks, VPN-only test services, or private CI environments where the backend URLs are known before the run.

The current default remains loopback-only evidence with source-visible JUnit and JDK `HttpServer` fixtures. Any private-network validation must be opt-in, documented, deterministic, and reviewable before traffic is sent.

## Non-Goals

- No public internet validation.
- No production gateway claim.
- No benchmark, certification, identity, TLS, WAF, or compliance proof.
- No host discovery, DNS enumeration, subnet scanning, or port scanning.
- No service installation, scheduled tasks, startup entries, persistence mechanisms, hidden agents, or credential storage.
- No native executables, installers, wrappers, packers, `native-image`, `launch4j`, `jpackage`, self-extracting archives, downloaded servers, or vendored binaries.
- No release assets, tags, release workflows, or `release-downloads/` mutation.

## Safety Boundaries

- Proxy mode stays disabled by default.
- Private-network validation must be an explicit operator action, not a default startup side effect.
- The profile must accept operator-provided explicit backend URLs only.
- The profile must not expand hostnames, CIDR ranges, IP ranges, or service names into target lists.
- Health checks may call only configured backend URLs plus the configured relative health path.
- Retries remain bounded and must keep non-idempotent retries disabled unless separately approved.
- Cooldown and counters remain process-local memory only.
- Generated evidence must stay under ignored build output such as `target/` and must not be committed as runtime output.

## Allowed Hosts Model

Future implementation should validate each configured backend URL before startup or reload succeeds. The first safe model is an allowlist, not discovery:

- Loopback: `localhost`, `127.0.0.0/8`, and `::1`.
- RFC1918 IPv4 private ranges: `10.0.0.0/8`, `172.16.0.0/12`, and `192.168.0.0/16`.
- IPv6 unique local addresses: `fc00::/7`, only if address validation is implemented clearly.
- Exact operator-approved hostnames may be allowed only when their resolved addresses are checked against the same local/private-network constraints.

URLs with user info, query strings, fragments, blank hosts, unsupported schemes, public addresses, wildcard domains, or ambiguous resolution should fail closed during startup or explicit reload validation.

Implemented configuration primitive: `ProxyBackendUrlClassifier` is a source-visible Java helper for offline classification only. It classifies literal `http`/`https` backend URLs as loopback allowed, private-network allowed, public-network rejected, invalid rejected, unsupported-scheme rejected, user-info rejected, or ambiguous-host rejected. When `loadbalancerpro.proxy.private-network-validation.enabled=true`, startup and explicit proxy reload validation use that classifier so unsafe backend URLs fail closed before becoming an active config. The gate does not resolve DNS, perform reachability checks, scan ports, discover hosts, change default/local/demo behavior, add private-network live execution, or add script, Postman, or smoke execution. The dry-run-only reviewer recipe in [`PRIVATE_NETWORK_PROXY_DRY_RUN.md`](PRIVATE_NETWORK_PROXY_DRY_RUN.md) generates ignored Markdown/JSON evidence from classifier samples without sending traffic. The future live path must satisfy [`PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md`](PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md) before any private-network traffic is implemented.

## API-Key And OAuth2 Expectations

Private-network validation must preserve the existing access boundary:

- Local/default mode remains demo-friendly and is not a security boundary.
- Prod and cloud-sandbox API-key modes must continue to require `X-API-Key` for `/proxy/**`, `GET /api/proxy/status`, and proxy reload.
- OAuth2 mode must continue to require the configured allocation role for `/proxy/**`, `GET /api/proxy/status`, and proxy reload.
- TLS termination, ingress exposure, rate limits, and identity provider controls remain deployment responsibilities.

Docs, scripts, examples, Postman files, and generated evidence must use placeholders or redaction for API keys, tokens, hostnames, and operator notes. No API key, OAuth2 token, credential, or secret may be persisted in the repository, browser `localStorage`, browser `sessionStorage`, URLs, or generated evidence.

## Evidence And Redaction

Evidence should show the minimum proof needed for review:

- profile name and opt-in flag;
- explicitly configured backend URL labels with sensitive hostnames redacted when needed;
- local/private-network classification result;
- request path through `/proxy/**`;
- backend receipt proof;
- response status, body label, and `X-LoadBalancerPro-Upstream` / `X-LoadBalancerPro-Strategy` headers;
- prod API-key `401`/`200` boundary or OAuth2 `401`/`403`/authorized boundary, depending on the profile.

Generated evidence should be Markdown or JSON under ignored `target/` output. It must never include raw API keys, bearer tokens, secrets, private hostnames marked for redaction, release assets, or copied files from `release-downloads/`.

## Failure Behavior

Invalid configuration should fail before traffic:

- unsupported scheme, user info, query string, fragment, blank host, public address, or unapproved address range fails startup or explicit reload;
- a backend that is configured with `healthy=false` remains disabled;
- no eligible backend returns controlled `503` behavior;
- unreachable selected backends return controlled `502` behavior;
- private-network validation must not fall back to public URLs, generated targets, or discovered hosts.

If active health checks are enabled, probes remain bounded to each explicitly configured backend and relative health path. Probe failures should be reported through the existing status/evidence paths without installing agents or persisting state.

## Test Strategy

Before any live private-network execution, add or preserve static tests that prove the plan is linked and keeps the no-scanning, no-persistence, no-native-tooling, no-secret-persistence, and no-release boundaries.

Runtime validation should progress in this order:

1. Keep CI on loopback-only JUnit/JDK `HttpServer` evidence.
2. Add pure Java unit tests for `ProxyBackendUrlClassifier` before it is wired into runtime.
3. Add startup/reload validation tests for allowed and rejected backend URLs.
4. Use the implemented dry-run-only private-network profile recipe to write ignored Markdown/JSON evidence from intended validation inputs without sending traffic.
5. Satisfy the live validation gate design in [`PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md`](PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md) before any private-network traffic is implemented.
6. Add opt-in private-network live smoke only after a separate reviewed task approves the exact environment gate and operator-provided URLs.

No test should scan ports, discover hosts, require public DNS, require live cloud resources, download servers, or write secrets.

## Rollout Plan

1. Design and guard: this plan plus static documentation tests only.
2. Classify: source-visible Java `ProxyBackendUrlClassifier` with focused unit tests.
3. Gate: opt-in configuration validation properties and explicit failure messages for unsupported hosts, without live private-network execution.
4. Evidence: implemented dry-run Markdown/JSON output under ignored `target/proxy-evidence/private-network-validation-dry-run.md` and `target/proxy-evidence/private-network-validation-dry-run.json`.
5. Live gate: document and test the exact future flags, operator approval, timeout, evidence, redaction, and fail-closed behavior before implementation.
6. Smoke: dry-run first, then separately approved private-network live smoke with operator-provided URLs only.
7. Review: merge only with CI, CodeQL, docs/static tests, security tests, and existing smoke dry-runs passing.
