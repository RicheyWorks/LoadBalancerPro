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
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class PrivateNetworkProxyProfilePlanDocumentationTest {
    private static final Path PLAN = Path.of("docs/PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md");
    private static final Path DRY_RUN = Path.of("docs/PRIVATE_NETWORK_PROXY_DRY_RUN.md");
    private static final Path LIVE_GATE = Path.of("docs/PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path LIVE_PROXY_CONTAINMENT = Path.of("docs/LIVE_PROXY_CONTAINMENT.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path SMOKE_SCRIPTS = Path.of("scripts/smoke");
    private static final Path POSTMAN_DOCS = Path.of("docs/postman");
    private static final Path DRY_RUN_TEST = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/api/proxy/PrivateNetworkProxyDryRunEvidenceTest.java");

    private static final Pattern PUBLIC_EXTERNAL_URL =
            Pattern.compile("https?://(?!127\\.0\\.0\\.1(?::|/|$)|localhost(?::|/|$))[^\\s\"'`]+");

    @Test
    void privateNetworkProxyProfilePlanExistsAndIsDesignOnly() throws Exception {
        String plan = read(PLAN);
        String normalized = plan.toLowerCase(Locale.ROOT);

        assertTrue(plan.contains("# Private-Network Proxy Profile Plan"));
        assertTrue(normalized.contains("design and rollout plan"));
        assertTrue(normalized.contains("opt-in configuration validation"));
        assertTrue(normalized.contains("default-off live gate primitive"));
        assertTrue(normalized.contains("junit-only loopback live executor proof"));
        assertTrue(normalized.contains("does not add runtime private-network live execution"));
        assertTrue(normalized.contains("postman/smoke live execution"));
        assertTrue(normalized.contains("proxy request-routing changes"));
        assertTrue(normalized.contains("default/local/demo behavior changes"));
        assertFalse(PUBLIC_EXTERNAL_URL.matcher(plan).find(), "plan must not introduce public URL targets");
    }

    @Test
    void planDefinesUseCaseNonGoalsAndAllowedHostsModel() throws Exception {
        String plan = read(PLAN);
        String normalized = plan.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("explicit local or private http backends"));
        assertTrue(normalized.contains("operator-provided explicit backend urls only"));
        assertTrue(normalized.contains("loopback"));
        assertTrue(plan.contains("127.0.0.0/8"));
        assertTrue(plan.contains("10.0.0.0/8"));
        assertTrue(plan.contains("172.16.0.0/12"));
        assertTrue(plan.contains("192.168.0.0/16"));
        assertTrue(plan.contains("fc00::/7"));
        assertTrue(normalized.contains("no public internet validation"));
        assertTrue(normalized.contains("no host discovery, dns enumeration, subnet scanning, or port scanning"));
        assertTrue(normalized.contains("must not expand hostnames, cidr ranges, ip ranges, or service names"));
        assertTrue(normalized.contains("unsupported schemes, public addresses, wildcard domains"));
        assertTrue(normalized.contains("fail closed"));
        assertTrue(plan.contains("ProxyBackendUrlClassifier"));
        assertTrue(normalized.contains("offline classification only"));
        assertTrue(normalized.contains("loopback allowed"));
        assertTrue(normalized.contains("private-network allowed"));
        assertTrue(normalized.contains("public-network rejected"));
        assertTrue(normalized.contains("invalid rejected"));
        assertTrue(normalized.contains("unsupported-scheme rejected"));
        assertTrue(normalized.contains("user-info rejected"));
        assertTrue(normalized.contains("ambiguous-host rejected"));
        assertTrue(plan.contains("loadbalancerpro.proxy.private-network-validation.enabled=true"));
        assertTrue(normalized.contains("startup and explicit proxy reload validation use that classifier"));
        assertTrue(normalized.contains("unsafe backend urls fail closed"));
        assertTrue(plan.contains("PRIVATE_NETWORK_PROXY_DRY_RUN.md"));
        assertTrue(plan.contains("PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md"));
        assertTrue(plan.contains("target/proxy-evidence/private-network-validation-dry-run.md"));
        assertTrue(plan.contains("target/proxy-evidence/private-network-validation-dry-run.json"));
        assertTrue(normalized.contains("does not resolve dns"));
        assertTrue(normalized.contains("perform reachability checks"));
        assertTrue(normalized.contains("scan ports"));
        assertTrue(normalized.contains("discover hosts"));
        assertTrue(normalized.contains("change default/local/demo behavior"));
        assertTrue(normalized.contains("add runtime private-network live execution"));
        assertTrue(plan.contains("PrivateNetworkLiveValidationGate"));
        assertTrue(plan.contains("loadbalancerpro.proxy.private-network-live-validation.enabled=true"));
        assertTrue(plan.contains("loadbalancerpro.proxy.private-network-live-validation.operator-approved=true"));
        assertTrue(normalized.contains("offline decision gate"));
        assertTrue(normalized.contains("default to `false`"));
        assertTrue(plan.contains("PrivateNetworkLiveValidationExecutor"));
        assertTrue(normalized.contains("bounded primitive"));
        assertTrue(normalized.contains("requires an allowed gate result"));
        assertTrue(normalized.contains("junit-only loopback traffic"));
        assertTrue(normalized.contains("jdk `httpserver`"));
        assertTrue(plan.contains("target/proxy-evidence/private-network-live-loopback-validation.md"));
        assertTrue(plan.contains("target/proxy-evidence/private-network-live-loopback-validation.json"));
        assertTrue(normalized.contains("broader private-lan live validation remains gated and unimplemented"));
    }

    @Test
    void planPreservesAntivirusSafeContainmentAndReleaseBoundaries() throws Exception {
        String plan = read(PLAN);
        String normalized = plan.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no service installation",
                "scheduled tasks",
                "persistence mechanisms",
                "hidden agents",
                "credential storage",
                "no native executables",
                "installers",
                "wrappers",
                "packers",
                "native-image",
                "launch4j",
                "jpackage",
                "self-extracting archives",
                "downloaded servers",
                "vendored binaries",
                "release-downloads/",
                "ignored `target/` output")) {
            assertTrue(normalized.contains(expected), "plan should preserve boundary: " + expected);
        }

        assertTrue(normalized.contains("no api key, oauth2 token, credential, or secret may be persisted"));
        assertTrue(normalized.contains("browser `localstorage`"));
        assertTrue(normalized.contains("browser `sessionstorage`"));
        assertTrue(normalized.contains("generated evidence"));
        assertFalse(normalized.contains("gh release create"), "plan must not create releases");
        assertFalse(normalized.contains("gh release upload"), "plan must not upload release assets");
        assertFalse(normalized.contains("git tag -"), "plan must not create tags");
    }

    @Test
    void planPreservesSecurityAndFailureBehavior() throws Exception {
        String plan = read(PLAN);
        String normalized = plan.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("prod and cloud-sandbox api-key modes must continue to require `x-api-key`"));
        assertTrue(normalized.contains("oauth2 mode must continue to require the configured allocation role"));
        assertTrue(normalized.contains("tls termination, ingress exposure, rate limits, and identity provider controls"));
        assertTrue(normalized.contains("prod api-key `401`/`200` boundary"));
        assertTrue(normalized.contains("oauth2 `401`/`403`/authorized boundary"));
        assertTrue(normalized.contains("invalid configuration should fail before traffic"));
        assertTrue(normalized.contains("no eligible backend returns controlled `503` behavior"));
        assertTrue(normalized.contains("unreachable selected backends return controlled `502` behavior"));
        assertTrue(normalized.contains("must not fall back to public urls, generated targets, or discovered hosts"));
    }

    @Test
    void planDefinesSafeTestAndRolloutStrategyWithoutPrivateNetworkExecution() throws Exception {
        String plan = read(PLAN);
        String normalized = plan.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("keep ci on loopback-only junit/jdk `httpserver` evidence"));
        assertTrue(normalized.contains("pure java unit tests"));
        assertTrue(normalized.contains("startup/reload validation tests"));
        assertTrue(normalized.contains("dry-run-only private-network profile recipe"));
        assertTrue(normalized.contains("opt-in private-network live smoke only after a separate reviewed task"));
        assertTrue(normalized.contains("implemented dry-run-only private-network profile recipe"));
        assertTrue(normalized.contains("without sending traffic"));
        assertTrue(normalized.contains("implemented live gate"));
        assertTrue(normalized.contains("disconnected from startup, postman, smoke, and proxy routing"));
        assertTrue(normalized.contains("junit-only `privatenetworklivevalidationexecutortest`"));
        assertTrue(normalized.contains("broader opt-in private-network live smoke only after a separate reviewed task"));
        assertTrue(normalized.contains("no test should scan ports, discover hosts, require public dns"));
        assertTrue(normalized.contains("download servers"));
        assertTrue(normalized.contains("write secrets"));
    }

    @Test
    void privateNetworkLiveValidationGateExistsAndIsDesignOnly() throws Exception {
        String gate = read(LIVE_GATE);
        String normalized = gate.toLowerCase(Locale.ROOT);

        assertTrue(gate.contains("# Private-Network Live Validation Gate"));
        assertTrue(normalized.contains("design gate"));
        assertTrue(gate.contains("PrivateNetworkLiveValidationGate"));
        assertTrue(normalized.contains("default-off live gate properties"));
        assertTrue(gate.contains("PrivateNetworkLiveValidationExecutor"));
        assertTrue(normalized.contains("bounded"));
        assertTrue(normalized.contains("junit-only loopback proof"));
        assertTrue(normalized.contains("does not wire private-network live validation into app startup"));
        assertTrue(normalized.contains("postman"));
        assertTrue(normalized.contains("smoke scripts"));
        assertTrue(normalized.contains("proxy request routing"));
        assertTrue(normalized.contains("default/local/demo behavior"));
        assertTrue(normalized.contains("broader private-lan live validation remains gated and unimplemented"));
        assertTrue(normalized.contains("null, blank, absolute, scheme-relative, query-string, fragment, traversal"));
        assertTrue(normalized.contains("encoded traversal"));
        assertTrue(normalized.contains("encoded control-character"));
        assertTrue(normalized.contains("raw control-character"));
        assertTrue(normalized.contains("backslash paths fail closed before transport"));
        assertTrue(normalized.contains("allowlisted deterministic validation request headers"));
        assertTrue(normalized.contains("allowlisted response summary headers"));
        assertTrue(normalized.contains("reports redirects without following them"));
        assertTrue(gate.contains("GET /api/proxy/status"));
        assertTrue(gate.contains("POST /api/proxy/private-network-live-validation"));
        assertTrue(gate.contains("privateNetworkLiveValidation"));
        assertTrue(gate.contains("trafficExecuted=false"));
        assertTrue(gate.contains("evidenceWritten=false"));
        assertTrue(gate.contains("evidenceEligible"));
        assertTrue(gate.contains("plannedEvidenceDirectory=\"target/proxy-evidence/\""));
        assertTrue(gate.contains("plannedEvidenceMarkdown=\"private-network-live-validation.md\""));
        assertTrue(gate.contains("plannedEvidenceJson=\"private-network-live-validation.json\""));
        assertTrue(gate.contains("redactionRequired=true"));
        assertTrue(gate.contains("auditTrail.auditTrailWritten=false"));
        assertTrue(gate.contains("target/proxy-evidence/private-network-live-validation-audit.jsonl"));
        assertTrue(gate.contains("NOT_IMPLEMENTED"));
        assertTrue(gate.contains("traffic execution is not wired in this release"));
        assertTrue(gate.contains("traffic not executed by this report"));
        assertTrue(normalized.contains("report-only view of the offline gate"));
        assertTrue(normalized.contains("does not call `privatenetworklivevalidationexecutor`"));
        assertTrue(normalized.contains("does not send validation traffic"));
        assertTrue(normalized.contains("non-executing operator command contract"));
        assertTrue(normalized.contains("does not write evidence"));
        assertTrue(gate.contains("PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md"));
        assertTrue(gate.contains("PRIVATE_NETWORK_PROXY_DRY_RUN.md"));
        assertTrue(gate.contains("LIVE_PROXY_CONTAINMENT.md"));
        assertTrue(gate.contains("REVIEWER_TRUST_MAP.md"));
    }

    @Test
    void privateNetworkLiveValidationGateRequiresExplicitApprovalAndDefaultOffFlags() throws Exception {
        String gate = read(LIVE_GATE);
        String normalized = gate.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("explicit reviewed task approving live private-network validation"));
        assertTrue(gate.contains("loadbalancerpro.proxy.enabled=true"));
        assertTrue(gate.contains("loadbalancerpro.proxy.private-network-validation.enabled=true"));
        assertTrue(gate.contains("loadbalancerpro.proxy.private-network-live-validation.enabled=true"));
        assertTrue(gate.contains("loadbalancerpro.proxy.private-network-live-validation.enabled=false"));
        assertTrue(gate.contains("loadbalancerpro.proxy.private-network-live-validation.operator-approved=true"));
        assertTrue(gate.contains("loadbalancerpro.proxy.private-network-live-validation.operator-approved=false"));
        assertTrue(normalized.contains("implemented offline gate"));
        assertTrue(normalized.contains("defaulting to `false`"));
        assertTrue(normalized.contains("operator-provided literal backend urls only"));
        assertTrue(normalized.contains("passing `proxybackendurlclassifier` results"));
        assertTrue(normalized.contains("prod/cloud-sandbox api-key or oauth2 boundary proof"));
        assertTrue(normalized.contains("requires an allowed gate result"));
        assertTrue(normalized.contains("fail closed before sending traffic"));
        assertTrue(normalized.contains("before making the candidate config active"));
        assertTrue(normalized.contains("status field does not call"));
        assertTrue(normalized.contains("existing proxy status boundary"));
        assertTrue(normalized.contains("command contract is protected"));
        assertTrue(normalized.contains("oauth2 mode requires the configured allocation role"));
    }

    @Test
    void privateNetworkLiveValidationGatePreservesContainmentBoundaries() throws Exception {
        String gate = read(LIVE_GATE);
        String normalized = gate.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no dns resolution",
                "inetaddress.getbyname",
                "reachability checks",
                "socket probes",
                "host discovery",
                "subnet scanning",
                "port scanning",
                "public-network validation",
                "no postman private-network live execution by default",
                "no smoke private-network live execution by default",
                "no persistence",
                "service installation",
                "scheduled tasks",
                "credential storage",
                "secret persistence",
                "no native executables",
                "native-image",
                "launch4j",
                "jpackage",
                "downloaded servers",
                "vendored binaries",
                "release-downloads/")) {
            assertTrue(normalized.contains(expected), "live gate should preserve boundary: " + expected);
        }
    }

    @Test
    void privateNetworkLiveValidationGateDefinesFailureEvidenceAndTestRequirements() throws Exception {
        String gate = read(LIVE_GATE);
        String normalized = gate.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("invalid or rejected backend urls fail before traffic"));
        assertTrue(normalized.contains("missing operator approval fails before traffic"));
        assertTrue(normalized.contains("failed classifier validation fails before traffic"));
        assertTrue(normalized.contains("controlled failure"));
        assertTrue(normalized.contains("explicit reload failure preserves the last-known-good active config"));
        assertTrue(normalized.contains("abort must stop validation promptly"));
        assertTrue(normalized.contains("generated evidence must be markdown or json under ignored `target/` output"));
        assertTrue(gate.contains("target/proxy-evidence/private-network-live-validation.md"));
        assertTrue(gate.contains("target/proxy-evidence/private-network-live-validation.json"));
        assertTrue(gate.contains("target/proxy-evidence/private-network-live-validation-audit.jsonl"));
        assertTrue(normalized.contains("command endpoint has an evidence and audit trail contract only"));
        assertTrue(normalized.contains("keeps `evidencewritten=false` plus `audittrail.audittrailwritten=false`"));
        assertTrue(gate.contains("target/proxy-evidence/private-network-live-loopback-validation.md"));
        assertTrue(gate.contains("target/proxy-evidence/private-network-live-loopback-validation.json"));
        assertTrue(normalized.contains("must never include raw api keys"));
        assertTrue(normalized.contains("bearer tokens"));
        assertTrue(normalized.contains("authorization"));
        assertTrue(normalized.contains("x-api-key"));
        assertTrue(normalized.contains("cookie"));
        assertTrue(normalized.contains("token"));
        assertTrue(normalized.contains("redirect target"));
        assertTrue(normalized.contains("raw backend url"));
        assertTrue(normalized.contains("broader private-lan validation claims"));
        assertTrue(normalized.contains("private hostnames marked for redaction"));
        assertTrue(normalized.contains("default-off behavior for every live-validation flag"));
        assertTrue(normalized.contains("missing operator approval fails closed before traffic"));
        assertTrue(normalized.contains("bounded timeout and controlled failure reporting"));
        assertTrue(normalized.contains("redacted ignored evidence output"));
        assertTrue(normalized.contains("no postman or smoke private-network live execution by default"));
        assertTrue(normalized.contains("explicit owner approval for execution wiring"));
        assertTrue(normalized.contains("request paths pass `privatenetworklivevalidationrequestpathvalidator`"));
        assertTrue(normalized.contains("exactly one validation request is sent per command"));
        assertTrue(normalized.contains("command audit trail output is redacted and written only under ignored `target/` output"));
    }

    @Test
    void privateNetworkDryRunRecipeIsConfigOnlyAndEvidenceBounded() throws Exception {
        String recipe = read(DRY_RUN);
        String normalized = recipe.toLowerCase(Locale.ROOT);

        assertTrue(recipe.contains("# Private-Network Proxy Dry Run"));
        assertTrue(recipe.contains("PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md"));
        assertTrue(recipe.contains("mvn -Dtest=PrivateNetworkProxyDryRunEvidenceTest test"));
        assertTrue(recipe.contains("loadbalancerpro.proxy.private-network-validation.enabled=true"));
        assertTrue(recipe.contains("target/proxy-evidence/private-network-validation-dry-run.md"));
        assertTrue(recipe.contains("target/proxy-evidence/private-network-validation-dry-run.json"));
        assertTrue(recipe.contains("http://127.0.0.1:18081"));
        assertTrue(recipe.contains("http://10.1.2.3:18082"));
        assertTrue(recipe.contains("PUBLIC_NETWORK_REJECTED"));
        assertTrue(recipe.contains("AMBIGUOUS_HOST_REJECTED"));
        assertTrue(recipe.contains("USERINFO_REJECTED"));
        assertTrue(recipe.contains("UNSUPPORTED_SCHEME_REJECTED"));
        assertTrue(recipe.contains("INVALID_REJECTED"));
        assertTrue(recipe.contains("trafficSent=false"));
        assertTrue(recipe.contains("dnsResolution=false"));
        assertTrue(recipe.contains("reachabilityChecks=false"));
        assertTrue(recipe.contains("portScanning=false"));
        assertTrue(recipe.contains("postmanExecution=false"));
        assertTrue(recipe.contains("smokeExecution=false"));
        assertTrue(recipe.contains("apiKeyPersisted=false"));
        assertTrue(recipe.contains("secretPersisted=false"));
        assertTrue(recipe.contains("failClosedBeforeActiveConfig=true"));
        assertTrue(normalized.contains("config-validation-only"));
        assertTrue(normalized.contains("does not start private-network smoke"));
        assertTrue(normalized.contains("does not change default/local/demo forwarding behavior"));
        assertTrue(normalized.contains("ignored maven `target/` output"));
        assertTrue(normalized.contains("not tracked documentation artifacts"));
        assertTrue(normalized.contains("must not contain api keys"));
        assertTrue(normalized.contains("does not add live private-network traffic"));
        assertTrue(normalized.contains("explicit operator approval"));
        assertTrue(recipe.contains("loadbalancerpro.proxy.private-network-live-validation.enabled=false"));
        assertTrue(recipe.contains("loadbalancerpro.proxy.private-network-live-validation.operator-approved=false"));
        assertTrue(normalized.contains("continued no-dns/no-discovery/no-scanning rules"));
    }

    @Test
    void privateNetworkDryRunEvidenceTestStaysOfflineAndSourceVisible() throws Exception {
        String source = read(DRY_RUN_TEST);

        for (String forbidden : List.of(
                "Inet" + "Address",
                "get" + "ByName",
                "Http" + "Client",
                "URL" + "Connection",
                "new " + "Socket",
                "Datagram" + "Socket",
                ".connect(")) {
            assertFalse(source.contains(forbidden), "dry-run evidence test must stay offline; found " + forbidden);
        }
    }

    @Test
    void reviewerDocsLinkPlanAndDescribeBoundaries() throws Exception {
        String trustMap = read(TRUST_MAP);
        String liveProxyContainment = read(LIVE_PROXY_CONTAINMENT);
        String runbook = read(RUNBOOK);
        String combined = trustMap + "\n" + liveProxyContainment + "\n" + runbook;
        String normalized = combined.toLowerCase(Locale.ROOT);

        assertTrue(trustMap.contains("PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md"));
        assertTrue(trustMap.contains("PRIVATE_NETWORK_PROXY_DRY_RUN.md"));
        assertTrue(trustMap.contains("PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md"));
        assertTrue(liveProxyContainment.contains("PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md"));
        assertTrue(liveProxyContainment.contains("PRIVATE_NETWORK_PROXY_DRY_RUN.md"));
        assertTrue(liveProxyContainment.contains("PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md"));
        assertTrue(runbook.contains("PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md"));
        assertTrue(runbook.contains("PRIVATE_NETWORK_PROXY_DRY_RUN.md"));
        assertTrue(runbook.contains("PRIVATE_NETWORK_LIVE_VALIDATION_GATE.md"));
        assertTrue(normalized.contains("explicit operator-provided backend urls only"));
        assertTrue(normalized.contains("local/private-network allowlisting"));
        assertTrue(normalized.contains("offline `proxybackendurlclassifier` review"));
        assertTrue(normalized.contains("opt-in startup/reload configuration validation"));
        assertTrue(normalized.contains("configuration-only at this stage"));
        assertTrue(normalized.contains("config-only classifier evidence without traffic"));
        assertTrue(normalized.contains("explicit operator approval before live traffic"));
        assertTrue(normalized.contains("default-off live gate properties"));
        assertTrue(normalized.contains("privatenetworklivevalidationgate"));
        assertTrue(normalized.contains("privatenetworklivevalidationexecutor"));
        assertTrue(normalized.contains("junit-only loopback"));
        assertTrue(normalized.contains("rejects unsafe validation paths before transport"));
        assertTrue(normalized.contains("allowlists validation request/response summary headers"));
        assertTrue(normalized.contains("reports redirects without following public `location` targets"));
        assertTrue(normalized.contains("fail-closed reload/startup behavior"));
        assertTrue(normalized.contains("target/proxy-evidence/private-network-validation-dry-run.md"));
        assertTrue(normalized.contains("target/proxy-evidence/private-network-validation-dry-run.json"));
        assertTrue(normalized.contains("target/proxy-evidence/private-network-live-loopback-validation.md"));
        assertTrue(normalized.contains("target/proxy-evidence/private-network-live-loopback-validation.json"));
        assertTrue(normalized.contains("/api/proxy/status.privatenetworklivevalidation"));
        assertTrue(normalized.contains("post /api/proxy/private-network-live-validation"));
        assertTrue(normalized.contains("trafficexecuted=false"));
        assertTrue(normalized.contains("evidencewritten=false"));
        assertTrue(normalized.contains("target/proxy-evidence/private-network-live-validation.md"));
        assertTrue(normalized.contains("target/proxy-evidence/private-network-live-validation.json"));
        assertTrue(normalized.contains("target/proxy-evidence/private-network-live-validation-audit.jsonl"));
        assertTrue(normalized.contains("audittrail.audittrailwritten=false"));
        assertTrue(normalized.contains("not-wired"));
        assertTrue(normalized.contains("report-only gate visibility"));
        assertTrue(normalized.contains("command contract"));
        assertTrue(normalized.contains("no dns or reachability checks"));
        assertTrue(normalized.contains("no discovery or scanning"));
        assertTrue(normalized.contains("no secret persistence"));
        assertTrue(normalized.contains("runtime/private-lan live traffic execution"));
    }

    @Test
    void privateNetworkValidationIsNotAddedToSmokeOrPostmanExecution() throws Exception {
        String combined = readTree(SMOKE_SCRIPTS) + "\n" + readTree(POSTMAN_DOCS);

        assertFalse(combined.contains("private-network-validation"),
                "smoke/Postman paths must not add private-network validation execution");
        assertFalse(combined.contains("private-network-live-validation"),
                "smoke/Postman paths must not add private-network live execution");
        assertFalse(combined.contains("ProxyBackendUrlClassifier"),
                "smoke/Postman paths must not invoke the Java classifier directly");
        assertFalse(combined.contains("PrivateNetworkLiveValidationExecutor"),
                "smoke/Postman paths must not invoke the live executor directly");
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String readTree(Path root) throws IOException {
        assertTrue(Files.exists(root), root + " should exist");
        StringBuilder content = new StringBuilder();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                content.append(read(path)).append('\n');
            }
        }
        return content.toString();
    }
}
