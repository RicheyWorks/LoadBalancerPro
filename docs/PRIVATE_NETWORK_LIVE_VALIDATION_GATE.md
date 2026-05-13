# Private-Network Live Validation Gate

This is a design gate for a future private-network live validation path. The current implementation adds default-off live gate properties and the offline `PrivateNetworkLiveValidationGate` decision helper, but it does not implement live private-network traffic execution, does not send traffic, does not change proxy request routing, and does not change default/local/demo behavior.

Use this after [`PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md`](PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md), [`PRIVATE_NETWORK_PROXY_DRY_RUN.md`](PRIVATE_NETWORK_PROXY_DRY_RUN.md), [`LIVE_PROXY_CONTAINMENT.md`](LIVE_PROXY_CONTAINMENT.md), and [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md). The current safe path remains the dry-run command:

```bash
mvn -Dtest=PrivateNetworkProxyDryRunEvidenceTest test
```

## Approval Gate

Private-network live validation must not be added by default or implied by existing proxy mode. The implemented offline gate evaluates the same approval requirements a future traffic executor must satisfy before any private-network request is sent:

- an explicit reviewed task approving live private-network validation;
- `loadbalancerpro.proxy.enabled=true`;
- `loadbalancerpro.proxy.private-network-validation.enabled=true`;
- `loadbalancerpro.proxy.private-network-live-validation.enabled=true`, defaulting to `false`;
- `loadbalancerpro.proxy.private-network-live-validation.operator-approved=true`, defaulting to `false`;
- operator-provided literal backend URLs only;
- passing `ProxyBackendUrlClassifier` results for every backend before activation;
- prod/cloud-sandbox API-key or OAuth2 boundary proof for protected proxy/status/reload surfaces.

If any gate is missing, malformed, ambiguous, or rejected, the offline gate returns not-enabled or blocked. A future live validation path must fail closed before sending traffic and before making the candidate config active.

## Allowed Backend Model

The future live path may target only explicit backend URLs that the operator provides in configuration or reload payloads. It must not expand hostnames, CIDR ranges, IP ranges, service names, wildcard patterns, inventory files, or environment-specific discovery into target lists.

Allowed targets are limited to classifier-approved loopback or private literal addresses:

- loopback: `localhost`, `127.0.0.0/8`, and `::1`;
- RFC1918 IPv4 private ranges: `10.0.0.0/8`, `172.16.0.0/12`, and `192.168.0.0/16`;
- IPv6 unique local addresses: `fc00::/7`.

Public internet targets, domain names without a separately approved resolver policy, userinfo URLs, unsupported schemes, query strings, fragments, blank hosts, wildcard hosts, broad hosts, and ambiguous numeric host forms must fail closed.

## Runtime Rules

A future implementation must keep the live path bounded and auditable:

- no DNS resolution, `InetAddress.getByName`, reachability checks, socket probes, host discovery, subnet scanning, port scanning, or public-network validation;
- no Postman private-network live execution by default;
- no smoke private-network live execution by default;
- no persistence, service installation, scheduled tasks, startup entries, hidden agents, credential storage, or secret persistence;
- no native executables, installers, wrappers, packers, `native-image`, `launch4j`, `jpackage`, self-extracting archives, downloaded servers, or vendored binaries;
- no release assets, tags, release workflows, or `release-downloads/` mutation.

Live validation must use existing Java/Spring source-visible code only. Any timeout must be bounded, documented, and short enough for operator review, such as a default at or below two seconds and a capped maximum. Retries must stay disabled unless a separate reviewed task approves bounded retry semantics for validation traffic.

## Failure And Abort Behavior

The future live validator must preserve the existing fail-closed configuration behavior:

- invalid or rejected backend URLs fail before traffic;
- missing operator approval fails before traffic;
- failed classifier validation fails before traffic;
- timeout, connection failure, unexpected status, oversized body, or malformed response produces a controlled failure;
- explicit reload failure preserves the last-known-good active config;
- startup failure must not silently fall back to public URLs, generated targets, discovered hosts, or default demo targets;
- abort must stop validation promptly without persisting state or leaving background work.

Operator warnings should be explicit before live validation runs. The warning must state that private-network traffic will be sent only to the configured classifier-approved literal URLs, that no public internet target is allowed, and that DNS, discovery, scanning, and persistence remain forbidden.

## Evidence And Redaction

Generated evidence must be Markdown or JSON under ignored `target/` output, for example `target/proxy-evidence/`. Evidence may record:

- profile name and live gate flags;
- redacted configured backend labels and classifier status;
- request method/path planned or executed by the validator;
- response status/header/body labels needed for proof;
- prod API-key `401`/`200` boundary or OAuth2 `401`/`403`/authorized boundary;
- timeout/failure classification when validation fails.

Evidence must never include raw API keys, bearer tokens, credentials, private hostnames marked for redaction, request secrets, release assets, or files copied from `release-downloads/`.

## Current Implemented Gate

Implemented source-visible pieces:

- default-false properties in `application.properties`:
  - `loadbalancerpro.proxy.private-network-live-validation.enabled=false`;
  - `loadbalancerpro.proxy.private-network-live-validation.operator-approved=false`;
- `PrivateNetworkLiveValidationGate`, which evaluates configuration only and returns allowed, not-enabled, or blocked results;
- focused tests proving missing flags, missing operator approval, disabled config validation, disabled proxy mode, and classifier-rejected targets fail closed;
- focused tests proving loopback/private literal targets can pass the offline gate without any backend listener running;
- source guards proving the gate does not use DNS, reachability, socket, probe, discovery, or scanning APIs.

The current gate is not called from app startup, Postman, smoke scripts, or a private-network traffic executor. It is a prerequisite for future live validation, not live execution.

## Implementation Checklist

Before any future live private-network traffic executor is added, the PR must prove:

- explicit property enablement is required and default-off;
- explicit `operator-approved=true` approval is required;
- backend URLs are operator-provided literals only;
- every backend passes `ProxyBackendUrlClassifier` before activation;
- DNS resolution is not used;
- discovery and scanning are not used;
- public internet targets fail closed;
- timeout is bounded and documented;
- no persistence, service installation, scheduled tasks, hidden agents, or credential storage is added;
- generated evidence is redacted and written only under ignored `target/` output;
- prod API-key boundary proof or OAuth2 boundary proof is included;
- reload failure preserves the last-known-good active config;
- local/private-only tests are deterministic and source-visible;
- Postman and smoke paths remain dry-run-only by default;
- no native tooling, downloaded helper binaries, release assets, or `release-downloads/` mutation is introduced.

## Required Tests Before Traffic Execution

The future traffic-execution PR must include focused tests for:

- default-off behavior for every live-validation flag;
- missing operator approval fails closed before traffic;
- classifier-rejected URLs fail before traffic;
- allowed loopback/private literal URLs pass config validation;
- bounded timeout and controlled failure reporting;
- redacted ignored evidence output;
- prod API-key `401`/`200` or OAuth2 unauthorized/authorized boundary;
- no DNS, discovery, scanning, reachability, or socket-probe APIs in the live traffic path;
- no Postman or smoke private-network live execution by default;
- no native tooling, persistence, service install, scheduled tasks, secret persistence, release actions, or `release-downloads/` mutation.

Until a separate approved task adds and tests a bounded traffic executor, private-network live traffic execution remains unimplemented. The current state is config validation, dry-run evidence, and an offline live gate decision helper only.
