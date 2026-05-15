package com.richmond423.loadbalancerpro.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enterprise-lab")
public class EnterpriseLabEvidenceExportPacketController {
    @GetMapping("/evidence-export-packet")
    public EvidenceExportPacket evidenceExportPacket() {
        return new EvidenceExportPacket(
                "/evidence-export-packet.html",
                "/evidence-timeline.html",
                "/operator-evidence-dashboard.html",
                "/enterprise-lab-reviewer.html",
                List.of(
                        "project/version/commit",
                        "reviewer/date",
                        "local verification commands",
                        "Enterprise Lab smoke result",
                        "evidence paths",
                        "CI dry-run artifact",
                        "dashboard/API links checked",
                        "proof summary",
                        "not-proven boundaries",
                        "residual risks",
                        "follow-up gates"),
                new EvidencePaths(
                        "target/enterprise-lab-runs/",
                        "target/container-dry-run-evidence/",
                        "generated target output remains ignored and should not be committed"),
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
                        "Packet label",
                        "Commit SHA",
                        "Branch",
                        "Reviewer",
                        "Date",
                        "Commands run",
                        "Test/package result",
                        "Smoke result",
                        "Evidence paths",
                        "CI artifact name",
                        "Dashboard links checked",
                        "Proof summary",
                        "Not-proven boundaries",
                        "Residual risks",
                        "Follow-up gates"),
                List.of(
                        "reviewer packet can be assembled from existing local/CI evidence",
                        "evidence locations are known",
                        "verification commands are explicit",
                        "repeated evidence runs can be summarized consistently"),
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
                        "human-readable handoff template only",
                        "no actual export file generation",
                        "no secrets, tokens, or private keys belong in the packet",
                        "no external calls, process execution, or secret reads are needed",
                        "generated evidence under target/ remains ignored output"));
    }

    public record EvidenceExportPacket(
            String dashboardPath,
            String timelinePath,
            String operatorDashboardPath,
            String reviewerDashboardPath,
            List<String> packetSections,
            EvidencePaths evidencePaths,
            CiArtifact ciArtifact,
            List<String> verificationCommands,
            List<String> packetTemplateFields,
            List<String> proves,
            List<String> doesNotProve,
            List<String> safetyBoundaries) {
    }

    public record EvidencePaths(
            String enterpriseLabRuns,
            String containerDryRunEvidence,
            String generatedOutputBoundary) {
    }

    public record CiArtifact(
            String name,
            String evidenceDirectory) {
    }
}
