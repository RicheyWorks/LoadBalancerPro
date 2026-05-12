# Operator Run Profiles

Use this guide when you want the shortest safe path from "which mode should I run?" to a copyable command. It packages the existing local, packaged-jar, API-key, OAuth2, proxy-loopback, and container paths without changing defaults.

Start reviewer evidence navigation with [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md). Use this page as the execution hub after you know which evidence path you want.

After choosing a profile, use [`DEPLOYMENT_SMOKE_KIT.md`](DEPLOYMENT_SMOKE_KIT.md) for the local-only packaged-jar, prod API-key, and proxy-loopback smoke path.

## Start Here

The local demo remains the easiest mode. It keeps browser review pages available, uses local-friendly CORS defaults, keeps live AWS mutation off, and keeps proxy mode disabled unless you explicitly enable a proxy profile or imported proxy config.

The prod and cloud-sandbox profiles are opt-in boundaries for controlled validation. In API-key mode, they require `X-API-Key` for protected mutation routes, `/proxy/**`, and `GET /api/proxy/status`. OAuth2 mode is available when an issuer or JWK set is configured.

Proxy mode is lightweight and optional. It forwards to configured upstreams only when `loadbalancerpro.proxy.enabled=true`; the default `src/main/resources/application.properties` keeps `loadbalancerpro.proxy.enabled=false`.

This guide does not claim production readiness, gateway hardening, security certification, benchmark results, TLS implementation, release publication, or live-cloud readiness. TLS termination, public ingress policy, secret rotation, rate limits, and environment-specific access controls remain deployment responsibilities.

## Profile Matrix

| Profile/mode | Intended use | Required flags/env vars | Proxy default | Auth boundary | TLS assumption | Cockpit access | Status/proxy verification command | What it proves | What it does not prove |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| local demo | Fastest source checkout demo and browser review | `mvn spring-boot:run` or `--spring.profiles.active=local` | Disabled | Demo-friendly local API-key mode; not a security boundary | Loopback HTTP only; terminate TLS externally before shared exposure | `http://localhost:8080/` and `http://localhost:8080/load-balancing-cockpit.html` | `curl -fsS http://127.0.0.1:8080/api/health` | App starts locally with static pages and health endpoint | Public exposure safety, identity, TLS, or gateway readiness |
| packaged jar local | Validate the built executable jar | `mvn -B -DskipTests package`, then `java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=8080` | Disabled | Same as local demo unless another profile is selected | Loopback HTTP only | Same cockpit URLs | `curl -fsS http://127.0.0.1:8080/api/health` | Packaged jar can start and serve local API/static resources | Release asset publication or signed provenance |
| prod API-key boundary | Local production-like API-key validation | `LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY`, `--spring.profiles.active=prod` | Disabled | `X-API-Key` protects protected API mutations, `/proxy/**`, and `GET /api/proxy/status` | Terminate TLS at trusted edge before non-local exposure | Local only unless intentionally routed through a trusted edge | `curl -i http://127.0.0.1:8080/api/proxy/status` then `curl -i -H "X-API-Key: $LOADBALANCERPRO_API_KEY" http://127.0.0.1:8080/api/proxy/status` | API-key fail-closed boundary for protected surfaces | Full identity, secret rotation, public readiness, or TLS |
| cloud-sandbox API-key boundary | Dry-run sandbox-profile validation with API-key boundary | `LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY`, `--spring.profiles.active=cloud-sandbox` | Disabled | Same API-key boundary as prod; cloud live mutation remains off by default | Terminate TLS at trusted edge before non-local exposure | Local/private only | `curl -i http://127.0.0.1:8080/api/proxy/status` then `curl -i -H "X-API-Key: $LOADBALANCERPRO_API_KEY" http://127.0.0.1:8080/api/proxy/status` | Sandbox profile starts dry-run and protected surfaces require API key | Live AWS behavior, IAM proof, or sandbox cleanup correctness |
| OAuth2 mode | App-native JWT role-check validation | `loadbalancerpro.auth.mode=oauth2` plus loopback issuer or JWK set config | Disabled unless explicitly enabled elsewhere | Bearer token required; configured allocation role defaults to `operator` for protected allocation/routing and proxy surfaces | Terminate TLS externally; do not send real tokens over plain shared networks | Private/demo review only | `curl -i -H "Authorization: Bearer CHANGE_ME_LOCAL_TOKEN" http://127.0.0.1:8080/api/proxy/status` | OAuth2 mode wiring and role boundary can be validated with a configured local identity/JWK source | Identity-provider operation, key rotation, or end-to-end encryption |
| proxy-enabled loopback validation | Real HTTP forwarding to local/private backends | `--spring.config.import=optional:file:docs/examples/operator-run-profiles/proxy-loopback.properties` | Explicitly enabled by imported example | Local/default mode is demo-friendly unless combined with prod or OAuth2 settings | Loopback HTTP only; terminate TLS externally before exposure | `http://localhost:8080/proxy-status.html` | `curl -i http://127.0.0.1:8080/proxy/api/health` and `curl -s http://127.0.0.1:8080/api/proxy/status` | Configured route/target forwarding and status visibility | Production gateway behavior, throughput, public ingress safety, or TLS |
| container run | Validate Dockerfile-based local runtime | `docker build -t loadbalancerpro:local .` then loopback-bound `docker run` | Disabled | Same as selected app profile; default container command uses local defaults | Container port is HTTP; terminate TLS externally | `http://localhost:8080/` when mapped to loopback | `curl -fsS http://127.0.0.1:8080/api/health` | Container image can start and answer health on loopback | Kubernetes, Helm, Compose, registry publishing, or production runtime posture |

