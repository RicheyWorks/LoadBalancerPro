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
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ContainerRegistrySigningRolloutDocumentationTest {
    private static final Path ROLLOUT = Path.of("docs/CONTAINER_REGISTRY_SIGNING_ROLLOUT.md");
    private static final Path DECISION_SUMMARY = Path.of("docs/RELEASE_READINESS_DECISION_SUMMARY.md");
    private static final Path SIGNING_RECORD = Path.of("docs/CONTAINER_SIGNING_DECISION_RECORD.md");
    private static final Path CONTAINER_GUIDE = Path.of("docs/CONTAINER_DEPLOYMENT.md");
    private static final Path HARDENING = Path.of("docs/DEPLOYMENT_HARDENING_GUIDE.md");
    private static final Path SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path README = Path.of("README.md");
    private static final Path RELEASE_INTENT = Path.of("docs/RELEASE_INTENT_REVIEW.md");
    private static final Pattern EXECUTABLE_CONTAINER_PUBLISH =
            Pattern.compile("(?im)^\\s*(docker\\s+push|cosign\\s+sign|oras\\s+push|helm\\s+push)\\b");

    @Test
    void rolloutAndDecisionSummaryExistAndAreLinked() throws Exception {
        assertTrue(Files.exists(ROLLOUT), "container registry/signing rollout doc should exist");
        assertTrue(Files.exists(DECISION_SUMMARY), "release readiness decision summary should exist");

        for (Path doc : List.of(SIGNING_RECORD, CONTAINER_GUIDE, HARDENING, SUMMARY, TRUST_MAP, README)) {
            assertTrue(read(doc).contains("CONTAINER_REGISTRY_SIGNING_ROLLOUT.md"),
                    doc + " should link the container rollout plan");
        }
        for (Path doc : List.of(SUMMARY, TRUST_MAP, README, RELEASE_INTENT, ROLLOUT)) {
            assertTrue(read(doc).contains("RELEASE_READINESS_DECISION_SUMMARY.md"),
                    doc + " should link the final release readiness decision summary");
        }
    }

    @Test
    void rolloutDocumentsFutureGatedNoPublicationStatus() throws Exception {
        String rollout = read(ROLLOUT);

        for (String expected : List.of(
                "no registry push",
                "no container signing",
                "no registry attestation",
                "no image publication",
                "no workflow mutation",
                "No container publication or container signing was performed in this sprint",
                "no-registry/no-container-signing")) {
            assertTrue(rollout.contains(expected), "rollout should document " + expected);
        }
    }

    @Test
    void rolloutCoversRegistryTagDigestSigningScanRollbackAndCredentials() throws Exception {
        String rollout = read(ROLLOUT);

        for (String expected : List.of(
                "Registry Choices",
                "Image Name Policy",
                "Tag And Immutable Digest Policy",
                "Dockerfile base images remain pinned by digest",
                "Build Provenance",
                "SBOM",
                "Trivy",
                "vulnerability scan",
                "Signing And Attestation Options",
                "GitHub artifact attestations",
                "Credential Handling Model",
                "Promotion Environments",
                "Manual Approvals",
                "Rollback And Retention Policy",
                "public/private visibility policy",
                "Automation Required Before First Publication")) {
            assertTrue(rollout.contains(expected), "rollout should cover " + expected);
        }
    }

    @Test
    void decisionSummaryStatesJarDocsFirstChoiceAndContainerCost() throws Exception {
        String decision = read(DECISION_SUMMARY);

        for (String expected : List.of(
                "Recommended exact release version: `v2.5.0`",
                "JAR/docs-first is sufficient",
                "Container distribution adds a deployable image",
                "registry ownership",
                "credential handling",
                "signing identity governance",
                "not Production-Certified",
                "RELEASE_INTENT_REVIEW.md",
                "CONTAINER_REGISTRY_SIGNING_ROLLOUT.md")) {
            assertTrue(decision.contains(expected), "decision summary should document " + expected);
        }
    }

    @Test
    void workflowsAndScriptsDoNotContainExecutableContainerPublicationCommands() throws Exception {
        for (Path root : List.of(Path.of(".github"), Path.of("scripts"))) {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    String content = read(path);
                    assertFalse(EXECUTABLE_CONTAINER_PUBLISH.matcher(content).find(),
                            path + " should not execute container publication or signing commands");
                }
            }
        }
    }

    @Test
    void rolloutDocsDoNotEmbedSecretsOrRegistryCredentials() throws Exception {
        String docs = (read(ROLLOUT) + "\n" + read(DECISION_SUMMARY)).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "change_me_local_api_key",
                "ghp_",
                "github_pat_",
                "client_secret",
                "registry_password",
                "-----begin")) {
            assertFalse(docs.contains(forbidden), "rollout docs should not contain " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
