# Private-Network Proxy Dry Run

Use this recipe when a reviewer or operator wants to see how the future private-network proxy profile will validate backend configuration without sending traffic.

This is config-validation-only. It uses the source-visible Java `ProxyBackendUrlClassifier`, runs under Maven/JUnit, writes ignored evidence under `target/`, and does not start private-network smoke, Postman execution, Docker, external services, downloaded tools, DNS resolution, reachability checks, socket probes, port scanning, or live proxy forwarding.

Start with [`PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md`](PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md), [`PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md`](PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md), [`LIVE_PROXY_CONTAINMENT.md`](LIVE_PROXY_CONTAINMENT.md), and [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md) for the surrounding safety boundaries.

## Focused Command

```bash
mvn -Dtest=PrivateNetworkProxyDryRunEvidenceTest test
```

Expected ignored build output:

- `target/proxy-evidence/private-network-validation-dry-run.md`
- `target/proxy-evidence/private-network-validation-dry-run.json`

These files are generated under ignored Maven `target/` output. They are not tracked documentation artifacts and must not contain API keys, bearer tokens, credentials, or operator secrets.

## Validation Inputs

The recipe demonstrates the future opt-in gate:

```properties
loadbalancerpro.proxy.private-network-validation.enabled=true
```

Allowed literal examples include:

- `http://localhost:18080`
- `http://127.0.0.1:18081`
- `http://10.1.2.3:18082`
- `http://172.16.1.10:18083`
- `http://192.168.1.10:18084`
- `http://[fd12:3456:789a::1]:18085`

Rejected examples include:

- `http://8.8.8.8:18081` as `PUBLIC_NETWORK_REJECTED`
- `http://example.com:18081` as `AMBIGUOUS_HOST_REJECTED`
- `http://<redacted-userinfo>@127.0.0.1:18081` as `USERINFO_REJECTED`
- `ftp://127.0.0.1:18081` as `UNSUPPORTED_SCHEME_REJECTED`
- `http://010.000.000.001:18081` as `AMBIGUOUS_HOST_REJECTED`
- malformed input as `INVALID_REJECTED`

## What It Proves

The Markdown evidence is the human review path. It lists the opt-in property, exact evidence paths, allowed loopback/private literal URL classifications, rejected public/domain/userinfo/unsupported/malformed examples, and safety flags such as `trafficSent=false`, `dnsResolution=false`, `reachabilityChecks=false`, `portScanning=false`, `postmanExecution=false`, and `smokeExecution=false`.

The JSON evidence is the structured review path. It records the same labels and statuses with `dryRunOnly=true`, `apiKeyPersisted=false`, `secretPersisted=false`, `releaseDownloadsMutated=false`, and `failClosedBeforeActiveConfig=true`.

Rejected inputs represent fail-closed configuration validation failures. With `loadbalancerpro.proxy.private-network-validation.enabled=true`, unsafe backend URLs fail before startup or explicit proxy reload can make them active. Existing tests continue to prove that rejected reload configuration preserves the last-known-good active config.

The default property remains `loadbalancerpro.proxy.private-network-validation.enabled=false`; this dry run does not change default/local/demo forwarding behavior and does not add live private-network traffic.

Before any future private-network live validation is implemented, [`PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md`](PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md) must be satisfied. That gate requires explicit operator approval, future default-off live flags, classifier approval for every literal backend URL, bounded timeout behavior, redacted ignored `target/` evidence, API-key/OAuth2 boundary proof, and continued no-DNS/no-discovery/no-scanning rules.
