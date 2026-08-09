# Session Manager

- Active work: `P-3.4` deterministic local proxy benchmark and nightly soak regression harness
- Branch: `codex/p-3-4-benchmark-soak-harness`
- PR: `#550`
- Exact source main: `54f1b7f9c27daadfa3aafb58c186ef665cfc79d9`
- Previous slot: `P-3.3` is `MAIN_GREEN` through PR `#547`, final head `965a251d31217233753e8fdc5c12adb2ee45bebe`, merge `54f1b7f9c27daadfa3aafb58c186ef665cfc79d9`
- Completed gates: exact-green source main; P-3.3 PR and merge SHA reconciliation; exact-main CI and CodeQL audit; P-3.4 acceptance and existing Compose/metrics/admin surface audit; local Bash/Compose/actionlint/focused/full/package gates; first-head remote CI/CodeQL and six-scenario smoke; first one-hour traffic phases completed but heap evidence was invalidated by parser defects
- Genuine blocker: none
- Next action: push the portable heap-parser repair, then require fresh exact-head CI/CodeQL and a successful one-hour soak with positive heap samples before merge