## Copyable Recipes

### Local Demo

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=8080"
curl -fsS http://127.0.0.1:8080/api/health
# Browser: http://localhost:8080/
# Browser: http://localhost:8080/load-balancing-cockpit.html
# Browser: http://localhost:8080/routing-demo.html
```

Packaged local demo variant:

```bash
mvn -B -DskipTests package
java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=local
curl -fsS http://127.0.0.1:8080/api/health
```

### Prod API-Key Boundary

PowerShell:

```powershell
$env:LOADBALANCERPRO_API_KEY="CHANGE_ME_LOCAL_API_KEY"
java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=prod
curl.exe -i http://127.0.0.1:8080/api/proxy/status
curl.exe -i -H "X-API-Key: $env:LOADBALANCERPRO_API_KEY" http://127.0.0.1:8080/api/proxy/status
curl.exe -i http://127.0.0.1:8080/proxy/demo
curl.exe -i -H "X-API-Key: $env:LOADBALANCERPRO_API_KEY" http://127.0.0.1:8080/proxy/demo
```

Unix shell:

```bash
export LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY
java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=prod
curl -i http://127.0.0.1:8080/api/proxy/status
curl -i -H "X-API-Key: $LOADBALANCERPRO_API_KEY" http://127.0.0.1:8080/api/proxy/status
curl -i http://127.0.0.1:8080/proxy/demo
curl -i -H "X-API-Key: $LOADBALANCERPRO_API_KEY" http://127.0.0.1:8080/proxy/demo
```

Expected boundary: the unauthenticated status/proxy calls return HTTP 401 in prod API-key mode. The authenticated status call returns the read-only status JSON. The authenticated `/proxy/demo` call still returns HTTP 404 unless proxy mode is explicitly enabled, preserving the disabled-by-default proxy boundary.

### Cloud-Sandbox API-Key Boundary

```bash
export LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY
java -jar target/LoadBalancerPro-2.4.2.jar --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=cloud-sandbox
curl -i http://127.0.0.1:8080/api/proxy/status
curl -i -H "X-API-Key: $LOADBALANCERPRO_API_KEY" http://127.0.0.1:8080/api/proxy/status
```

Cloud-sandbox remains dry-run by default and does not require AWS credentials just to start. Do not add live-cloud flags unless a separate sandbox plan exists.

### OAuth2 Mode

Use OAuth2 only when you have a real or local test issuer/JWK source configured. This recipe uses loopback placeholders and is not a working identity provider by itself:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --server.address=127.0.0.1 \
  --server.port=8080 \
  --spring.profiles.active=prod \
  --loadbalancerpro.auth.mode=oauth2 \
  --loadbalancerpro.auth.oauth2.jwk-set-uri=http://127.0.0.1:18090/.well-known/jwks.json \
  --loadbalancerpro.auth.required-role.allocation=operator
```

