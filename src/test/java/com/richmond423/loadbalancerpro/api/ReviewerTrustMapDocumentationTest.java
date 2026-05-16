package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ReviewerTrustMapDocumentationTest {
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path README = Path.of("README.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path OPERATOR_PACKAGING = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path INSTALL_MATRIX = Path.of("docs/OPERATOR_INSTALL_RUN_MATRIX.md");
    private static final Path RELEASE_DRY_RUN = Path.of("docs/RELEASE_CANDIDATE_DRY_RUN.md");
    private static final Path REAL_BACKEND_EXAMPLES = Path.of("docs/REAL_BACKEND_PROXY_EXAMPLES.md");
    private static final Path PRIVATE_NETWORK_DRY_RUN = Path.of("docs/PRIVATE_NETWORK_PROXY_DRY_RUN.md");
    private static final Path PRIVATE_NETWORK_LIVE_GATE = Path.of("docs/PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md");
    private static final Path TESTING_COVERAGE = Path.of("docs/TESTING_COVERAGE.md");
    private static final Path V1_9_1_EVIDENCE_PLAN = Path.of("docs/V1_9_1_RELEASE_EVIDENCE_DOCS_PLAN.md");
    private static final Path RELEASE_ARTIFACT_EVIDENCE = Path.of("evidence/RELEASE_ARTIFACT_EVIDENCE.md");
    private static final Path THIS_TEST = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/api/ReviewerTrustMapDocumentationTest.java");

    private static final List<Path> REVIEWER_NAV_DOCS = List.of(
            TRUST_MAP,
            README,
            RUNBOOK,
            OPERATOR_PACKAGING,
            INSTALL_MATRIX,
            RELEASE_DRY_RUN,
            REAL_BACKEND_EXAMPLES,
            PRIVATE_NETWORK_DRY_RUN,
            PRIVATE_NETWORK_LIVE_GATE,
            TESTING_COVERAGE);

    private static final List<Path> RELEASE_FREE_DOCS = List.of(
            TRUST_MAP,
            OPERATOR_PACKAGING,
            INSTALL_MATRIX,
            RELEASE_DRY_RUN,
            REAL_BACKEND_EXAMPLES,
            PRIVATE_NETWORK_DRY_RUN,
            PRIVATE_NETWORK_LIVE_GATE,
            TESTING_COVERAGE);

    private static final Pattern FAKE_HASH =
            Pattern.compile("\\b[0-9a-fA-F]{40}\\b|\\b[0-9a-fA-F]{64}\\b");
    private static final Pattern MARKDOWN_LINK =
            Pattern.compile("\\[[^\\]]+\\]\\(([^)\\s]+\\.md(?:#[^)\\s]+)?)\\)");
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+CloudManager\\s*\\(|CloudManager\\s*\\(");

    @Test
    void reviewerTrustMapExistsAndProvidesTopLevelNavigation() throws Exception {
        String trustMap = read(TRUST_MAP);

        assertTrue(trustMap.contains("# Reviewer Trust Map"));
        assertTrue(trustMap.contains("## Start Here"));
        assertTrue(trustMap.contains("## Enterprise Lab Cockpit, Not a Demo"));
        assertTrue(trustMap.contains("## Reviewer Lab Review Path"));
        assertTrue(trustMap.contains("## Evidence Matrix"));
        assertTrue(trustMap.contains("## Recommended Reviewer Flows"));
        assertTrue(trustMap.contains("## Safety Boundaries"));
        assertTrue(trustMap.contains("## Current Limitations"));
        assertTrue(trustMap.contains("10-Minute Quick Review"));
        assertTrue(trustMap.contains("Proxy-Focused Review"));
        assertTrue(trustMap.contains("Release-Readiness Review"));
        assertTrue(trustMap.contains("Operator Install/Run Review"));
    }

    @Test
    void reviewerTrustMapCoversPrimaryEvidencePaths() throws Exception {
        String trustMap = read(TRUST_MAP);

        for (String expected : List.of(
                "TESTING_COVERAGE.md",
                "REVERSE_PROXY_MODE.md",
                "REVERSE_PROXY_HEALTH_AND_METRICS.md",
                "REVERSE_PROXY_RESILIENCE.md",
                "PROXY_OPERATOR_STATUS_UI.md",
                "LocalOnlyRealBackendProxyValidationTest",
                "LocalProxyEvidenceExportTest",
                "target/proxy-evidence/local-proxy-evidence.md",
                "target/proxy-evidence/local-proxy-evidence.json",
                "PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md",
                "PRIVATE_NETWORK_PROXY_DRY_RUN.md",
                "PrivateNetworkProxyDryRunEvidenceTest",
                "target/proxy-evidence/private-network-validation-dry-run.md",
                "target/proxy-evidence/private-network-validation-dry-run.json",
                "PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md",
                "PrivateNetworkLiveValidationExecutorTest",
                "/api/proxy/status.privateNetworkLiveValidation",
                "POST /api/proxy/private-network-live-validation",
                "target/proxy-evidence/private-network-live-loopback-validation.md",
                "target/proxy-evidence/private-network-live-loopback-validation.json",
                "operator-run-profiles-smoke.ps1",
                "postman-enterprise-lab-safe-smoke.ps1",
                "PROXY_STRATEGY_DEMO_LAB.md",
                "PROXY_DEMO_STACK.md",
                "PROXY_DEMO_FIXTURE_LAUNCHER.md",
                "routing-demo.html",
                "Enterprise Lab Cockpit Monitor",
                "Decision Chain Trace",
                "Why This Backend Was Selected",
                "Why Other Candidates Were Not Selected",
                "Alternative Candidate Evidence",
                "Decision Vector Foundation",
                "ENTERPRISE_LAB_DECISION_VECTOR.md",
                "Candidate Comparison Limits",
                "Candidate Evidence Cards",
                "Candidate Review Questions",
                "copyable alternative candidate evidence",
                "Known vs Unknown Signals",
                "Investigation Playbook",
                "copyable decision trace explanation",
                "How This Routing Decision Was Made",
                "What This Lab Needs to Monitor",
                "How to Reproduce and Explain This Lab Proof",
                "How to Read Lab Edge Cases",
                "copyable lab monitor explanation",
                "Reviewer Workflow Checklist",
                "Evidence Associations",
                "Association Legend",
                "Routing Proof Summary",
                "Scenario Comparison",
                "Reviewer Confidence Signals",
                "Evidence Navigation",
                "REAL_BACKEND_PROXY_EXAMPLES.md",
                "OPERATOR_INSTALL_RUN_MATRIX.md",
                "OPERATOR_PACKAGING.md",
                "LOCAL_ARTIFACT_VERIFICATION.md",
                "CI_ARTIFACT_CONSUMER_GUIDE.md",
                "RELEASE_CANDIDATE_DRY_RUN.md",
                "RELEASE_INTENT_CHECKLIST.md",
                "JAVAFX_OPTIONAL_UI.md",
                "PACKAGE_NAMING.md",
                "jacoco-coverage-report",
                "packaged-artifact-smoke",
                "loadbalancerpro-sbom",
                "zero skipped tests")) {
            assertTrue(trustMap.contains(expected), "trust map should mention " + expected);
        }
    }

    @Test
    void reviewerDemoPathAnswersFastDemoQuestionsAndLocksEvidenceFiles() throws Exception {
        String trustMap = read(TRUST_MAP);
        String readme = read(README);
        String demoPath = section(trustMap, "## Reviewer Lab Review Path", "## Evidence Matrix");
        String normalized = demoPath.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("about five minutes"));
        assertTrue(normalized.contains("what this project is"));
        assertTrue(normalized.contains("enterprise lab cockpit"));
        assertTrue(normalized.contains("operator-focused proxy foundation"));
        assertTrue(normalized.contains("what can be proven quickly"));
        assertTrue(demoPath.contains("mvn spring-boot:run"));
        assertTrue(demoPath.contains("http://localhost:8080/"));
        assertTrue(demoPath.contains("http://localhost:8080/routing-demo.html"));
        assertTrue(demoPath.contains("http://localhost:8080/load-balancing-cockpit.html"));
        assertTrue(demoPath.contains("Enterprise Lab Cockpit Monitor"));
        assertTrue(demoPath.contains("Decision Chain Trace"));
        assertTrue(demoPath.contains("Why This Backend Was Selected"));
        assertTrue(demoPath.contains("Why Other Candidates Were Not Selected"));
        assertTrue(demoPath.contains("Alternative Candidate Evidence"));
        assertTrue(demoPath.contains("Decision Vector Foundation"));
        assertTrue(demoPath.contains("Candidate Comparison Limits"));
        assertTrue(demoPath.contains("Candidate Evidence Cards"));
        assertTrue(demoPath.contains("Candidate Review Questions"));
        assertTrue(demoPath.contains("Known vs Unknown Signals"));
        assertTrue(demoPath.contains("Investigation Playbook"));
        assertTrue(demoPath.contains("How This Routing Decision Was Made"));
        assertTrue(demoPath.contains("What This Lab Needs to Monitor"));
        assertTrue(demoPath.contains("How to Reproduce and Explain This Lab Proof"));
        assertTrue(demoPath.contains("How to Read Lab Edge Cases"));
        assertTrue(demoPath.contains("Reviewer Workflow Checklist"));
        assertTrue(demoPath.contains("Evidence Associations"));
        assertTrue(demoPath.contains("Association Legend"));
        assertTrue(demoPath.contains("Routing Proof Summary"));
        assertTrue(demoPath.contains("Scenario Comparison"));
        assertTrue(demoPath.contains("Reviewer Confidence Signals"));
        assertTrue(demoPath.contains("Evidence Navigation reviewer path"));
        assertTrue(demoPath.contains("selected scenario-to-decision mapping"));
        assertTrue(demoPath.contains("selected strategy/backend"));
        assertTrue(demoPath.contains("selected-vs-non-selected candidate comparison"));
        assertTrue(demoPath.contains("structured decision evidence"));
        assertTrue(demoPath.contains("known versus unknown signal handling"));
        assertTrue(demoPath.contains("scenario-to-scenario what-changed notes"));
        assertTrue(demoPath.contains("visible input signal deltas"));
        assertTrue(demoPath.contains("known versus unknown signals"));
        assertTrue(demoPath.contains("alternative-candidate limits"));
        assertTrue(demoPath.contains("degradation/recovery notes"));
        assertTrue(demoPath.contains("local verification commands"));
        assertTrue(demoPath.contains("routing-to-evidence dashboard links"));
        assertTrue(demoPath.contains("copyable decision trace explanation"));
        assertTrue(demoPath.contains("copyable alternative candidate evidence"));
        assertTrue(demoPath.contains("copyable Decision Vector summary"));
        assertTrue(demoPath.contains("copyable lab monitor explanation"));
        assertTrue(demoPath.contains("copyable evidence association summary"));
        assertTrue(demoPath.contains("copyable end-to-end reviewer walkthrough"));
        assertTrue(demoPath.contains("explicit not-proven boundaries"));
        assertTrue(demoPath.contains("mvn -Dtest=LocalProxyEvidenceExportTest test"));
        assertTrue(demoPath.contains("target/proxy-evidence/local-proxy-evidence.md"));
        assertTrue(demoPath.contains("target/proxy-evidence/local-proxy-evidence.json"));
        assertTrue(demoPath.contains("mvn -Dtest=PrivateNetworkProxyDryRunEvidenceTest test"));
        assertTrue(demoPath.contains("target/proxy-evidence/private-network-validation-dry-run.md"));
        assertTrue(demoPath.contains("target/proxy-evidence/private-network-validation-dry-run.json"));
        assertTrue(demoPath.contains("mvn -Dtest=PrivateNetworkLiveValidationExecutorTest test"));
        assertTrue(demoPath.contains("target/proxy-evidence/private-network-live-loopback-validation.md"));
        assertTrue(demoPath.contains("target/proxy-evidence/private-network-live-loopback-validation.json"));
        assertTrue(demoPath.contains("/api/proxy/status.privateNetworkLiveValidation"));
        assertTrue(demoPath.contains("/api/proxy/private-network-live-validation"));
        assertTrue(demoPath.contains("trafficExecuted=false"));
        assertTrue(demoPath.contains("evidenceWritten=false"));
        assertTrue(demoPath.contains("auditTrail.auditTrailWritten=false"));
        assertTrue(demoPath.contains("target/proxy-evidence/"));
        assertTrue(normalized.contains("command contract"));
        assertTrue(normalized.contains("writes no evidence"));
        assertTrue(normalized.contains("writes no evidence or audit files"));
        assertTrue(demoPath.contains("operator-run-profiles-smoke.ps1 -DryRun"));
        assertTrue(demoPath.contains("postman-enterprise-lab-safe-smoke.ps1 -DryRun"));
        assertTrue(demoPath.contains("CI, CodeQL, Dependency Review"));
        assertTrue(demoPath.contains("jacoco-coverage-report"));
        assertTrue(demoPath.contains("packaged-artifact-smoke"));
        assertTrue(demoPath.contains("loadbalancerpro-sbom"));
        assertTrue(normalized.contains("generated evidence is ignored `target/` output"));
        assertTrue(normalized.contains("loopback/local-only jdk `httpserver` fixtures"));
        assertTrue(normalized.contains("config-validation-only and sends no traffic"));
        assertTrue(normalized.contains("sends one bounded junit-only request"));
        assertTrue(normalized.contains("not startup, postman, smoke, or proxy-routing execution"));
        assertTrue(normalized.contains("private-network live gate status is report-only"));
        assertTrue(normalized.contains("does not call `privatenetworklivevalidationexecutor` or send traffic"));
        assertTrue(normalized.contains("private-network live validation command contract is protected"));
        assertTrue(normalized.contains("returns `trafficexecuted=false`"));
        assertTrue(normalized.contains("rejects unsafe validation paths before transport"));
        assertTrue(normalized.contains("allowlists validation headers"));
        assertTrue(normalized.contains("allowlists response summary headers"));
        assertTrue(normalized.contains("reports redirects without following public targets"));
        assertTrue(normalized.contains("api keys, bearer tokens, credentials, and secrets are redacted or not written"));
        assertTrue(normalized.contains("no dns resolution"));
        assertTrue(normalized.contains("reachability checks"));
        assertTrue(normalized.contains("discovery"));
        assertTrue(normalized.contains("subnet scanning"));
        assertTrue(normalized.contains("port scanning"));
        assertTrue(normalized.contains("no native tooling"));
        assertTrue(normalized.contains("downloaded helper binaries"));
        assertTrue(normalized.contains("service installation"));
        assertTrue(normalized.contains("scheduled tasks"));
        assertTrue(normalized.contains("persistence"));
        assertTrue(normalized.contains("release assets"));
        assertTrue(normalized.contains("release-downloads/"));
        assertTrue(normalized.contains("broader private-lan live traffic execution is not implemented yet"));
        assertTrue(demoPath.contains("PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md"));
        assertTrue(readme.contains("REVIEWER_TRUST_MAP.md#reviewer-lab-review-path"));
    }

    @Test
    void routingDemoScenarioComparisonTrustMapEntryIsDiscoverableAndBounded() throws Exception {
        String trustMap = read(TRUST_MAP);
        String matrix = section(trustMap, "## Evidence Matrix", "## Recommended Reviewer Flows");
        String normalized = matrix.toLowerCase(Locale.ROOT);

        assertTrue(matrix.contains("How should a reviewer walk the routing cockpit end-to-end?"));
        assertTrue(matrix.contains("How do routing decisions connect to reviewer evidence pages?"));
        assertTrue(matrix.contains("Why did this backend win, and why not the alternatives?"));
        assertTrue(matrix.contains("How can reviewers compare selected and non-selected candidates?"));
        assertTrue(matrix.contains("What structured contract supports replayable routing explainability?"));
        assertTrue(matrix.contains("How do routing proof scenarios compare?"));
        assertTrue(matrix.contains("How does the Enterprise Lab cockpit monitor and explain routing proof?"));
        assertTrue(matrix.contains("/routing-demo.html"));
        assertTrue(matrix.contains("RoutingDecisionDemoTest"));
        assertTrue(matrix.contains("Enterprise Lab Cockpit Monitor"));
        assertTrue(matrix.contains("Why This Backend Was Selected"));
        assertTrue(matrix.contains("Why Other Candidates Were Not Selected"));
        assertTrue(matrix.contains("Alternative Candidate Evidence"));
        assertTrue(matrix.contains("Decision Vector Foundation"));
        assertTrue(matrix.contains("Structured Decision Evidence"));
        assertTrue(matrix.contains("EnterpriseLabDecisionVectorDocumentationTest"));
        assertTrue(matrix.contains("Candidate Decision Vector"));
        assertTrue(matrix.contains("Candidate Comparison Limits"));
        assertTrue(matrix.contains("Candidate Evidence Cards"));
        assertTrue(matrix.contains("Candidate Review Questions"));
        assertTrue(matrix.contains("Known vs Unknown Signals"));
        assertTrue(matrix.contains("Investigation Playbook"));
        assertTrue(matrix.contains("Decision Trace Cards"));
        assertTrue(matrix.contains("Reviewer Questions Answered"));
        assertTrue(matrix.contains("copyable alternative candidate evidence"));
        assertTrue(matrix.contains("copyable Decision Vector summary"));
        assertTrue(matrix.contains("copyable decision trace explanation"));
        assertTrue(matrix.contains("How This Routing Decision Was Made"));
        assertTrue(matrix.contains("What This Lab Needs to Monitor"));
        assertTrue(matrix.contains("How to Reproduce and Explain This Lab Proof"));
        assertTrue(matrix.contains("How to Read Lab Edge Cases"));
        assertTrue(matrix.contains("copyable lab monitor explanation"));
        assertTrue(matrix.contains("Reviewer Workflow Checklist"));
        assertTrue(matrix.contains("Evidence Associations"));
        assertTrue(matrix.contains("Association Legend"));
        assertTrue(matrix.contains("Routing Proof Summary"));
        assertTrue(matrix.contains("Scenario Comparison"));
        assertTrue(matrix.contains("Reviewer Confidence Signals"));
        assertTrue(matrix.contains("Evidence Navigation reviewer path"));
        assertTrue(matrix.contains("copyable evidence association summary"));
        assertTrue(matrix.contains("selected scenario-to-decision mapping"));
        assertTrue(matrix.contains("selected strategy/backend mapping"));
        assertTrue(matrix.contains("selected-vs-non-selected candidate language"));
        assertTrue(matrix.contains("selected and non-selected candidate vectors"));
        assertTrue(matrix.contains("factor contribution placeholder"));
        assertTrue(matrix.contains("replay readiness"));
        assertTrue(matrix.contains("what-if readiness"));
        assertTrue(matrix.contains("known candidate signals"));
        assertTrue(matrix.contains("unknown or unexposed candidate signals"));
        assertTrue(matrix.contains("key input signal mapping"));
        assertTrue(matrix.contains("scenario comparison delta"));
        assertTrue(matrix.contains("export packet handoff link"));
        assertTrue(matrix.contains("previous/current scenario names"));
        assertTrue(matrix.contains("selected strategy/backend delta"));
        assertTrue(matrix.contains("input signal deltas"));
        assertTrue(matrix.contains("degradation/recovery notes"));
        assertTrue(matrix.contains("routing-to-evidence dashboard links"));
        assertTrue(matrix.contains("copyable local reviewer summary"));
        assertTrue(matrix.contains("copyable end-to-end reviewer walkthrough"));
        assertTrue(matrix.contains("reviewer/operator/timeline/export packet pages"));
        assertTrue(normalized.contains("controlled lab scenario to routing comparison"));
        assertTrue(normalized.contains("evidence dashboards, timeline, and evidence export packet"));
        assertTrue(normalized.contains("without adding server-side exports or external calls"));
        assertTrue(normalized.contains("packaged normal-load lab baseline"));
        assertTrue(normalized.contains("edited local lab payloads"));
        assertTrue(normalized.contains("same-origin local api responses"));
        assertTrue(normalized.contains("browser-local copy text"));
        assertTrue(normalized.contains("visible known signals from unknown/unexposed signals"));
        assertTrue(normalized.contains("compare visible selected and non-selected candidate signals"));
        assertTrue(normalized.contains("controlled lab contract"));
        assertTrue(normalized.contains("future factor contribution analysis"));
        assertTrue(normalized.contains("decision replay"));
        assertTrue(normalized.contains("what-if experiments"));
        assertTrue(normalized.contains("structured decision logging"));
        assertTrue(normalized.contains("strategy plugin explainability"));
        assertTrue(normalized.contains("data center signal modeling"));
        assertTrue(normalized.contains("without inventing hidden scoring"));
        assertTrue(normalized.contains("alternatives are not explainable"));
        assertTrue(normalized.contains("exact production scoring"));
        assertTrue(normalized.contains("hidden production weights"));
        assertTrue(normalized.contains("production telemetry proof"));
        assertTrue(normalized.contains("governance-applied proof"));
        assertTrue(normalized.contains("upload/share endpoint behavior"));
        assertTrue(normalized.contains("server-side export/pdf/zip generation"));
        assertTrue(normalized.contains("production deployment certification"));
        assertTrue(normalized.contains("live cloud validation"));
        assertTrue(normalized.contains("real tenant traffic proof"));
        assertTrue(normalized.contains("production sla/slo evidence"));
        assertTrue(normalized.contains("server-side export/share artifact creation"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("container signing"));
    }

    @Test
    void enterpriseLabCockpitMonitoringTrustMapSectionIsStrictAndBounded() throws Exception {
        String trustMap = read(TRUST_MAP);
        String section = section(trustMap, "### Enterprise Lab Cockpit Monitoring and How-Question Guide",
                "### Cockpit Evidence Associations");
        String normalized = section.toLowerCase(Locale.ROOT);

        assertTrue(section.contains("strict Enterprise Lab proof cockpit"));
        assertTrue(section.contains("It is not a demo"));
        assertTrue(section.contains("active lab scenario"));
        assertTrue(section.contains("routing comparison request status"));
        assertTrue(section.contains("selected strategy"));
        assertTrue(section.contains("selected backend/server"));
        assertTrue(section.contains("backend health state"));
        assertTrue(section.contains("visible latency/load/active connection/capacity/weight-style signals"));
        assertTrue(section.contains("scenario delta state"));
        assertTrue(section.contains("degradation/fallback/recovery state"));
        assertTrue(section.contains("evidence association path"));
        assertTrue(section.contains("reviewer handoff readiness"));
        assertTrue(section.contains("local lab boundary"));
        assertTrue(section.contains("production proof gaps"));
        assertTrue(section.contains("how the selected backend was chosen"));
        assertTrue(section.contains("how strategy choice influenced the result"));
        assertTrue(section.contains("how visible lab input signals affect the proof"));
        assertTrue(section.contains("how to reproduce and explain the local lab proof"));
        assertTrue(section.contains("copyable lab monitor explanation"));
        assertTrue(normalized.contains("browser-local text only"));
        assertTrue(normalized.contains("same-origin local api responses"));
        assertTrue(normalized.contains("no telemetry"));
        assertTrue(normalized.contains("does not prove production traffic"));
        assertTrue(normalized.contains("production telemetry"));
        assertTrue(normalized.contains("live cloud behavior"));
        assertTrue(normalized.contains("real tenant behavior"));
        assertTrue(normalized.contains("sla/slo achievement"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("container signing"));
        assertTrue(normalized.contains("governance-applied status"));
        assertTrue(normalized.contains("production certification"));
        assertTrue(normalized.contains("does not create upload/share endpoints"));
        assertTrue(normalized.contains("server-side export/pdf/zip files"));
    }

    @Test
    void routingCockpitEvidenceAssociationsTrustMapSectionIsDiscoverableAndBounded() throws Exception {
        String trustMap = read(TRUST_MAP);
        String section = section(trustMap, "### Cockpit Evidence Associations", "### Cockpit Decision Trace Depth");
        String normalized = section.toLowerCase(Locale.ROOT);

        assertTrue(section.contains("Cockpit Evidence Associations"));
        assertTrue(section.contains("/routing-demo.html"));
        assertTrue(section.contains("Evidence Associations panel"));
        assertTrue(section.contains("selected scenario -> routing decision"));
        assertTrue(section.contains("selected strategy/backend"));
        assertTrue(section.contains("key input signals"));
        assertTrue(section.contains("scenario comparison delta"));
        assertTrue(section.contains("supporting evidence pages"));
        assertTrue(section.contains("export packet/reviewer handoff"));
        assertTrue(section.contains("copyable evidence association summary"));
        assertTrue(section.contains("Association Legend"));
        assertTrue(section.contains("scenario = controlled lab input state"));
        assertTrue(section.contains("strategy = selected routing method"));
        assertTrue(section.contains("backend/server = selected local candidate from the lab response"));
        assertTrue(section.contains("signals = controlled lab metrics used for explanation"));
        assertTrue(section.contains("evidence pages = reviewer navigation aids, not production certification"));
        assertTrue(normalized.contains("browser-local text only"));
        assertTrue(normalized.contains("same-origin local api results"));
        assertTrue(normalized.contains("static evidence page links"));
        assertTrue(normalized.contains("does not prove production traffic"));
        assertTrue(normalized.contains("production telemetry"));
        assertTrue(normalized.contains("live cloud behavior"));
        assertTrue(normalized.contains("real tenant behavior"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("container signing"));
        assertTrue(normalized.contains("production certification"));
    }

    @Test
    void routingCockpitDecisionTraceDepthTrustMapSectionIsDiscoverableAndBounded() throws Exception {
        String trustMap = read(TRUST_MAP);
        String section = section(trustMap, "### Cockpit Decision Trace Depth", "### Alternative Candidate Evidence");
        String normalized = section.toLowerCase(Locale.ROOT);

        assertTrue(section.contains("Cockpit Decision Trace Depth"));
        assertTrue(section.contains("/routing-demo.html"));
        assertTrue(section.contains("why the selected backend appears favored"));
        assertTrue(section.contains("why alternatives may not be explainable"));
        assertTrue(section.contains("which visible lab signals are known"));
        assertTrue(section.contains("which signals are unknown or unavailable"));
        assertTrue(section.contains("surprising controlled lab outcomes"));
        assertTrue(section.contains("copyable decision trace explanation"));
        assertTrue(normalized.contains("browser-local text only"));
        assertTrue(normalized.contains("does not expose hidden scoring"));
        assertTrue(normalized.contains("production telemetry"));
        assertTrue(normalized.contains("production monitoring"));
        assertTrue(normalized.contains("production certification"));
        assertTrue(normalized.contains("upload/share endpoints"));
        assertTrue(normalized.contains("server-side export behavior"));
    }

    @Test
    void routingCockpitAlternativeCandidateEvidenceTrustMapSectionIsDiscoverableAndBounded() throws Exception {
        String trustMap = read(TRUST_MAP);
        String section = section(trustMap, "### Alternative Candidate Evidence", "### Decision Vector Contract");
        String normalized = section.toLowerCase(Locale.ROOT);

        assertTrue(section.contains("Alternative Candidate Evidence"));
        assertTrue(section.contains("/routing-demo.html"));
        assertTrue(section.contains("selected backend/server"));
        assertTrue(section.contains("visible non-selected candidates"));
        assertTrue(section.contains("selected-vs-non-selected candidate names"));
        assertTrue(section.contains("visible health state"));
        assertTrue(section.contains("latency signal"));
        assertTrue(section.contains("load/connection pressure"));
        assertTrue(section.contains("capacity/weight signal"));
        assertTrue(section.contains("Known from lab response labels"));
        assertTrue(section.contains("Not exposed/unknown labels"));
        assertTrue(section.contains("Why Not This Candidate guidance"));
        assertTrue(section.contains("Candidate Comparison Limits"));
        assertTrue(section.contains("Candidate Evidence Cards"));
        assertTrue(section.contains("Candidate Review Questions"));
        assertTrue(section.contains("copyable alternative candidate evidence"));
        assertTrue(normalized.contains("browser-local text only"));
        assertTrue(normalized.contains("same-origin local api interpretation"));
        assertTrue(normalized.contains("must not invent hidden scoring"));
        assertTrue(normalized.contains("infer hidden routing internals"));
        assertTrue(normalized.contains("unknown candidate signals as investigation items"));
        assertTrue(normalized.contains("exact production scoring"));
        assertTrue(normalized.contains("production telemetry"));
        assertTrue(normalized.contains("production monitoring"));
        assertTrue(normalized.contains("production certification"));
        assertTrue(normalized.contains("live-cloud behavior"));
        assertTrue(normalized.contains("real tenant behavior"));
        assertTrue(normalized.contains("upload/share endpoints"));
        assertTrue(normalized.contains("server-side export/pdf/zip generation"));
    }

    @Test
    void routingCockpitDecisionVectorTrustMapSectionIsDiscoverableAndBounded() throws Exception {
        String trustMap = read(TRUST_MAP);
        String section = section(trustMap, "### Decision Vector Contract", "## Reviewer Lab Review Path");
        String normalized = section.toLowerCase(Locale.ROOT);

        assertTrue(section.contains("Decision Vector Contract"));
        assertTrue(section.contains("ENTERPRISE_LAB_DECISION_VECTOR.md"));
        assertTrue(section.contains("Decision Vector Foundation"));
        assertTrue(section.contains("/routing-demo.html"));
        assertTrue(section.contains("structured contract behind the cockpit explanation"));
        assertTrue(section.contains("selected strategy"));
        assertTrue(section.contains("selected backend/server"));
        assertTrue(section.contains("candidate backend list"));
        assertTrue(section.contains("selected candidate vector"));
        assertTrue(section.contains("non-selected candidate vectors"));
        assertTrue(section.contains("known signals"));
        assertTrue(section.contains("unknown or unexposed signals"));
        assertTrue(section.contains("exact scoring availability or absence"));
        assertTrue(section.contains("factor contribution availability or absence"));
        assertTrue(section.contains("replay readiness"));
        assertTrue(section.contains("Candidate Decision Vector"));
        assertTrue(section.contains("fallback text when non-selection cannot be explained from visible data"));
        assertTrue(normalized.contains("prevents hidden scoring invention"));
        assertTrue(section.contains("ServerScoreCalculator factor contribution contract"));
        assertTrue(normalized.contains("additive extraction for existing local calculator components"));
        assertTrue(normalized.contains("preserves score values"));
        assertTrue(normalized.contains("does not retune strategy weights"));
        assertTrue(normalized.contains("future/not implemented roadmap items"));
        assertTrue(normalized.contains("api/cockpit rendering"));
        assertTrue(normalized.contains("decision replay"));
        assertTrue(normalized.contains("what-if experiments"));
        assertTrue(normalized.contains("structured decision logging"));
        assertTrue(normalized.contains("strategy plugin explainability"));
        assertTrue(normalized.contains("rack/zone/topology modeling"));
        assertTrue(normalized.contains("correlated failure modeling"));
        assertTrue(normalized.contains("live interrogation mode"));
        assertTrue(normalized.contains("does not claim exact production scoring"));
        assertTrue(normalized.contains("production telemetry"));
        assertTrue(normalized.contains("production monitoring"));
        assertTrue(normalized.contains("production certification"));
        assertTrue(normalized.contains("upload/share endpoints"));
        assertTrue(normalized.contains("server-side export/pdf/zip generation"));
    }

    @Test
    void routingCockpitReviewerWorkflowTrustMapSectionIsDiscoverableAndBounded() throws Exception {
        String trustMap = read(TRUST_MAP);
        String section = section(trustMap, "### Routing Cockpit Reviewer Workflow", "### Enterprise Lab Cockpit Monitoring and How-Question Guide");
        String normalized = section.toLowerCase(Locale.ROOT);

        assertTrue(section.contains("Routing Cockpit Reviewer Workflow"));
        assertTrue(section.contains("/routing-demo.html"));
        assertTrue(section.contains("load controlled lab scenario"));
        assertTrue(section.contains("run routing comparison"));
        assertTrue(section.contains("inspect Routing Proof Summary"));
        assertTrue(section.contains("compare scenario deltas"));
        assertTrue(section.contains("follow Evidence Navigation links"));
        assertTrue(section.contains("copy reviewer proof note"));
        assertTrue(section.contains("export/print packet from `/evidence-export-packet.html`"));
        assertTrue(section.contains("Reviewer Confidence Signals"));
        assertTrue(normalized.contains("local repeatability"));
        assertTrue(normalized.contains("deterministic lab scenarios"));
        assertTrue(normalized.contains("same-origin api usage"));
        assertTrue(normalized.contains("static browser-only notes/copy actions"));
        assertTrue(normalized.contains("not-production-certified boundary"));
        assertTrue(normalized.contains("browser-local copy text only"));
        assertTrue(normalized.contains("no upload/share endpoint"));
        assertTrue(normalized.contains("no server-side export/pdf/zip generation"));
        assertTrue(normalized.contains("no production traffic proof"));
        assertTrue(normalized.contains("no production telemetry proof"));
        assertTrue(normalized.contains("no live cloud proof"));
        assertTrue(normalized.contains("no real tenant proof"));
        assertTrue(normalized.contains("no registry publication proof"));
        assertTrue(normalized.contains("no container signing proof"));
    }

    @Test
    void keyDocsLinkBackToReviewerTrustMap() throws Exception {
        for (Path doc : List.of(README, RUNBOOK, OPERATOR_PACKAGING, INSTALL_MATRIX,
                RELEASE_DRY_RUN, REAL_BACKEND_EXAMPLES, PRIVATE_NETWORK_DRY_RUN,
                PRIVATE_NETWORK_LIVE_GATE, TESTING_COVERAGE)) {
            assertTrue(read(doc).contains("REVIEWER_TRUST_MAP.md"), doc + " should link to trust map");
        }
    }

    @Test
    void localProxyEvidenceExportRecipeIsConciseAndSafetyBounded() throws Exception {
        String trustMap = read(TRUST_MAP);
        String readme = read(README);
        String runbook = read(RUNBOOK);
        String recipe = section(trustMap, "### Local Proxy Evidence Export", "### Release-Readiness Review");
        String normalized = recipe.toLowerCase(Locale.ROOT);

        assertTrue(recipe.contains("mvn -Dtest=LocalProxyEvidenceExportTest test"));
        assertTrue(recipe.contains("target/proxy-evidence/local-proxy-evidence.md"));
        assertTrue(recipe.contains("target/proxy-evidence/local-proxy-evidence.json"));
        assertTrue(normalized.contains("markdown file is the human review path"));
        assertTrue(normalized.contains("json file is the structured evidence path"));
        assertTrue(normalized.contains("loopback/local-only jdk `httpserver`"));
        assertTrue(recipe.contains("/proxy/**"));
        assertTrue(normalized.contains("backend receipt"));
        assertTrue(normalized.contains("forwarded status/body/header proof"));
        assertTrue(normalized.contains("prod api-key `401`/`200` boundary"));
        assertTrue(normalized.contains("ignored `target/` output"));
        assertTrue(normalized.contains("not tracked docs"));
        assertTrue(normalized.contains("do not write api keys or secrets"));
        assertTrue(normalized.contains("do not add external network behavior"));
        assertTrue(recipe.contains("apiKeyRedacted=\"<REDACTED>\""));
        assertTrue(readme.contains("REVIEWER_TRUST_MAP.md#local-proxy-evidence-export"));
        assertTrue(runbook.contains("REVIEWER_TRUST_MAP.md#local-proxy-evidence-export"));
    }

    @Test
    void privateNetworkDryRunRecipeIsConciseAndSafetyBounded() throws Exception {
        String trustMap = read(TRUST_MAP);
        String readme = read(README);
        String runbook = read(RUNBOOK);
        String recipe = section(trustMap, "### Private-Network Validation Dry Run",
                "### Release-Readiness Review");
        String normalized = recipe.toLowerCase(Locale.ROOT);

        assertTrue(recipe.contains("mvn -Dtest=PrivateNetworkProxyDryRunEvidenceTest test"));
        assertTrue(recipe.contains("target/proxy-evidence/private-network-validation-dry-run.md"));
        assertTrue(recipe.contains("target/proxy-evidence/private-network-validation-dry-run.json"));
        assertTrue(recipe.contains("loadbalancerpro.proxy.private-network-validation.enabled=true"));
        assertTrue(recipe.contains("LOOPBACK_ALLOWED"));
        assertTrue(recipe.contains("PRIVATE_NETWORK_ALLOWED"));
        assertTrue(recipe.contains("PUBLIC_NETWORK_REJECTED"));
        assertTrue(recipe.contains("AMBIGUOUS_HOST_REJECTED"));
        assertTrue(recipe.contains("USERINFO_REJECTED"));
        assertTrue(recipe.contains("UNSUPPORTED_SCHEME_REJECTED"));
        assertTrue(recipe.contains("INVALID_REJECTED"));
        assertTrue(normalized.contains("human review path"));
        assertTrue(normalized.contains("structured evidence path"));
        assertTrue(normalized.contains("loopback/local-only and private literal url samples"));
        assertTrue(normalized.contains("fail-closed-before-active-config"));
        assertTrue(normalized.contains("dryrunonly=true"));
        assertTrue(normalized.contains("trafficsent=false"));
        assertTrue(normalized.contains("dnsresolution=false"));
        assertTrue(normalized.contains("reachabilitychecks=false"));
        assertTrue(normalized.contains("portscanning=false"));
        assertTrue(normalized.contains("postmanexecution=false"));
        assertTrue(normalized.contains("smokeexecution=false"));
        assertTrue(normalized.contains("apikeypersisted=false"));
        assertTrue(normalized.contains("secretpersisted=false"));
        assertTrue(normalized.contains("ignored `target/` output"));
        assertTrue(normalized.contains("do not write api keys or secrets"));
        assertTrue(normalized.contains("do not add private-network live execution"));
        assertTrue(readme.contains("PRIVATE_NETWORK_PROXY_DRY_RUN.md"));
        assertTrue(runbook.contains("PRIVATE_NETWORK_PROXY_DRY_RUN.md"));
        assertTrue(read(PRIVATE_NETWORK_DRY_RUN).contains("REVIEWER_TRUST_MAP.md"));
    }

    @Test
    void privateNetworkLiveLoopbackProofRecipeLocksHardeningAndRedaction() throws Exception {
        String trustMap = read(TRUST_MAP);
        String recipe = section(trustMap, "### Private-Network Live Loopback Proof",
                "### Private-Network Live Validation Gate");
        String normalized = recipe.toLowerCase(Locale.ROOT);

        assertTrue(recipe.contains("mvn -Dtest=PrivateNetworkLiveValidationExecutorTest test"));
        assertTrue(recipe.contains("target/proxy-evidence/private-network-live-loopback-validation.md"));
        assertTrue(recipe.contains("target/proxy-evidence/private-network-live-loopback-validation.json"));
        assertTrue(normalized.contains("markdown file is the human review path"));
        assertTrue(normalized.contains("json file is the structured evidence path"));
        assertTrue(normalized.contains("loopbackonly=true"));
        assertTrue(normalized.contains("trafficsent=true"));
        assertTrue(normalized.contains("requestcount=1"));
        assertTrue(normalized.contains("boundedtimeoutms=2000"));
        assertTrue(normalized.contains("dnsresolution=false"));
        assertTrue(normalized.contains("discovery=false"));
        assertTrue(normalized.contains("portscanning=false"));
        assertTrue(normalized.contains("postmanexecution=false"));
        assertTrue(normalized.contains("smokeexecution=false"));
        assertTrue(normalized.contains("releasedownloadsmutated=false"));
        assertTrue(normalized.contains("secretpersisted=false"));
        assertTrue(normalized.contains("broaderprivatelanvalidation=false"));
        assertTrue(normalized.contains("apikeyredacted=\"<redacted>\""));
        assertTrue(normalized.contains("unsafe validation paths fail before transport"));
        assertTrue(normalized.contains("allowlisted deterministic validation headers are propagated"));
        assertTrue(normalized.contains("allowlisted response summary headers are captured"));
        assertTrue(normalized.contains("redirects are reported without following public `location` targets"));
        assertTrue(normalized.contains("excludes api keys, bearer tokens, cookies, tokens, redirect targets"));
        assertTrue(normalized.contains("raw backend urls"));
        assertTrue(normalized.contains("broader private-lan validation claims"));
        assertTrue(normalized.contains("ignored `target/` output"));
        assertTrue(normalized.contains("do not write api keys or secrets"));
        assertTrue(normalized.contains("do not add startup, postman, smoke, proxy-routing"));
        assertTrue(normalized.contains("public-network"));
        assertTrue(normalized.contains("broader private-lan live execution"));
    }

    @Test
    void privateNetworkLiveValidationGateRecipeIsConciseAndSafetyBounded() throws Exception {
        String trustMap = read(TRUST_MAP);
        String readme = read(README);
        String runbook = read(RUNBOOK);
        String gate = read(PRIVATE_NETWORK_LIVE_GATE);
        String recipe = section(trustMap, "### Private-Network Live Validation Gate",
                "### Release-Readiness Review");
        String normalized = recipe.toLowerCase(Locale.ROOT);
        String gateNormalized = gate.toLowerCase(Locale.ROOT);

        assertTrue(recipe.contains("PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md"));
        assertTrue(normalized.contains("offline decision helper"));
        assertTrue(recipe.contains("PrivateNetworkLiveValidationExecutor"));
        assertTrue(recipe.contains("PrivateNetworkLiveValidationExecutorTest"));
        assertTrue(recipe.contains("/api/proxy/status.privateNetworkLiveValidation"));
        assertTrue(recipe.contains("POST /api/proxy/private-network-live-validation"));
        assertTrue(recipe.contains("trafficExecuted=false"));
        assertTrue(recipe.contains("evidenceWritten=false"));
        assertTrue(recipe.contains("auditTrail.auditTrailWritten=false"));
        assertTrue(recipe.contains("target/proxy-evidence/private-network-live-validation.md"));
        assertTrue(recipe.contains("target/proxy-evidence/private-network-live-validation.json"));
        assertTrue(recipe.contains("target/proxy-evidence/private-network-live-validation-audit.jsonl"));
        assertTrue(recipe.contains("target/proxy-evidence/private-network-live-loopback-validation.md"));
        assertTrue(recipe.contains("target/proxy-evidence/private-network-live-loopback-validation.json"));
        assertTrue(normalized.contains("status/report-only gate visibility"));
        assertTrue(normalized.contains("protected command contract"));
        assertTrue(normalized.contains("neither surface calls the executor or sends validation traffic"));
        assertTrue(normalized.contains("not-wired/not-executed responses"));
        assertTrue(normalized.contains("planned ignored future outputs"));
        assertTrue(normalized.contains("redacted ignored `target/` evidence and audit output"));
        assertTrue(normalized.contains("request-path validation"));
        assertTrue(normalized.contains("one bounded request only"));
        assertTrue(normalized.contains("bounded executor primitive"));
        assertTrue(normalized.contains("requires an allowed gate result"));
        assertTrue(normalized.contains("broader runtime/private-lan live traffic execution is not implemented yet"));
        assertTrue(normalized.contains("separate approved task"));
        assertTrue(normalized.contains("default properties"));
        assertTrue(recipe.contains("loadbalancerpro.proxy.private-network-live-validation.enabled=false"));
        assertTrue(recipe.contains("loadbalancerpro.proxy.private-network-live-validation.operator-approved=false"));
        assertTrue(recipe.contains("loadbalancerpro.proxy.private-network-live-validation.enabled=true"));
        assertTrue(recipe.contains("loadbalancerpro.proxy.private-network-live-validation.operator-approved=true"));
        assertTrue(normalized.contains("explicit operator approval"));
        assertTrue(normalized.contains("operator-provided literal backend urls"));
        assertTrue(normalized.contains("proxybackendurlclassifier"));
        assertTrue(normalized.contains("bounded timeout behavior"));
        assertTrue(normalized.contains("redacted ignored `target/` evidence"));
        assertTrue(normalized.contains("api-key/oauth2 boundary proof"));
        assertTrue(normalized.contains("no dns"));
        assertTrue(normalized.contains("no discovery"));
        assertTrue(normalized.contains("no scanning"));
        assertTrue(normalized.contains("no persistence"));
        assertTrue(normalized.contains("no service installation"));
        assertTrue(normalized.contains("no scheduled tasks"));
        assertTrue(normalized.contains("no native tooling"));
        assertTrue(normalized.contains("no secret persistence"));
        assertTrue(normalized.contains("fail-closed startup/reload behavior"));
        assertTrue(normalized.contains("current live traffic proof is limited"));
        assertTrue(normalized.contains("fail-closed path validation"));
        assertTrue(normalized.contains("allowlisted validation headers"));
        assertTrue(normalized.contains("allowlisted response summary headers"));
        assertTrue(normalized.contains("no redirect following"));
        assertTrue(gate.contains("loadbalancerpro.proxy.private-network-live-validation.enabled=true"));
        assertTrue(gate.contains("loadbalancerpro.proxy.private-network-live-validation.enabled=false"));
        assertTrue(gate.contains("loadbalancerpro.proxy.private-network-live-validation.operator-approved=true"));
        assertTrue(gate.contains("loadbalancerpro.proxy.private-network-live-validation.operator-approved=false"));
        assertTrue(gateNormalized.contains("private-network live traffic outside the junit loopback proof remains unimplemented"));
        assertTrue(readme.contains("PRIVATE_NETWORK_PROXY_DRY_RUN.md")
                || readme.contains("PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md"));
        assertTrue(runbook.contains("PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md"));
        assertTrue(gate.contains("REVIEWER_TRUST_MAP.md"));
    }

    @Test
    void readmeAndRunbookLinksResolveToReviewerTrustMap() throws Exception {
        assertLocalMarkdownLinkResolvesTo(README, "docs/REVIEWER_TRUST_MAP.md", TRUST_MAP);
        assertLocalMarkdownLinkResolvesTo(RUNBOOK, "REVIEWER_TRUST_MAP.md", TRUST_MAP);
    }

    @Test
    void reviewerTrustMapMarkdownReferencesResolve() throws Exception {
        for (String link : markdownLinks(read(TRUST_MAP))) {
            Path resolved = resolveMarkdownLink(TRUST_MAP, link);
            assertTrue(Files.exists(resolved), "trust map link should resolve: " + link + " -> " + resolved);
        }
    }

    @Test
    void releaseEvidencePlanUsesResolvableEvidenceLink() throws Exception {
        String plan = read(V1_9_1_EVIDENCE_PLAN);

        assertTrue(plan.contains("[`RELEASE_ARTIFACT_EVIDENCE.md`](../evidence/RELEASE_ARTIFACT_EVIDENCE.md)"));
        assertFalse(plan.contains("](evidence/RELEASE_ARTIFACT_EVIDENCE.md)"),
                "docs-local markdown links must not point to a nonexistent docs/evidence path");
        assertEquals(RELEASE_ARTIFACT_EVIDENCE,
                resolveMarkdownLink(V1_9_1_EVIDENCE_PLAN, "../evidence/RELEASE_ARTIFACT_EVIDENCE.md"));
        assertTrue(Files.exists(RELEASE_ARTIFACT_EVIDENCE), "release artifact evidence should exist");
    }

    @Test
    void reviewerTrustMapDocumentsSafetyBoundariesWithoutReleaseActions() throws Exception {
        String trustMap = read(TRUST_MAP);

        assertTrue(trustMap.contains("Proxy is disabled by default."));
        assertTrue(trustMap.contains("JavaFX is optional"));
        assertTrue(trustMap.contains("Release-free docs do not create tags, GitHub Releases, or release assets."));
        assertTrue(trustMap.contains("`release-downloads/` remains manual and explicit only."));
        assertTrue(trustMap.contains("Workflow artifacts are not GitHub Release assets."));
        assertTrue(trustMap.contains("do not construct or mutate `CloudManager`"));
        assertTrue(trustMap.contains("explicit operator-provided backend URLs only"));
        assertTrue(trustMap.contains("dry-run-only evidence under ignored `target/`"));
        assertTrue(trustMap.contains("explicit operator approval before live traffic"));
        assertTrue(trustMap.contains("loopback-only executor proof under JUnit"));
        assertTrue(trustMap.contains("no runtime/private-LAN live traffic execution until separately approved"));
    }

    @Test
    void reviewerTrustMapAvoidsFakeEvidenceAndUnsafeClaims() throws Exception {
        String trustMap = read(TRUST_MAP);
        String normalized = trustMap.toLowerCase(Locale.ROOT);

        assertFalse(FAKE_HASH.matcher(trustMap).find(), "trust map must not include fake hashes");
        assertFalse(normalized.contains("fake evidence"), "trust map must not include fake evidence");
        assertFalse(normalized.contains("gh release"), "trust map must not add release commands");
        assertFalse(normalized.contains("git tag"), "trust map must not add tag commands");
        assertNoCloudManagerConstruction(TRUST_MAP, trustMap);
        assertFalse(normalized.contains("production-grade"), "trust map must not add production-grade claims");
        assertFalse(normalized.contains("benchmark proof"), "trust map must not add benchmark proof claims");
        assertFalse(normalized.contains("certification proof"), "trust map must not add certification proof claims");
    }

    @Test
    void reviewerNavigationDocsAvoidPositiveProductionBenchmarkAndCertificationClaims() throws Exception {
        for (Path doc : REVIEWER_NAV_DOCS) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            assertFalse(normalized.contains("production-grade gateway"), doc + " must not add production-grade claims");
            assertFalse(normalized.contains("production-ready gateway"), doc + " must not add production-ready claims");
            assertFalse(normalized.contains("is a production benchmark"), doc + " must not add benchmark claims");
            assertFalse(normalized.contains("benchmark result"), doc + " must not add benchmark result claims");
            assertFalse(normalized.contains("certification proof"), doc + " must not add certification proof claims");
            assertFalse(normalized.contains("certified gateway"), doc + " must not add certification claims");
        }
    }

    @Test
    void releaseFreeDocsAvoidTagReleaseAndAssetCreationCommands() throws Exception {
        for (Path doc : RELEASE_FREE_DOCS) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            assertFalse(normalized.contains("gh release create"), doc + " must not create releases");
            assertFalse(normalized.contains("gh release upload"), doc + " must not upload release assets");
            assertFalse(normalized.contains("gh release edit"), doc + " must not edit releases");
            assertFalse(normalized.contains("git tag -"), doc + " must not create tags");
        }
    }

    @Test
    void docsAndStaticReferenceTestAvoidCloudManagerConstruction() throws Exception {
        for (Path doc : REVIEWER_NAV_DOCS) {
            assertNoCloudManagerConstruction(doc, read(doc));
        }
        assertNoCloudManagerConstruction(V1_9_1_EVIDENCE_PLAN, read(V1_9_1_EVIDENCE_PLAN));
        assertNoCloudManagerConstruction(THIS_TEST, read(THIS_TEST));
    }

    private static void assertLocalMarkdownLinkResolvesTo(Path source, String link, Path expected) throws IOException {
        assertTrue(read(source).contains("(" + link + ")"), source + " should link to " + link);
        assertEquals(expected, resolveMarkdownLink(source, link));
        assertTrue(Files.exists(expected), expected + " should exist");
    }

    private static List<String> markdownLinks(String markdown) {
        Matcher matcher = MARKDOWN_LINK.matcher(markdown);
        List<String> links = new ArrayList<>();
        while (matcher.find()) {
            links.add(matcher.group(1));
        }
        return links;
    }

    private static Path resolveMarkdownLink(Path source, String link) {
        String linkWithoutFragment = link.split("#", 2)[0];
        Path parent = source.getParent();
        Path base = parent == null ? Path.of("") : parent;
        return base.resolve(linkWithoutFragment).normalize();
    }

    private static void assertNoCloudManagerConstruction(Path source, String text) {
        assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(text).find(), source + " must not construct CloudManager");
    }

    private static String section(String text, String startHeading, String endHeading) {
        int start = text.indexOf(startHeading);
        assertTrue(start >= 0, "missing section start: " + startHeading);
        int end = text.indexOf(endHeading, start + startHeading.length());
        assertTrue(end > start, "missing section end: " + endHeading);
        return text.substring(start, end);
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
