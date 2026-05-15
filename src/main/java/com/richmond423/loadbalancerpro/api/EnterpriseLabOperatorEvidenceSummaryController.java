package com.richmond423.loadbalancerpro.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enterprise-lab")
public class EnterpriseLabOperatorEvidenceSummaryController {
    @GetMapping("/operator-evidence-summary")
    public OperatorEvidenceSummary operatorEvidenceSummary() {
        return new OperatorEvidenceSummary(
                "/operator-evidence-dashboard.html",
                "/enterprise-lab-reviewer.html",
                "/api/enterprise-lab/reviewer-summary",
                new EvidencePaths(
                        "target/enterprise-lab-runs/",
                        "target/container-dry-run-evidence/",
                        "ignored generated target output; not committed source"),
                new CiArtifact(
                        "container-dry-run-evidence-no-publish-no-sign",
                        "target/container-dry-run-evidence/"),
                List.of(
                        "mvn -q test",
                        "mvn -q -DskipTests package",
                        "mvn -B package",
                        "git diff --check",
                        "powershell -NoProfile -ExecutionPolicy Bypass -File .\\scripts\\smoke\\enterprise-lab-workflow.ps1 -Package"),
                List.of(
                        "local tests, package build, and Enterprise Lab smoke can pass",
                        "CI can publish a no-publish/no-sign dry-run evidence artifact",
                        "local dashboards summarize evidence paths and boundaries",
                        "documentation and tests guard against readiness overclaims"),
                List.of(
                        "production certification",
                        "live cloud validation",
                        "private-network production validation",
                        "real tenant/IdP proof",
                        "registry publication",
                        "container signing",
                        "production SLO/SLA proof",
                        "GitHub governance enforcement by repo files alone"),
                List.of(
                        "generated evidence under target/ remains ignored output",
                        "generated evidence should not be committed",
                        "container publication and signing require separate approval",
                        "cloud and GitHub settings stay outside this dashboard"));
    }

    public record OperatorEvidenceSummary(
            String dashboardPath,
            String reviewerDashboardPath,
            String reviewerSummaryApi,
            EvidencePaths evidencePaths,
            CiArtifact ciArtifact,
            List<String> commands,
            List<String> proves,
            List<String> doesNotProve,
            List<String> safetyBoundaries) {
    }

    public record EvidencePaths(
            String enterpriseLabRuns,
            String containerDryRunEvidence,
            String sourceControlBoundary) {
    }

    public record CiArtifact(
            String name,
            String evidenceDirectory) {
    }
}
