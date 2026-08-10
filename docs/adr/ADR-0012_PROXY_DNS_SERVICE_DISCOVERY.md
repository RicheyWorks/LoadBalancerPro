# ADR-0012 Proxy DNS Service Discovery

## Status

Accepted for the scoped P-4.2 implementation.

Decision type: proxy backend discovery and runtime routing behavior.

Implementation status: design contract recorded before implementation; verification remains required.

## Date

2026-08-09.

## Context

Proxy targets are currently static HTTP or HTTPS URLs. P-4.2 adds periodic DNS discovery using the source-plan form
`discovery=dns:<name>:<port>`, with a bounded refresh floor and independent health for each resolved address. The
change must keep resolution away from request and status threads, preserve immutable request snapshots and existing
static-target behavior, avoid unbounded metric or scheduler cardinality, and continue to enforce the optional
private-network validation boundary after every resolution.

The JDK resolver does not expose authoritative DNS record TTLs. The configured TTL floor therefore controls the
minimum re-resolution interval, while JVM and operating-system DNS caching may make the effective interval longer.
This is periodic discovery, not authoritative-DNS, DNSSEC, or cache-bypass proof.

The current JDK `HttpClient` path also cannot safely connect an HTTPS request to an explicit discovered address while
retaining hostname-based certificate identity and SNI on a per-request basis. Disabling hostname verification is not
acceptable. DNS-discovered targets are consequently HTTP-only in this slot; HTTPS discovery is rejected until a
transport with reviewed address pinning plus service-name TLS verification exists.

The same client derives the HTTP `Host` authority from the literal member URI, and its per-request API does not allow
a safe restricted-header override. P-4.2 therefore uses an explicit address-authority contract rather than silently
claiming virtual-host preservation. Discovery configuration must set `discovery-authority=address`; the selected
literal address and port are both the connection endpoint and backend HTTP authority. Logical-host authority is not
supported in this slot, and no global restricted-header property is enabled.

## Decision

### Configuration contract

Each existing upstream may optionally set `discovery` to exactly `dns:<name>:<port>`. A discovered upstream keeps its
existing `url` as an HTTP transport and base-path template. The URL host and effective port must equal the normalized
discovery name and port, and the URL must otherwise satisfy the existing no-user-info, no-query, and no-fragment
rules. Valid percent escapes in its raw base path are retained exactly when literal member URIs are built.
`discovery-authority` must be exactly `address` for discovered upstreams and must be blank for static upstreams.
Blank discovery retains current static-target behavior. Unsupported schemes, IP literals as discovery names,
wildcards, trailing-dot ambiguity, control characters, invalid IDN/ASCII labels, and malformed or out-of-range ports
fail configuration atomically.

`loadbalancerpro.proxy.dns-discovery.ttl-floor` defines the minimum interval between attempts and defaults to 30
seconds. `stale-after` defaults to five minutes, must not be shorter than the floor, and bounds retention of the last
successful answer after repeated failures. `resolution-timeout` defaults to two seconds. Per-name answers and the
aggregate effective target set remain within the existing route and configuration target limits.

### Resolution and publication

A dedicated bounded daemon component resolves configured names through an injectable resolver abstraction. The
production resolver uses the JVM/operating-system resolver; deterministic tests use only injected literal-loopback
answers and never query external DNS. A name has at most one in-flight lookup. Results are canonicalized to literal
IPv4 or unscoped IPv6 addresses, deduplicated, sorted by address bytes, and capped before publication. Unspecified,
multicast, and malformed addresses are rejected.

When private-network validation is enabled, every resolved literal address must independently pass the existing
loopback/private classifier before it can enter an effective snapshot. A hostname is never accepted merely because
its text looks internal. Public answers are allowed only when the existing private-network validation feature is
disabled, matching the static-target policy.

More than 32 usable unique addresses is a failed refresh. The runtime retains the prior non-empty snapshot only
within its stale bound; it never truncates and publishes a partial answer that could distort weights or canary state.

A successful non-empty answer atomically replaces that logical upstream's member set. Empty answers and resolver
failures retain the last successful set only until `stale-after`; after that bound the member set becomes empty and
new requests fail closed with the existing no-healthy-upstream response. Resolution failures never fall back to
sending traffic to the unresolved hostname.

### Routing, health, and reload semantics

Each resolved address becomes an immutable effective member with a stable bounded identifier derived from the
logical upstream ID plus the canonical address. Routing strategies, capacity, runtime telemetry, cooldown, retries,
and active health checks operate on those member identifiers, so health for one address cannot mark its siblings
healthy or unhealthy. Split groups continue to reference logical configured target IDs and are expanded only within
their existing group; discovery cannot let retries escape the selected canary group.

The outbound HTTP request uses the selected member URI unchanged. Its backend `Host` authority is consequently the
canonical literal address plus the port when non-default; the configured discovery name is not sent as `Host`.

Discovery publication rebuilds the affected effective route snapshot under the existing configuration lock and
atomically swaps it for new requests. Requests already holding the previous snapshot finish against it. Unchanged
member sets do not churn strategy or health state; additions receive fresh state, and removals stop receiving new
requests while existing requests remain valid. Operator reload replaces the logical discovery contract atomically
and stale callbacks from an older generation are ignored.

Protected status and admin responses distinguish logical discovery configuration from effective members. They may
expose the configured discovery name, canonical literal member address, refresh status, and bounded timestamps, but
never resolver exception text. Metrics and access logs use only bounded stable identifiers; DNS names, raw failures,
or address lists do not become unbounded tags.

## Rejected alternatives

- Sending requests to the unresolved hostname cannot guarantee that the address selected by routing and health is
  the address used by the HTTP client.
- Disabling TLS hostname verification to support HTTPS discovery weakens an existing security boundary.
- Resolving on request, status, or reload threads introduces latency and availability coupling on user-facing paths.
- Retaining the last successful answer forever can route indefinitely to stale infrastructure after DNS failure.
- Adding an authoritative DNS library only to obtain record TTLs expands dependency and parser surface beyond the
  source requirement; the JDK resolver plus an explicit refresh floor is the bounded initial contract.
- Treating all addresses for a name as one health identity violates the source-plan requirement for per-IP health.

## Consequences

Static upstreams remain unchanged. DNS discovery adds bounded scheduler and effective-target state proportional to
configured names and accepted addresses. Address changes can reset strategy state for the changed membership and can
move consistent-hash assignments, which is expected. An unresolved or expired service fails closed instead of
silently bypassing discovery. HTTPS service discovery remains unavailable rather than weakening backend TLS.

## Verification contract

Focused tests must cover configuration parsing and rejection, deterministic address canonicalization and limits,
off-request-thread resolution, refresh-floor scheduling, last-known-good expiry, private/public answer filtering,
per-address health isolation, membership add/remove publication, split-group confinement, retry behavior, stale
generation suppression, immutable concurrent request snapshots, status/admin visibility, reload copying, shutdown,
and unchanged static upstream behavior. Resolver tests use injected loopback answers only. Full tests, packaging, the
existing proxy Compose smoke, CodeQL, dependency review, SBOM, and image scans remain required before merge.

## Not-proven boundaries

This decision and later local/CI tests will not prove authoritative TTL observation, DNS cache bypass, DNSSEC,
split-horizon correctness, logical-host HTTP authority or virtual-host compatibility, HTTPS discovery,
service-name TLS identity across pinned addresses, production DNS
availability, production throughput or latency, live-cloud behavior, production readiness, or production
certification.
