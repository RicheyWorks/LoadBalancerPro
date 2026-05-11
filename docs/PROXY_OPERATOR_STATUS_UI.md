# Proxy Operator Status UI

LoadBalancerPro serves a no-dependency browser page for the optional lightweight reverse proxy status endpoint:

```text
http://localhost:8080/proxy-status.html
```

The page reads only:

```text
GET /api/proxy/status
```

It does not write backend state, reset counters, mutate cooldown state, persist browser state, or call external services.

## What It Shows

- proxy enabled or disabled state
- configured routing strategy
- active health-check enabled state
- retry enabled state
- cooldown enabled state
- last selected upstream
- total forwarded and failure counters
- total retry and cooldown activation counters
- status-class counters for `2xx`, `3xx`, `4xx`, `5xx`, and `other`
- per-upstream configured health, effective health, cooldown active state, cooldown remaining milliseconds, and consecutive failure count
- per-upstream forwarded, failure, retry, and cooldown activation counters
- raw JSON returned by `/api/proxy/status`

Missing fields are shown as `Unavailable` instead of inferred.

## Refresh Behavior

Use **Refresh status** for a manual point-in-time fetch.

Use **Start live refresh** to poll the status endpoint every few seconds for the current page session. Live refresh is opt-in and in memory only. **Stop live refresh** clears the interval. The page does not use `localStorage`, `sessionStorage`, cookies, or server-side report files.

## Copy Controls

**Copy status summary** produces deterministic Markdown-style text from the currently loaded status response. If status has not loaded, the copied summary says that status is not loaded.

**Copy demo curl commands** copies loopback demo commands for `scripts/proxy-demo.ps1` modes, `/proxy/demo`, `/proxy/weighted`, `/proxy/failover`, and `/api/proxy/status`.

For the complete local demo-stack path, use [`PROXY_DEMO_STACK.md`](PROXY_DEMO_STACK.md). It points Windows and Unix users to the fixture scripts, checked-in `proxy-demo-*` profiles, startup commands, curl verification, status-page review, and cleanup steps.

For strategy-specific recipes, use [`PROXY_STRATEGY_DEMO_LAB.md`](PROXY_STRATEGY_DEMO_LAB.md). It covers `ROUND_ROBIN`, `WEIGHTED_ROUND_ROBIN`, and health-aware failover examples that pair forwarded response headers with the status page.

## Safety Boundaries

- Read-only browser view.
- Same-origin `GET /api/proxy/status` only.
- No backend writes.
- No metric reset controls.
- No cooldown mutation controls.
- No browser storage.
- No cloud mutation.
- No external scripts, CDNs, fonts, images, or services.
- Lightweight local operator view, not production monitoring.

## Limitations

The page reflects the current app process and the process-local counters in `/api/proxy/status`. It is not a durable dashboard, alert manager, metrics database, service discovery system, gateway control plane, TLS terminator, or public traffic management product.
