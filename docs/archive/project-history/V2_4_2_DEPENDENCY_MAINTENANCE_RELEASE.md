# v2.4.2 Dependency Maintenance Release

## Purpose

`v2.4.2` is a combined dependency-maintenance release for the low-risk updates merged after `v2.4.1`.

## Included Dependency Updates

- Updated `org.json:json` from `20231013` to `20251224`.
- Updated AWS SDK for Java v2 BOM from `2.42.35` to `2.44.1`.

## Usage And Safety Notes

- `org.json:json` is used directly for JSON import/export and serialization paths.
- AWS SDK v2 is used by `CloudAwsClients` and `CloudManager` for guarded EC2, CloudWatch, and Auto Scaling integration.
- Cloud guardrails remained intact.
- Dry-run/default safety was preserved.
- Mocked AWS client tests and placeholder credential rejection tests passed.
- No live AWS calls were required for verification.

## Scope Notes

- No behavior changes are intended.
- No deprecated shims were removed.
- No deferred major Dependabot PRs were merged.
- No `public/main` work is included.
