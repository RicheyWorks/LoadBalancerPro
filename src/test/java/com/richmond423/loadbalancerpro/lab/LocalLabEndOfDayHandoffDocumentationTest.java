package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabEndOfDayHandoffDocumentationTest {
    private static final Path HANDOFF = Path.of("docs/LOCAL_LAB_PROGRESS_HANDOFF.md");
    private static final Path NEXT_STEPS = Path.of("docs/LOCAL_LAB_NEXT_STEPS_BOUNDARY.md");
    private static final Path READINESS = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");

    @Test
    void handoffDocsExistAndListCurrentLocalLabStackLayers() throws Exception {
        String handoff = read(HANDOFF);
        String nextSteps = read(NEXT_STEPS);

        for (String expected : List.of(
                "ADR-0009 local lab plan",
                "Local lab scenario matrix",
                "Passive fake backend scenario model/catalog",
                "Passive response fixtures",
                "Passive request/response transcript fixtures",
                "Passive transcript summary renderer",
                "Passive reviewer checklist mapping",
                "Implementation readiness gate",
                "Test-scope in-memory fake backend handler",
                "Handler/transcript consistency tests",
                "Test-scope loopback fake backend server harness",
                "Loopback lifecycle hardening",
                "Test-scope multi-backend loopback harness",
                "Multi-backend transcript alignment tests",
                "Deterministic loopback traffic smoke client",
                "In-memory traffic smoke summary renderer",
                "Traffic smoke reviewer checklist mapping",
                "Deterministic traffic matrix tests",
                "Traffic matrix summary renderer",
                "Traffic matrix reviewer checklist mapping",
                "Local-lab progress handoff docs",
                "Optional local-lab Bruno collection skeleton",
                "Optional local-lab Toxiproxy config skeleton")) {
            assertTrue(handoff.contains(expected), "handoff should list " + expected);
            assertTrue(nextSteps.contains(expected), "next-steps boundary should list " + expected);
        }
    }

    @Test
    void handoffDocsStateLocalTestScopeLoopbackAndEphemeralPortBoundaries() throws Exception {
        String handoff = read(HANDOFF);
        String nextSteps = read(NEXT_STEPS);

        assertTrue(handoff.contains("all current evidence is local/test-scope only"));
        assertTrue(handoff.contains("bind to `127.0.0.1` only"));
        assertTrue(handoff.contains("OS-assigned ephemeral ports"));
        assertTrue(nextSteps.contains("all current evidence is local/test-scope only"));
        assertTrue(nextSteps.contains("only `127.0.0.1` loopback harness URLs"));
        assertTrue(nextSteps.contains("OS-assigned ephemeral ports"));
    }

    @Test
    void handoffDocsIncludeRequiredNotProvenBoundaries() throws Exception {
        String combined = read(HANDOFF) + "\n" + read(NEXT_STEPS);

        for (String expected : List.of(
                "not production readiness",
                "not production certification",
                "not live-cloud validation",
                "not real-tenant validation",
                "not runtime enforcement",
                "not production traffic behavior",
                "not Docker/Bruno/Toxiproxy execution",
                "not automatic k6 execution",
                "not automatic Bruno execution",
                "not automatic Toxiproxy execution",
                "not expanded k6 scenario implementation",
                "not expanded Bruno collection implementation",
                "not expanded Toxiproxy fault execution",
                "not replay execution",
                "not evidence/report generation",
                "not storage/export behavior",
                "not autonomous production traffic shifting",
                "not carbon-aware routing",
                "not GPU orchestration",
                "not power/grid control",
                "not facility automation")) {
            assertTrue(combined.contains(expected), "handoff should preserve boundary " + expected);
        }
    }

    @Test
    void handoffDocsIncludeNextSafeLanesAndStopConditions() throws Exception {
        String combined = read(HANDOFF) + "\n" + read(NEXT_STEPS);

        for (String expected : List.of(
                "Lane A: docs-only k6/Bruno/Toxiproxy implementation plan",
                "Lane A4a: first optional local-lab k6 smoke script skeleton",
                "Lane A4b: future expanded k6 scenario files only after separate review",
                "Lane A5a: first optional local-lab Bruno collection skeleton",
                "Lane A5b: future expanded Bruno collection files only after separate review",
                "Lane A6a: first optional local-lab Toxiproxy config skeleton",
                "Lane A6b: future expanded Toxiproxy fault execution only after separate review",
                "Lane B: test-scope bounded request burst smoke test, still loopback-only",
                "Lane C: test-scope fault-style fixture expansion, no Toxiproxy execution yet",
                "Lane D: docs-only Docker Compose design boundary, no compose file yet",
                "Lane E: future actual Docker/k6/Bruno PR only after a separate boundary plan",
                "stop if a non-loopback host appears",
                "stop if a fixed port appears",
                "stop if `src/main/java` changes are needed",
                "stop if production endpoint wiring appears",
                "stop if Maven dependencies are required",
                "stop if Docker/Toxiproxy platform implementation sneaks in",
                "stop if expanded k6 implementation sneaks in",
                "stop if the optional k6 smoke skeleton becomes CI-gated, Dockerized, or non-loopback by default",
                "stop if the optional Bruno collection skeleton becomes CI-gated, Dockerized, Toxiproxy integration, k6 execution, or non-loopback by default",
                "stop if the optional Toxiproxy config skeleton becomes CI-gated, Dockerized, Docker Compose orchestration, wired into the application, wired into Maven, wired into k6 execution, wired into Bruno execution, starts Toxiproxy, starts the application, binds to `0.0.0.0`, or becomes non-loopback by default",
                "stop if docs start claiming production validation")) {
            assertTrue(combined.contains(expected), "handoff should include lane or stop condition " + expected);
        }
    }

    @Test
    void coreDocsLinkEndOfDayHandoffBoundaryAndStayDocsTestOnly() throws Exception {
        for (Path doc : List.of(READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String text = read(doc);

            assertTrue(text.contains("docs/test-only end-of-day handoff and next-step boundary cleanup only"));
            assertTrue(text.contains("It does not add local-lab harness functionality"));
            assertTrue(text.contains("client functionality"));
            assertTrue(text.contains("server functionality"));
            assertTrue(text.contains("production endpoints"));
            assertTrue(text.contains("production listeners"));
            assertTrue(text.contains("Docker/k6/Bruno/Toxiproxy implementation"));
            assertTrue(text.contains("replay execution"));
            assertTrue(text.contains("evidence/report generation"));
            assertTrue(text.contains("file writing"));
            assertTrue(text.contains("storage"));
            assertTrue(text.contains("export"));
            assertTrue(text.contains("runtime behavior"));
            assertTrue(text.contains("LOCAL_LAB_PROGRESS_HANDOFF.md"));
            assertTrue(text.contains("LOCAL_LAB_NEXT_STEPS_BOUNDARY.md"));
        }
    }

    @Test
    void handoffDocsAvoidValidationToolingReplayReportStorageExportAndAutonomyOverclaims() throws Exception {
        for (Path doc : List.of(HANDOFF, NEXT_STEPS, READINESS, MATRIX, TRUST_MAP, ADR_0009)) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "production-ready",
                    "production certified",
                    "production certification is proven",
                    "production validation is complete",
                    "live-cloud validated",
                    "live-cloud validation is complete",
                    "real-tenant validated",
                    "real-tenant validation is complete",
                    "runtime enforcement is implemented",
                    "docker compose is implemented",
                    "k6 scenario is implemented",
                    "bruno collection is implemented",
                    "toxiproxy config is implemented",
                    "prometheus/grafana dashboard is implemented",
                    "replay execution is implemented",
                    "evidence report is generated",
                    "report generation is implemented",
                    "storage is implemented",
                    "export behavior is implemented",
                    "autonomous production traffic shifting is implemented",
                    "carbon-aware routing is implemented",
                    "gpu orchestration is implemented",
                    "power/grid control is implemented",
                    "facility automation is implemented")) {
                assertFalse(normalized.contains(forbidden), doc + " must not overclaim " + forbidden);
            }
        }
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
