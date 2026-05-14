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

class PostReleaseVerificationDocumentationTest {
    private static final Path POST_RELEASE = Path.of("docs/V2_5_0_POST_RELEASE_VERIFICATION.md");
    private static final Path README = Path.of("README.md");
    private static final Path RELEASE_NOTES = Path.of("docs/RELEASE_NOTES_v2.5.0.md");
    private static final Path PRODUCTION_SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path DECISION_SUMMARY = Path.of("docs/RELEASE_READINESS_DECISION_SUMMARY.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path SUPPLY_CHAIN = Path.of("evidence/SUPPLY_CHAIN_EVIDENCE.md");
    private static final Path TEST_EVIDENCE = Path.of("evidence/TEST_EVIDENCE.md");

    @Test
    void postReleaseVerificationExistsAndIsLinkedFromReviewerOperatorDocs() throws Exception {
        assertTrue(Files.exists(POST_RELEASE), "post-release verification note should exist");

        for (Path doc : List.of(README, RELEASE_NOTES, PRODUCTION_SUMMARY, TRUST_MAP, DECISION_SUMMARY, RUNBOOK,
                SUPPLY_CHAIN, TEST_EVIDENCE)) {
            assertTrue(read(doc).contains("V2_5_0_POST_RELEASE_VERIFICATION.md"),
                    doc + " should link the v2.5.0 post-release verification note");
        }
    }

    @Test
    void postReleaseVerificationRecordsExactReleaseIdentityAndWorkflowEvidence() throws Exception {
        String note = read(POST_RELEASE);

        for (String expected : List.of(
                "Release tag | `v2.5.0`",
                "Release commit | `4cc03750be5479d9f8f88f8ef8014e05a8dc587a`",
                "Project version | `2.5.0`",
                "https://github.com/RicheyWorks/LoadBalancerPro/releases/tag/v2.5.0",
                "https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/25838247936",
                "Release Artifacts workflow completed successfully",
                "Tag-push CI completed successfully")) {
            assertTrue(note.contains(expected), "post-release note should record " + expected);
        }
    }

    @Test
    void postReleaseVerificationRecordsAssetsChecksumsSbomsAndAttestations() throws Exception {
        String note = read(POST_RELEASE);

        for (String expected : List.of(
                "LoadBalancerPro-2.5.0.jar",
                "LoadBalancerPro-2.5.0-bom.json",
                "LoadBalancerPro-2.5.0-bom.xml",
                "LoadBalancerPro-2.5.0-SHA256SUMS.txt",
                "04457ad3404835301a4b0763a77877967750ec03753af23dea0ff2db18372859",
                "ca5fc9498589a1833dbe478fe139ce87b6130791b4feb37b38ca80fbf6e1a75b",
                "2d30c91e413e614305efe8b6316d93438219a12b5a7008b8545d5be1ac640090",
                "matched `LoadBalancerPro-2.5.0-SHA256SUMS.txt`",
                "SBOM JSON and SBOM XML assets are present",
                "`gh attestation verify` verified SLSA provenance",
                "JAR/SBOM attestation step also completed successfully")) {
            assertTrue(note.contains(expected), "post-release note should record " + expected);
        }
    }

    @Test
    void postReleaseVerificationKeepsContainerAndProductionCertificationLimitsHonest() throws Exception {
        String note = read(POST_RELEASE);

        for (String expected : List.of(
                "No container image was published",
                "No container signing was performed",
                "No registry push was performed",
                "Container registry publication and signing remain deferred",
                "No production deployment certification is claimed",
                "No real enterprise IdP tenant proof is included",
                "Production TLS, IAM, ingress",
                "operator-owned deployment responsibilities")) {
            assertTrue(note.contains(expected), "post-release note should record " + expected);
        }
    }

    @Test
    void currentReviewerDocsNoLongerClaimV250IsUnpublished() throws Exception {
        String docs = (read(README) + "\n" + read(RELEASE_NOTES) + "\n" + read(PRODUCTION_SUMMARY) + "\n"
                + read(DECISION_SUMMARY)).toLowerCase(Locale.ROOT);

        for (String stale : List.of(
                "not tagged or published by this repository state",
                "not tagged or published in this release-prep pr",
                "prepare the future `v2.5.0` jar/docs-first release decision",
                "can proceed only after this `v2.5.0` alignment merges")) {
            assertFalse(docs.contains(stale), "current reviewer docs should not contain stale v2.5.0 claim: " + stale);
        }
    }

    @Test
    void postReleaseDocsDoNotEmbedSecretValuesOrPublishCommands() throws Exception {
        String docs = read(POST_RELEASE).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "git tag",
                "git push",
                "gh release create",
                "gh release upload",
                "docker push",
                "cosign sign",
                "change_me_local_api_key",
                "ghp_",
                "github_pat_",
                "client_secret",
                "-----begin")) {
            assertFalse(docs.contains(forbidden), "post-release docs should not contain " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
