# LoadBalancerPro Next Release Review

Prepared: 2026-05-03

> Phase 1 repo-move note, 2026-05-09: This release review is historical and predates the `RicheyWorks/LoadBalancerPro` repository move. Do not use its old `public` remote, `richmond423/LoadBalancerPro`, or old-account instructions for current release work. GitHub Release publishing remains manual in the RicheyWorks repo after artifact workflow verification.

Scope: review and planning only. No branches were merged, no tags were created, no remotes were changed, no pushes were made, no files were deleted, and no production code was touched while preparing this document.

## Current Public Branch State

Local branch:

```text
loadbalancerpro-clean
```

Current commit:

```text
f894585a2b95336db28bd55868c3f9c932b3a31a
Document repo truth audit and publication plan
```

Remote state verified:

```text
origin/loadbalancerpro-clean = f894585a2b95336db28bd55868c3f9c932b3a31a
public/loadbalancerpro-clean = f894585a2b95336db28bd55868c3f9c932b3a31a
public/main                  = d09701dc31a5cedaab8c6b21ee85bac31b0930ab
public tags                  = none advertised
```

Local state verified:

```text
On branch loadbalancerpro-clean
Your branch is up to date with 'origin/loadbalancerpro-clean'.
nothing to commit, working tree clean
```

Remotes:

```text
origin = https://github.com/cs-olympic/finalcs2-richmond423.git
public = https://github.com/richmond423/LoadBalancerPro.git
```

## Default Branch Readiness

`loadbalancerpro-clean` is ready to be reviewed as the public default branch from a visibility and repository-truth standpoint:

- It is published to the intended public repo.
- It includes the completed codebase plus the repo-truth audit and publication plan.
- It has not disturbed public `main`.
- No public tags were pushed.
- The last full Maven test run recorded in `docs/REPO_TRUTH_AUDIT.md` passed with 464 tests, 0 failures, 0 errors, and 0 skipped tests.

Before making it the public default branch, run a fresh verification pass because the default-branch switch is a release/public-presentation action even if it does not change code.

Recommended default-branch decision:

```text
Make loadbalancerpro-clean the public default branch only after one fresh verification pass.
Leave public main untouched for now.
```

## Public Main Recommendation

Do not replace public `main` yet.

Recommended path:

1. Leave public `main` untouched at `d09701dc31a5cedaab8c6b21ee85bac31b0930ab`.
2. Use GitHub settings to make `loadbalancerpro-clean` the default branch, or keep it as the visible completed branch while reviewing.
3. Decide later whether `main` should be archived, renamed, or replaced.

Avoid a direct merge into public `main` for now. The audit found that the public `main` commit was not present in the local repository object database, which suggests unrelated or divergent history. A pull request or merge may be noisy or awkward. If the goal is only to show the completed project by default, changing the default branch is safer than rewriting `main`.

Only consider replacing `main` after explicit backup and review. If replacement is chosen later, use `--force-with-lease`, never plain `--force`, and only after confirming branch protection, Pages/Actions settings, and any classroom or portfolio links.

## Version Recommendation

Recommended immediate release label:

```text
v1.0.1
```

Use `v1.0.1` if the next release is the current published branch at `f894585`, with no additional branch merges. This is best framed as a public publication and documentation baseline for the already completed v1 line.

Recommended later hardening release label:

```text
v1.1.0
```

Use `v1.1.0` if the next public release includes `codex/robust-safety-test-expansion`, `codex/supply-chain-pinning`, or both. Those branches add meaningful hardening and supply-chain changes beyond publication documentation.

Do not move `v1.0.0`. The existing local/configured-origin `v1.0.0` points to an earlier release commit, and the current branch is ahead of it. Moving it would make provenance harder to explain.

## Unmerged Branch Review

### `codex/robust-safety-test-expansion`

Unmerged commits:

```text
928be4a Add robust safety tests batch 1
b837e82 Add telemetry guardrail edge tests
32b1b76 Add replay cloud isolation tests
f4f4e92 Add input API hardening tests
```

Touched areas:

