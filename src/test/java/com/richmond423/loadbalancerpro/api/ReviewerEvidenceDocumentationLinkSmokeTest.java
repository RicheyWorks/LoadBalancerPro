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

class ReviewerEvidenceDocumentationLinkSmokeTest {
    private static final Path README = Path.of("README.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");

    private static final List<String> REVIEWER_EVIDENCE_PAGES = List.of(
            "/enterprise-lab-reviewer.html",
            "/operator-evidence-dashboard.html",
            "/evidence-timeline.html",
            "/evidence-export-packet.html");

    private static final List<Path> REVIEWER_ENTRY_DOCS = List.of(README, REVIEWER_TRUST_MAP);

    private static final List<String> COMMON_LOCAL_BOUNDARY_PHRASES = List.of(
            "controlled lab evidence",
            "local reproducibility",
            "does not claim production certification",
            "live-cloud proof",
            "real-tenant proof",
            "sla/slo proof",
            "registry publication",
            "container signing",
            "governance application");

    private static final Pattern UNSAFE_COMMAND =
            Pattern.compile("(?im)^\\s*(gh\\s+release|git\\s+tag|docker\\s+(?:login|push)"
                    + "|cosign\\s+(?:sign|attest)|terraform\\s+(?:apply|destroy)|pulumi\\s+up"
                    + "|kubectl\\s+apply|gh\\s+(?:api|secret|variable)\\b.*"
                    + "(?:ruleset|secret|environment|branch_protection|protection))\\b");

    private static final Pattern CLOUD_MUTATION_COMMAND =
            Pattern.compile("(?im)^\\s*(aws\\s+(?:cloudformation|ecs|eks|elbv2|autoscaling|ec2)\\s+"
                    + "(?:create|delete|modify|put|update|run|authorize)|az\\s+"
                    + "(?:deployment|aks|network|vm)\\s+(?:create|delete|update)|gcloud\\s+.*\\s+"
                    + "(?:create|delete|update))\\b");



    @Test
    void reviewerEntryDocsAvoidUnsafeCommandsAndAffirmativeOverclaims() throws Exception {
        for (Path doc : REVIEWER_ENTRY_DOCS) {
            String content = read(doc);
            String normalized = normalized(content);

            assertFalse(UNSAFE_COMMAND.matcher(content).find(),
                    doc + " should not instruct reviewers to release, tag, publish, sign, or mutate GitHub settings");
            assertFalse(CLOUD_MUTATION_COMMAND.matcher(content).find(),
                    doc + " should not instruct reviewers to mutate cloud resources");

            for (String unsafeClaim : List.of(
                    "production certification complete",
                    "production certified gateway",
                    "production-certified gateway",
                    "sla proof complete",
                    "slo proof complete",
                    "live cloud validated",
                    "live-cloud validated",
                    "live cloud validation complete",
                    "real tenant proof complete",
                    "real-tenant proof complete",
                    "signed container published",
                    "signed-container proof complete",
                    "registry publish complete",
                    "registry publication complete",
                    "container signing complete",
                    "github governance settings applied",
                    "governance settings applied: true",
                    "governance settings applied: yes",
                    "governance-applied proof complete")) {
                assertFalse(normalized.contains(unsafeClaim),
                        doc + " must not include affirmative overclaim: " + unsafeClaim);
            }
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String normalized(String content) {
        return content.toLowerCase(Locale.ROOT);
    }
}
