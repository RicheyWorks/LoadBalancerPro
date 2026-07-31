# LoadBalancerPro v2.4.0 Package Namespace Migration Plan

Date: 2026-05-04

## A. Current State

v2.3.2 is shipped.

The Maven `groupId` is currently `com.example`, which now stands out because the repository otherwise has mature enterprise-demo release discipline, supply-chain evidence, operations/deployment documentation, and dependency-maintenance hygiene.

The Java source packages are still flat:

- `api`
- `cli`
- `core`
- `gui`
- `util`

The current scan found 160 package declarations under `src/main/java` and `src/test/java`, plus 146 imports that reference the flat package roots directly. This is broad enough to require a dedicated refactor slice.

## B. Why Namespace Migration Matters

`com.example` looks template-like and undercuts the otherwise polished project presentation.

A project-owned root package improves professional presentation, makes source ownership clearer, and lowers collision risk if the code is reused as a library or embedded in larger demos.

The migration is mostly mechanical, but it touches many files. It must be handled carefully because broad package churn can hide accidental behavior changes.

## C. Proposed Target Namespace

Recommended root namespace:

```text
com.richmond423.loadbalancerpro
```

Package mapping:

| Current package | Target package |
| --- | --- |
| `api` | `com.richmond423.loadbalancerpro.api` |
| `api.config` | `com.richmond423.loadbalancerpro.api.config` |
| `cli` | `com.richmond423.loadbalancerpro.cli` |
| `core` | `com.richmond423.loadbalancerpro.core` |
| `gui` | `com.richmond423.loadbalancerpro.gui` |
| `util` | `com.richmond423.loadbalancerpro.util` |

Test packages need an explicit policy during implementation. Several tests currently use `api`, `cli`, and `core`, while others use `test.core` or `test.util`. The safest plan is to migrate tests to packages that either mirror production packages or use a consistent test namespace under `com.richmond423.loadbalancerpro`.

## D. pom.xml Changes To Plan

Plan these Maven identity and main-class changes:

- Change `groupId` from `com.example` to `com.richmond423`.
- Keep the product artifact identity as `loadbalancerpro` unless the owner explicitly chooses another artifactId policy.
- Decide whether to preserve the current `artifactId` value `LoadBalancerPro` for release artifact naming compatibility, or normalize it to `loadbalancerpro` as a separate owner-approved decision.
- Update Spring Boot plugin `mainClass` from `api.LoadBalancerApiApplication` to `com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication`.
- Check whether any `Start-Class`, `exec.mainClass`, launch metadata, or CLI execution examples need updates.
- Do not combine this with a release version bump until the namespace refactor passes locally.

## E. Files Likely To Change

Likely implementation changes:

- Most `src/main/java` files.
- Most `src/test/java` files.
- `pom.xml`.
- `README.md` where active class/package or `exec.mainClass` examples appear.
- Docs that reference active class/package names, especially current architecture or CLI examples.
- `.github/workflows/ci.yml` if startup class string checks still reference `LoadBalancerApiApplication`.
- `.github/workflows/release-artifacts.yml` only if package/main-class names are referenced there.
- `Dockerfile` only if a main class/package name is referenced there; current inspection found only Maven package commands, not a Java package reference.
- Resource paths if package-dependent resources are moved. In particular, GUI code currently loads `gui.messages`; moving GUI packages may require a resource-location decision.

Historical release plans and old evidence docs may contain old package names. Do not rewrite historical docs wholesale unless they would confuse active usage.

## F. Risks

- Massive import churn can hide real behavior changes.
- Spring Boot component scanning can break if `LoadBalancerApiApplication` is moved under a package that does not scan all required API configuration classes.
- Test imports and package-private access can break.
- CLI launch examples can break if `exec.mainClass` is not updated.
- JavaFX launch paths and GUI resource bundle loading can break if `gui.messages` is moved or renamed incorrectly.
- Documentation examples can drift from the new package names.
- Release metadata can get confusing if namespace migration and version bump are mixed too early.
- Review becomes harder if this is combined with dependency updates or unrelated refactors.