Verification shape after a local issuer is running:

```bash
curl -i http://127.0.0.1:8080/api/proxy/status
curl -i -H "Authorization: Bearer CHANGE_ME_LOCAL_OPERATOR_JWT" http://127.0.0.1:8080/api/proxy/status
```

Expected boundary: missing or invalid bearer tokens return HTTP 401, and authenticated tokens without the configured role return HTTP 403. Do not use real tokens on plain shared networks.

### Proxy-Enabled Loopback Validation

Start two local HTTP services on loopback, or use the Java fixture launcher documented in [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md). Then import the explicit proxy-loopback example:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
  --server.address=127.0.0.1 \
  --server.port=8080 \
  --spring.config.import=optional:file:docs/examples/operator-run-profiles/proxy-loopback.properties

curl -i http://127.0.0.1:8080/proxy/api/health
curl -s http://127.0.0.1:8080/api/proxy/status
# Browser: http://localhost:8080/proxy-status.html
```

When you need the prod API-key boundary and proxy loopback together, combine the import with `--spring.profiles.active=prod` and `LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY`, then send `X-API-Key` on `/proxy/**`, `GET /api/proxy/status`, and any operator-controlled `POST /api/proxy/reload` check.

Runtime proxy config reload is optional and process-local. Use it only with a reviewed local JSON payload, the same operator auth/TLS boundary as the proxy surfaces, and `/api/proxy/status.reload` verification. Invalid reloads preserve the last known-good config; restart remains recommended after deployment secret, TLS, auth, JVM, or non-proxy config changes.

### Container Run

The repository has a Dockerfile. There is no checked-in compose file, so keep this to a loopback-bound single-container smoke:

```bash
docker build -t loadbalancerpro:local .
docker run --rm --name loadbalancerpro-demo -p 127.0.0.1:8080:8080 loadbalancerpro:local
curl -fsS http://127.0.0.1:8080/api/health
```

Prod API-key boundary in a local container:

```bash
docker run --rm --name loadbalancerpro-prod \
  -p 127.0.0.1:8080:8080 \
  -e LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY \
  loadbalancerpro:local \
  --server.address=0.0.0.0 \
  --spring.profiles.active=prod
```

Do not bake secrets into the image. Keep container examples loopback-bound unless a reviewed deployment edge supplies TLS, access control, logging, and rate limits.

## Example Config Files

Copy/adapt examples live under `docs/examples/operator-run-profiles`:

```text
docs/examples/operator-run-profiles/local-demo.properties
docs/examples/operator-run-profiles/prod-api-key.properties
docs/examples/operator-run-profiles/cloud-sandbox-api-key.properties
docs/examples/operator-run-profiles/proxy-loopback.properties
```

They are not active defaults. The only example that enables proxy mode is `proxy-loopback.properties`.

## Operator Cautions

- Demo mode is not a security boundary.
- Proxy mode is disabled by default and remains lightweight.
- `/proxy/**`, `GET /api/proxy/status`, `POST /api/proxy/reload`, and `/proxy-status.html` should not be exposed publicly without deployment-level access control and TLS termination.
- TLS termination is expected at a trusted reverse proxy, ingress, managed load balancer, platform edge, or service mesh unless in-app TLS is explicitly configured and tested in a future slice.
- API-key and OAuth2 modes are access boundaries for controlled validation; they are not certification, full identity lifecycle management, or secret rotation.
- Use placeholder secrets only in docs and examples. Store real secrets outside Git.
- Keep loopback examples on `127.0.0.1` or `localhost`.
- This guide does not create tags, GitHub Releases, release assets, `release-downloads/` evidence, generated reports, or generated hashes.
