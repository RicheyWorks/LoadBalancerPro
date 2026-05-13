# IdP Claim Mapping Examples

Use this guide when configuring an enterprise identity provider for `loadbalancerpro.auth.mode=oauth2`.

LoadBalancerPro treats OAuth2 scopes and application roles as separate concepts. Standard `scope` and `scp` claims are useful for OAuth2 consent and API-audience metadata, but they do not grant LoadBalancerPro application roles. Application roles must appear in a dedicated role claim.

This guide uses placeholder issuer URLs, placeholder audiences, and synthetic local test tokens only. Do not paste real tenant IDs, client IDs, private keys, client secrets, refresh tokens, or production JWTs into source-controlled docs, tests, Postman files, screenshots, or evidence.

## Accepted Role Claims

The application maps these dedicated claims to Spring Security `ROLE_*` authorities:

- `roles`
- `role`
- `authorities`
- `realm_access.roles`

Examples:

```json
{
  "sub": "reviewer-operator",
  "aud": "loadbalancerpro-api",
  "iss": "https://idp.example.test/tenant",
  "roles": ["operator"]
}
```

```json
{
  "sub": "reviewer-admin",
  "aud": "loadbalancerpro-api",
  "iss": "https://idp.example.test/tenant",
  "roles": ["admin"]
}
```

```json
{
  "sub": "reviewer-operator",
  "aud": "loadbalancerpro-api",
  "iss": "https://idp.example.test/tenant",
  "realm_access": {
    "roles": ["operator"]
  }
}
```

`operator` is the default role for allocation, routing, and proxy operator routes. `observer` is used for limited read-style LASE shadow review. If an environment intentionally changes the allocation role to `admin`, the token must include `admin` in a dedicated role claim such as `roles`, not only in `scope`.

## Scope-Only Tokens Do Not Grant App Roles

These tokens authenticate but do not satisfy LoadBalancerPro role checks:

```json
{
  "sub": "scope-only-operator",
  "aud": "loadbalancerpro-api",
  "iss": "https://idp.example.test/tenant",
  "scope": "operator observer"
}
```

```json
{
  "sub": "scope-only-admin",
  "aud": "loadbalancerpro-api",
  "iss": "https://idp.example.test/tenant",
  "scp": ["admin"]
}
```

Expected behavior:

- `scope=operator` does not create `ROLE_operator`.
- `scp=["operator"]` does not create `ROLE_operator`.
- `scope=admin` does not create `ROLE_admin`.
- `authorities=["SCOPE_operator"]` does not create `ROLE_operator`.
- Missing dedicated role claims fail closed for role-required routes.

## Route Expectations

With the default role settings:

- `GET /api/health` remains public.
- `GET /api/lase/shadow` accepts `observer` or `operator` in a dedicated role claim.
- Allocation, routing, proxy, proxy reload, and private-network validation command routes require `operator` in a dedicated role claim.
- `/v3/api-docs` and Swagger UI require an authenticated viewer, observer, or operator role unless `loadbalancerpro.auth.docs-public=true` is intentionally set for a private review environment.

With `loadbalancerpro.auth.required-role.allocation=admin`, allocation/routing/proxy operator routes require `admin` in a dedicated role claim. A token with only `scope=admin` fails with HTTP 403.

## Local Mock Validation

Use the existing mocked-resource-server tests to validate claim behavior without a real IdP, real JWK set, tenant secrets, or live network calls:

```bash
mvn -q "-Dtest=OAuth2AuthorizationTest" test
```

The test fixture uses synthetic bearer token names such as:

- `roles-operator-token`: includes `roles: ["operator"]` and is allowed for operator routes.
- `realm-operator-token`: includes `realm_access.roles: ["operator"]` and is allowed for operator routes.
- `scope-operator-token`: includes `scope: "operator observer"` and is denied for operator routes.
- `scp-operator-token`: includes `scp: ["operator", "observer"]` and is denied for operator routes.
- `scope-admin-token`: includes `scope: "admin"` and is denied when the required role is configured as `admin`.
- `roles-admin-token`: includes `roles: ["admin"]` and is allowed when the required role is configured as `admin`.
- `no-role-token`: has no dedicated role claim and is denied for role-required routes.

These are not real JWTs and should not be copied into deployment configuration.

## Operator Checklist

- Configure the IdP application to emit `operator`, `observer`, or environment-specific app roles in a dedicated role claim.
- Keep OAuth2 `scope` or `scp` values for protocol scopes only; do not rely on them for LoadBalancerPro app authorization.
- Keep issuer/JWK settings in deployment configuration, not in source-controlled docs or examples.
- Keep secrets, tenant IDs, client secrets, private keys, refresh tokens, and real JWTs out of evidence.
- Re-run `OAuth2AuthorizationTest` after changing role-claim expectations or required-role properties.
- Revisit this guide before adding a browser OAuth2 cockpit flow, reverse-proxy SSO trust, or custom role mapping.
