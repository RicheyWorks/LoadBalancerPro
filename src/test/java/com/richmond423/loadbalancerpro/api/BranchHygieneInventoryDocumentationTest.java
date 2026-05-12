package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class BranchHygieneInventoryDocumentationTest {
    private static final Path INVENTORY = Path.of("docs/BRANCH_HYGIENE_INVENTORY.md");
    private static final Pattern EXECUTABLE_DELETE_COMMAND =
            Pattern.compile("(?im)^(?!\\s*#)\\s*(?:git\\s+push\\s+origin\\s+--delete\\s+\\S+|for\\s+|foreach\\s+)");
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+" + "CloudManager\\s*\\(|" + "CloudManager\\s*\\(");
    private static final Pattern RELEASE_COMMAND =
            Pattern.compile("(?im)^\\s*(gh\\s+release|git\\s+tag)\\b");

    @Test
    void branchHygieneInventoryExistsAndRecordsReadOnlyPosture() throws Exception {
        String doc = read(INVENTORY);

        assertTrue(doc.contains("# Branch Hygiene Inventory"));
        assertTrue(doc.contains("Audited `origin/main`: `fdde2f48b0fc1db1d82130032e17d8d175915b3e`"));
        assertTrue(doc.contains("Total remote branches audited: 125"));
        assertTrue(doc.contains("Merged into `origin/main`: 124"));
        assertTrue(doc.contains("Not merged into `origin/main`: 1"));
        assertTrue(doc.contains("No branches were deleted"));
        assertTrue(doc.contains("Owner approval required before deletion"));
    }

    @Test
    void branchHygieneInventoryHasAllRequiredBuckets() throws Exception {
        String doc = read(INVENTORY);

        assertTrue(doc.contains("## KEEP"));
        assertTrue(doc.contains("## DELETE CANDIDATE"));
        assertTrue(doc.contains("## UNKNOWN / DO NOT TOUCH"));
        assertTrue(doc.contains("| KEEP | 1 |"));
        assertTrue(doc.contains("| DELETE CANDIDATE | 119 |"));
        assertTrue(doc.contains("| UNKNOWN / DO NOT TOUCH | 5 |"));
        assertTrue(doc.contains("`origin/main`"));
        assertTrue(doc.contains("`origin/codex/csrf-disposition-proof`"));
        assertTrue(doc.contains("`origin/codex/enterprise-proxy-foundation`"));
        assertTrue(doc.contains("`origin/codex/readme-visibility-polish`"));
    }

    @Test
    void backupReleaseFeatureAndUnmergedBranchesArePreservedAsDoNotTouch() throws Exception {
        String doc = read(INVENTORY);

        for (String branch : List.of(
                "origin/backup/pre-normalization-main-2026-05-09",
                "origin/feature/v2.4.0-package-namespace-migration",
                "origin/release/v1.1.0-hardening-review",
                "origin/release/v1.1.1-version-alignment",
                "origin/release/v1.2.0-routing-engine")) {
            assertTrue(doc.contains("`" + branch + "`"), branch + " should be preserved");
        }

        assertTrue(doc.contains("backup branch and the only audited branch not merged"));
        assertTrue(doc.contains("release/history branch"));
    }

    @Test
    void inventoryDoesNotContainExecutableMassDeleteOrUnsafeMutationGuidance() throws Exception {
        String doc = read(INVENTORY);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertFalse(EXECUTABLE_DELETE_COMMAND.matcher(doc).find(),
                "inventory must not contain executable branch deletion loops or commands");
        assertTrue(doc.contains("`# git push origin --delete <branch-name>`"),
                "deletion command should remain a commented template only");
        assertFalse(RELEASE_COMMAND.matcher(doc).find(), "inventory must not add release or tag commands");
        assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(doc).find(), "inventory must not construct CloudManager");
        assertFalse(normalized.contains("production-grade security"));
        assertFalse(normalized.contains("enterprise security certification"));
        assertFalse(normalized.contains("benchmark result"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
