package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class Adr0009LocalLabKitAndSimulatedDatacenterTestHarnessPlanDocumentationTest {
    private static final Path ADR =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PHASE0_INDEX = Path.of("docs/PHASE_0_ARCHITECTURE_ADR_INDEX.md");
    private static final Path ALIGNMENT = Path.of("docs/ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md");
    private static final Path ADR_0001 = Path.of("docs/adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md");
    private static final Path ADR_0002 = Path.of("docs/adr/ADR-0002_LASE_INTEGRATION_MODEL.md");
    private static final Path ADR_0003 = Path.of("docs/adr/ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md");
    private static final Path ADR_0004 = Path.of("docs/adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md");
    private static final Path ADR_0005 = Path.of("docs/adr/ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md");
    private static final Path ADR_0006 =
            Path.of("docs/adr/ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md");
    private static final Path ADR_0007 = Path.of("docs/adr/ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md");
    private static final Path ADR_0008 =
            Path.of("docs/adr/ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md");
    private static final Path LASE_CONTRACT = Path.of("docs/LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md");
    private static final Path WORKLOAD_CONTRACT =
            Path.of("docs/WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md");
    private static final Path EXTERNAL_SIGNAL_CONTRACT = Path.of("docs/EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md");
    private static final Path POM = Path.of("pom.xml");

    @Test
    void adr0009DocumentExistsAndStatesPlanningOnlyBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "# ADR-0009 Local Lab Kit And Simulated Datacenter Test Harness Plan",
                "Proposed / planning-only.",
                "Decision type: architecture planning.",
                "Implementation status: not implemented.",
                "This ADR is planning-only.",
                "This does not implement Docker Compose files, scripts, fake nodes, k6 tests beyond the optional local-lab smoke skeleton, expanded Bruno collections beyond the optional local-lab skeleton, expanded Toxiproxy fault execution beyond the optional local-lab config skeleton, telemetry ingestion, replay, report generation, storage, export behavior, or runtime routing behavior yet.",
                "This ADR does not add Docker Compose implementation.",
                "This ADR does not add scripts implementation.",
                "This ADR does not add fake backend node implementation.",
                "This ADR does not add expanded k6 scenario implementation.",
                "This ADR does not add expanded Bruno collection implementation.",
                "This ADR does not add Prometheus/Grafana implementation.",
                "This ADR does not add Toxiproxy implementation.",
                "This ADR does not change API behavior.",
                "This ADR does not change routing behavior.",
                "This ADR does not add Maven dependency changes.",
                "This ADR does not add CI changes.")) {
            assertTrue(adr.contains(expected), "ADR-0009 should state planning boundary " + expected);
        }
    }

    @Test
    void adr0009DocumentsLocalLabPurposeAndConceptualTopology() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Local Lab Kit Purpose",
                "provide a repeatable local testing environment before real server hardware is purchased",
                "simulate a small datacenter on one Windows machine",
                "support future expansion to LAN server hardware and an Acer AI mini machine",
                "partial degradation",
                "p95/p99 tail latency",
                "overload and queue pressure",
                "error-prone backends",
                "recovery behavior",
                "explainable routing decisions",
                "reviewer/operator evidence",
                "guardrails before autonomy",
                "trusted adaptive routing instead of black-box routing",
                "Planned Conceptual Topology",
                "Fake backend node 1: healthy/fast",
                "Fake backend node 2: slow/tail-latency heavy",
                "Fake backend node 3: partial degradation",
                "Fake backend node 4: error-prone",
                "Fake backend node 5: overloaded/queue-depth simulated",
                "Optional Toxiproxy network degradation layer",
                "Optional k6 traffic generator",
                "Optional Bruno API checks",
                "Optional Prometheus/Grafana observability later")) {
            assertTrue(adr.contains(expected), "ADR-0009 should include local lab/topology item " + expected);
        }
    }

    @Test
    void adr0009DocumentsFutureToolingBoundariesAndScenarioCategories() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Tooling Boundary",
                "Docker Desktop and Docker Compose could run local containerized services",
                "k6 could run smoke, load, stress, spike, and tail-latency tests",
                "Bruno could store local API collection checks",
                "WireMock or lightweight fake services could provide controlled backend behavior",
                "Toxiproxy could provide latency, timeout, bandwidth, reset, and network damage scenarios",
                "Prometheus/Grafana could later visualize local lab metrics",
                "Java/Maven remains the build and local run path",
                "Windows PowerShell should be the preferred local command shell for examples",
                "These are future/planned roles, not production implementation files in this sprint.",
                "Scenario Categories",
                "healthy baseline",
                "slow backend / tail latency",
                "partial degradation",
                "intermittent 500 errors",
                "overload / queue pressure",
                "all-unhealthy or no-good-choice scenario",
                "recovery scenario",
                "strategy comparison scenario",
                "evidence completeness scenario",
                "safety-mode boundary scenario",
                "local hardware expansion scenario",
                "operator-review scenario")) {
            assertTrue(adr.contains(expected), "ADR-0009 should include tooling/scenario item " + expected);
        }
    }

    @Test
    void adr0009DocumentsEvidenceExpectationsAndHardwareExpansionPath() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Evidence Expectations",
                "selected backend",
                "rejected backends",
                "signals used",
                "latency/error/load observations",
                "p95/p99 tail latency observations",
                "queue pressure or overload observations",
                "policy gate status",
                "safety mode",
                "what changed during the scenario",
                "what was proven",
                "what was not proven",
                "why local simulation is not production proof",
                "why evidence completeness matters before autonomy",
                "Hardware Expansion Path",
                "Current Windows PC runs LoadBalancerPro and test tools.",
                "Same PC simulates backends in containers.",
                "Future LAN server can host backend nodes.",
                "Future Acer AI mini machine can support coding, research, local inference, and telemetry experiments.",
                "Additional machines can become degraded or overloaded nodes.",
                "Real hardware tests still do not equal production certification.",
                "Live-cloud and real-tenant validation remain separate future gates.",
                "The Acer AI mini machine is documented only as future local lab/coding/research/local inference/telemetry support, not as production infrastructure.")) {
            assertTrue(adr.contains(expected), "ADR-0009 should include evidence/hardware item " + expected);
        }
    }

    @Test
    void adr0009DocumentsPriorAdrRelationshipsAndNorthStarVision() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Relationship To Prior ADRs",
                "ADR-0001 architecture boundary",
                "ADR-0002 LASE boundary",
                "ADR-0003 external signal/source boundary",
                "ADR-0004 workload realism/scenario modeling",
                "ADR-0005 safety boundaries and guardrails",
                "ADR-0006 evidence packet and replay boundary model",
                "ADR-0007 reviewer evidence and trust model",
                "ADR-0008 runtime enforcement and package boundary plan",
                "Relationship To The North-Star Vision",
                "future datacenter adaptive traffic control",
                "partial degradation instead of simple up/down health",
                "p95/p99 tail latency under pressure",
                "observe-only, recommendation, shadow, and active-experiment modes",
                "structured evidence, signals, policy checks, rejected options, risk boundaries, and what was not proven",
                "guardrails before autonomy",
                "trusted adaptive routing should avoid black-box routing",
                "building a local evidence-driven lab before real hardware expansion")) {
            assertTrue(adr.contains(expected), "ADR-0009 should include relationship/north-star item " + expected);
        }
    }

    @Test
    void adr0009IncludesNonGoalsAndNotProvenBoundaries() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "Safety And Non-Goals",
                "no Docker Compose lab implementation",
                "no scripts implementation",
                "no fake backend node implementation",
                "no k6 scenario files",
                "no expanded Bruno collections beyond the optional local-lab skeleton",
                "no Prometheus/Grafana dashboards",
                "no expanded Toxiproxy fault execution beyond the optional local-lab config skeleton",
                "no runtime LASE enforcement",
                "no package-boundary enforcement",
                "no ArchUnit enforcement",
                "no source-name guard implementation",
                "no routing behavior change",
                "no scoring behavior change",
                "no strategy behavior change",
                "no proxy behavior change",
                "no API behavior change",
                "no reviewer portal implementation",
                "no reviewer dashboard implementation",
                "no reviewer API implementation",
                "no EvidencePacket implementation",
                "no EvidenceAssembler implementation",
                "no replay execution",
                "no evidence generation",
                "no report generation",
                "no storage or persistence",
                "no filesystem-writing implementation",
                "no export, upload, download, PDF, or ZIP behavior",
                "no workload generation",
                "no trace import",
                "no external signal ingestion",
                "no autonomous production traffic shifting",
                "no carbon-aware routing",
                "no GPU orchestration",
                "no power/grid control",
                "no facility automation",
                "Not-Proven Boundaries",
                "local simulation as production proof",
                "Acer AI mini machine use as production infrastructure",
                "ADR-0009 approval beyond proposed/planning-only status")) {
            assertTrue(adr.contains(expected), "ADR-0009 should include non-goal/not-proven item " + expected);
        }
    }

    @Test
    void localLabScenarioMatrixIsPlanningOnlyAndCrossLinked() throws Exception {
        String matrix = read(MATRIX);

        for (String expected : List.of(
                "# Local Lab Scenario Matrix",
                "docs/test-only planning",
                "ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md",
                "It is planning-only.",
                "healthy baseline",
                "slow backend / tail latency",
                "partial degradation",
                "intermittent 500 errors",
                "overload / queue pressure",
                "all-unhealthy or no-good-choice scenario",
                "recovery scenario",
                "strategy comparison scenario",
                "evidence completeness scenario",
                "safety-mode boundary scenario",
                "local hardware expansion scenario",
                "operator-review scenario",
                "selected backend",
                "rejected backends",
                "signals used",
                "policy gate status",
                "safety mode",
                "Local simulation is not production certification",
                "Future Acer AI mini machine can support coding, research, local inference, and telemetry experiments.",
                "not production infrastructure",
                "Non-Goals",
                "Docker Compose lab implementation",
                "fake backend node implementation",
                "k6 scenario files",
                "expanded Bruno collections beyond the optional local-lab skeleton",
                "expanded Toxiproxy fault execution beyond the optional local-lab config skeleton")) {
            assertTrue(matrix.contains(expected), "local lab matrix should include " + expected);
        }

        assertTrue(read(ADR).contains("LOCAL_LAB_SCENARIO_MATRIX.md"), "ADR-0009 should link the scenario matrix");
        assertTrue(read(TRUST_MAP).contains("LOCAL_LAB_SCENARIO_MATRIX.md"),
                "trust map should link the scenario matrix");
    }

    @Test
    void adr0009LinksRelatedDocsAndReviewerEntryPoints() throws Exception {
        String adr = read(ADR);

        for (String expected : List.of(
                "../PHASE_0_ARCHITECTURE_ADR_INDEX.md",
                "../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md",
                "../REVIEWER_TRUST_MAP.md",
                "../ENTERPRISE_READINESS_AUDIT.md",
                "../LOCAL_LAB_SCENARIO_MATRIX.md",
                "ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md",
                "ADR-0002_LASE_INTEGRATION_MODEL.md",
                "ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md",
                "ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md",
                "ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md",
                "ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md",
                "ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md",
                "ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md",
                "../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md",
                "../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md",
                "../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md")) {
            assertTrue(adr.contains(expected), "ADR-0009 should link " + expected);
        }

        for (Path path : List.of(README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001, ADR_0002,
                ADR_0003, ADR_0004, ADR_0005, ADR_0006, ADR_0007, ADR_0008, LASE_CONTRACT,
                WORKLOAD_CONTRACT, EXTERNAL_SIGNAL_CONTRACT)) {
            assertTrue(read(path).contains("ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md"),
                    path + " should link ADR-0009");
        }
    }

    @Test
    void adr0009DocsAvoidImplementationAndProductionOverclaims() throws Exception {
        for (Path path : List.of(ADR, MATRIX, README, TRUST_MAP, AUDIT, PHASE0_INDEX, ALIGNMENT, ADR_0001,
                ADR_0002, ADR_0003, ADR_0004, ADR_0005, ADR_0006, ADR_0007, ADR_0008, LASE_CONTRACT,
                WORKLOAD_CONTRACT, EXTERNAL_SIGNAL_CONTRACT)) {
            String normalized = read(path).toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "adr-0009 is approved",
                    "adr-0009 is accepted",
                    "adr-0009 implements",
                    "adr-0009 enforces",
                    "local lab kit is implemented by adr-0009",
                    "simulated datacenter is implemented by adr-0009",
                    "docker compose lab is implemented",
                    "fake backend nodes are implemented",
                    "k6 scenarios are implemented",
                    "bruno collections are implemented",
                    "toxiproxy configuration is implemented",
                    "prometheus/grafana dashboards are implemented",
                    "runtime enforcement is implemented by adr-0009",
                    "replay execution is implemented by adr-0009",
                    "storage/persistence is implemented by adr-0009",
                    "export/upload/download/pdf/zip is implemented by adr-0009",
                    "acer ai mini machine is production infrastructure",
                    "production-ready now",
                    "production certified",
                    "production certification is proven",
                    "live-cloud validation is complete",
                    "real-tenant validation is complete",
                    "autonomous production traffic shifting is implemented",
                    "carbon-aware routing is implemented",
                    "gpu orchestration is implemented",
                    "power/grid control is implemented",
                    "facility automation is implemented")) {
                assertFalse(normalized.contains(forbidden), path + " must not overclaim " + forbidden);
            }
        }
    }

    @Test
    void sprintDoesNotIntroduceLocalLabImplementationFilesOrBuildDependencies() throws Exception {
        for (Path unexpected : List.of(
                Path.of("docker-compose.local-lab.yml"),
                Path.of("compose.local-lab.yml"),
                Path.of("scripts/local-lab"),
                Path.of("src/test/resources/local-lab/k6"),
                Path.of("docs/bruno"),
                Path.of("bruno"),
                Path.of("k6"),
                Path.of("toxiproxy"))) {
            assertFalse(Files.exists(unexpected), "ADR-0009 sprint should not add implementation path " + unexpected);
        }

        String pom = read(POM).toLowerCase(Locale.ROOT);
        assertFalse(pom.contains("archunit"), "this sprint must not add an ArchUnit dependency or build change");
        assertFalse(pom.contains("wiremock"), "this sprint must not add fake-service dependencies");
        assertFalse(pom.contains("testcontainers"), "this sprint must not add container test dependencies");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
