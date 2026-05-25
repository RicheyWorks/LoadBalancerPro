package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class AgentEvidenceAuditRuntimeConfigurationAuditDocumentationTest {
    private static final Path AUDIT = Path.of("docs/agent/EVIDENCE_AUDIT_RUNTIME_CONFIGURATION_AUDIT.md");
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path PROD_PROPERTIES = Path.of("src/main/resources/application-prod.properties");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path EVIDENCE_MAP = Path.of("docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md");
    private static final Path BOARD = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentEvidenceAuditRuntimeConfigurationAuditDocumentationTest.java");

    @Test
    void auditExistsAndNamesSlotNineScope() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot 9",
                "runtime configuration audit",
                "documentation/test-only",
                "src/main/resources/application.properties",
                "src/main/resources/application-prod.properties",
                "codex/evidence-audit-runtime-config",
                "0fc6a5431f400eb4e5f71a70805b3fcb317f1c69",
                "without changing runtime resources",
                "does not edit any runtime configuration file",
                "does not start the app",
                "does not call endpoints")) {
            assertTrue(audit.contains(expected), "Missing slot 9 audit scope: " + expected);
        }
    }

    @Test
    void auditCoversRequiredRuntimeConfigurationSurfaces() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "actuator exposure includes `health,info,metrics,prometheus`",
                "actuator exposure is limited to `health,info`",
                "prometheus metrics export is enabled",
                "prometheus metrics export is disabled",
                "otlp metrics export is disabled",
                "otlp metrics export remains opt-in",
                "api auth mode and api key default behavior question",
                "protected requests fail closed with http 401",
                "cors boundary",
                "http://localhost:3000",
                "http://localhost:8080",
                "prod cors origins",
                "loadbalancerpro.api.rate-limit.enabled=false",
                "loadbalancerpro.lase.shadow.enabled=false",
                "loadbalancerpro.proxy.enabled=false",
                "loadbalancerpro.proxy.private-network-validation.enabled=false",
                "loadbalancerpro.proxy.private-network-live-validation.enabled=false",
                "loadbalancerpro.proxy.private-network-live-validation.operator-approved=false",
                "cloud.livemode=false",
                "prod profile narrowing",
                "reviewer questions",
                "remaining limits")) {
            assertTrue(audit.contains(expected), "Missing runtime configuration audit wording: " + expected);
        }
    }

    @Test
    void sourceControlledRuntimePropertiesMatchAuditedDefaults() throws IOException {
        String local = read(DEFAULT_PROPERTIES).toLowerCase(Locale.ROOT);
        String prod = read(PROD_PROPERTIES).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "management.endpoints.web.exposure.include=health,info,metrics,prometheus",
                "management.prometheus.metrics.export.enabled=true",
                "management.otlp.metrics.export.enabled=false",
                "management.otlp.metrics.export.url=${otel_exporter_otlp_metrics_endpoint:}",
                "management.endpoint.health.probes.enabled=true",
                "management.endpoint.health.show-details=when_authorized",
                "management.metrics.tags.environment=local",
                "loadbalancerpro.auth.mode=api-key",
                "loadbalancerpro.api.cors.allowed-origins=http://localhost:3000,http://localhost:8080",
                "loadbalancerpro.api.rate-limit.enabled=false",
                "loadbalancerpro.lase.shadow.enabled=false",
                "loadbalancerpro.proxy.enabled=false",
                "loadbalancerpro.proxy.private-network-validation.enabled=false",
                "loadbalancerpro.proxy.private-network-live-validation.enabled=false",
                "loadbalancerpro.proxy.private-network-live-validation.operator-approved=false",
                "loadbalancerpro.proxy.health-check.enabled=false",
                "loadbalancerpro.proxy.retry.enabled=false",
                "loadbalancerpro.proxy.cooldown.enabled=false")) {
            assertTrue(local.contains(expected), "Missing local runtime property: " + expected);
        }

        for (String expected : List.of(
                "management.endpoints.web.exposure.include=health,info",
                "management.prometheus.metrics.export.enabled=false",
                "management.otlp.metrics.export.enabled=${loadbalancerpro_otlp_metrics_enabled:false}",
                "management.otlp.metrics.export.url=${otel_exporter_otlp_metrics_endpoint:}",
                "management.metrics.tags.environment=prod",
                "loadbalancerpro.api.cors.allowed-origins=${loadbalancerpro_cors_allowed_origins:}",
                "loadbalancerpro.api.key=${loadbalancerpro_api_key:}",
                "loadbalancerpro.lase.shadow.enabled=false",
                "cloud.livemode=false",
                "server.forward-headers-strategy=framework")) {
            assertTrue(prod.contains(expected), "Missing prod runtime property: " + expected);
        }

        assertFalse(local.contains("change_me"), "local properties must not include placeholder secrets");
        assertFalse(prod.contains("change_me"), "prod properties must not include placeholder secrets");
    }

    @Test
    void navigationAndCampaignStateReferenceRuntimeConfigurationAudit() throws IOException {
        String readme = read(README).toLowerCase(Locale.ROOT);
        String trustMap = read(TRUST_MAP).toLowerCase(Locale.ROOT);
        String evidenceMap = read(EVIDENCE_MAP).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("docs/agent/evidence_audit_runtime_configuration_audit.md"),
                "README should link to the runtime configuration audit");
        assertTrue(trustMap.contains("agent/evidence_audit_runtime_configuration_audit.md"),
                "Reviewer Trust Map should link to the runtime configuration audit");
        assertTrue(evidenceMap.contains("evidence_audit_runtime_configuration_audit.md"),
                "repository evidence map should link to the runtime configuration audit");

        for (String expected : List.of(
                "slot 9",
                "runtime configuration audit",
                "codex/evidence-audit-runtime-config",
                "slot 8 result",
                "#323",
                "b1a1c578eca4a11b55a60f2213d45bf48cc28838",
                "0fc6a5431f400eb4e5f71a70805b3fcb317f1c69",
                "post-merge main ci and codeql were green",
                "slot 9 branch created")) {
            assertTrue(board.contains(expected) || session.contains(expected),
                    "Missing slot 9 campaign checkpoint: " + expected);
        }
    }

    @Test
    void auditPreservesScopeAndNotProvenBoundaries() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not prove production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "registry publication",
                "container signing",
                "production telemetry",
                "production monitoring",
                "broader automation")) {
            assertTrue(audit.contains(expected), "Missing runtime configuration audit boundary: " + expected);
        }

        for (String forbidden : List.of(
                "proves production readiness",
                "production certification proof",
                "live-cloud proof",
                "real-tenant proof",
                "runtime enforcement proof",
                "benchmark proof",
                "throughput proof")) {
            assertFalse(audit.contains(forbidden), "runtime audit must not overclaim: " + forbidden);
        }
    }

    @Test
    void guardTestOnlyReadsTrackedFiles() throws IOException {
        String source = read(SOURCE);

        for (String forbidden : List.of(
                "Files." + "write",
                "Files." + "create",
                "Files." + "delete",
                "Process" + "Builder",
                "Runtime." + "getRuntime",
                ".ex" + "ec(",
                "Http" + "Client",
                "URL" + "Connection",
                "Socket" + "(")) {
            assertFalse(source.contains(forbidden), "guard test must not use " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
