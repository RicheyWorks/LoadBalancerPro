# Container Deployment

Use this guide when you want a local-only container path for the same Spring Boot service that the packaged-jar smoke kit validates. It keeps image building, container running, API-key checks, local/demo override guidance, and proxy caveats in one place without changing source-checkout developer defaults.

Start with [`OPERATOR_RUN_PROFILES.md`](OPERATOR_RUN_PROFILES.md) when choosing a run mode. Use [`DEPLOYMENT_SMOKE_KIT.md`](DEPLOYMENT_SMOKE_KIT.md) for the packaged-jar smoke path before adapting these container commands. Use [`ANTIVIRUS_SAFE_DEVELOPMENT.md`](ANTIVIRUS_SAFE_DEVELOPMENT.md) for safe local artifact and tooling defaults, and [`LIVE_PROXY_CONTAINMENT.md`](LIVE_PROXY_CONTAINMENT.md) before adapting proxy validation to real local/private backends.

## What It Proves

- The checked-in `Dockerfile` can build a local image from the repository.
- The image starts the packaged application as a non-root user.
- The checked-in Dockerfile defaults `SPRING_PROFILES_ACTIVE=prod` for protected container startup.
- Container/default deployment mode is protected by the prod API-key profile for protected API, proxy, OpenAPI, and Swagger routes.
- The default container command keeps proxy mode disabled unless explicitly overridden.
- A loopback-bound published port can answer `/api/health` and the root landing page.
- Prod API-key mode can be exercised with placeholder local secrets passed at run time.
- Local developer mode is intentionally permissive when selected explicitly; do not expose local/demo mode on public interfaces.

## What It Does Not Prove

- Public ingress safety, identity lifecycle, TLS termination, rate limiting, high availability, vulnerability management, or environment-specific operations.
- Kubernetes, Helm, Compose, cloud deployment, registry publication, image signing, or release asset publication.
- Throughput, capacity, SLO, compliance, or managed gateway suitability.

## Prerequisites

- Docker or another Docker-compatible local engine.
- Java and Maven only if you also want to run the packaged-jar smoke kit outside the container.
- A local checkout of this repository.

## Build The Local Image

The Docker build is self-contained. It does not require a prebuilt jar, AWS credentials, cloud credentials, release assets, or files under `release-downloads/`.

```bash
docker build -t loadbalancerpro:local .
```

Safety boundaries:

- Do not run `docker push`.
- Do not publish this image to Docker Hub, GHCR, ECR, or any registry from this repo path.
- Do not bake API keys, OAuth2 tokens, AWS credentials, private keys, or operator secrets into the image.
- Treat base-image tag/digest refreshes as supply-chain changes that need a focused review.

## Protected Default Container

Run the app locally with the Dockerfile's protected default profile and proxy mode disabled by default. The placeholder API key is passed at run time; do not bake real secrets into the image.

```bash
docker run --rm --name loadbalancerpro-demo \
  -p 127.0.0.1:8080:8080 \
  -e LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY \
  loadbalancerpro:local
```

Verify:

```bash
curl -fsS http://127.0.0.1:8080/api/health
curl -fsS http://127.0.0.1:8080/
```

The process binds to `0.0.0.0` inside the container so Docker port publishing works. The host publishing example binds to `127.0.0.1` so the smoke path stays local to the workstation.

Detached healthcheck check:

```bash
docker run --rm -d --name loadbalancerpro-demo \
  -p 127.0.0.1:8080:8080 \
  -e LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY \
  loadbalancerpro:local
docker inspect --format='{{.State.Health.Status}}' loadbalancerpro-demo
docker stop loadbalancerpro-demo
```

Boundary checks:

```bash
curl -i http://127.0.0.1:8080/api/proxy/status
curl -i http://127.0.0.1:8080/v3/api-docs
curl -i -H "X-API-Key: CHANGE_ME_LOCAL_API_KEY" http://127.0.0.1:8080/api/proxy/status
```

Expected boundary:

- Missing `X-API-Key` returns HTTP 401 for protected proxy/status surfaces.
- OpenAPI/Swagger routes return HTTP 401 without `X-API-Key`.
- The placeholder `X-API-Key` returns the read-only proxy status response.
- Proxy forwarding remains disabled unless `loadbalancerpro.proxy.enabled=true` is provided through explicit configuration.

## Explicit Local Demo Container

