package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class EnterpriseLabCockpitFramingDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path FRAMING = Path.of("docs/ENTERPRISE_LAB_COCKPIT_FRAMING.md");
    private static final Path AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PRODUCTION_SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path TRUST_HARDENING = Path.of("docs/ENTERPRISE_LAB_TRUST_HARDENING_SPRINT.md");
    private static final Path ROUTING_COCKPIT = Path.of("src/main/resources/static/routing-demo.html");

    private static final List<Path> ACTIVE_FRAMING_FILES = List.of(
            README,
            TRUST_MAP,
            FRAMING,
            AUDIT,
            PRODUCTION_SUMMARY,
            TRUST_HARDENING,
            ROUTING_COCKPIT);

    @Test
    void readmeMakesEnterpriseLabCockpitPivotHardToMiss() throws Exception {
        String readme = read(README);

        assertTrue(readme.contains("## Enterprise Lab Cockpit, Not a Demo"));
        assertTrue(readme.contains(
                "LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo."));
        assertTrue(readme.contains("The Enterprise Lab Cockpit provides controlled lab evidence, local reproducibility, and reviewer/operator explanations."));
        assertTrue(readme.contains("It does not claim production certification, live-cloud proof, real-tenant proof, SLA/SLO proof, registry publication, container signing, governance application, production telemetry, or production monitoring proof."));
        assertTrue(readme.contains("It is not a casual demo, toy, mockup, playground, or sample-only page."));
        assertTrue(readme.contains("## What the Enterprise Lab Cockpit Monitors"));
        assertTrue(readme.contains("Active lab scenario."));
        assertTrue(readme.contains("Routing comparison request state."));
        assertTrue(readme.contains("Selected strategy."));
        assertTrue(readme.contains("Selected backend/server."));
        assertTrue(readme.contains("Backend health."));
        assertTrue(readme.contains("Visible latency, load, connection, capacity, and weight-style signals"));
        assertTrue(readme.contains("Degradation, fallback, and recovery state."));
        assertTrue(readme.contains("Scenario-to-scenario delta."));
        assertTrue(readme.contains("Evidence association path."));
        assertTrue(readme.contains("Reviewer handoff readiness."));
        assertTrue(readme.contains("Local lab proof boundary."));
        assertTrue(readme.contains("Production proof gaps."));
        assertTrue(readme.contains("## What the Enterprise Lab Cockpit Answers"));
        assertTrue(readme.contains("How routing decisions are made"));
        assertTrue(readme.contains("How strategies affect backend selection."));
        assertTrue(readme.contains("How input signals influence outcomes."));
        assertTrue(readme.contains("How unhealthy, degraded, and recovery states are interpreted."));
        assertTrue(readme.contains("How lab proof is reproduced locally."));
        assertTrue(readme.contains("What remains not proven for production."));
    }

    @Test
    void readmeLinksEnterpriseLabCockpitEvidencePathAndLegacyRoute() throws Exception {
        String readme = read(README);

        for (String expected : List.of(
                "Legacy route name: [`/routing-demo.html`](http://localhost:8080/routing-demo.html). Product identity: Enterprise Lab routing cockpit.",
                "http://localhost:8080/enterprise-lab-reviewer.html",
                "http://localhost:8080/operator-evidence-dashboard.html",
                "http://localhost:8080/evidence-timeline.html",
                "http://localhost:8080/evidence-export-packet.html",
                "docs/REVIEWER_TRUST_MAP.md",
                "docs/ENTERPRISE_LAB_COCKPIT_FRAMING.md")) {
            assertTrue(readme.contains(expected), "README should link or name " + expected);
        }
    }

    @Test
    void reviewerTrustMapAndFramingDocContainExplicitNotDemoBoundary() throws Exception {
        String trustMap = read(TRUST_MAP);
        String framing = read(FRAMING);

        for (String doc : List.of(trustMap, framing)) {
            assertTrue(doc.contains("Enterprise Lab Cockpit, Not a Demo"));
            assertTrue(doc.contains(
                    "LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo."));
            assertTrue(doc.contains("controlled lab evidence"));
            assertTrue(doc.contains("local reproducibility"));
            assertTrue(doc.contains("reviewer/operator explanations"));
            assertTrue(doc.contains("production certification"));
            assertTrue(doc.contains("live-cloud proof"));
            assertTrue(doc.contains("real-tenant proof"));
            assertTrue(doc.contains("SLA/SLO proof"));
            assertTrue(doc.contains("registry publication"));
            assertTrue(doc.contains("container signing"));
            assertTrue(doc.contains("governance application"));
            assertTrue(doc.contains("production telemetry"));
            assertTrue(doc.contains("production monitoring proof"));
        }

        assertTrue(trustMap.contains("## Reviewer Lab Review Path"));
        assertTrue(trustMap.contains("Route names such as `/routing-demo.html` may remain for compatibility; the product identity is Enterprise Lab Cockpit."));
        assertTrue(framing.contains("## Product Identity"));
        assertTrue(framing.contains("## What the Cockpit Monitors"));
        assertTrue(framing.contains("## What Questions It Answers"));
        assertTrue(framing.contains("## Controlled Lab Proof Chain"));
        assertTrue(framing.contains("## Monitored Proof Chain"));
        assertTrue(framing.contains("controlled lab scenario -> visible input signals -> selected strategy -> selected backend/server -> comparison delta -> evidence association -> reviewer handoff"));
        assertTrue(framing.contains("This monitoring depth is reviewer-facing interpretation for controlled pre-production routing validation."));
        assertTrue(framing.contains("## Decision Trace Depth"));
        assertTrue(framing.contains("why the selected backend appears to have won using only visible controlled lab response data"));
        assertTrue(framing.contains("The cockpit may explain why alternatives were not selected only when visible signal comparison supports that interpretation."));
        assertTrue(framing.contains("Known signals are the fields visible in the local lab request and same-origin routing comparison response."));
        assertTrue(framing.contains("Unknown signals include missing local fields, unavailable API responses, exact scoring not exposed by the API"));
        assertTrue(framing.contains("The investigation playbook should guide reviewers to inspect the Signal Interpretation Guide, Decision Chain Trace, Scenario Comparison, Evidence Associations, and Export Packet"));
        assertTrue(framing.contains("Decision trace depth remains controlled lab evidence for pre-production routing validation."));
        assertTrue(framing.contains("## How to Investigate Surprising Lab Decisions"));
        assertTrue(framing.contains("If the backend changed unexpectedly"));
        assertTrue(framing.contains("If all candidates are unhealthy"));
        assertTrue(framing.contains("the monitored decision chain is incomplete"));
        assertTrue(framing.contains("## Evidence Association Model"));
        assertTrue(framing.contains("## Reviewer Handoff Flow"));
        assertTrue(framing.contains("## Production Not-Proven Boundaries"));
        assertTrue(framing.contains("## Naming Guidance"));
    }

    @Test
    void cockpitVisibleCopyUsesEnterpriseLabIdentityInsteadOfDemoIdentity() throws Exception {
        String page = read(ROUTING_COCKPIT);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("<title>LoadBalancerPro Enterprise Lab Routing Cockpit</title>"));
        assertTrue(page.contains("Enterprise Lab Routing Cockpit - controlled pre-production routing validation, not a demo."));
        assertTrue(page.contains("Enterprise Lab Routing Proof Cockpit"));
        assertTrue(page.contains("controlled lab routing decisions"));
        assertTrue(page.contains("same-origin local APIs only"));
        assertFalse(page.contains("<title>LoadBalancerPro Routing Decision Demo</title>"));
        assertFalse(normalized.contains("demo page"));
        assertFalse(normalized.contains("demo-only"));
        assertFalse(normalized.contains("sample-only"));
        assertFalse(normalized.contains("toy"));
        assertFalse(normalized.contains("mockup"));
        assertFalse(normalized.contains("playground"));
    }

    @Test
    void framingDocIsLinkedFromRequiredMarkdownDocs() throws Exception {
        for (Path path : List.of(README, TRUST_MAP, AUDIT, PRODUCTION_SUMMARY, TRUST_HARDENING)) {
            assertTrue(read(path).contains("ENTERPRISE_LAB_COCKPIT_FRAMING.md"),
                    path + " should link the Enterprise Lab cockpit framing doc");
        }
    }

    @Test
    void activeFramingFilesAvoidCasualDemoIdentityAndProductionProofOverclaims() throws Exception {
        for (Path path : ACTIVE_FRAMING_FILES) {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String normalized = line.toLowerCase(Locale.ROOT);
                assertNoCasualIdentity(path, i + 1, normalized);
                assertNoProductionProofOverclaim(path, i + 1, normalized);
                assertLegacyRouteReferencesAreExplicit(path, i + 1, line, normalized);
            }
        }
    }

    private static void assertNoCasualIdentity(Path path, int lineNumber, String normalized) {
        boolean negativeFraming = normalized.contains("not a demo")
                || normalized.contains("not a casual demo")
                || normalized.contains("it is not")
                || normalized.contains("is not");
        boolean routeOrLegacyName = normalized.contains("routing-demo.html")
                || normalized.contains("legacy route")
                || normalized.contains("legacy-named")
                || normalized.contains("legacy `routing decision demo`");

        for (String forbidden : List.of("demo-only", "demo page", "toy", "mockup", "fake", "playground",
                "sample-only")) {
            if (normalized.contains(forbidden)) {
                assertTrue(negativeFraming || routeOrLegacyName,
                        path + ":" + lineNumber + " must not present " + forbidden + " as product identity");
            }
        }
    }

    private static void assertNoProductionProofOverclaim(Path path, int lineNumber, String normalized) {
        for (String boundedPhrase : List.of("production telemetry proof", "production monitoring proof",
                "production certification", "live-cloud proof", "real-tenant proof", "sla/slo proof")) {
            if (normalized.contains(boundedPhrase)) {
                boolean negated = normalized.contains("does not")
                        || normalized.contains("do not")
                        || normalized.contains("not ")
                        || normalized.contains("no ")
                        || normalized.contains("without")
                        || normalized.contains("not-proven")
                        || normalized.contains("did any wording imply")
                        || normalized.startsWith("|");
                assertTrue(negated, path + ":" + lineNumber + " must keep " + boundedPhrase + " negated");
            }
        }
    }

    private static void assertLegacyRouteReferencesAreExplicit(Path path, int lineNumber, String line,
            String normalized) {
        if (normalized.contains("routing-demo.html")) {
            assertTrue(line.contains("/routing-demo.html") || normalized.contains("legacy route")
                    || normalized.contains("route/file name"),
                    path + ":" + lineNumber + " should reference routing-demo.html as a route/file compatibility name");
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
