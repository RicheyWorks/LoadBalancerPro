package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.core.CloudManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@SpringBootTest
@AutoConfigureMockMvc
class RoutingDecisionDemoTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path ROUTING_DEMO_PAGE = Path.of("src/main/resources/static/routing-demo.html");
    private static final Path COMPARE_FIXTURE =
            Path.of("src/test/resources/routing-demo/compare-strategies-sample.json");
    private static final Path LEAST_CONNECTIONS_FIXTURE =
            Path.of("src/test/resources/routing-demo/least-connections-sample.json");
    private static final List<Path> ROUTING_FIXTURES = List.of(
            COMPARE_FIXTURE,
            Path.of("src/test/resources/routing-demo/round-robin-sample.json"),
            Path.of("src/test/resources/routing-demo/weighted-sample.json"),
            LEAST_CONNECTIONS_FIXTURE,
            Path.of("src/test/resources/routing-demo/tail-latency-sample.json"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void routingDemoPageExistsAndIsServed() throws Exception {
        assertTrue(Files.exists(ROUTING_DEMO_PAGE), "routing cockpit page should be source-controlled");

        mockMvc.perform(get("/routing-demo.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Enterprise Lab Routing Proof Cockpit")))
                .andExpect(content().string(containsString("data-action=\"compare\"")))
                .andExpect(content().string(containsString("/api/routing/compare")));
    }

    @Test
    void routingDemoPageContainsExpectedEndpointsControlsAndStrategyText() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("/api/health"));
        assertTrue(page.contains("/actuator/health/readiness"));
        assertTrue(page.contains("/api/routing/compare"));
        assertTrue(page.contains("Strategy comparison"));
        assertTrue(page.contains("Sample request editor/viewer"));
        assertTrue(page.contains("Results table/cards"));
        assertTrue(page.contains("Why this server?"));
        assertTrue(page.contains("Routing Proof Summary"));
        assertTrue(page.contains("Selected strategy"));
        assertTrue(page.contains("Selected backend/server"));
        assertTrue(page.contains("Key input signals"));
        assertTrue(page.contains("Fallback/degradation"));
        assertTrue(page.contains("Local lab boundary"));
        assertTrue(page.contains("What this proves"));
        assertTrue(page.contains("What this does not prove"));
        assertTrue(page.contains("Copy proof summary"));
        assertTrue(page.contains("Copy proof commands"));
        assertTrue(page.contains("proof-summary-output"));
        assertTrue(page.contains("proof-commands"));
        assertTrue(page.contains("Reviewer Workflow Checklist"));
        assertTrue(page.contains("Enterprise Lab Cockpit Monitor"));
        assertTrue(page.contains("Copy lab monitor explanation"));
        assertTrue(page.contains("lab-monitor-output"));
        assertTrue(page.contains("How This Routing Decision Was Made"));
        assertTrue(page.contains("What This Lab Needs to Monitor"));
        assertTrue(page.contains("How to Reproduce and Explain This Lab Proof"));
        assertTrue(page.contains("How to Read Lab Edge Cases"));
        assertTrue(page.contains("Copy end-to-end reviewer walkthrough"));
        assertTrue(page.contains("reviewer-walkthrough-output"));
        assertTrue(page.contains("Load lab scenario"));
        assertTrue(page.contains("Run routing comparison"));
        assertTrue(page.contains("Inspect Routing Proof Summary"));
        assertTrue(page.contains("Compare scenario deltas"));
        assertTrue(page.contains("Follow Evidence Navigation links"));
        assertTrue(page.contains("Copy reviewer proof note"));
        assertTrue(page.contains("Export/print packet from evidence export page"));
        assertTrue(page.contains("Reviewer Confidence Signals"));
        assertTrue(page.contains("Local repeatability"));
        assertTrue(page.contains("Deterministic lab scenarios"));
        assertTrue(page.contains("Same-origin API usage"));
        assertTrue(page.contains("Static browser-only notes/copy actions"));
        assertTrue(page.contains("Not-production-certified boundary"));
        assertTrue(page.contains("Evidence Associations"));
        assertTrue(page.contains("Copy evidence association summary"));
        assertTrue(page.contains("evidence-association-output"));
        assertTrue(page.contains("Selected scenario -> routing decision"));
        assertTrue(page.contains("Routing decision -> selected strategy"));
        assertTrue(page.contains("Routing decision -> selected backend/server"));
        assertTrue(page.contains("Selected backend/server -> key input signals"));
        assertTrue(page.contains("Scenario comparison -> what changed"));
        assertTrue(page.contains("Routing proof summary -> supporting evidence pages"));
        assertTrue(page.contains("Evidence pages -> export packet / reviewer handoff"));
        assertTrue(page.contains("How was the selected backend chosen?"));
        assertTrue(page.contains("How did the selected strategy influence the decision?"));
        assertTrue(page.contains("How did visible input signals influence the proof?"));
        assertTrue(page.contains("How Strategy Choice Matters"));
        assertTrue(page.contains("How Evidence Supports the Decision"));
        assertTrue(page.contains("Association Legend"));
        assertTrue(page.contains("scenario = controlled lab input state"));
        assertTrue(page.contains("strategy = selected routing method"));
        assertTrue(page.contains("backend/server = selected local candidate from the lab response"));
        assertTrue(page.contains("signals = controlled lab metrics used for explanation"));
        assertTrue(page.contains("evidence pages = reviewer navigation aids, not production certification"));
        assertTrue(page.contains("Scenario Comparison"));
        assertTrue(page.contains("Previous scenario"));
        assertTrue(page.contains("Current scenario"));
        assertTrue(page.contains("Selected strategy change"));
        assertTrue(page.contains("Selected backend/server change"));
        assertTrue(page.contains("Key input signal changes"));
        assertTrue(page.contains("Degradation/recovery explanation"));
        assertTrue(page.contains("Copy scenario comparison"));
        assertTrue(page.contains("scenario-comparison-output"));
        assertTrue(page.contains("Evidence Navigation"));
        assertTrue(page.contains("Routing evidence navigation links"));
        assertTrue(page.contains("/load-balancing-cockpit.html"));
        assertTrue(page.contains("/enterprise-lab-reviewer.html"));
        assertTrue(page.contains("/operator-evidence-dashboard.html"));
        assertTrue(page.contains("/evidence-timeline.html"));
        assertTrue(page.contains("/evidence-export-packet.html"));
        assertTrue(page.contains("Reviewer path"));
        assertTrue(page.contains("Where to go next after controlled lab routing proof review"));
        assertTrue(page.contains("Next: Enterprise Lab reviewer dashboard"));
        assertTrue(page.contains("Next: Operator evidence dashboard"));
        assertTrue(page.contains("Next: Evidence timeline"));
        assertTrue(page.contains("Finish: Evidence export packet"));
        assertTrue(page.contains("after controlled lab routing proof review"));
        assertTrue(page.contains("Copy curl"));
        assertTrue(page.contains("Copy payload"));
        assertTrue(page.contains("Copy summary"));
        assertTrue(page.contains("Copy raw response"));
        assertTrue(page.contains("Reset lab"));
        assertTrue(page.contains("Load lab scenario"));
        assertTrue(page.contains("Compare strategies"));
        assertTrue(page.contains("TAIL_LATENCY_POWER_OF_TWO"));
        assertTrue(page.contains("WEIGHTED_LEAST_LOAD"));
        assertTrue(page.contains("WEIGHTED_LEAST_CONNECTIONS"));
        assertTrue(page.contains("WEIGHTED_ROUND_ROBIN"));
        assertTrue(page.contains("ROUND_ROBIN"));
        assertFalse(page.contains("RESPONSE_TIME"));
    }

    @Test
    void routingDemoProofSummaryContainsLocalCommandsAndNotProvenBoundaries() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("mvn spring-boot:run"));
        assertTrue(page.contains("curl -fsS http://localhost:8080/api/health"));
        assertTrue(page.contains("curl -fsS http://localhost:8080/actuator/health/readiness"));
        assertTrue(page.contains("curl -fsS -X POST http://localhost:8080/api/routing/compare"));
        assertTrue(page.contains("--data-binary @routing-compare-request.json"));
        assertTrue(page.contains("same-origin local API"));
        assertTrue(page.contains("controlled lab payloads"));
        assertTrue(page.contains("browser-only summaries"));
        assertTrue(page.contains("selectedStrategy: "));
        assertTrue(page.contains("selectedBackend: "));
        assertTrue(page.contains("keyInputSignals: "));
        assertTrue(page.contains("fallbackDegradationBoundary: "));
        assertTrue(page.contains("localLabBoundary: same-origin local API"));
        assertTrue(normalized.contains("no production deployment proof"));
        assertTrue(normalized.contains("no service-level agreement, service-level objective, or real tenant evidence"));
        assertTrue(normalized.contains("no live cloud validation, registry publication, or container signing evidence"));
        assertTrue(normalized.contains("no service-level agreement or service-level objective evidence"));
        assertTrue(normalized.contains("no registry publication or container signing evidence"));
        assertTrue(normalized.contains("no production telemetry proof"));
        assertTrue(normalized.contains("runtimeReportWritten: false".toLowerCase(Locale.ROOT)));
        assertTrue(normalized.contains("externalServices: false".toLowerCase(Locale.ROOT)));
    }

    @Test
    void routingDemoScenarioComparisonContainsLocalOnlyBoundariesAndCopyableDeltas() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("Packaged normal-load baseline"));
        assertTrue(page.contains("Current request editor payload"));
        assertTrue(page.contains("previousScenario: "));
        assertTrue(page.contains("currentScenario: "));
        assertTrue(page.contains("selectedStrategyChange: "));
        assertTrue(page.contains("selectedBackendChange: "));
        assertTrue(page.contains("keyInputSignalChanges: "));
        assertTrue(page.contains("degradationRecoveryExplanation: "));
        assertTrue(page.contains("emptyOrUnavailableFallback: "));
        assertTrue(page.contains("localLabBoundary: browser-local comparison"));
        assertTrue(page.contains("No primary strategy change"));
        assertTrue(page.contains("Same lab scenario run twice or unchanged from baseline"));
        assertTrue(page.contains("No previous lab scenario has been captured yet"));
        assertTrue(page.contains("The selected backend is unchanged, but visible lab signals changed"));
        assertTrue(page.contains("Strategy changed but backend stayed the same"));
        assertTrue(page.contains("Backend changed but strategy stayed the same"));
        assertTrue(page.contains("Selected backend changed after compare"));
        assertTrue(page.contains("candidates "));
        assertTrue(page.contains("healthy "));
        assertTrue(page.contains("unhealthy "));
        assertTrue(page.contains("inFlight "));
        assertTrue(page.contains("maxP95LatencyMs "));
        assertTrue(page.contains("Degradation pressure increased"));
        assertTrue(page.contains("Recovery visible"));
        assertTrue(page.contains("same-origin local API results only"));
        assertTrue(normalized.contains("browser-local comparison of controlled lab payloads"));
        assertTrue(normalized.contains("no production traffic"));
        assertTrue(normalized.contains("production telemetry"));
        assertTrue(normalized.contains("no live cloud validation"));
        assertTrue(normalized.contains("no real tenant data"));
        assertTrue(normalized.contains("no upload, share endpoint, or server-side export/pdf/zip generation"));
    }

    @Test
    void routingDemoEvidenceNavigationIsLocalStaticAndBounded() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"routing-evidence-navigation-panel\""));
        assertTrue(page.contains("Where to go next after controlled lab routing proof review across local static evidence pages."));
        assertTrue(page.contains("Use Routing Proof Summary and Scenario Comparison"));
        assertTrue(page.contains("Review Enterprise Lab posture"));
        assertTrue(page.contains("Check operator evidence locations"));
        assertTrue(page.contains("Compare local and CI evidence stages"));
        assertTrue(page.contains("Use the browser-local packet page"));
        assertTrue(page.contains("Reviewer path after controlled lab routing proof review"));
        assertTrue(page.contains("inspect the Enterprise Lab reviewer dashboard"));
        assertTrue(page.contains("check the Operator evidence dashboard"));
        assertTrue(page.contains("review the Evidence timeline"));
        assertTrue(page.contains("Evidence export packet page"));
        assertTrue(normalized.contains("controlled lab evidence only"));
        assertTrue(normalized.contains("no production traffic proof"));
        assertTrue(normalized.contains("no production telemetry proof"));
        assertTrue(normalized.contains("no live cloud proof"));
        assertTrue(normalized.contains("no real tenant proof"));
        assertTrue(normalized.contains("registry publication proof"));
        assertTrue(normalized.contains("container signing proof"));
        assertFalse(page.contains("href=\"http"));
        assertFalse(page.contains("href=\"//"));
        assertFalse(normalized.contains("navigator.sendbeacon"));
        assertFalse(normalized.contains("fetch(\"https://"));
        assertFalse(normalized.contains("fetch('https://"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("server-side packet"));
    }

    @Test
    void routingDemoReviewerWorkflowChecklistAndWalkthroughAreLocalAndBounded() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"reviewer-workflow-checklist-panel\""));
        assertTrue(page.contains("aria-label=\"Reviewer workflow checklist cards\""));
        assertTrue(page.contains("data-copy-target=\"reviewer-walkthrough-output\""));
        assertTrue(page.contains("# End-to-End Routing Reviewer Walkthrough"));
        assertTrue(page.contains("workflowChecklist:"));
        assertTrue(page.contains("1. load controlled lab scenario"));
        assertTrue(page.contains("2. run routing comparison"));
        assertTrue(page.contains("3. inspect Routing Proof Summary"));
        assertTrue(page.contains("4. compare scenario deltas"));
        assertTrue(page.contains("5. follow Evidence Navigation links"));
        assertTrue(page.contains("6. copy reviewer proof note"));
        assertTrue(page.contains("7. export/print packet from /evidence-export-packet.html"));
        assertTrue(page.contains("routingProofSummary:"));
        assertTrue(page.contains("scenarioComparison:"));
        assertTrue(page.contains("evidencePath: /routing-demo.html -> /enterprise-lab-reviewer.html -> /operator-evidence-dashboard.html -> /evidence-timeline.html -> /evidence-export-packet.html"));
        assertTrue(page.contains("copyBoundary: browser-local copy action only; no upload/share endpoint; no server-side export/PDF/ZIP generation"));
        assertTrue(page.contains("notProven:"));
        assertTrue(normalized.contains("browser-local copy actions read visible page text only"));
        assertTrue(normalized.contains("no upload/share endpoint"));
        assertTrue(normalized.contains("no server-side export/pdf/zip generation"));
        assertTrue(normalized.contains("no external services"));
        assertTrue(normalized.contains("no production traffic proof"));
        assertTrue(normalized.contains("no production telemetry proof"));
        assertTrue(normalized.contains("no live cloud proof"));
        assertTrue(normalized.contains("no real tenant proof"));
        assertTrue(normalized.contains("no registry publication proof"));
        assertTrue(normalized.contains("no container signing proof"));
        assertTrue(normalized.contains("no service-level agreement or service-level objective evidence"));
    }

    @Test
    void routingDemoReviewerConfidenceSignalsAreStaticLocalAndNotOverclaimed() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"reviewer-confidence-signals-panel\""));
        assertTrue(page.contains("aria-label=\"Reviewer confidence signal cards\""));
        assertTrue(page.contains("Local repeatability"));
        assertTrue(page.contains("Deterministic lab scenarios"));
        assertTrue(page.contains("Same-origin API usage"));
        assertTrue(page.contains("Static browser-only notes/copy actions"));
        assertTrue(page.contains("Not-production-certified boundary"));
        assertTrue(normalized.contains("packaged controlled lab inputs"));
        assertTrue(normalized.contains("packaged server names, weights, health states, load, and latency fields"));
        assertTrue(normalized.contains("browser calls target local app paths"));
        assertTrue(normalized.contains("do not create server-side files"));
        assertTrue(normalized.contains("controlled lab evidence only"));
        assertTrue(normalized.contains("no production traffic proof"));
        assertTrue(normalized.contains("no production telemetry proof"));
        assertTrue(normalized.contains("no live cloud proof"));
        assertTrue(normalized.contains("no real tenant proof"));
        assertTrue(normalized.contains("no registry publication proof"));
        assertTrue(normalized.contains("no container signing proof"));
        assertFalse(normalized.contains("enterprise production ready"));
        assertFalse(normalized.contains("registry published"));
        assertFalse(normalized.contains("signed container"));
        assertTrue(normalized.contains("no governance-applied proof"));
    }

    @Test
    void routingDemoEnterpriseLabMonitorExplainsSignalsBoundariesAndCopyText() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"enterprise-lab-cockpit-monitor-panel\""));
        assertTrue(page.contains("<title>LoadBalancerPro Enterprise Lab Routing Cockpit</title>"));
        assertTrue(page.contains("Enterprise Lab Cockpit Monitor"));
        assertTrue(page.contains("This is lab-bounded proof, not production monitoring."));
        assertTrue(page.contains("Enterprise Lab Routing Cockpit - controlled pre-production routing validation, not a demo."));
        assertTrue(page.contains("It is an Enterprise Lab proof cockpit, not a production monitoring surface."));
        assertTrue(page.contains("data-copy-target=\"lab-monitor-output\""));
        assertTrue(page.contains("# Enterprise Lab Cockpit Monitor Explanation"));
        assertTrue(page.contains("activeLabScenario: "));
        assertTrue(page.contains("routingComparisonStatus: "));
        assertTrue(page.contains("selectedStrategy: "));
        assertTrue(page.contains("selectedBackend: "));
        assertTrue(page.contains("backendHealthState: "));
        assertTrue(page.contains("visibleInputSignals: "));
        assertTrue(page.contains("scenarioComparisonState: "));
        assertTrue(page.contains("edgeCaseState: "));
        assertTrue(page.contains("evidenceAssociationPath: "));
        assertTrue(page.contains("reproductionSteps:"));
        assertTrue(page.contains("labProofBoundaries: controlled lab evidence"));
        assertTrue(page.contains("productionNotProven: no production traffic proof; no production telemetry proof; no live cloud proof; no real tenant proof; no SLA/SLO proof; no registry publication proof; no container signing proof; no production certification claim"));
        assertTrue(page.contains("copyBoundary: browser-local copy action only; no upload/share endpoint; no server-side export/PDF/ZIP generation; no external calls; no telemetry"));
        assertTrue(page.contains("Evidence links remain available before a lab run, but they do not certify production behavior."));
        assertTrue(page.contains("No previous lab scenario has been captured yet."));
        assertTrue(page.contains("The local lab API response is unavailable; static reviewer guidance remains available."));
        assertTrue(page.contains("Copy failed; use the visible lab explanation as the fallback."));
        assertTrue(normalized.contains("backend health state"));
        assertTrue(normalized.contains("visible latency/load/connection/capacity signals"));
        assertTrue(normalized.contains("scenario delta state"));
        assertTrue(normalized.contains("degradation/fallback/recovery state"));
        assertTrue(normalized.contains("reviewer handoff readiness"));
        assertFalse(normalized.contains("production monitoring proof"));
        assertFalse(normalized.contains("live production telemetry is available"));
        assertFalse(normalized.contains("server-side lab monitor export"));
    }

    @Test
    void routingDemoMonitoringDepthExplainsWhatTheCockpitMonitors() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("What the Enterprise Lab Cockpit Monitors"));
        assertTrue(page.contains("aria-label=\"Monitored proof chain explanation cards\""));
        assertTrue(page.contains("Active controlled lab scenario"));
        assertTrue(page.contains("Shows which local lab input state is under review"));
        assertTrue(page.contains("Routing comparison request state"));
        assertTrue(page.contains("not run, completed, empty, or unavailable"));
        assertTrue(page.contains("Selected strategy"));
        assertTrue(page.contains("connect strategy behavior to backend selection"));
        assertTrue(page.contains("Selected backend/server"));
        assertTrue(page.contains("compare it with alternatives and visible signals"));
        assertTrue(page.contains("Candidate backend health states"));
        assertTrue(page.contains("degraded inputs are visible"));
        assertTrue(page.contains("Visible latency signal"));
        assertTrue(page.contains("whether latency pressure helped explain the selected candidate"));
        assertTrue(page.contains("Visible load/connection pressure signal"));
        assertTrue(page.contains("Capacity/weight signal if exposed"));
        assertTrue(page.contains("exact production scoring is not claimed"));
        assertTrue(page.contains("Degradation/fallback/recovery state"));
        assertTrue(page.contains("Scenario-to-scenario delta"));
        assertTrue(page.contains("Evidence association path"));
        assertTrue(page.contains("Reviewer handoff readiness"));
        assertTrue(normalized.contains("production proof gaps"));
    }

    @Test
    void routingDemoMonitorStatusCardsAndSignalGuideStayLabBounded() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("Monitor Status Cards"));
        assertTrue(page.contains("aria-label=\"Monitor status cards\""));
        assertTrue(page.contains("id=\"monitor-status-scenario\""));
        assertTrue(page.contains("id=\"monitor-status-strategy\""));
        assertTrue(page.contains("id=\"monitor-status-backend\""));
        assertTrue(page.contains("id=\"monitor-status-health\""));
        assertTrue(page.contains("id=\"monitor-status-pressure\""));
        assertTrue(page.contains("id=\"monitor-status-delta\""));
        assertTrue(page.contains("id=\"monitor-status-handoff\""));
        assertTrue(page.contains("not production telemetry and do not monitor production behavior"));
        assertTrue(page.contains("Signal Interpretation Guide"));
        assertTrue(page.contains("id=\"signal-interpretation-guide-panel\""));
        assertTrue(page.contains("visible lab signals derived from the local comparison response"));
        assertTrue(page.contains("Exact production scoring is not claimed unless exposed by the API."));
        assertTrue(page.contains("Healthy vs unhealthy backend state"));
        assertTrue(page.contains("Lower or higher latency"));
        assertTrue(page.contains("Load or active connection pressure"));
        assertTrue(page.contains("Capacity/weight assumptions where exposed"));
        assertTrue(page.contains("Unchanged backend with changed signals"));
        assertTrue(page.contains("Backend changes under the same strategy"));
        assertTrue(page.contains("Strategy changes but backend remains the same"));
        assertTrue(page.contains("All backends degraded or unhealthy"));
        assertTrue(page.contains("Missing or unavailable local API data"));
        assertFalse(normalized.contains("live production telemetry is available"));
        assertFalse(normalized.contains("production monitoring proof"));
    }

    @Test
    void routingDemoDecisionChainTraceAndCopyTextAreBrowserLocal() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"decision-chain-trace-panel\""));
        assertTrue(page.contains("Decision Chain Trace"));
        assertTrue(page.contains("data-copy-target=\"monitored-decision-chain-output\""));
        assertTrue(page.contains("Copy monitored decision chain"));
        assertTrue(page.contains("controlled lab scenario -> visible input signals -> selected strategy -> selected backend ->"));
        assertTrue(page.contains("id=\"decision-chain-scenario\""));
        assertTrue(page.contains("id=\"decision-chain-signals\""));
        assertTrue(page.contains("id=\"decision-chain-strategy\""));
        assertTrue(page.contains("id=\"decision-chain-backend\""));
        assertTrue(page.contains("id=\"decision-chain-delta\""));
        assertTrue(page.contains("id=\"decision-chain-evidence\""));
        assertTrue(page.contains("id=\"decision-chain-handoff\""));
        assertTrue(page.contains("id=\"decision-chain-not-proven\""));
        assertTrue(page.contains("# Monitored Decision Chain"));
        assertTrue(page.contains("activeLabScenario: "));
        assertTrue(page.contains("runStatus: "));
        assertTrue(page.contains("visibleSignalInterpretation: "));
        assertTrue(page.contains("degradationFallbackRecoveryState: "));
        assertTrue(page.contains("scenarioDelta: "));
        assertTrue(page.contains("decisionChainTrace: controlled lab scenario -> visible input signals -> selected strategy -> selected backend -> comparison delta -> evidence association -> reviewer handoff"));
        assertTrue(page.contains("reviewerHandoffPath: /evidence-export-packet.html browser-local copy/download/print handoff"));
        assertTrue(page.contains("copyBoundary: browser-local copy action only; no upload/share endpoint; no server-side export/PDF/ZIP generation; no external calls; no telemetry"));
        assertTrue(normalized.contains("no production traffic proof"));
        assertTrue(normalized.contains("no production telemetry proof"));
        assertTrue(normalized.contains("no live-cloud proof"));
        assertTrue(normalized.contains("no real-tenant proof"));
        assertTrue(normalized.contains("no registry publication proof"));
        assertTrue(normalized.contains("no container signing proof"));
        assertTrue(normalized.contains("no governance-applied proof"));
        assertTrue(normalized.contains("no upload/share endpoint"));
        assertTrue(normalized.contains("no server-side export/pdf/zip generation"));
    }

    @Test
    void routingDemoDecisionTraceDepthExplainsSelectedBackendAndAlternatives() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"backend-selection-depth-panel\""));
        assertTrue(page.contains("Why This Backend Was Selected"));
        assertTrue(page.contains("Copy decision trace explanation"));
        assertTrue(page.contains("data-copy-target=\"decision-trace-explanation-output\""));
        assertTrue(page.contains("Reviewer-facing interpretation from visible lab signal data only."));
        assertTrue(page.contains("controlled lab response"));
        assertTrue(page.contains("exact production scoring is not claimed unless exposed by the API"));
        assertTrue(page.contains("id=\"decision-trace-selected-backend\""));
        assertTrue(page.contains("id=\"decision-trace-selected-strategy\""));
        assertTrue(page.contains("id=\"decision-trace-health-signal\""));
        assertTrue(page.contains("id=\"decision-trace-latency-signal\""));
        assertTrue(page.contains("id=\"decision-trace-pressure-signal\""));
        assertTrue(page.contains("id=\"decision-trace-capacity-signal\""));
        assertTrue(page.contains("id=\"decision-trace-reason-summary\""));
        assertTrue(page.contains("Why Other Candidates Were Not Selected"));
        assertTrue(page.contains("Visible signal comparison only; hidden scoring is not invented"));
        assertTrue(page.contains("visible signal comparison suggests"));
        assertTrue(page.contains("alternatives cannot be fully explained from available data"));
        assertTrue(page.contains("selected backend is known but non-selected candidate reasons are not exposed"));
        assertTrue(normalized.contains("all-unhealthy/degraded handling"));
        assertFalse(normalized.contains("exact production scoring is claimed"));
        assertFalse(normalized.contains("hidden production scoring is available"));
        assertFalse(normalized.contains("production monitoring proof"));
    }

    @Test
    void routingDemoKnownUnknownSignalsAndInvestigationPlaybookStayLabBounded() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"known-unknown-signals-panel\""));
        assertTrue(page.contains("Known vs Unknown Signals"));
        assertTrue(page.contains("Known visible lab signals"));
        assertTrue(page.contains("Missing or unavailable signals"));
        assertTrue(page.contains("Exact scoring, hidden production weights, production telemetry, and unavailable local API data are not inferred."));
        assertTrue(page.contains("Local API unavailable state"));
        assertTrue(page.contains("Use controlled lab evidence for pre-production validation."));
        assertTrue(page.contains("id=\"decision-investigation-playbook-panel\""));
        assertTrue(page.contains("Investigation Playbook"));
        assertTrue(page.contains("Backend changed unexpectedly"));
        assertTrue(page.contains("Backend did not change despite signal changes"));
        assertTrue(page.contains("Strategy changed but backend stayed the same"));
        assertTrue(page.contains("Backend changed while strategy stayed the same"));
        assertTrue(page.contains("All candidates unhealthy/degraded"));
        assertTrue(page.contains("Copied handoff text unavailable"));
        assertTrue(page.contains("Evidence pages before or after lab run"));
        assertTrue(page.contains("Signal Interpretation Guide, Decision Chain Trace, Scenario Comparison, Evidence Associations, and Export Packet"));
        assertTrue(normalized.contains("no upload, share endpoint, or server-side export is implied"));
        assertFalse(normalized.contains("production telemetry is available"));
        assertFalse(normalized.contains("production monitoring proof"));
        assertFalse(normalized.contains("server-side decision trace export"));
    }

    @Test
    void routingDemoDecisionTraceCopyAndStretchCardsAreBrowserLocal() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("Decision Trace Cards"));
        assertTrue(page.contains("aria-label=\"Decision trace summary cards\""));
        assertTrue(page.contains("id=\"decision-trace-card-backend\""));
        assertTrue(page.contains("id=\"decision-trace-card-leading-signal\""));
        assertTrue(page.contains("id=\"decision-trace-card-alternative\""));
        assertTrue(page.contains("id=\"decision-trace-card-unknown\""));
        assertTrue(page.contains("id=\"decision-trace-card-next-step\""));
        assertTrue(page.contains("Reviewer Questions Answered"));
        assertTrue(page.contains("Why this backend?"));
        assertTrue(page.contains("Why not the others?"));
        assertTrue(page.contains("What is known?"));
        assertTrue(page.contains("What is unknown?"));
        assertTrue(page.contains("What remains not proven?"));
        assertTrue(page.contains("# Decision Trace Explanation"));
        assertTrue(page.contains("selectedBackend: "));
        assertTrue(page.contains("selectedStrategy: "));
        assertTrue(page.contains("knownVisibleSignals: "));
        assertTrue(page.contains("unknownSignals: "));
        assertTrue(page.contains("whySelectedBackendAppearsFavored: "));
        assertTrue(page.contains("whyAlternativesWereNotSelected: "));
        assertTrue(page.contains("investigationGuidance: "));
        assertTrue(page.contains("evidenceAssociationPath: /routing-demo.html -> /enterprise-lab-reviewer.html -> /operator-evidence-dashboard.html -> /evidence-timeline.html -> /evidence-export-packet.html"));
        assertTrue(page.contains("copyBoundary: browser-local copy action only; no upload/share endpoint; no server-side export/PDF/ZIP generation; no external calls; no telemetry"));
        assertTrue(normalized.contains("no production traffic proof"));
        assertTrue(normalized.contains("no production telemetry proof"));
        assertTrue(normalized.contains("no governance-applied proof"));
        assertTrue(normalized.contains("no production certification claim"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
        assertFalse(normalized.contains("sendbeacon"));
    }

    @Test
    void routingDemoSurprisingDecisionGuideDirectsReviewerInvestigation() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"surprising-decision-investigation-panel\""));
        assertTrue(page.contains("How to Investigate a Surprising Decision"));
        assertTrue(page.contains("Backend changed unexpectedly"));
        assertTrue(page.contains("Backend did not change despite signal changes"));
        assertTrue(page.contains("All candidates unhealthy"));
        assertTrue(page.contains("Local API unavailable"));
        assertTrue(page.contains("Copied handoff text unavailable"));
        assertTrue(page.contains("Reviewer next inspection path"));
        assertTrue(page.contains("Inspect Signal Interpretation Guide, Scenario Comparison, Evidence Associations, Evidence Navigation, and the local export packet"));
        assertTrue(normalized.contains("no server-side sharing or export is implied"));
        assertFalse(normalized.contains("automatic upload"));
        assertFalse(normalized.contains("server-side investigation export"));
    }

    @Test
    void routingDemoEvidenceAssociationsMapProofToEvidencePath() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"evidence-associations-panel\""));
        assertTrue(page.contains("aria-label=\"Evidence association map\""));
        assertTrue(page.contains("data-copy-target=\"evidence-association-output\""));
        assertTrue(page.contains("id=\"association-scenario-decision\""));
        assertTrue(page.contains("id=\"association-strategy\""));
        assertTrue(page.contains("id=\"association-backend\""));
        assertTrue(page.contains("id=\"association-signals\""));
        assertTrue(page.contains("id=\"association-delta\""));
        assertTrue(page.contains("id=\"association-evidence-pages\""));
        assertTrue(page.contains("id=\"association-handoff\""));
        assertTrue(page.contains("Selected scenario -> routing decision"));
        assertTrue(page.contains("Routing decision -> selected strategy"));
        assertTrue(page.contains("Routing decision -> selected backend/server"));
        assertTrue(page.contains("Selected backend/server -> key input signals"));
        assertTrue(page.contains("Scenario comparison -> what changed"));
        assertTrue(page.contains("Routing proof summary -> supporting evidence pages"));
        assertTrue(page.contains("Evidence pages -> export packet / reviewer handoff"));
        assertTrue(page.contains("/enterprise-lab-reviewer.html"));
        assertTrue(page.contains("/operator-evidence-dashboard.html"));
        assertTrue(page.contains("/evidence-timeline.html"));
        assertTrue(page.contains("/evidence-export-packet.html"));
        assertTrue(normalized.contains("association boundary: controlled lab evidence only"));
        assertTrue(normalized.contains("no production traffic proof"));
        assertTrue(normalized.contains("no production telemetry proof"));
        assertTrue(normalized.contains("no live cloud proof"));
        assertTrue(normalized.contains("no real tenant proof"));
        assertTrue(normalized.contains("no registry publication proof"));
        assertTrue(normalized.contains("no container signing proof"));
        assertTrue(normalized.contains("no production certification claim"));
        assertTrue(normalized.contains("no upload/share endpoint"));
        assertTrue(normalized.contains("no server-side export/pdf/zip generation"));
        assertTrue(normalized.contains("no external calls"));
    }

    @Test
    void routingDemoEvidenceAssociationSummaryIsBrowserLocalAndBounded() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("# Cockpit Evidence Association Summary"));
        assertTrue(page.contains("currentScenario: "));
        assertTrue(page.contains("routingDecision: "));
        assertTrue(page.contains("selectedStrategy: "));
        assertTrue(page.contains("selectedBackend: "));
        assertTrue(page.contains("routingReason: "));
        assertTrue(page.contains("keyInputSignals: "));
        assertTrue(page.contains("scenarioComparisonDelta: "));
        assertTrue(page.contains("degradationRecoveryExplanation: "));
        assertTrue(page.contains("evidenceNavigationPath: "));
        assertTrue(page.contains("reviewerUse: "));
        assertTrue(page.contains("associationMap:"));
        assertTrue(page.contains("- selected scenario -> routing decision: "));
        assertTrue(page.contains("- routing decision -> selected strategy: "));
        assertTrue(page.contains("- routing decision -> selected backend/server: "));
        assertTrue(page.contains("- selected backend/server -> key input signals: "));
        assertTrue(page.contains("- scenario comparison -> what changed: "));
        assertTrue(page.contains("- routing proof summary -> supporting evidence pages: /enterprise-lab-reviewer.html, /operator-evidence-dashboard.html, /evidence-timeline.html"));
        assertTrue(page.contains("- evidence pages -> export packet / reviewer handoff: /evidence-export-packet.html"));
        assertTrue(page.contains("copyBoundary: browser-local copy action only; no upload/share endpoint; no server-side export/PDF/ZIP generation; no external calls"));
        assertTrue(page.contains("notProven:"));
        assertTrue(normalized.contains("no production traffic proof"));
        assertTrue(normalized.contains("no production telemetry proof"));
        assertTrue(normalized.contains("no live cloud proof"));
        assertTrue(normalized.contains("no real tenant proof"));
        assertTrue(normalized.contains("no registry publication proof"));
        assertTrue(normalized.contains("no container signing proof"));
        assertTrue(normalized.contains("no production certification claim"));
    }

    @Test
    void routingDemoHowToAndEdgeCasesAreExplicitReviewerGuidance() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"reviewer-how-to-panel\""));
        assertTrue(page.contains("aria-label=\"How this routing decision was made cards\""));
        assertTrue(page.contains("How was the selected backend chosen?"));
        assertTrue(page.contains("The first returned comparison result is the primary controlled lab decision"));
        assertTrue(page.contains("How did the selected strategy influence the decision?"));
        assertTrue(page.contains("exact production scoring is not claimed unless exposed by the API"));
        assertTrue(page.contains("How did visible input signals influence the proof?"));
        assertTrue(page.contains("backend health, latency, load, active connections, capacity, weight, error rate, queue depth, and network-awareness fields"));
        assertTrue(page.contains("If multiple candidates appear close, compare returned reason text and visible signals"));
        assertTrue(page.contains("What This Lab Needs to Monitor"));
        assertTrue(page.contains("Monitor healthy/unhealthy state, selected backend availability"));
        assertTrue(page.contains("Monitor p95 latency, in-flight load, queue depth, configured capacity, estimated concurrency, and weight"));
        assertTrue(page.contains("Unhealthy backends are counted from <code>healthy=false</code>"));
        assertTrue(page.contains("all-unhealthy payloads are treated as a degradation boundary"));
        assertTrue(page.contains("Recovery is represented when visible lab inputs show fewer unhealthy candidates"));
        assertTrue(page.contains("How to Reproduce and Explain This Lab Proof"));
        assertTrue(page.contains("Run routing comparison against the same-origin local <code>/api/routing/compare</code> endpoint"));
        assertTrue(page.contains("Follow evidence association links to reviewer dashboard, operator evidence dashboard, evidence timeline, and evidence export packet"));
        assertTrue(page.contains("How Strategy Choice Matters"));
        assertTrue(page.contains("How Evidence Supports the Decision"));
        assertTrue(page.contains("Timeline and export packet support local reviewer handoff"));
        assertTrue(normalized.contains("does not prove production traffic"));
        assertTrue(normalized.contains("production telemetry"));
        assertTrue(normalized.contains("live cloud behavior"));
        assertTrue(normalized.contains("real tenant behavior"));
        assertTrue(normalized.contains("sla/slo"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("container signing"));
        assertTrue(normalized.contains("production certification"));
    }

    @Test
    void routingDemoEdgeCaseFallbackTextIsSpecificAndBrowserLocal() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("No previous lab scenario has been captured yet; packaged normal-load lab baseline remains the static reference"));
        assertTrue(page.contains("Same lab scenario run twice or unchanged from baseline: strategy order and input signal totals should read as unchanged."));
        assertTrue(page.contains("The selected backend is unchanged, but visible lab signals changed."));
        assertTrue(page.contains("Strategy changed but backend stayed the same: the selected backend can remain preferred under multiple routing methods."));
        assertTrue(page.contains("Backend changed but strategy stayed the same: visible lab signal deltas likely changed which candidate the same strategy preferred."));
        assertTrue(page.contains("All backends unhealthy/degraded: every visible backend is unhealthy, so treat the result as a degradation boundary instead of production proof."));
        assertTrue(page.contains("Empty comparison response: The local lab API returned an empty comparison response; static reviewer guidance remains available."));
        assertTrue(page.contains("Local API unavailable/error: The local lab API response is unavailable; static reviewer guidance remains available."));
        assertTrue(page.contains("Clipboard copy failure: Copy failed; use the visible lab explanation as the fallback."));
        assertTrue(page.contains("Page loaded before interaction: static reviewer guidance remains available before a lab run."));
        assertTrue(page.contains("Local lab API returned HTTP "));
        assertTrue(page.contains("The local lab API returned an empty comparison response; static reviewer guidance remains available."));
        assertTrue(page.contains("The local lab API response is unavailable; static reviewer guidance remains available."));
        assertTrue(page.contains("Malformed lab request JSON; static reviewer guidance remains available."));
        assertTrue(page.contains("Static page loaded without prior interaction; static reviewer guidance and evidence links remain available before running a lab scenario."));
        assertTrue(page.contains("Browser clipboard permission may be unavailable."));
        assertTrue(page.contains("Evidence links remain available before a lab run, but they do not certify production behavior."));
        assertTrue(normalized.contains("browser-local copy"));
        assertTrue(normalized.contains("no upload/share endpoint"));
        assertTrue(normalized.contains("no server-side export/pdf/zip generation"));
        assertFalse(normalized.contains("server-side association export"));
        assertFalse(normalized.contains("automatic upload"));
    }

    @Test
    void routingDemoAssociationLegendExplainsProofTermsWithoutCertificationClaim() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"association-legend-panel\""));
        assertTrue(page.contains("aria-label=\"Association legend cards\""));
        assertTrue(page.contains("scenario = controlled lab input state"));
        assertTrue(page.contains("strategy = selected routing method"));
        assertTrue(page.contains("backend/server = selected local candidate from the lab response"));
        assertTrue(page.contains("signals = controlled lab metrics used for explanation"));
        assertTrue(page.contains("evidence pages = reviewer navigation aids, not production certification"));
        assertTrue(normalized.contains("synthetic local lab state under review"));
        assertTrue(normalized.contains("method returned by the local compare endpoint"));
        assertTrue(normalized.contains("local candidate in the visible request and response pair"));
        assertTrue(normalized.contains("visible health, load, latency, error, queue, and network-awareness fields"));
        assertTrue(normalized.contains("do not certify production behavior"));
        assertFalse(normalized.contains("production certified"));
        assertFalse(normalized.contains("certified for production"));
    }

    @Test
    void routingDemoPageContainsSafetyLimitationsAndPostmanParity() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("Postman parity: run the <strong>Routing Decision Demo</strong> folder"));
        assertTrue(normalized.contains("local enterprise lab cockpit only"));
        assertTrue(normalized.contains("not certification"));
        assertTrue(normalized.contains("not benchmark proof"));
        assertTrue(normalized.contains("not legal compliance proof"));
        assertTrue(normalized.contains("not identity proof"));
        assertTrue(normalized.contains("no cloud mutation"));
        assertTrue(normalized.contains("no cloudmanager required for routing lab cockpit"));
        assertTrue(normalized.contains("no external services/dependencies"));
        assertTrue(normalized.contains("no external scripts/cdns"));
        assertTrue(normalized.contains("api server required for browser/postman lab review"));
        assertTrue(normalized.contains("no runtime reports or lab transcripts are written"));
    }

    @Test
    void routingDemoPageHasNoExternalScriptsStorageSecretsOrMutableControls() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("<script src="));
        assertFalse(normalized.contains("<link rel=\"stylesheet\""));
        assertFalse(normalized.contains("<img"));
        assertFalse(normalized.contains("background-image"));
        assertFalse(normalized.contains("cdn."));
        assertFalse(normalized.contains("fonts.googleapis"));
        assertFalse(normalized.contains("fonts.gstatic"));
        assertFalse(normalized.contains("localstorage"));
        assertFalse(normalized.contains("sessionstorage"));
        assertFalse(normalized.contains("date.now"));
        assertFalse(normalized.contains("new date"));
        assertFalse(normalized.contains("math.random"));
        assertFalse(normalized.contains("randomuuid"));
        assertFalse(normalized.contains("crypto.randomuuid"));
        assertFalse(normalized.contains("x-api-key"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("type=\"password\""));
        assertFalse(normalized.contains("password"));
        assertFalse(normalized.contains("access key"));
        assertFalse(normalized.contains("secret key"));
        assertFalse(normalized.contains("data-action=\"admin"));
        assertFalse(normalized.contains("data-action=\"release"));
        assertFalse(normalized.contains("data-action=\"ruleset"));
        assertFalse(normalized.contains("data-action=\"cloud"));
        assertFalse(normalized.contains("/rulesets"));
        assertFalse(normalized.contains("/repos/"));
        assertFalse(normalized.contains("create release"));
        assertFalse(normalized.contains("create tag"));
        assertFalse(normalized.contains("delete-branch"));
        assertFalse(normalized.contains("new cloudmanager"));
        assertFalse(normalized.contains("construct cloudmanager"));
        assertFalse(normalized.contains("certified operator"));
        assertFalse(normalized.contains("production benchmark"));
        assertFalse(normalized.contains("legal training compliance"));
        assertFalse(normalized.contains("identity verified"));
        assertTrue(countOccurrences(normalized, "cloudmanager") <= 2,
                "CloudManager may appear only in safety limitation text");
    }

    @Test
    void routingDemoFixturesAreValidJsonAndContainSafeSyntheticInputs() throws Exception {
        for (Path fixture : ROUTING_FIXTURES) {
            JsonNode body = readJson(fixture);
            String normalized = Files.readString(fixture, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);

            assertTrue(body.path("strategies").isArray(), fixture + " should list strategies");
            assertTrue(body.path("servers").isArray(), fixture + " should list servers");
            assertTrue(body.path("servers").size() >= 1, fixture + " should include a sample server");
            assertFalse(normalized.contains("http://"));
            assertFalse(normalized.contains("https://"));
            assertFalse(normalized.contains("arn:"));
            assertFalse(normalized.contains("prod-"));
            assertFalse(normalized.contains("password"));
            assertFalse(normalized.contains("secret"));
        }

        JsonNode compare = readJson(COMPARE_FIXTURE);
        assertEquals(5, compare.path("strategies").size());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", compare.at("/strategies/0").asText());
        assertEquals("ROUND_ROBIN", compare.at("/strategies/4").asText());
        assertEquals("edge-alpha", compare.at("/servers/0/serverId").asText());
        assertEquals("edge-drain", compare.at("/servers/2/serverId").asText());
        assertFalse(compare.at("/servers/2/healthy").asBoolean());
    }

    @Test
    void packagedLeastConnectionsFixtureProducesDeterministicRoutingResultWithoutCloudManager() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/routing/compare")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Files.readString(LEAST_CONNECTIONS_FIXTURE, StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestedStrategies[0]", is("WEIGHTED_LEAST_CONNECTIONS")))
                    .andExpect(jsonPath("$.candidateCount", is(2)))
                    .andExpect(jsonPath("$.results[0].strategyId", is("WEIGHTED_LEAST_CONNECTIONS")))
                    .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$.results[0].chosenServerId", is("edge-weighted")))
                    .andExpect(jsonPath("$.results[0].reason", containsString("weighted least-connections")))
                    .andExpect(jsonPath("$.results[0].scores.edge-standard", is(5.0)))
                    .andExpect(jsonPath("$.results[0].scores.edge-weighted", is(3.0)));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "routing demo comparison must not construct CloudManager");
        }
    }

    @Test
    void unsupportedStrategyAndMalformedRoutingRequestsReturnControlledErrors() throws Exception {
        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategies": ["NOT_A_STRATEGY"],
                                  "servers": [
                                    {
                                      "serverId": "edge-alpha",
                                      "healthy": true,
                                      "inFlightRequestCount": 1,
                                      "averageLatencyMillis": 10.0,
                                      "p95LatencyMillis": 20.0,
                                      "p99LatencyMillis": 30.0,
                                      "recentErrorRate": 0.0
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("Unsupported routing strategy")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));

        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategies": ["WEIGHTED_LEAST_LOAD"],
                                  "servers": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));
    }

    @Test
    void routingDecisionDemoPostmanFolderIsValidAndReadOnly() throws Exception {
        JsonNode collection = readJson(Path.of("postman/LoadBalancerPro.postman_collection.json"));
        JsonNode folder = findFolder(collection, "Routing Decision Demo");
        assertNotNull(folder, "Postman collection should include a Routing Decision Demo folder");
        assertEquals(6, folder.path("item").size());

        List<String> expectedNames = List.of(
                "GET Routing Demo Health Check",
                "GET Routing Demo Readiness Check",
                "POST Compare All Supported Strategies",
                "POST Weighted Strategy Sample",
                "POST Least Connections Sample",
                "POST Tail Latency Sample");
        for (int i = 0; i < expectedNames.size(); i++) {
            assertEquals(expectedNames.get(i), folder.at("/item/" + i + "/name").asText());
        }

        assertEquals("{{baseUrl}}/api/routing/compare", folder.at("/item/2/request/url/raw").asText());
        assertTrue(folder.at("/item/2/request/body/raw").asText().contains("TAIL_LATENCY_POWER_OF_TWO"));
        assertTrue(folder.at("/item/2/request/body/raw").asText().contains("WEIGHTED_ROUND_ROBIN"));
        assertTrue(folder.at("/item/2/request/body/raw").asText().contains("ROUND_ROBIN"));

        String normalized = Files.readString(Path.of("postman/LoadBalancerPro.postman_collection.json"),
                StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertFalse(normalized.contains("x-api-key"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("/rulesets"));
        assertFalse(normalized.contains("create release"));
        assertFalse(normalized.contains("create tag"));
        assertFalse(normalized.contains("delete-branch"));
    }

    private static JsonNode readJson(Path path) throws Exception {
        return OBJECT_MAPPER.readTree(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static JsonNode findFolder(JsonNode node, String folderName) {
        if (folderName.equals(node.path("name").asText()) && node.path("item").isArray()) {
            return node;
        }
        for (JsonNode item : node.path("item")) {
            JsonNode found = findFolder(item, folderName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
