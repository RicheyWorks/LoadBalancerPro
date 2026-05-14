package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnterpriseLabEvidenceExporterTest {

    @Test
    void exporterWritesCatalogRunSummaryAndMetadataUnderTarget() throws Exception {
        EnterpriseLabRunService service = new EnterpriseLabRunService();
        EnterpriseLabRun run = service.run(null, "all", "summary");
        Path output = Path.of("target", "enterprise-lab-exporter-test");

        EnterpriseLabEvidenceExporter.EnterpriseLabEvidenceManifest manifest =
                new EnterpriseLabEvidenceExporter().export(output, service.listScenarioMetadata(), run, "test-sha");

        assertTrue(Files.exists(Path.of(manifest.scenarioCatalogJson())));
        assertTrue(Files.exists(Path.of(manifest.labRunJson())));
        assertTrue(Files.exists(Path.of(manifest.markdownSummary())));
        assertTrue(Files.exists(Path.of(manifest.metadataJson())));
        String summary = Files.readString(Path.of(manifest.markdownSummary()), StandardCharsets.UTF_8);
        assertTrue(summary.contains("Enterprise Adaptive Routing Lab Run"));
        assertTrue(summary.contains("lab evidence only / not production activation"));
        assertTrue(summary.contains("| Guardrail-blocked influence count |"));
    }

    @Test
    void exporterRejectsOutputOutsideTarget(@TempDir Path tempDir) {
        EnterpriseLabRunService service = new EnterpriseLabRunService();
        EnterpriseLabRun run = service.run(null, "all", "summary");

        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabEvidenceExporter().export(tempDir, service.listScenarioMetadata(), run,
                        "test-sha"));
    }
}

