# ADR-0011 Proxy Route Matching And Canary Splits

## Status

Accepted for the scoped P-4.1 implementation.

Decision type: proxy routing behavior.

Implementation status: design contract recorded before implementation; verification remains required.

## Date

2026-08-09.

## Context

Named proxy routes currently match only a normalized path prefix and select the longest matching prefix. P-4.1 adds
exact host and header predicates plus deterministic percentage canary groups. The change must preserve immutable
reload snapshots, route-owned strategy state, bounded configuration, retry behavior, existing path-only routes, and
the legacy global-upstream fallback.

Host and header values are client-controlled routing inputs, not authentication or tenant identity. Percentage
results are process-local routing behavior, not distributed traffic guarantees. This decision does not add public
ingress, DNS discovery, WebSocket support, HTTP/2 guarantees, external targets, or production certification.

## Decision

### Route match model

Each named route may configure `match.host` and `match.header.<name>` in addition to its existing `path-prefix`.
Predicates use AND semantics: the path, configured host, and every configured header must match. Host comparison is
case-insensitive after safe authority normalization and ignores an optional request port. Header names are validated
HTTP field names and compared case-insensitively by the servlet API; configured values use exact, case-sensitive
comparison. Wildcard, suffix, regular-expression, and trusted-forwarded-host matching are not supported.

Matching routes use this total deterministic order:

1. a route with an exact host predicate outranks a hostless route;
2. the longer normalized path prefix wins;
3. the route with more exact header predicates wins;
4. the lexicographically smaller route name is the stable final tie-break.

The route name tie-break prevents map iteration order from becoming behavior. It does not turn overlapping rules into
an authorization boundary; operators should keep rules intentionally distinct.

### Percentage split model

A route may configure named `split` groups. Each group has a positive integer `percentage` and one or more
`target-ids` referencing that route's existing targets. Split groups are accepted only when:

- percentages total exactly 100;
- group names and referenced target IDs pass existing bounded identifier rules;
- every route target belongs to exactly one group;
- no target appears twice and no unknown target is referenced;
- ordinary unsplit routes omit the `split` map.

The request's existing route routing key is hashed into a bucket from 0 through 99 and assigned once to the ordered
cumulative percentage ranges. The chosen group is retained for the entire request. Retries, affinity lookup,
capacity filtering, health filtering, and strategy selection see only that group's targets; the proxy does not spill
a failed group into another percentage group. Each group owns an independent strategy instance so round-robin and
weighted strategy state cannot leak between groups or routes. Unchanged reloads may retain matching route/group
strategy instances; changed target membership or strategy replaces them.

### Visibility and redaction

Protected status and admin configuration responses expose host match, header names, split names, percentages, and
target IDs. They do not expose configured header match values. Existing metrics and access logs continue to use the
bounded route and upstream identifiers; no client-supplied host, header value, or split key becomes a metric tag.

## Rejected alternatives

- Reusing upstream routing weights does not express hard percentage groups and can let retries cross canary bounds.
- Random assignment makes tests and request stickiness nondeterministic.
- Falling back to another split group when one group is unhealthy violates the configured percentage boundary.
- Declaration-order tie-breaking makes behavior depend on configuration map ordering.
- Wildcard host or regex header rules create a larger validation and ambiguity surface than P-4.1 requires.

## Consequences

Existing path-only and legacy configurations keep their current behavior. Host-specific routes deliberately outrank
longer hostless paths. Header-specific routes can refine otherwise equal host/path rules. Canary percentages are
deterministic for a routing key but are not promised to be exact for small samples, across multiple processes, or
after routing-key changes. A fully unhealthy assigned group fails closed for that request instead of escaping to a
different group.

## Verification contract

Focused tests must cover host normalization and rejection, exact header matching, precedence, stable tie-breaking,
split validation, deterministic 90/10 assignment, group-local retries and affinity, reload copy/state isolation,
status/admin redaction, legacy compatibility, and concurrent immutable-config acquisition. Full tests, packaging,
the existing proxy Compose smoke, CodeQL, dependency review, SBOM, and image scans remain required before merge.

## Not-proven boundaries

This decision and its local deterministic tests do not establish production SLOs, exact fleet-wide percentages,
real-tenant isolation, trusted host identity, public-ingress safety, multi-process coordination, high availability,
live-cloud validation, production readiness, or production certification.
