package com.richmond423.loadbalancerpro.api.proxy;

import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.AMBIGUOUS_HOST_REJECTED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.INVALID_REJECTED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.LOOPBACK_ALLOWED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.PRIVATE_NETWORK_ALLOWED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.PUBLIC_NETWORK_REJECTED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.UNSUPPORTED_SCHEME_REJECTED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.USERINFO_REJECTED;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.api.PrivateNetworkEvidenceRedactor;
import org.junit.jupiter.api.Test;

class PrivateNetworkProxyDryRunEvidenceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path APPLICATION_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path EVIDENCE_DIR = Path.of("target", "proxy-evidence");
    private static final Path MARKDOWN_EVIDENCE =
            EVIDENCE_DIR.resolve("private-network-validation-dry-run.md");
    private static final Path JSON_EVIDENCE =
            EVIDENCE_DIR.resolve("private-network-validation-dry-run.json");
    private static final String VALIDATION_FLAG =
            "loadbalancerpro.proxy.private-network-validation.enabled";
    private static final String API_KEY_SENTINEL = "TEST_PRIVATE_NETWORK_DRY_RUN_API_KEY";

    @Test
    void exportsConfigOnlyDryRunEvidenceWithoutTrafficOrSecrets() throws Exception {
        List<Sample> samples = List.of(
                Sample.allowed("loopback-localhost", "http://localhost:18080", LOOPBACK_ALLOWED),
                Sample.allowed("loopback-ipv4", "http://127.0.0.1:18081", LOOPBACK_ALLOWED),
                Sample.allowed("private-10", "http://10.1.2.3:18082", PRIVATE_NETWORK_ALLOWED),
                Sample.allowed("private-172", "http://172.16.1.10:18083", PRIVATE_NETWORK_ALLOWED),
                Sample.allowed("private-192", "http://192.168.1.10:18084", PRIVATE_NETWORK_ALLOWED),
                Sample.allowed("private-ipv6-unique-local", "http://[fd12:3456:789a::1]:18085",
                        PRIVATE_NETWORK_ALLOWED),
                Sample.rejected("public-ipv4", "http://8.8.8.8:18081", PUBLIC_NETWORK_REJECTED),
                Sample.rejected("public-domain", "http://example.com:18081", AMBIGUOUS_HOST_REJECTED),
                Sample.rejected("userinfo-redacted", "http://user:pass@127.0.0.1:18081",
                        USERINFO_REJECTED, "http://<redacted-userinfo>@127.0.0.1:18081"),
                Sample.rejected("unsupported-scheme", "ftp://127.0.0.1:18081",
                        UNSUPPORTED_SCHEME_REJECTED),
                Sample.rejected("ambiguous-ipv4", "http://010.000.000.001:18081",
                        AMBIGUOUS_HOST_REJECTED),
                Sample.rejected("malformed-url", "http://[bad", INVALID_REJECTED));

        List<Result> results = samples.stream().map(this::classify).toList();
        for (Result result : results) {
            assertExpectedClassification(result);
        }

        writeEvidence(results);

        String markdown = Files.readString(MARKDOWN_EVIDENCE, StandardCharsets.UTF_8);
        String json = Files.readString(JSON_EVIDENCE, StandardCharsets.UTF_8);
        String combined = markdown + "\n" + json;
        JsonNode evidence = JSON.readTree(json);

        assertAll(
                () -> assertTrue(markdown.contains("# Private-Network Validation Dry Run")),
                () -> assertTrue(markdown.contains("dryRunOnly=true")),
                () -> assertTrue(markdown.contains("trafficSent=false")),
                () -> assertTrue(markdown.contains("dnsResolution=false")),
                () -> assertTrue(markdown.contains("reachabilityChecks=false")),
                () -> assertTrue(markdown.contains("portScanning=false")),
                () -> assertTrue(markdown.contains("postmanExecution=false")),
                () -> assertTrue(markdown.contains("smokeExecution=false")),
                () -> assertTrue(markdown.contains("failClosedBeforeActiveConfig=true")),
                () -> assertTrue(markdown.contains("target/proxy-evidence/private-network-validation-dry-run.md")),
                () -> assertTrue(markdown.contains("target/proxy-evidence/private-network-validation-dry-run.json")),
                () -> assertTrue(markdown.contains("LOOPBACK_ALLOWED")),
                () -> assertTrue(markdown.contains("PRIVATE_NETWORK_ALLOWED")),
                () -> assertTrue(markdown.contains("PUBLIC_NETWORK_REJECTED")),
                () -> assertTrue(markdown.contains("AMBIGUOUS_HOST_REJECTED")),
                () -> assertTrue(markdown.contains("USERINFO_REJECTED")),
                () -> assertTrue(markdown.contains("UNSUPPORTED_SCHEME_REJECTED")),
                () -> assertTrue(json.contains("\"generatedBy\": \"PrivateNetworkProxyDryRunEvidenceTest\"")),
                () -> assertTrue(json.contains("\"dryRunOnly\": true")),
                () -> assertTrue(json.contains("\"trafficSent\": false")),
                () -> assertTrue(json.contains("\"dnsResolution\": false")),
                () -> assertTrue(json.contains("\"reachabilityChecks\": false")),
                () -> assertTrue(json.contains("\"portScanning\": false")),
                () -> assertTrue(json.contains("\"postmanExecution\": false")),
                () -> assertTrue(json.contains("\"smokeExecution\": false")),
                () -> assertTrue(json.contains("\"apiKeyPersisted\": false")),
                () -> assertTrue(json.contains("\"secretPersisted\": false")),
                () -> assertTrue(json.contains("\"releaseDownloadsMutated\": false")),
                () -> assertTrue(json.contains("\"failClosedBeforeActiveConfig\": true")),
                () -> assertTrue(json.contains("\"status\": \"LOOPBACK_ALLOWED\"")),
                () -> assertTrue(json.contains("\"status\": \"PRIVATE_NETWORK_ALLOWED\"")),
                () -> assertTrue(json.contains("\"status\": \"PUBLIC_NETWORK_REJECTED\"")),
                () -> assertTrue(json.contains("\"status\": \"AMBIGUOUS_HOST_REJECTED\"")),
                () -> assertTrue(json.contains("\"status\": \"USERINFO_REJECTED\"")),
                () -> assertEquals("PrivateNetworkProxyDryRunEvidenceTest",
                        evidence.path("generatedBy").asText()),
                () -> assertEquals("target/proxy-evidence",
                        evidence.path("evidenceOutputScope").asText()),
                () -> assertEquals("target/proxy-evidence/private-network-validation-dry-run.md",
                        evidence.path("markdownEvidence").asText()),
                () -> assertEquals("target/proxy-evidence/private-network-validation-dry-run.json",
                        evidence.path("jsonEvidence").asText()),
                () -> assertEquals(VALIDATION_FLAG + "=true",
                        evidence.path("recipeProperty").asText()),
                () -> assertTrue(evidence.path("dryRunOnly").asBoolean()),
                () -> assertFalse(evidence.path("trafficSent").asBoolean()),
                () -> assertFalse(evidence.path("dnsResolution").asBoolean()),
                () -> assertFalse(evidence.path("reachabilityChecks").asBoolean()),
                () -> assertFalse(evidence.path("portScanning").asBoolean()),
                () -> assertFalse(evidence.path("postmanExecution").asBoolean()),
                () -> assertFalse(evidence.path("smokeExecution").asBoolean()),
                () -> assertFalse(evidence.path("apiKeyPersisted").asBoolean()),
                () -> assertFalse(evidence.path("secretPersisted").asBoolean()),
                () -> assertFalse(evidence.path("releaseDownloadsMutated").asBoolean()),
                () -> assertTrue(evidence.path("failClosedBeforeActiveConfig").asBoolean()),
                () -> assertTrue(evidence.path("samples").isArray()),
                () -> assertEquals(samples.size(), evidence.path("samples").size()),
                () -> assertSamplesHaveExpectedShape(evidence.path("samples")),
                () -> assertFalse(combined.contains(API_KEY_SENTINEL)),
                () -> assertFalse(combined.contains("pass@")),
                () -> assertFalse(combined.contains("user:pass")),
                () -> assertFalse(combined.contains("X-API-Key")),
                () -> assertFalse(combined.contains("Authorization")),
                () -> assertFalse(combined.contains("Bearer")),
                () -> assertFalse(combined.contains("release-" + "downloads")));
        PrivateNetworkEvidenceRedactor.assertNoSensitiveEvidence(
                combined,
                API_KEY_SENTINEL,
                "pass@",
                "user:pass",
                "release-" + "downloads");
    }

    @Test
    void privateNetworkValidationPropertyRemainsDefaultFalse() throws Exception {
        String properties = Files.readString(APPLICATION_PROPERTIES, StandardCharsets.UTF_8);

        assertTrue(properties.contains(VALIDATION_FLAG + "=false"),
                VALIDATION_FLAG + " must remain default false");
    }

    private Result classify(Sample sample) {
        ProxyBackendUrlClassifier.Classification classification =
                ProxyBackendUrlClassifier.classify(sample.rawUrl());
        return new Result(sample, classification);
    }

    private static void assertExpectedClassification(Result result) {
        Sample sample = result.sample();
        ProxyBackendUrlClassifier.Classification classification = result.classification();

        assertEquals(sample.expectedStatus(), classification.status(), sample.label());
        assertEquals(sample.allowed(), classification.allowed(), sample.label());
        if (sample.allowed()) {
            assertFalse(classification.normalizedUrl().isBlank(), sample.label());
        } else {
            assertTrue(classification.normalizedUrl().isBlank(), sample.label());
        }
        assertFalse(classification.reason().contains("pass"), sample.label());
    }

    private static void writeEvidence(List<Result> results) throws IOException {
        Files.createDirectories(EVIDENCE_DIR);
        Files.writeString(MARKDOWN_EVIDENCE, markdownEvidence(results), StandardCharsets.UTF_8);
        Files.writeString(JSON_EVIDENCE, jsonEvidence(results), StandardCharsets.UTF_8);
    }

    private static String markdownEvidence(List<Result> results) {
        List<String> lines = new java.util.ArrayList<>(List.of(
                "# Private-Network Validation Dry Run",
                "",
                "- Generated by: `PrivateNetworkProxyDryRunEvidenceTest`",
                "- Recipe property: `" + VALIDATION_FLAG + "=true`",
                "- Evidence Markdown: `target/proxy-evidence/private-network-validation-dry-run.md`",
                "- Evidence JSON: `target/proxy-evidence/private-network-validation-dry-run.json`",
                "- dryRunOnly=true",
                "- trafficSent=false",
                "- dnsResolution=false",
                "- reachabilityChecks=false",
                "- portScanning=false",
                "- postmanExecution=false",
                "- smokeExecution=false",
                "- apiKeyPersisted=false",
                "- secretPersisted=false",
                "- releaseDownloadsMutated=false",
                "- failClosedBeforeActiveConfig=true",
                "",
                "## Classification Samples",
                "",
                "| label | rendered URL | expected status | allowed |",
                "| --- | --- | --- | --- |"));
        for (Result result : results) {
            Sample sample = result.sample();
            lines.add("| `" + sample.label() + "` | `" + sample.renderedUrl() + "` | `"
                    + result.classification().status() + "` | `" + result.classification().allowed() + "` |");
        }
        lines.add("");
        lines.add("Rejected URLs are dry-run validation failures and cannot become active config when the "
                + VALIDATION_FLAG + " gate is enabled.");
        lines.add("");
        return String.join(System.lineSeparator(), lines);
    }

    private static String jsonEvidence(List<Result> results) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                {
                  "generatedBy": "PrivateNetworkProxyDryRunEvidenceTest",
                  "evidenceOutputScope": "target/proxy-evidence",
                  "markdownEvidence": "target/proxy-evidence/private-network-validation-dry-run.md",
                  "jsonEvidence": "target/proxy-evidence/private-network-validation-dry-run.json",
                  "recipeProperty": "loadbalancerpro.proxy.private-network-validation.enabled=true",
                  "dryRunOnly": true,
                  "trafficSent": false,
                  "dnsResolution": false,
                  "reachabilityChecks": false,
                  "portScanning": false,
                  "postmanExecution": false,
                  "smokeExecution": false,
                  "apiKeyPersisted": false,
                  "secretPersisted": false,
                  "releaseDownloadsMutated": false,
                  "failClosedBeforeActiveConfig": true,
                  "samples": [
                """);
        for (int index = 0; index < results.size(); index++) {
            Result result = results.get(index);
            Sample sample = result.sample();
            ProxyBackendUrlClassifier.Classification classification = result.classification();
            builder.append("""
                    {
                      "label": "%s",
                      "renderedUrl": "%s",
                      "status": "%s",
                      "allowed": %s
                    }""".formatted(
                    jsonEscape(sample.label()),
                    jsonEscape(sample.renderedUrl()),
                    classification.status(),
                    classification.allowed()));
            if (index < results.size() - 1) {
                builder.append(',');
            }
            builder.append(System.lineSeparator());
        }
        builder.append("""
                  ]
                }
                """);
        return builder.toString();
    }

    private static void assertSamplesHaveExpectedShape(JsonNode samples) {
        for (JsonNode sample : samples) {
            assertTrue(sample.hasNonNull("label"));
            assertTrue(sample.hasNonNull("renderedUrl"));
            assertTrue(sample.hasNonNull("status"));
            assertTrue(sample.hasNonNull("allowed"));
            assertTrue(sample.path("allowed").isBoolean());
        }
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private record Sample(
            String label,
            String rawUrl,
            String renderedUrl,
            ProxyBackendUrlClassifier.Status expectedStatus,
            boolean allowed) {
        private static Sample allowed(String label, String rawUrl, ProxyBackendUrlClassifier.Status expectedStatus) {
            return new Sample(label, rawUrl, rawUrl, expectedStatus, true);
        }

        private static Sample rejected(String label, String rawUrl, ProxyBackendUrlClassifier.Status expectedStatus) {
            return rejected(label, rawUrl, expectedStatus, rawUrl);
        }

        private static Sample rejected(String label,
                                       String rawUrl,
                                       ProxyBackendUrlClassifier.Status expectedStatus,
                                       String renderedUrl) {
            return new Sample(label, rawUrl, renderedUrl, expectedStatus, false);
        }
    }

    private record Result(
            Sample sample,
            ProxyBackendUrlClassifier.Classification classification) {
    }
}
