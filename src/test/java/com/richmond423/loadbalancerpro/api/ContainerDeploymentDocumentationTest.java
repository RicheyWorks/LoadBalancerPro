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

class ContainerDeploymentDocumentationTest {
    private static final Path CONTAINER_GUIDE = Path.of("docs/CONTAINER_DEPLOYMENT.md");
    private static final Path DOCKERFILE = Path.of("Dockerfile");
    private static final Path DOCKERIGNORE = Path.of(".dockerignore");
    private static final Path README = Path.of("README.md");
    private static final Path RUN_PROFILES = Path.of("docs/OPERATOR_RUN_PROFILES.md");
    private static final Path PACKAGING = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path SMOKE_KIT = Path.of("docs/DEPLOYMENT_SMOKE_KIT.md");
    private static final Path HARDENING = Path.of("docs/DEPLOYMENT_HARDENING_GUIDE.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path API_SECURITY =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/config/ApiSecurityConfiguration.java");
    private static final Path CI_WORKFLOW = Path.of(".github/workflows/ci.yml");
    private static final Path RELEASE_WORKFLOW = Path.of(".github/workflows/release-artifacts.yml");
    private static final Pattern RELEASE_COMMAND =
            Pattern.compile("(?im)^\\s*(gh\\s+release|git\\s+tag)\\b");
    private static final Pattern IMAGE_PUBLISHING =
            Pattern.compile("(?im)^\\s*(docker\\s+push|docker\\s+login)\\b|ghcr\\.io|docker\\.io|ecr\\.");
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+" + "CloudManager\\s*\\(|" + "CloudManager\\s*\\(");

    @Test
    void containerGuideExistsAndKeyDocsLinkToIt() throws Exception {
        assertTrue(Files.exists(CONTAINER_GUIDE), "container deployment guide should exist");

        for (Path doc : List.of(README, RUN_PROFILES, PACKAGING, SMOKE_KIT, HARDENING, RUNBOOK, REVIEWER_TRUST_MAP)) {
            assertTrue(read(doc).contains("CONTAINER_DEPLOYMENT.md"),
                    doc + " should link to the container deployment guide");
        }
    }

    @Test
    void dockerfileAndDockerignorePreserveLocalOnlyRuntimeSafety() throws Exception {
        String dockerfile = read(DOCKERFILE);
        String dockerignore = read(DOCKERIGNORE);

        assertTrue(dockerfile.contains("USER loadbalancer:loadbalancer"));
        assertTrue(dockerfile.contains("HEALTHCHECK"));
        assertTrue(dockerfile.contains("http://127.0.0.1:8080/api/health"));
        assertTrue(dockerfile.contains("CMD [\"--server.address=0.0.0.0\"]"));
        assertFalse(dockerfile.contains("LOADBALANCERPRO_API_KEY"));
        assertFalse(dockerfile.contains("CHANGE_ME_LOCAL_API_KEY"));
        assertFalse(dockerfile.contains("loadbalancerpro.proxy.enabled=true"));
        assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(dockerfile).find());
        assertTrue(dockerignore.contains("target"));
        assertTrue(dockerignore.contains("release-downloads/"));
    }

    @Test
    void containerGuideDocumentsLocalRecipesAndBoundaries() throws Exception {
        String guide = read(CONTAINER_GUIDE);

        assertTrue(guide.contains("docker build -t loadbalancerpro:local ."));
        assertTrue(guide.contains("-p 127.0.0.1:8080:8080"));
        assertTrue(guide.contains("curl -fsS http://127.0.0.1:8080/api/health"));
        assertTrue(guide.contains("curl -fsS http://127.0.0.1:8080/"));
        assertTrue(guide.contains("LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY"));
        assertTrue(guide.contains("curl -i http://127.0.0.1:8080/api/proxy/status"));
        assertTrue(guide.contains("curl -i -H \"X-API-Key: CHANGE_ME_LOCAL_API_KEY\""));
        assertTrue(guide.contains("Missing `X-API-Key` returns HTTP 401"));
        assertTrue(guide.contains("Proxy forwarding remains disabled unless `loadbalancerpro.proxy.enabled=true`"));
        assertTrue(guide.contains("host.docker.internal"));
        assertTrue(guide.contains("Do not publish this image to Docker Hub, GHCR, ECR, or any registry"));
        assertTrue(guide.contains("Terminate TLS at a trusted reverse proxy"));
    }

    @Test
    void containerDocsAvoidSecretsInflatedClaimsAndReleaseMutation() throws Exception {
        String guide = read(CONTAINER_GUIDE);
        String normalized = guide.toLowerCase(Locale.ROOT);

        assertFalse(guide.contains("BEGIN PRIVATE KEY"));
        assertFalse(guide.contains("real-secret"));
        assertFalse(guide.contains("AKIA"));
        assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(guide).find());
        assertFalse(RELEASE_COMMAND.matcher(guide).find());
        assertFalse(normalized.contains("production-grade"));
        assertFalse(normalized.contains("certified gateway"));
        assertFalse(normalized.contains("certification proof"));
        assertFalse(normalized.contains("benchmark result"));
        assertFalse(guide.contains("SHA256="));
    }

    @Test
    void workflowsDoNotPublishContainerImagesOrReleaseAssets() throws Exception {
        for (Path workflow : List.of(CI_WORKFLOW, RELEASE_WORKFLOW)) {
            String content = read(workflow);
            assertFalse(IMAGE_PUBLISHING.matcher(content).find(),
                    workflow + " must not publish container images");
        }

        String ciWorkflow = read(CI_WORKFLOW).toLowerCase(Locale.ROOT);
        assertFalse(ciWorkflow.contains("gh release"));
        assertFalse(ciWorkflow.contains("git tag"));
        assertFalse(ciWorkflow.contains("release-downloads"));
    }

    @Test
    void proxyDefaultsAndApiKeyReloadBoundariesRemainVisible() throws Exception {
        String defaults = read(DEFAULT_PROPERTIES);
        String security = read(API_SECURITY);

        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));
        assertTrue(security.contains("HttpMethod.GET, \"/api/proxy/status\""));
        assertTrue(security.contains("HttpMethod.POST, \"/api/proxy/reload\""));
        assertTrue(security.contains("hasRole(allocationRole)"));
    }

    @Test
    void existingProxySecurityReloadAndSmokeTestsRemainPresent() {
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/ProdApiKeyProtectionTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/ReverseProxyReloadSecurityTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/ReverseProxyDisabledTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/DeploymentSmokeKitDocumentationTest.java")));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
