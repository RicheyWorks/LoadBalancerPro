# Session Manager

- Active work: `P-3.4` deterministic local proxy benchmark and nightly soak regression harness
- Branch: `codex/p-3-4-benchmark-soak-harness`
- PR: `#550`
- Exact source main: `54f1b7f9c27daadfa3aafb58c186ef665cfc79d9`
- Previous slot: `P-3.3` is `MAIN_GREEN` through PR `#547`, final head `965a251d31217233753e8fdc5c12adb2ee45bebe`, merge `54f1b7f9c27daadfa3aafb58c186ef665cfc79d9`
- Completed gates: exact-green source main; P-3.3 PR and merge SHA reconciliation; exact-main CI and CodeQL audit; P-3.4 acceptance and existing Compose/metrics/admin surface audit; Bash syntax/validation; Compose render; workflow actionlint; focused harness/campaign/reload/admin/metrics/access-log checks; full 2,612-test zero-skip suite; skipped-test and full package modes
- Genuine blocker: none
- Next action: push the PR-state checkpoint, then require exact-head CI/CodeQL and the one-hour workflow-dispatch soak before merge