```text
Production code:
src/main/java/api/ApiErrorResponse.java
src/main/java/api/RestExceptionHandler.java
src/main/java/core/CloudManager.java
src/main/java/core/LaseShadowAdvisor.java
src/main/java/core/LoadBalancer.java

Tests:
src/test/java/api/AllocatorControllerTest.java
src/test/java/api/OAuth2AuthorizationTest.java
src/test/java/api/ProdApiKeyModeAliasProtectionTest.java
src/test/java/api/ProdApiKeyProtectionTest.java
src/test/java/api/ProdCorsOverrideConfigurationTest.java
src/test/java/api/config/TelemetryConfigurationTest.java
src/test/java/cli/LaseReplayCommandTest.java
src/test/java/core/CloudManagerGuardrailTest.java
src/test/java/core/LaseShadowAdvisorTest.java
src/test/java/util/UtilsTest.java
```

Change type:

- Production code: yes.
- Tests: yes.
- Docs: no.
- CI/Docker: no.

Merge friction:

- Merge-base review showed 15 files changed, 760 insertions, and 7 deletions.
- `git merge-tree` did not show conflict markers.
- Because the branch touches CloudManager and error/log sanitization paths, a clean merge is not the same thing as risk-free.

Risk notes:

- CloudManager sandbox resource-prefix behavior changes.
- API error shape changes for method-not-allowed and unsupported-media-type paths.
- LASE shadow advisor failure-message sanitization changes.
- LoadBalancer outer shadow-observation logging changes.
- These are desirable hardening changes, but they alter production behavior and should not be slipped into a public default/tag without focused review.

Recommendation:

```text
Defer to v1.1.0 unless there is a specific reason to harden before the first public default-branch switch.
```

If the project owner wants the strongest public baseline before any release tag, merge this branch first and call the result `v1.1.0`, not `v1.0.1`.

Verification after merging:

```bash
mvn -q test
mvn -q -DskipTests package
java -jar target/LoadBalancerPro-1.0.0.jar --lase-demo=healthy
java -jar target/LoadBalancerPro-1.0.0.jar --lase-demo=overloaded
java -jar target/LoadBalancerPro-1.0.0.jar --lase-demo=invalid-name
```

Also run an API smoke with the packaged JAR and verify:

```text
GET /api/health returns HTTP 200
POST /api/allocate/capacity-aware still returns finite JSON
unsupported method/content-type errors return safe structured JSON
no CloudManager call occurs during LASE replay
```

### `codex/supply-chain-pinning`

Unmerged commit:

```text
eb29627 Pin supply-chain inputs
```

Touched areas:

```text
.github/workflows/ci.yml
Dockerfile
README.md
```

Change type:

- Production code: no.
- Tests: no.
- Docs: yes.
- CI: yes.
- Docker: yes.

What it does:

- Pins GitHub Actions to specific commit SHAs while preserving comments for the upstream action names/version tags.
- Pins Docker base images by digest.
- Adds README notes about digest/action pinning and update review.

Merge friction:

- Merge-base review showed 3 files changed, 19 insertions, and 7 deletions.
- `git merge-tree` reported a README area changed in both branches, but no conflict markers.
- The README hunk should still be reviewed manually after merge.

Risk notes:

- Pinned GitHub Action SHAs must be verified as the intended upstream actions.
- Docker image digests must match the desired architectures and tags.
- CI could fail if a pinned action or image digest becomes unavailable or incompatible.
- Docker builds may become less flexible until digest update process is documented and followed.

Recommendation:

```text
Defer to v1.1.0 with robust-safety-test-expansion, or merge as a separate supply-chain hardening PR after v1.0.1.
```

It is attractive hardening work, but it is not required for the immediate public default-branch action. Deferring keeps the first public release simple and avoids mixing publication repair with CI/Docker pinning risk.

Verification after merging:

```bash
mvn -q test
mvn -q -DskipTests package
docker build -t loadbalancerpro:supply-chain-check .
docker run --rm -d --name loadbalancerpro-supply-chain-check -p 127.0.0.1:18081:8080 loadbalancerpro:supply-chain-check
curl -fsS http://127.0.0.1:18081/api/health
docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' loadbalancerpro-supply-chain-check
docker stop loadbalancerpro-supply-chain-check
```

Also verify in GitHub after pushing:

