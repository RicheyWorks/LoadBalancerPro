package com.richmond423.loadbalancerpro.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enterprise-lab")
public class EnterpriseLabCiEvidenceGateSummaryController {
    @GetMapping("/ci-evidence-gate-summary")
    public CiEvidenceGateSummary ciEvidenceGateSummary() {
        return new CiEvidenceGateSummary(
                "CI Evidence Gate Prototype",
                "ci-evidence-gate-artifact/v1",
                "ci-evidence-gate-summary",
                "docs/CI_EVIDENCE_GATE_ARTIFACT_CONTRACT.md",
                "docs/examples/ci-evidence-gate-summary.template.json",
                "prototype/local-review",
                "READY_FOR_LOCAL_REVIEW",
                "NOT_ENFORCED",
                "/ci-evidence-gate.html",
                "/api/enterprise-lab/ci-evidence-gate-summary",
                List.of(
                        "/enterprise-lab-reviewer.html",
                        "/operator-evidence-dashboard.html",
                        "/evidence-timeline.html",
                        "/evidence-export-packet.html"),
                List.of(
                        new EvidenceInput(
                                "Enterprise Lab workflow run",
                                "target/enterprise-lab-runs/enterprise-lab-run-summary.md",
                                "scripts/smoke/enterprise-lab-workflow.ps1 -Package",
                                "lab evidence only / not production activation"),
                        new EvidenceInput(
                                "Controlled adaptive-routing policy",
                                "target/controlled-adaptive-routing/controlled-adaptive-routing-policy-summary.md",
                                "scripts/smoke/controlled-adaptive-routing-policy.ps1 -Package",
                                "off, shadow, recommend, and guarded active-experiment review"),
                        new EvidenceInput(
                                "Enterprise Lab observability pack",
                                "target/enterprise-lab-observability/observability-summary.md",
                                "scripts/smoke/enterprise-lab-observability-pack.ps1 -Package",
                                "process-local metrics and dashboard/alert templates only"),
                        new EvidenceInput(
                                "Measured local performance baseline",
                                "target/performance-baseline/performance-summary.md",
                                "scripts/smoke/performance-baseline.ps1 -Package",
                                "local loopback fixtures with warning-only thresholds"),
                        new EvidenceInput(
                                "Mocked enterprise auth proof",
                                "target/enterprise-auth-proof/enterprise-auth-proof-summary.md",
                                "scripts/smoke/enterprise-auth-proof.ps1 -Package",
                                "synthetic mock IdP/JWKS role-claim behavior"),
                        new EvidenceInput(
                                "Adaptive-routing experiment comparison",
                                "target/adaptive-routing-experiments/adaptive-routing-experiment.md",
                                "scripts/smoke/adaptive-routing-experiment.ps1 -Package",
                                "offline baseline versus shadow versus active-experiment comparison")),
                List.of(
                        "target/enterprise-lab-runs/",
                        "target/controlled-adaptive-routing/",
                        "target/enterprise-lab-observability/",
                        "target/performance-baseline/",
                        "target/enterprise-auth-proof/",
                        "target/adaptive-routing-experiments/"),
                List.of(
                        new ReadinessCheck(
                                "Evidence input catalog",
                                "PASS_STYLE",
                                "All candidate evidence inputs are named with ignored target/ paths.",
                                "A future gate can require these paths after schema and runtime cost approval."),
                        new ReadinessCheck(
                                "Local-only producers",
                                "PASS_STYLE",
                                "Prototype references existing local smoke scripts only.",
                                "Reviewers still run or inspect outputs manually today."),
                        new ReadinessCheck(
                                "Enforcement boundary",
                                "WARN_STYLE",
                                "Enforcement is not active and this is not a required GitHub check.",
                                "Branch protection, rulesets, required checks, and GitHub settings remain unchanged."),
                        new ReadinessCheck(
                                "Unsafe proof boundaries",
                                "PASS_STYLE",
                                "Not-proven boundaries are explicit for production, cloud, tenant, IdP, signing, registry, and governance claims.",
                                "Future automation should fail review when those claims appear as completed proof."),
                        new ReadinessCheck(
                                "Generated evidence boundary",
                                "FAIL_STYLE_BLOCKER",
                                "Generated evidence committed outside ignored target/ paths would block the future gate.",
                                "Reviewers still confirm this by diff review today.")),
                List.of(
                        "Confirm latest CI and CodeQL passed for the exact commit under review.",
                        "Run or inspect local Enterprise Lab smoke evidence when needed.",
                        "Confirm generated evidence stays under ignored target/ paths and is not committed.",
                        "Confirm release-downloads/ was not changed.",
                        "Confirm no branch protection, rulesets, required checks, GitHub settings, secrets, or environments changed.",
                        "Confirm no release, tag, registry publication, container signing, cloud mutation, private-network validation, real tenant validation, or real enterprise IdP validation occurred."),
                List.of(
                        "No production certification",
                        "No production performance proof",
                        "No production SLO/SLA proof",
                        "No live-cloud validation",
                        "No real tenant validation",
                        "No real enterprise IdP validation",
                        "No signed-container proof",
                        "No registry publish completion",
                        "No GitHub governance-applied proof"),
                List.of(
                        "static deterministic response",
                        "same-origin browser fetch only",
                        "no file reads",
                        "no process execution",
                        "no environment variable or secret reads",
                        "no external network calls",
                        "no filesystem mutation",
                        "no branch protection or required-check mutation"),
                List.of(
                        "Use this prototype for local reviewer orientation.",
                        "Keep evidence generators local and deterministic.",
                        "Define an evidence schema before implementing a real CI parser.",
                        "Approve failure policy and runtime cost before adding any CI enforcement.",
                        "Keep GitHub settings and branch protection changes outside this prototype."));
    }

    public record CiEvidenceGateSummary(
            String gateName,
            String artifactVersion,
            String artifactKind,
            String artifactContract,
            String artifactTemplatePath,
            String mode,
            String decision,
            String enforcementStatus,
            String dashboardPath,
            String apiPath,
            List<String> linkedReviewerPages,
            List<EvidenceInput> requiredEvidenceInputs,
            List<String> localEvidencePaths,
            List<ReadinessCheck> readinessChecks,
            List<String> manualReviewSteps,
            List<String> notProvenBoundaries,
            List<String> safetyBoundaries,
            List<String> recommendedNextSteps) {
    }

    public record EvidenceInput(
            String name,
            String localEvidencePath,
            String localProducer,
            String reviewPurpose) {
    }

    public record ReadinessCheck(
            String name,
            String state,
            String summary,
            String futureGateMeaning) {
    }
}
