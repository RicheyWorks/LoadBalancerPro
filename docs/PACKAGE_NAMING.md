# Package Naming Decision

LoadBalancerPro is hosted as `RicheyWorks/LoadBalancerPro`, while the Java namespace remains:

```text
com.richmond423.loadbalancerpro
```

This is an intentional stable legacy namespace decision for the current codebase.

## Decision

- The GitHub repository identity is `RicheyWorks/LoadBalancerPro`.
- The Maven group and Java package root remain `com.richmond423.loadbalancerpro`.
- A package rename is deferred because it would touch many source files, imports, tests, examples, and docs.
- The current package name does not change runtime behavior, proxy behavior, API contracts, or cloud-safety boundaries.
- A future namespace migration can be planned as its own focused refactor if the owner wants repository branding and package naming to match more closely.

## Reviewer Note

Treat this as a naming consistency decision, not a functional defect. The package root is stable for now so packaging, tests, scripts, and demo launcher commands keep working without broad migration churn.

This note does not make legal, identity, certification, ownership, or trademark claims. It only documents the current engineering decision.
