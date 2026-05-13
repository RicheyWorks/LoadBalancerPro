# Executive Summary

LoadBalancerPro is a Java 17 / Spring Boot load-balancing simulator and operator-focused proxy foundation. It demonstrates routing strategy comparison, local reverse-proxy validation, guarded cloud boundaries, API-key/OAuth2 deployment modes, reload/status observability, CI evidence artifacts, and a local-only container path whose default runtime profile is protected while source-checkout local mode stays convenient.

## What It Demonstrates

- Load-balancing and routing decisions with deterministic API, browser, and Postman review paths.
- Optional lightweight proxy mode with operator-configured routes and backend targets.
- API-key and OAuth2 deployment boundaries for protected proxy/status surfaces where those modes are configured.
- Proxy status, metrics, retry/cooldown counters, reload status, and read-only browser status review.
- Release-free evidence through CI artifacts, local smoke scripts, SBOM output, packaged-jar inspection, and Docker runtime smoke checks.
- Semantic-tag release evidence through deterministic GitHub Release JAR/SBOM/checksum assets, SHA-256 verification, and GitHub artifact attestations.
- A [`PRODUCTION_CANDIDATE_EVIDENCE_GATE.md`](PRODUCTION_CANDIDATE_EVIDENCE_GATE.md) checklist that separates automated CI/release controls from manual operator verification before any release-ready claim.
- Guarded cloud behavior where live mutation is disabled by default and isolated behind explicit `CloudManager` guardrails.

## Fastest Evaluation Path

1. Open [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md) for the evidence map.
2. Run the app locally and open `http://localhost:8080/`.
3. Open `http://localhost:8080/load-balancing-cockpit.html` for the browser cockpit.
4. Run [`DEPLOYMENT_SMOKE_KIT.md`](DEPLOYMENT_SMOKE_KIT.md) to check packaged-jar startup, API-key boundary behavior, and proxy-loopback forwarding.
5. Use [`CONTAINER_DEPLOYMENT.md`](CONTAINER_DEPLOYMENT.md) for local-only Docker build/run validation.
6. Inspect `jacoco-coverage-report`, `packaged-artifact-smoke`, and `loadbalancerpro-sbom` from a successful CI run.

## What It Does Not Claim

- It is not a managed cloud load balancer, public ingress approval, compliance proof, certification outcome, or benchmark result.
- It does not publish a live hosted demo from this repository.
- It does not claim a fixed coverage percentage in documentation; coverage numbers must come from a generated JaCoCo report or CI log for a specific run.
- It does not create tags, GitHub Releases, release assets, cloud resources, or `release-downloads/` evidence during normal docs, tests, smoke scripts, or CI review paths.

## Best Next Review

For a quick public-facing review, read this page, run the cockpit, then follow [`DEMO_WALKTHROUGH.md`](DEMO_WALKTHROUGH.md). For a deeper engineering review, use [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md), [`TESTING_COVERAGE.md`](TESTING_COVERAGE.md), [`OPERATOR_RUN_PROFILES.md`](OPERATOR_RUN_PROFILES.md), and [`DEPLOYMENT_HARDENING_GUIDE.md`](DEPLOYMENT_HARDENING_GUIDE.md).
