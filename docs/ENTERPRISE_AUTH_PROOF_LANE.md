# Enterprise Auth Proof Lane

The Enterprise Auth Proof Lane makes LoadBalancerPro OAuth2 role behavior reviewable without a real enterprise IdP tenant, real tenant IDs, private keys, client secrets, refresh tokens, production JWTs, or live IdP calls.

This is a local/test-backed proof lane. It is not production SSO certification, not a browser login flow, provides no real enterprise IdP tenant validation, and is not proof that a specific enterprise tenant has been configured correctly.

## Role-Claim Contract

In OAuth2 mode, LoadBalancerPro application roles come only from dedicated role claims:

- `roles`
- `role`
- `authorities`
- `realm_access.roles`

The default application roles are:

| Role | Default purpose |
| --- | --- |
| `viewer` | Read-style generated docs and limited read surfaces where configured. |
| `observer` | Limited LASE shadow/scenario read access. |
| `operator` | Allocation, routing, lab run, policy, metrics, proxy, and operator command access. |

The required role names can be changed with `loadbalancerpro.auth.required-role.lase-shadow` and `loadbalancerpro.auth.required-role.allocation`, but the IdP must still emit those values in a dedicated role claim.

## Scope-Only Denial

Standard OAuth2 `scope` and `scp` claims are protocol scopes. They do not grant LoadBalancerPro application roles.

These examples authenticate but do not satisfy operator/admin route checks:

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

Expected result: role-required routes return HTTP `403` unless a dedicated role claim also contains the configured application role.

## Token Lifetime Expectations

Operators should configure short-lived access tokens and validate expiration through the platform IdP. The local proof lane includes an expired-token rejection case, but production lifetime policy belongs to the enterprise IdP and gateway:

- access tokens should expire predictably;
- refresh-token handling should stay outside source-controlled docs and evidence;
- clock-skew tolerance should be configured in the IdP/resource-server boundary, not hardcoded into tests;
- failed or expired tokens should return `401` without exposing token contents.

## Issuer And Audience Guidance

Production OAuth2 mode should validate issuer and audience according to the deployment's IdP contract. The checked-in proof fixture uses placeholders:

- issuer: `https://idp.example.test/tenant`
- audience: `loadbalancerpro-api`

Do not commit real tenant IDs, client IDs, issuer URLs from private tenants, private keys, client secrets, refresh tokens, or production JWTs.

## Key Rotation Guidance

The source-visible mock fixture at `src/test/resources/auth-proof/mock-idp-claims.json` contains synthetic `kid` values `test-key-a` and `test-key-b` to document the key-rotation expectation. It does not contain private key material.

Operational key rotation expectations:

1. publish the new JWK before issuing tokens signed by the new key;
2. keep the old key available until all old tokens expire;
3. monitor `401` spikes during the rotation window;
4. remove old keys only after the maximum token lifetime plus clock-skew window;
5. rerun the local proof lane after claim-shape or role-name changes.

## Local Proof Command

Run the source-visible proof lane:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\enterprise-auth-proof.ps1 -Package
```

The script writes ignored evidence under `target/enterprise-auth-proof/`:

- `enterprise-auth-proof-results.json`
- `mock-idp-jwks-fixture-summary.json`
- `enterprise-auth-proof-summary.md`
- `enterprise-auth-proof-manifest.json`

The script proves:

- unauthenticated prod-style API-key requests are denied;
- prod-style requests with the placeholder local API key are allowed;
- a dedicated `roles` claim grants operator access;
- a dedicated `realm_access.roles` claim grants operator access in the key-rotation example;
- `scope`/`scp` operator/admin values do not grant application roles;
- missing role claims fail closed;
- ambiguous `SCOPE_operator` authority-style values fail closed;
- expired, wrong-issuer, and wrong-audience fixture tokens are rejected.

The packaged app is not started with a real or local IdP server in this lane. OAuth2 role behavior is proven through mocked-resource-server tests and synthetic fixture claims.

## What This Does Not Prove

- real enterprise tenant validation;
- production SSO or browser login/session UX;
- customer identity lifecycle management;
- token refresh or revocation behavior;
- TLS, ingress, WAF, IAM, or zero-trust policy;
- production deployment certification.

Use this lane to verify the role-claim contract before a real IdP integration sprint, then require tenant-specific evidence before claiming enterprise identity readiness.
