# LoadBalancerPro v2.4.0 Docker Runtime Evidence

## Purpose

Document Docker/runtime verification after the `v2.4.0` namespace migration.

## Docker Build

- Docker version: `28.0.4`
- Image name: `loadbalancerpro:v2.4.0-runtime-smoke`
- Image ID: `sha256:6001f35a472e6ef6ca83b63c3a61439a4375287bf60615d553ffee4dc4d5ce62`
- Build result: passed after retry.

## Runtime

- Container name: `loadbalancerpro-v240-smoke`
- Port binding: `127.0.0.1:18084 -> 8080`
- Container status: healthy
- Runtime user: non-root `loadbalancer`
- Java version: `17.0.18`
- Tomcat port: `8080`

## Endpoint Results

- `GET /api/health` returned `{"status":"ok","version":"2.4.0"}`.
- `GET /actuator/info` confirmed app version `2.4.0` and build group `com.richmond423`.
- `GET /actuator/health` returned `{"status":"UP","groups":["liveness","readiness"]}`.

## Namespace And Package Confidence

- `Dockerfile` has no old package or class references.
- `deploy/docker-compose.prod-like.yml` has no Java package or class references.
- `.dockerignore` exists and excludes `target/`.
- Runtime logs showed `LoadBalancerApiApplication v2.4.0`.
- Build metadata reports group `com.richmond423`.

## Local Environment Notes

- Docker initially warned that `C:\Users\730ri\.docker\config.json` could not be read due to access permissions.
- The first build attempt hit a transient Docker Desktop Linux engine pipe error.
- Docker daemon and context checks succeeded, and the build passed on retry.
- These were local Docker environment issues, not project failures.

## Safety Notes

- No files changed during the Docker audit.
- `public/main` was untouched.
- No behavior changes were made.
