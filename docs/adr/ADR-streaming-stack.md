# ADR: HTTP/2 And WebSocket Streaming Stack

## Status

Accepted for the scoped P-4.3 implementation.

Decision type: proxy transport architecture.

## Date

2026-08-13.

## Context

The proxy already streams HTTP request and response bodies through Spring MVC, embedded Tomcat, and the JDK
`HttpClient`. It also owns routing, health, admission, per-upstream concurrency, TLS-bundle, reload, graceful-drain,
metrics, and access-log behavior in that servlet stack. P-4.3 adds inbound HTTP/2 and WebSocket passthrough without
weakening those controls or introducing a second routing implementation.

WebSocket connections are long lived and bidirectional. An implementation must bound message and send queues, reject
unsafe handshake headers, preserve authentication at the inbound boundary, reuse verified backend TLS configuration,
hold routing/configuration leases for the connection lifetime, and release all counters during normal close, failure,
or shutdown.

## Decision

Retain Spring MVC and embedded Tomcat.

- Enable inbound HTTP/2 through Spring Boot's `server.http2.enabled` connector configuration. TLS deployments use ALPN;
  the integration test also proves Tomcat's cleartext h2c upgrade path. This applies to ordinary HTTP proxy requests.
- Handle WebSockets through Spring's servlet WebSocket support and use the route planner plus JDK `HttpClient`
  `WebSocket` client for the upstream connection. RFC 6455 WebSockets still use an HTTP/1.1 Upgrade handshake; this
  decision does not implement RFC 8441 extended CONNECT over HTTP/2.
- Preserve the configured upstream authority and raw path/query while converting only `http` to `ws` and `https` to
  `wss`. The existing per-upstream HTTP client supplies verified trust and optional mTLS identity.
- Select one upstream during the handshake using the existing route, split, affinity, health, weight, DNS-discovery,
  admission, and per-upstream concurrency rules. Retries are not attempted after a WebSocket upgrade.
- Hold the active immutable configuration and runtime counters until the tunnel closes. Shutdown closes downstream
  sessions, aborts upstream sessions, and releases those leases.
- Forward text, binary, ping, pong, partial-message, and close frames. Downstream sends use a bounded terminating
  buffer; upstream sends wait only for the configured timeout; text and binary messages have independent size limits.
- Strip hop-by-hop, connection-token, forwarding, and `Sec-WebSocket-*` handshake headers before building the upstream
  handshake. Rebuild forwarding headers from the trusted-forwarded policy, apply existing route header rewrites, and
  require operators to remove inbound proxy credentials such as `X-API-Key` when an upstream must not receive them.
- Keep WebSockets disabled by default. Allowed browser origins and accepted subprotocols are explicit bounded lists;
  the empty origin list retains Spring's same-origin policy.

WebSocket transport settings require application restart. Runtime route reload remains available, but an established
tunnel keeps the configuration snapshot and selected upstream it acquired at open time.

## Rejected alternatives

- Migrating the proxy to WebFlux/Netty would duplicate or replace the established streaming, TLS, reload, admission,
  observability, and graceful-lifecycle paths for no demonstrated P-4.3 requirement.
- A raw Tomcat upgrade handler would bypass Spring's handshake, origin, session, and container-limit integration.
- Unbounded asynchronous sends would allow a slow peer to grow memory without a deterministic limit.
- Retrying an upgraded tunnel could replay application messages or silently change upstream identity.

## Consequences

HTTP and WebSocket traffic share routing and safety policy without a broad runtime migration. The servlet bridge uses a
bounded wait while forwarding each WebSocket send, so a slow peer consumes a container worker only until the configured
send timeout and then fails closed. This favors predictable memory and failure containment over maximum connection
density. A future high-density benchmark may justify a reactive transport ADR, but it is not assumed here.

## Verification contract

`ReverseProxyHttp2IntegrationTest` must prove that the running Tomcat connector installs HTTP/2 support and negotiates
a real h2c request. `ReverseProxyWebSocketIntegrationTest` must prove API-key rejection, credential removal, route and
query preservation, forwarding headers, subprotocol negotiation, text/binary forwarding, clean close, and oversized
message termination against loopback servers. Configuration tests must prove positive bounded timeouts. The full test
and package suites plus exact-head required GitHub checks remain required before merge.

## Not-proven boundaries

This decision does not establish RFC 8441, gRPC proxying, HTTP/2 backend negotiation guarantees, WebSocket retry or
resume, compression-extension forwarding, fleet-wide connection limits, benchmark capacity, public-ingress safety,
production SLOs, high availability, or production certification.