Local developer mode is intentionally permissive for source-checkout demos, generated-client inspection, and local-only browser review. Use it in a container only when you explicitly want that behavior and keep the host port bound to loopback:

```bash
docker run --rm --name loadbalancerpro-local-demo \
  -p 127.0.0.1:8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  loadbalancerpro:local
```

Do not expose local/demo mode on public interfaces. Use the protected default container path, `prod`, `cloud-sandbox`, or OAuth2 mode before any shared-network review.

## Prod API-Key Boundary Container

The Dockerfile already defaults to `SPRING_PROFILES_ACTIVE=prod`, so the normal protected run does not need a profile argument. Use placeholder local secrets for validation. Keep real secrets in your deployment secret system, not in Git, Dockerfile layers, image labels, or shell history.

```bash
docker run --rm --name loadbalancerpro-prod \
  -p 127.0.0.1:8080:8080 \
  -e LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY \
  loadbalancerpro:local
```

In another terminal:

```bash
curl -i http://127.0.0.1:8080/api/proxy/status
curl -i http://127.0.0.1:8080/v3/api-docs
curl -i -H "X-API-Key: CHANGE_ME_LOCAL_API_KEY" http://127.0.0.1:8080/api/proxy/status
```

Expected boundary:

- Missing `X-API-Key` returns HTTP 401 for protected proxy/status surfaces.
- OpenAPI/Swagger routes return HTTP 401 without `X-API-Key`.
- The placeholder `X-API-Key` returns the read-only proxy status response.
- Proxy forwarding remains disabled unless `loadbalancerpro.proxy.enabled=true` is provided through explicit configuration.

## Proxy-Loopback Container Caveat

The packaged-jar smoke kit validates proxy-loopback forwarding directly on the host. Containers add platform-specific host networking behavior:

- Docker Desktop usually exposes the host as `host.docker.internal`.
- Native Linux engines may require a user-defined bridge, explicit host gateway mapping, or backend containers on the same network.
- Do not assume `127.0.0.1` inside the container reaches host services.

For a local-only container experiment with host services, adapt the proxy target URLs at run time:

```bash
docker run --rm --name loadbalancerpro-proxy \
  -p 127.0.0.1:8080:8080 \
  -e LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY \
  loadbalancerpro:local \
  --server.address=0.0.0.0 \
  --loadbalancerpro.proxy.enabled=true \
  --loadbalancerpro.proxy.routes.api.path-prefix=/api \
  --loadbalancerpro.proxy.routes.api.strategy=ROUND_ROBIN \
  --loadbalancerpro.proxy.routes.api.targets[0].id=local-a \
  --loadbalancerpro.proxy.routes.api.targets[0].url=http://host.docker.internal:18181 \
  --loadbalancerpro.proxy.routes.api.targets[0].weight=1 \
  --loadbalancerpro.proxy.routes.api.targets[1].id=local-b \
  --loadbalancerpro.proxy.routes.api.targets[1].url=http://host.docker.internal:18183 \
  --loadbalancerpro.proxy.routes.api.targets[1].weight=1
```

Verify only after the local backends are running:

```bash
curl -i -H "X-API-Key: CHANGE_ME_LOCAL_API_KEY" http://127.0.0.1:8080/proxy/api/smoke
curl -s -H "X-API-Key: CHANGE_ME_LOCAL_API_KEY" http://127.0.0.1:8080/api/proxy/status
```

If Docker cannot reach the host backends, stop and use the packaged-jar smoke kit. Do not reinterpret a networking mismatch as proxy correctness or failure.

## TLS And Access Boundary

The container listens over HTTP. Terminate TLS at a trusted reverse proxy, ingress, managed load balancer, platform edge, or service mesh before shared-network exposure.

Keep `/proxy/**`, `GET /api/proxy/status`, `POST /api/proxy/reload`, and `/proxy-status.html` behind deployment-level access control and TLS termination before exposing proxy mode beyond localhost or a trusted private network.

## Cleanup

```bash
docker stop loadbalancerpro-demo 2>/dev/null || true
docker stop loadbalancerpro-prod 2>/dev/null || true
docker stop loadbalancerpro-proxy 2>/dev/null || true
```

Remove the local image only when you no longer need it:

```bash
docker image rm loadbalancerpro:local
```

These cleanup commands affect only local containers/images you created with the names above. They do not create tags, GitHub Releases, release assets, cloud resources, or `release-downloads/` evidence.
