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

class EnterpriseReadinessAuditDocumentationTest {
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path SPRINT_PACKET = Path.of("docs/ENTERPRISE_LAB_TRUST_HARDENING_SPRINT.md");
    private static final Path CONTAINER_EVIDENCE_LANE =
            Path.of("docs/CONTAINER_DISTRIBUTION_SIGNING_EVIDENCE_LANE.md");
    private static final Path CONTAINER_DRY_RUN_LANE =
            Path.of("docs/CONTAINER_SIGNING_DRY_RUN_VERIFICATION_LANE.md");
    private static final Path CI_WORKFLOW = Path.of(".github/workflows/ci.yml");
    private static final Path README = Path.of("README.md");
    private static final Path EXECUTIVE_SUMMARY = Path.of("docs/EXECUTIVE_SUMMARY.md");
    private static final Path PRODUCTION_SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SECURITY_POSTURE = Path.of("evidence/SECURITY_POSTURE.md");
    private static final List<Path> PUBLIC_READINESS_DOCS = List.of(
            README,
            AUDIT,
            SPRINT_PACKET,
            CONTAINER_EVIDENCE_LANE,
            CONTAINER_DRY_RUN_LANE,
            EXECUTIVE_SUMMARY,
            PRODUCTION_SUMMARY,
            TRUST_MAP,
            SECURITY_POSTURE);
    private static final List<String> UNSAFE_AFFIRMATIVE_CLAIMS = List.of(
            "enterprise production ready",
            "enterprise-production ready",
            "enterprise-production-ready",
            "production certified gateway",
            "production-certified gateway",
            "container signing complete",
            "container signing is complete",
            "signed container artifact",
            "signed image artifact",
            "registry-published artifact",
            "signed container published",
            "signed image published",
            "registry publish complete",
            "registry publication complete",
            "dry-run signed successfully",
            "real tenant proof complete",
            "real idp proof complete",
            "live cloud validated",
            "live aws validated",
            "production slo certified",
            "production sla certified");
    private static final Pattern NEGATED_BOUNDARY =
            Pattern.compile("\\b(?:not|no|without)\\s+[a-z0-9\\-/ ]{0,80}$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Test
    void auditRefreshesEnterpriseReadinessForCurrentLabIdentity() throws Exception {
        String audit = read(AUDIT);

        for (String expected : List.of(
                "Date: 2026-05-14",
                "LoadBalancerPro Enterprise Lab",
                "Enterprise Lab ready",
                "Production Gateway Candidate",
                "main",
                "11c60ce621357a76ca946ddfb8729a38b2f149a1",
                "2.5.0",
                "Protect main",
                "Build, Test, Package, Smoke",
                "Analyze Java (java-kotlin)",
                "Open Dependabot alerts: `0`",
                "Open code-scanning alerts: `0`",
                "Open secret-scanning alerts: `0`",
                "requires `0` approving reviews",
                "Container Distribution Is Deferred",
                "Final Readiness Call")) {
            assertTrue(audit.contains(expected), "audit should mention " + expected);
        }
    }

    @Test
    void auditDoesNotOverclaimProductionReadiness() throws Exception {
        String normalized = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "enterprise production ready",
                "production gateway certified",
                "production deployment certified",
                "certification proof",
                "real tenant proof complete",
                "container signing is complete",
                "published registry image")) {
            assertFalse(normalized.contains(forbidden), "audit must not overclaim: " + forbidden);
        }

