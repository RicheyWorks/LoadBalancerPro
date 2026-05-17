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

class CiEvidenceGateReadinessLaneDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PERFORMANCE_AUTH_LANE =
            Path.of("docs/MEASURED_PERFORMANCE_BASELINE_AND_AUTH_PROOF_LANE.md");
    private static final Path CI_GATE_LANE = Path.of("docs/CI_EVIDENCE_GATE_READINESS_LANE.md");

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
    void ciEvidenceGateReadinessLaneExistsAndIsLinkedFromReviewerEntryPoints() throws Exception {
        assertTrue(Files.exists(CI_GATE_LANE), "CI evidence gate readiness lane should exist");

        for (Path entryPoint : List.of(README, TRUST_MAP, READINESS_AUDIT, PERFORMANCE_AUTH_LANE)) {
            assertTrue(read(entryPoint).contains("CI_EVIDENCE_GATE_READINESS_LANE.md"),
                    entryPoint + " should link the CI evidence gate readiness lane");
        }
    }

    @Test
    void ciEvidenceGateReadinessLaneDefinesFutureInputsAndPassFailSemantics() throws Exception {
        String lane = read(CI_GATE_LANE);

        for (String expected : List.of(
                "Status: readiness lane only. No CI enforcement change is made by this document.",
                "local flight simulator and black-box recorder",
                "future gate",
                "Candidate Evidence Inputs",
                "Candidate Local Producers",
                "Future Pass Conditions",
                "Future Fail Conditions",
                "What Remains Manual Today",
                "Intentionally Not Enforced Yet",
                "target/enterprise-lab-runs/enterprise-lab-run-summary.md",
                "target/controlled-adaptive-routing/controlled-adaptive-routing-policy-summary.md",
                "target/enterprise-lab-observability/observability-summary.md",
                "target/performance-baseline/performance-summary.md",
                "target/enterprise-auth-proof/enterprise-auth-proof-summary.md",
                "target/adaptive-routing-experiments/adaptive-routing-experiment.md",
                "scripts\\smoke\\enterprise-lab-workflow.ps1 -Package",
                "scripts\\smoke\\controlled-adaptive-routing-policy.ps1 -Package",
                "scripts\\smoke\\enterprise-lab-observability-pack.ps1 -Package",
                "scripts\\smoke\\performance-baseline.ps1 -Package",
                "scripts\\smoke\\enterprise-auth-proof.ps1 -Package",
                "scripts\\smoke\\adaptive-routing-experiment.ps1 -Package")) {
            assertTrue(lane.contains(expected), "lane should mention " + expected);
        }
    }

    @Test
    void ciEvidenceGateReadinessLaneKeepsEnforcementBoundariesExplicit() throws Exception {
        String lane = read(CI_GATE_LANE);
        String normalized = lane.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not create a required check",
                "mutate branch protection",
                "mutate rulesets",
                "change github settings",
                "does not add or change `.github/workflows/` files",
                "does not add required checks",
                "does not enforce branch protection",
                "change repository settings",
                "not enforced yet",
                "what remains manual today")) {
            assertTrue(normalized.contains(expected), "lane should keep boundary: " + expected);
        }

        for (String unsafeClaim : List.of(
                "branch protection has been changed",
                "required checks have been changed",
                "rulesets have been changed",
                "github settings have been changed",
                "ci evidence gate is enforced",
                "live enforcement is enabled",
                "gate result: pass",
                "gate result: fail")) {
            assertFalse(normalized.contains(unsafeClaim), "lane must not claim " + unsafeClaim);
        }
    }

    @Test
    void ciEvidenceGateReadinessLaneAvoidsUnsafeCommandsAndOverclaims() throws Exception {
        String lane = read(CI_GATE_LANE);
        String normalized = lane.toLowerCase(Locale.ROOT);

        assertFalse(UNSAFE_COMMAND.matcher(lane).find(),
                "lane should not instruct release, tag, publish, sign, or GitHub settings mutation commands");
        assertFalse(CLOUD_MUTATION_COMMAND.matcher(lane).find(),
                "lane should not instruct cloud mutation commands");

        for (String unsafeClaim : List.of(
                "production certification complete",
                "production certified gateway",
                "production slo proof is complete",
                "production sla proof is complete",
                "live cloud validated",
                "real tenant validation complete",
                "real enterprise idp validation is complete",
                "signed container published",
                "registry publish complete",
                "github governance settings applied",
                "governance-applied proof complete",
                "benchmark result:",
                "p95 latency is",
                "p99 latency is",
                "requests per second is")) {
            assertFalse(normalized.contains(unsafeClaim), "lane must not include overclaim: " + unsafeClaim);
        }

        for (String boundary : List.of(
                "does not prove production readiness",
                "production slo/sla behavior",
                "live-cloud behavior",
                "real tenant behavior",
                "real enterprise idp behavior",
                "signed-container provenance",
                "registry publication",
                "governance-applied status")) {
            assertTrue(normalized.contains(boundary), "lane should keep not-proven boundary: " + boundary);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
