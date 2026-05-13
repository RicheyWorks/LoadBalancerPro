package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ContainerSigningDecisionDocumentationTest {
    private static final Path DECISION = Path.of("docs/CONTAINER_SIGNING_DECISION_RECORD.md");
    private static final Path CONTAINER_GUIDE = Path.of("docs/CONTAINER_DEPLOYMENT.md");
    private static final Path HARDENING = Path.of("docs/DEPLOYMENT_HARDENING_GUIDE.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SUPPLY_CHAIN = Path.of("evidence/SUPPLY_CHAIN_EVIDENCE.md");
    private static final Path PRODUCTION_GATE = Path.of("docs/PRODUCTION_CANDIDATE_EVIDENCE_GATE.md");
    private static final Path README = Path.of("README.md");
    private static final Path DOCKERFILE = Path.of("Dockerfile");
    private static final Path CI_WORKFLOW = Path.of(".github/workflows/ci.yml");
    private static final Path RELEASE_WORKFLOW = Path.of(".github/workflows/release-artifacts.yml");
    private static final Pattern IMAGE_PUBLISHING =
            Pattern.compile("(?im)^\\s*(docker\\s+push|docker\\s+login)\\b|ghcr\\.io|docker\\.io|ecr\\.");

    @Test
    void decisionRecordDocumentsCurrentContainerPosture() throws Exception {
        String decision = read(DECISION);
        String dockerfile = read(DOCKERFILE);

        for (String expected : List.of(
                "multi-stage `Dockerfile`",
                "non-root `loadbalancer` user",
                "HEALTHCHECK",
                "SPRING_PROFILES_ACTIVE=prod",
                "prod API-key profile",
                "LOADBALANCERPRO_API_KEY",
                "pinned by digest",
                "Trivy image scanning")) {
            assertTrue(decision.contains(expected), "decision record should document " + expected);
        }
        assertTrue(dockerfile.contains("ENV SPRING_PROFILES_ACTIVE=prod"));
        assertTrue(dockerfile.contains("USER loadbalancer:loadbalancer"));
        assertTrue(dockerfile.contains("@sha256:"));
    }

    @Test
    void decisionRecordStatesPublicationAndSigningAreDeferred() throws Exception {
        String decision = read(DECISION);

        assertTrue(decision.contains("No container registry publication path exists"));
        assertTrue(decision.contains("No release image naming policy exists"));
        assertTrue(decision.contains("No container signing or cosign publication path exists"));
        assertTrue(decision.contains("No container attestation publication path exists"));
        assertTrue(decision.contains("It does not publish images, push to a registry"));
    }

    @Test
    void decisionGateRequiresFutureRegistrySigningScanRollbackAndRetentionDecisions() throws Exception {
        String decision = read(DECISION);

        for (String expected : List.of(
                "Registry target",
                "Image tag policy",
                "Immutable digest policy",
                "Signing approach",
                "Attestation scope",
                "Vulnerability scan evidence",
                "Rollback policy",
                "Retention policy",
                "Secret handling")) {
            assertTrue(decision.contains(expected), "future gate should require " + expected);
        }
    }

    @Test
    void containerDecisionRecordIsLinkedFromReviewerDocs() throws Exception {
        for (Path path : List.of(CONTAINER_GUIDE, HARDENING, TRUST_MAP, SUPPLY_CHAIN, PRODUCTION_GATE, README)) {
            assertTrue(read(path).contains("CONTAINER_SIGNING_DECISION_RECORD.md"),
                    path + " should link the container signing decision record");
        }
    }

    @Test
    void workflowsStillDoNotPublishContainerImages() throws Exception {
        assertFalse(IMAGE_PUBLISHING.matcher(read(CI_WORKFLOW)).find(),
                "CI workflow must not publish container images");
        assertFalse(IMAGE_PUBLISHING.matcher(read(RELEASE_WORKFLOW)).find(),
                "release workflow must not publish container images");
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
