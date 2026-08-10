# Contributing

Thanks for improving LoadBalancerPro. Engineering changes are welcome when they preserve the repository's security and
evidence boundaries.

## Workflow

- Branch from current `main` and keep the pull request focused on one outcome.
- Preserve unrelated work and avoid force-pushing shared history.
- Include implementation, tests, and the documentation needed to explain the same behavior in one pull request.
- Keep dependency or release changes separate when they are not required by the feature.

## Verification

Run the smallest focused tests while developing. Before review, run the affected integration or contract tests. Run the
full suite for shared runtime, security, build, or broad test/governance changes:

```text
mvn -B test
```

Build the executable artifact for a merge candidate or when packaging/runtime resources change:

```text
mvn -B package
```

Required GitHub CI, CodeQL, dependency review, SBOM, container smoke, and image scans remain authoritative for the exact
pull-request head.

## Credentials And External Systems

Never commit credentials, tokens, private keys, customer data, private account identifiers, or secret-bearing logs.
Use sanitized examples, mocks, and loopback fixtures. External, cloud, tenant, and production targets require explicit
authorization and reviewed safety controls.

## Cloud And Release Safety

- Keep CloudManager dry-run and account/region/capacity gates intact.
- Do not connect recommendation-only routing or allocation output directly to live mutation.
- Do not move existing tags, force-push release history, publish artifacts, or create releases without explicit scope
  and successful release verification.

## Pull Requests

Describe the outcome, important verification, and remaining material risk. Keep transient command logs and CI details in
the pull request or GitHub Actions rather than permanent repository documentation.
