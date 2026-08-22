package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class DeploymentPackagingContractTest {
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path PROFILE = Path.of("src/main/resources/application-proxy-prod.properties");
    private static final Path COMPOSE = Path.of("deploy/docker-compose.proxy-prod.yml");
    private static final Path FIXTURE_DOCKERFILE = Path.of("deploy/fixture/Dockerfile");
    private static final Path MANIFEST = Path.of("deploy/kubernetes-proxy-prod.yaml");
    private static final Path SMOKE = Path.of("scripts/smoke/proxy-prod-compose-smoke.sh");
    private static final Path IMAGE_SBOM_VALIDATOR = Path.of("scripts/smoke/validate-container-image-sbom.sh");
    private static final Path CI = Path.of(".github/workflows/ci.yml");

    @Test
    void proxyProdIsExplicitAndUsesRequiredExternalUpstreams() throws Exception {
        String defaults = read(DEFAULT_PROPERTIES);
        String profile = read(PROFILE);

        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));
        for (String expected : List.of(
                "loadbalancerpro.auth.mode=api-key",
                "loadbalancerpro.auth.protect-actuator=true",
                "loadbalancerpro.proxy.enabled=true",
                "loadbalancerpro.proxy.health-check.enabled=${LBP_HEALTH_CHECK_ENABLED:true}",
                "loadbalancerpro.proxy.cooldown.enabled=${LBP_COOLDOWN_ENABLED:true}",
                "loadbalancerpro.proxy.limits.max-in-flight=${LBP_MAX_IN_FLIGHT:100}",
                "loadbalancerpro.proxy.max-request-bytes=${LBP_MAX_REQUEST_BYTES:65536}",
                "loadbalancerpro.proxy.upstreams[0].url=${LBP_UPSTREAM_0_URL}",
                "loadbalancerpro.proxy.upstreams[1].url=${LBP_UPSTREAM_1_URL}",
                "management.prometheus.metrics.export.enabled=true")) {
            assertTrue(profile.contains(expected), "missing proxy-prod property: " + expected);
        }
        assertFalse(profile.contains("tls.verify=false"));
    }

    @Test
    void composeKeepsSecretsAndTlsExternalAndHardensEveryContainer() throws Exception {
        String compose = read(COMPOSE);

        assertEquals(1, yamlDocumentCount(compose));

        for (String expected : List.of(
                "127.0.0.1:${LBP_PROXY_PROD_PORT:-18443}:8080",
                "SPRING_PROFILES_ACTIVE: prod,proxy-prod",
                "SPRING_CONFIG_IMPORT: configtree:/run/secrets/",
                "LBP_UPSTREAM_0_URL: http://backend-a:8080",
                "LBP_UPSTREAM_1_URL: http://backend-b:8080",
                "LBP_PROXY_STRATEGY: ${LBP_PROXY_STRATEGY:-ROUND_ROBIN}",
                "target: loadbalancerpro.api.key",
                "target: /run/tls",
                "target: /run/trust",
                "target: /run/identity",
                "read_only: true",
                "X-API-Key: $$(cat /run/secrets/loadbalancerpro.api.key)",
                "no-new-privileges:true",
                "stop_signal: SIGTERM",
                "stop_grace_period: 35s")) {
            assertTrue(compose.contains(expected), "missing Compose boundary: " + expected);
        }
        assertTrue(count(compose, "cap_drop:") >= 3);
        assertTrue(count(compose, "- ALL") >= 3);
        assertFalse(compose.contains("BEGIN PRIVATE KEY"));
        assertFalse(compose.contains("tls.verify=false"));
        assertFalse(compose.contains("LOADBALANCERPRO_API_KEY:"));
    }

    @Test
    void fixtureUsesPinnedJavaImagesAndRunsNonRoot() throws Exception {
        String dockerfile = read(FIXTURE_DOCKERFILE);

        assertTrue(count(dockerfile, "@sha256:") == 2);
        assertTrue(dockerfile.contains("javac --release 17"));
        assertTrue(dockerfile.contains("USER fixture:fixture"));
        assertTrue(dockerfile.contains("ENTRYPOINT [\"java\""));
        assertFalse(dockerfile.contains("curl"));
        assertFalse(dockerfile.contains("ADD http"));
    }

    @Test
    void kubernetesBaseHasTheCanonicalLifecycleAndSecurityBoundary() throws Exception {
        String manifest = read(MANIFEST);

        assertEquals(5, yamlDocumentCount(manifest));

        for (String expected : List.of(
                "replicas: 2",
                "type: RollingUpdate",
                "maxUnavailable: 0",
                "maxSurge: 1",
                "minReadySeconds: 10",
                "progressDeadlineSeconds: 600",
                "registry.invalid/loadbalancerpro@sha256:",
                "imagePullPolicy: IfNotPresent",
                "kind: PodDisruptionBudget",
                "minAvailable: 1",
                "topologySpreadConstraints:",
                "minDomains: 2",
                "topologyKey: topology.kubernetes.io/zone",
                "whenUnsatisfiable: DoNotSchedule",
                "whenUnsatisfiable: ScheduleAnyway",
                "automountServiceAccountToken: false",
                "enableServiceLinks: false",
                "runAsNonRoot: true",
                "runAsUser: 10001",
                "readOnlyRootFilesystem: true",
                "allowPrivilegeEscalation: false",
                "drop: [\"ALL\"]",
                "terminationGracePeriodSeconds: 45",
                "sleep 10",
                "startupProbe:",
                "https://${LBP_TLS_HOSTNAME}:8080/actuator/health",
                "X-API-Key: $(cat /run/secrets/loadbalancerpro.api.key)",
                "secretName: loadbalancerpro-api-key",
                "secretName: loadbalancerpro-server-tls")) {
            assertTrue(manifest.contains(expected), "missing manifest boundary: " + expected);
        }
        assertFalse(manifest.contains("tls.verify=false"));
    }

    @Test
    void ciExecutesTheRuntimeSmokeAndUnsuppressedImageScans() throws Exception {
        String smoke = read(SMOKE);
        String imageSbomValidator = read(IMAGE_SBOM_VALIDATOR);
        String ci = read(CI);
        assertEquals(1, yamlDocumentCount(ci));

        for (String expected : List.of(
                "docker image save",
                "docker kill --signal=TERM",
                "effectiveHealthyBackendCount",
                "$base_url/actuator/health",
                "actuator/prometheus",
                "basicConstraints=critical,CA:TRUE",
                "extendedKeyUsage=serverAuth",
                "ReadonlyRootfs",
                "CapDrop")) {
            assertTrue(smoke.contains(expected), "missing smoke proof: " + expected);
        }
        assertTrue(ci.contains("Smoke test proxy-prod Compose deployment"));
        assertTrue(ci.contains("Scan proxy-prod fixture image"));
        assertTrue(ci.contains("bash scripts/bench/topology-validator-contract-test.sh"));
        assertTrue(ci.contains("Scan active-active ingress fixture image"));
        assertTrue(ci.contains("Scan immutable rollout candidate image"));
        for (String expected : List.of(
                "Generate image CycloneDX SBOM",
                "Validate image CycloneDX SBOM evidence",
                "format: cyclonedx",
                "image-ref: loadbalancerpro:ci-dry-run-${{ github.event.pull_request.head.sha || github.sha }}",
                "output: target/container-dry-run-evidence/image-sbom.cdx.json",
                "bash -n scripts/smoke/validate-container-image-sbom.sh",
                "bash scripts/smoke/validate-container-image-sbom.sh",
                "image-sbom.cdx.sha256",
                "image-sbom-binding.json")) {
            assertTrue(ci.contains(expected), "missing image SBOM evidence boundary: " + expected);
        }
        for (String expected : List.of(
                ".bomFormat == \"CycloneDX\"",
                "sha256sum --check --strict",
                "Dry-run image identity changed before SBOM binding",
                ".dryRunImageTag == $dryRunImageTag",
                "published:false",
                "signed:false")) {
            assertTrue(imageSbomValidator.contains(expected),
                    "missing image SBOM validator boundary: " + expected);
        }
        assertFalse(imageSbomValidator.contains("docker push"));
        assertFalse(imageSbomValidator.contains("docker login"));
        assertFalse(imageSbomValidator.contains("cosign"));
        assertTrue(count(ci, "ignore-unfixed: false") >= 4);
        assertFalse(smoke.contains("cp \"$tls_dir/certificate.pem\" \"$tls_dir/ca.pem\""));
        assertFalse(ci.contains("ignore-unfixed: true"));
        assertFalse(ci.contains("trivyignores:"));
        assertFalse(ci.contains("\n          docker push "));
        assertFalse(ci.contains("\n          docker login "));
        assertFalse(ci.contains("\n          cosign "));
    }

    private static long count(String content, String token) {
        return content.lines().filter(line -> line.contains(token)).count();
    }

    private static long yamlDocumentCount(String content) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        return StreamSupport.stream(yaml.loadAll(content).spliterator(), false).count();
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
