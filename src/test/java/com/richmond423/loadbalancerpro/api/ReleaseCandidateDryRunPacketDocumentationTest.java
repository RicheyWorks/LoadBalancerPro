package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class ReleaseCandidateDryRunPacketDocumentationTest {
    private static final Path PACKET = Path.of("docs/RELEASE_CANDIDATE_DRY_RUN_PACKET.md");
    private static final Path SCRIPT = Path.of("scripts/smoke/release-candidate-dry-run-packet.ps1");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path GATE = Path.of("docs/PRODUCTION_CANDIDATE_EVIDENCE_GATE.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path EXISTING_DRY_RUN = Path.of("docs/RELEASE_CANDIDATE_DRY_RUN.md");

    @Test
    void packetExistsAndIsLinkedFromReviewerEntryPoints() throws Exception {
        assertTrue(Files.exists(PACKET), "release-candidate packet doc should exist");
        assertTrue(Files.exists(SCRIPT), "release-candidate packet script should exist");

        for (Path doc : List.of(README, TRUST_MAP, SUMMARY, GATE, RUNBOOK, EXISTING_DRY_RUN)) {
            assertTrue(read(doc).contains("RELEASE_CANDIDATE_DRY_RUN_PACKET.md"),
                    doc + " should link the release-candidate dry-run packet");
        }
    }

    @Test
    void packetDocumentsReleaseFreeSafetyBoundary() throws Exception {
        String packet = read(PACKET);

        for (String expected : List.of(
                "This is not a release procedure.",
                "does not create tags",
                "GitHub Releases",
                "release assets",
                "registry images",
                "container signatures",
                "`release-downloads/`",
                "No container publication or container signing is performed by this dry-run packet",
                "does not require secrets")) {
            assertTrue(packet.contains(expected), "packet should document " + expected);
        }
    }

    @Test
    void packetCoversBuildSecuritySupplyChainAndSmokeEvidence() throws Exception {
        String packet = read(PACKET);

        for (String expected : List.of(
                "Commit under review",
                "mvn -q clean test",
                "mvn -q verify",
                "mvn -q -DskipTests package",
                "CycloneDX SBOM",
                "SHA-256 checksum",
                "GitHub artifact attestation",
                "CodeQL",
                "Dependency Review",
                "Trivy",
                "Docker base digest pinning",
                "Container default prod profile",
                "Prod API-key deny-by-default",
                "OAuth2 dedicated role claim",
                "DTO omitted-field validation",
                "Postman enterprise lab dry-run",
                "Operator run profiles dry-run",
                "packaged-jar security smoke")) {
            assertTrue(packet.contains(expected), "packet should cover " + expected);
        }
    }

    @Test
    void packetStatesContainerRegistrySigningDecisionWithoutPublishing() throws Exception {
        String packet = read(PACKET);

        for (String expected : List.of(
                "GitHub Release artifact-based",
                "Registry publication and container signing become required if distribution depends on deployable container images",
                "Registry target decision",
                "Image naming and immutable tag/digest policy",
                "Vulnerability scan evidence location",
                "Signing and attestation approach",
                "Rollback and retention policy",
                "Credential handling plan")) {
            assertTrue(packet.contains(expected), "packet should document " + expected);
        }
    }

    @Test
    void scriptWritesOnlyIgnoredTargetPacketEvidence() throws Exception {
        String script = read(SCRIPT);
        String normalized = script.toLowerCase(Locale.ROOT);

        assertTrue(script.contains("[string]$OutputDir = \"target/release-candidate-dry-run\""));
        assertTrue(script.contains("Assert-SafeOutputDirectory"));
        assertTrue(script.contains("Evidence output must stay under target/."));
        assertTrue(script.contains("release-candidate-dry-run-packet.md"));
        assertTrue(script.contains("release-candidate-dry-run-packet.json"));
        assertTrue(script.contains("LoadBalancerPro-$shortCommit-SHA256SUMS.txt"));
        assertTrue(script.contains("Test-Checksums"));
        assertTrue(script.contains("checksumVerified"));
        assertTrue(script.contains("[switch]$IncludeValidation"));
        assertTrue(script.contains("validated-externally"));
        assertTrue(script.contains("releaseDownloadsMutated = $false"));
        assertFalse(normalized.contains("remove-item"));
    }

    @Test
    void scriptAvoidsReleaseRegistrySigningCommandsAndSecretValues() throws Exception {
        String script = read(SCRIPT).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "git clean",
                "gh release create",
                "gh release upload",
                "gh release delete",
                "docker push",
                "git tag",
                "git push --tags",
                "cosign sign",
                "oras push",
                "helm push",
                "change_me_local_api_key",
                "ghp_",
                "github_pat_",
                "client_secret",
                "-----begin")) {
            assertFalse(script.contains(forbidden), "script should not contain " + forbidden);
        }

        assertTrue(script.contains("<redacted>"), "script should use placeholders for local smoke API-key values");
    }

    @Test
    void packetAvoidsEmbeddedSecretValuesAndUnsafeCommands() throws Exception {
        String packet = read(PACKET).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "gh release create",
                "gh release upload",
                "gh release delete",
                "docker push",
                "git tag -",
                "git push --tags",
                "cosign sign",
                "change_me_local_api_key",
                "ghp_",
                "github_pat_",
                "client_secret",
                "-----begin")) {
            assertFalse(packet.contains(forbidden), "packet should not contain " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