        assertTrue(normalized.contains("not enterprise-production ready"));
        assertTrue(normalized.contains("not ready to be sold, documented, or operated as a production enterprise gateway"));
    }

    @Test
    void sprintPacketDocumentsTrustHardeningBoundariesAndNextGate() throws Exception {
        String packet = read(SPRINT_PACKET);

        for (String expected : List.of(
                "Enterprise Lab ready",
                "not production certified",
                "not enterprise-production ready",
                "reviewer-ready Enterprise Lab",
                "manual GitHub settings change required",
                "future gated path",
                "Container distribution/signing lane",
                "This sprint does not execute the lane",
                "No registry publish in this sprint",
                "No signing in this sprint",
                "No live AWS validation",
                "mvn -q test",
                "mvn -q -DskipTests package",
                "git diff --check",
                "No release, registry, cloud, private-network, or ruleset mutation happened")) {
            assertTrue(packet.contains(expected), "sprint packet should mention " + expected);
        }
    }

    @Test
    void publicReadinessDocsRejectUnsafeAffirmativeClaims() throws Exception {
        for (Path path : PUBLIC_READINESS_DOCS) {
            assertNoUnsafeAffirmativeClaims(path, read(path));
        }
    }

    @Test
    void containerDistributionSigningEvidenceLaneDocumentsFutureGatedBoundaries() throws Exception {
        String lane = read(CONTAINER_EVIDENCE_LANE);

        for (String expected : List.of(
                "future gated",
                "No container signing is performed by this sprint",
                "not production certified",
                "not enterprise-production ready",
                "pending approval",
                "not executed in this sprint",
                "No registry publish in this sprint",
                "No signing in this sprint",
                "GitHub Container Registry",
                "Sigstore/cosign keyless signing",
                "Current Local-Only Verification")) {
            assertTrue(lane.contains(expected), "container evidence lane should mention " + expected);
        }
    }

    @Test
    void containerSigningDryRunLaneDocumentsNoPublishNoSignBoundaries() throws Exception {
        String lane = read(CONTAINER_DRY_RUN_LANE);

        for (String expected : List.of(
                "dry-run verification lane",
                "No container is published",
                "No container is signed",
                "not production certified",
                "not enterprise-production ready",
                "No registry credentials, signing keys, or secrets are required",
                "CI Dry-Run Evidence Artifact",
                "container-dry-run-evidence-no-publish-no-sign",
                "target/container-dry-run-evidence/",
                "dry-run-summary.md",
                "trivy-summary.txt",
                "No registry login is performed",
                "no registry credentials are used",
                "future examples only",
                "not run in this sprint",
                "No registry publish",
                "No signing",
                "git diff --check",
                "docker build -t loadbalancerpro:dry-run .")) {
            assertTrue(lane.contains(expected), "container dry-run lane should mention " + expected);
        }
    }

    @Test
    void ciWorkflowUploadsContainerDryRunEvidenceWithoutPublishSignLogin() throws Exception {
        String workflow = read(CI_WORKFLOW);
        String normalized = workflow.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "Capture container dry-run evidence",
                "target/container-dry-run-evidence",
                "SOURCE_SHA",
                "WORKFLOW_SHA",
                "loadbalancerpro:ci-dry-run-${SOURCE_SHA}",
                "Source commit SHA",
                "Workflow SHA",
                "docker image inspect",
                "image-inspect.json",
                "image-history.txt",
                "image-list.txt",
                "image-id.txt",
                "repo-digests.json",
                "dry-run-summary.md",
                "trivy-summary.txt",
                "Upload container dry-run evidence",
                "container-dry-run-evidence-no-publish-no-sign",
                "No container was published",
                "No container was signed",
                "No registry login was performed",
                "No registry credentials were used")) {
            assertTrue(workflow.contains(expected), "CI workflow should mention " + expected);
        }

        for (String forbidden : List.of(
                "docker push",
                "docker login",
                "cosign sign",
                "cosign attest",
                "gh release",
                "git tag")) {
            assertFalse(normalized.contains(forbidden), "CI workflow must not perform " + forbidden);
        }

        assertNoUnsafeAffirmativeClaims(CI_WORKFLOW, workflow);
    }

    @Test
    void currentReviewerEntryPointsLinkEnterpriseReadinessAudit() throws Exception {
        for (Path path : List.of(README, EXECUTIVE_SUMMARY, PRODUCTION_SUMMARY, TRUST_MAP, SECURITY_POSTURE)) {
            assertTrue(read(path).contains("ENTERPRISE_READINESS_AUDIT.md"),
                    path + " should link the current enterprise readiness audit");
        }
    }

    @Test
    void currentReviewerEntryPointsLinkTrustHardeningSprintPacket() throws Exception {
        for (Path path : List.of(README, AUDIT, PRODUCTION_SUMMARY, TRUST_MAP)) {
            assertTrue(read(path).contains("ENTERPRISE_LAB_TRUST_HARDENING_SPRINT.md"),
                    path + " should link the trust hardening sprint packet");
        }
    }

    @Test
    void currentReviewerEntryPointsLinkContainerEvidenceLane() throws Exception {
        for (Path path : List.of(AUDIT, SPRINT_PACKET, PRODUCTION_SUMMARY, TRUST_MAP)) {
            assertTrue(read(path).contains("CONTAINER_DISTRIBUTION_SIGNING_EVIDENCE_LANE.md"),
                    path + " should link the container distribution/signing evidence lane");
        }
    }

    @Test
    void currentReviewerEntryPointsLinkContainerDryRunLane() throws Exception {
        for (Path path : List.of(CONTAINER_EVIDENCE_LANE, AUDIT, SPRINT_PACKET, PRODUCTION_SUMMARY, TRUST_MAP)) {
            assertTrue(read(path).contains("CONTAINER_SIGNING_DRY_RUN_VERIFICATION_LANE.md"),
                    path + " should link the container signing dry-run verification lane");
        }
    }

    @Test
    void securityPostureUsesCurrentAuditAnchorAndPolicyLanguage() throws Exception {
        String posture = read(SECURITY_POSTURE);

        assertTrue(posture.contains("Current audit anchor: `main`"));
        assertTrue(posture.contains("docs/ENTERPRISE_READINESS_AUDIT.md"));
        assertTrue(posture.contains("## LASE Policy Posture"));
        assertTrue(posture.contains("LASE policy defaults to `off`"));
        assertTrue(posture.contains("active-experiment` is explicit, guarded, bounded, audited"));
        assertFalse(posture.contains("Audited baseline: `loadbalancerpro-clean`"));
        assertFalse(posture.contains("LASE remains advisory/shadow-only."));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void assertNoUnsafeAffirmativeClaims(Path path, String content) {
        String[] lines = content.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            String normalized = normalizeForClaimSearch(lines[index]);
            for (String forbidden : UNSAFE_AFFIRMATIVE_CLAIMS) {
                int start = normalized.indexOf(forbidden);
                if (start >= 0 && !isHonestNegativeBoundary(normalized, start)) {
                    assertFalse(true, path + ":" + (index + 1) + " must not affirm unsafe claim: " + forbidden);
                }
            }
        }
    }

    private static String normalizeForClaimSearch(String line) {
        return WHITESPACE.matcher(line.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    private static boolean isHonestNegativeBoundary(String line, int forbiddenStart) {
        // Guard rejects affirmative overclaims while allowing truthful disclaimers such as
        // "not production certified" or "not enterprise-production ready".
        String prefix = line.substring(0, forbiddenStart);
        if (NEGATED_BOUNDARY.matcher(prefix).find()) {
            return true;
        }
        String trimmed = line.trim();
        return trimmed.startsWith("no ") || trimmed.startsWith("- no ");
    }
}