## G. Recommended Implementation Strategy

Use a single dedicated refactor branch:

```text
feature/v2.4.0-package-namespace-migration
```

Implementation should:

1. Move files into the matching directory tree under `src/main/java/com/richmond423/loadbalancerpro`.
2. Move or normalize test files under `src/test/java/com/richmond423/loadbalancerpro` according to the chosen test package policy.
3. Update package declarations.
4. Update imports.
5. Update `pom.xml` `groupId`.
6. Update Spring Boot `mainClass`.
7. Update README and docs only where active references exist.
8. Keep historical docs stable unless the reference is active guidance.
9. Run full tests and package.
10. Run JAR smoke checks.
11. Run Docker build and health smoke if local Docker access is available.
12. Run local SBOM generation.
13. Do not change runtime behavior.
14. Do not touch CloudManager logic except package/import changes.

## H. Verification Plan

After implementation:

```powershell
mvn -q test
mvn -q -DskipTests package
java -jar target/LoadBalancerPro-<version>.jar --version
java -jar target/LoadBalancerPro-<version>.jar --lase-demo=healthy
java -jar target/LoadBalancerPro-<version>.jar --lase-demo=overloaded
java -jar target/LoadBalancerPro-<version>.jar --lase-demo=invalid-name
mvn -B org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom -DoutputFormat=all -DoutputDirectory=target -DoutputName=bom "-Dcyclonedx.skipAttach=true"
git diff --check
```

`--lase-demo=invalid-name` should fail safely with valid-scenario guidance and no raw stack trace.

Optional if Docker is available:

```powershell
docker build -t loadbalancerpro:namespace-migration .
docker run --rm -d --name loadbalancerpro-namespace-migration -p 127.0.0.1:18083:8080 loadbalancerpro:namespace-migration
curl -fsS http://127.0.0.1:18083/api/health
docker stop loadbalancerpro-namespace-migration
```

Additional namespace checks:

```powershell
rg -n "^package (api|cli|core|gui|util|test)\b" src/main/java src/test/java
rg -n "import (api|cli|core|gui|util)\." src/main/java src/test/java
rg -n "api\.LoadBalancerApiApplication|cli\.LoadBalancerCLI|exec.mainClass=cli\.|core\.|gui\.|util\." pom.xml README.md src .github Dockerfile docs evidence
```

Expected result: no active flat package declarations or flat-package imports remain, except historical docs intentionally left unchanged.

## I. What Not To Change

- Do not change behavior.
- Do not change routing logic.
- Do not change CloudManager or AWS guardrails.
- Do not change allocation endpoint contracts.
- Do not change CLI workflows except package/main-class references.
- Do not combine with deprecated shim removal.
- Do not combine with Dependabot updates.
- Do not combine with Docker, Kubernetes, Terraform, or IAM work.
- Do not touch public main.
- Do not move existing tags.

## J. Alternatives

- Change `groupId` only first, then move packages later.
- Move packages first, then change `groupId` later.
- Leave packages flat and document why the project intentionally keeps a compact teaching/demo package layout.
- Choose `com.richmondworks.loadbalancerpro` instead of `com.richmond423.loadbalancerpro` if the owner wants a less username-specific namespace.
- Normalize `artifactId` separately from the package migration if release artifact naming compatibility is important.

## K. Recommendation

Proceed with planning now, but do not implement namespace migration until the owner is ready for a broad mechanical refactor.

Safest next step: implement the full namespace migration in a dedicated `feature/v2.4.0-package-namespace-migration` branch before taking more risky Dependabot PRs, because the current codebase is already verified at v2.3.2 and the namespace issue is now a visible polish gap. Keep the slice purely namespace-focused, with no dependency updates, behavior changes, Docker/Kubernetes/IAM work, or deprecated-shim cleanup mixed in.
