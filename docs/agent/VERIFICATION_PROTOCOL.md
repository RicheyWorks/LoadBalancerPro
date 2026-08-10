# Verification Guide

Choose verification according to the changed risk surface.

## During Development

- Run the smallest focused test that covers the changed behavior or invariant.
- Add or strengthen executable tests when behavior, security, concurrency, persistence, or failure handling changes.
- Do not preserve an obsolete exact-prose assertion merely to keep a documentation test green.

## Before Review

- Run affected integration and contract tests.
- Run `mvn -B test` for shared runtime, security, build, or broad test/governance changes.
- Run `mvn -B package` for packaging/runtime-resource changes and merge candidates.
- Run relevant artifact, loopback, Compose, benchmark, or enterprise-lab smoke checks when those surfaces changed or
  form part of acceptance.
- Run `git diff --check` and inspect the complete branch diff.

An unchanged SHA does not need an identical expensive local rerun solely for reporting.

## Remote And Merge Gates

Required GitHub checks are the source of truth for the exact pull-request head. Apply Build/Test/Package/Smoke,
CodeQL, dependency review, SBOM, container, benchmark, and image gates when configured. Do not accept failed,
cancelled, stale, skipped-only, duplicate-only, queued, or pending required evidence.

Merge normally only after the current head is reviewed, mergeable, and green. Then verify the merge commit is on local
`main` and wait for exact merge-main CI and CodeQL before calling main green.