```text
Actions workflow resolves all pinned actions.
Docker build step pulls pinned base image digests successfully.
Trivy scan still runs.
Dependency review job still runs on pull requests.
```

## Large Historical JavaFX File Warning

During the earlier public branch push, GitHub warned about:

```text
lib/javafx-sdk/bin/jfxwebkit.dll
```

The current HEAD tree does not contain a `lib/` directory, and `git lfs ls-files` currently reports no tracked LFS files. This suggests the warning is from reachable Git history, not the current working tree.

Risks:

- Clones may remain larger than expected because history contains a large binary.
- GitHub may keep warning on future pushes involving that history.
- If additional large historical files exist, the repo may feel heavier than the current tree suggests.
- Cleaning it requires history rewriting, which would disrupt already-pushed refs and require force-push coordination.

Recommendation:

```text
Defer large-file history cleanup.
```

Do not rewrite history tonight. The current public branch has already been safely published and verified. History cleanup should be its own planned maintenance task, with explicit backup, fresh clone testing, and coordinated force-push approval if the project owner decides the repository size matters enough.

If cleanup is done later, use a dedicated plan with tools such as `git filter-repo` or BFG, then verify all branches/tags that should survive. Do not combine history cleanup with the default-branch switch or release tagging.

## DO NOT RUN YET: Proposed Next Command Sequence

### Option A: Conservative v1.0.1 Public Baseline

Use this when the goal is to make the already published completed branch the public baseline without merging more work.

```bash
# DO NOT RUN YET
git status
git branch --show-current
git log --oneline --decorate -8
git ls-remote --heads public loadbalancerpro-clean
git ls-remote --heads public main
git ls-remote --tags public

mvn -q test
mvn -q -DskipTests package

# Optional smoke checks after package:
java -jar target/LoadBalancerPro-1.0.0.jar --lase-demo=healthy
java -jar target/LoadBalancerPro-1.0.0.jar --lase-demo=overloaded

# After review only:
git tag -a v1.0.1 -m "LoadBalancerPro v1.0.1: public publication baseline"
git push origin v1.0.1
git push public v1.0.1
```

After this, change GitHub default branch to `loadbalancerpro-clean` through GitHub settings, or leave `main` untouched and link directly to the completed branch.

### Option B: v1.1.0 Hardening Release

Use this when the goal is to include the unmerged safety and supply-chain hardening before a public release tag.

```bash
# DO NOT RUN YET
git status
git switch loadbalancerpro-clean

git merge --no-ff codex/robust-safety-test-expansion
git diff --stat HEAD~1..HEAD
mvn -q test
mvn -q -DskipTests package

git merge --no-ff codex/supply-chain-pinning
git diff --stat HEAD~1..HEAD
mvn -q test
mvn -q -DskipTests package
docker build -t loadbalancerpro:release-check .

# After review only:
git tag -a v1.1.0 -m "LoadBalancerPro v1.1.0: public hardening release"
git push origin loadbalancerpro-clean
git push public loadbalancerpro-clean:loadbalancerpro-clean
git push origin v1.1.0
git push public v1.1.0
```

Do not run this as a batch without inspecting the merge results, especially the CloudManager changes, README hunk, CI workflow, and Dockerfile digests.

### Option C: Public Main Replacement

This remains the high-risk path.

```bash
# DO NOT RUN YET
git push --force-with-lease public loadbalancerpro-clean:main
```

Do not run this unless the public smaller `main` has been backed up, branch protection/default branch behavior has been reviewed, and the owner explicitly approves replacing public `main`.

## Recommendation

Recommended next safest path:

```text
Use Option A first.
```

Make `loadbalancerpro-clean` the public default branch after a fresh verification pass, leave public `main` untouched, and create `v1.0.1` only after that verification. Then handle `codex/robust-safety-test-expansion` and `codex/supply-chain-pinning` as a focused `v1.1.0` hardening release.

Why:

- It separates publication repair from hardening changes.
- It avoids forcing over public `main`.
- It preserves `v1.0.0` history.
- It avoids mixing a history-cleanup problem with release work.
- It gives reviewers a stable public branch before more production/CI/Docker changes are introduced.
