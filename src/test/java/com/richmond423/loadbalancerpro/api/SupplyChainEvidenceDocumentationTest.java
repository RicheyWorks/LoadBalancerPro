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

class SupplyChainEvidenceDocumentationTest {
    private static final Path SUPPLY_CHAIN = Path.of("evidence/SUPPLY_CHAIN_EVIDENCE.md");
    private static final Path RELEASE_WORKFLOW = Path.of(".github/workflows/release-artifacts.yml");
    private static final Path CI_WORKFLOW = Path.of(".github/workflows/ci.yml");
    private static final Path CODEQL_WORKFLOW = Path.of(".github/workflows/codeql.yml");
    private static final Path DOCKERFILE = Path.of("Dockerfile");
    private static final Path POM = Path.of("pom.xml");
    private static final Path HARDENING_GUIDE = Path.of("docs/DEPLOYMENT_HARDENING_GUIDE.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path EXECUTIVE_SUMMARY = Path.of("docs/EXECUTIVE_SUMMARY.md");

    @Test
    void supplyChainEvidenceMatchesReleaseWorkflowPublicationModes() throws Exception {
        String evidence = read(SUPPLY_CHAIN);
        String workflow = read(RELEASE_WORKFLOW);

        assertTrue(evidence.contains("semantic tag pushes publish release assets"));
        assertTrue(evidence.contains("manual `workflow_dispatch` runs are non-publishing dry runs"));
        assertTrue(workflow.contains("workflow_dispatch:"));
        assertTrue(workflow.contains("RELEASE_PUBLISH=false"));
        assertTrue(workflow.contains("RELEASE_MODE=dry-run"));
        assertTrue(workflow.contains("Publish GitHub Release assets"));
        assertTrue(workflow.contains("Verify GitHub Release assets"));
        assertTrue(workflow.contains("gh release create"));
        assertTrue(workflow.contains("gh release upload"));
        assertTrue(workflow.contains("--verify-tag"));
        assertTrue(workflow.contains("if: steps.resolve-release.outputs.release_publish == 'true'"));
        assertFalse(workflow.contains("release-downloads"));
    }

    @Test
    void supplyChainEvidenceListsCurrentReleaseAssetsAttestationsAndChecksums() throws Exception {
        String evidence = read(SUPPLY_CHAIN);
        String workflow = read(RELEASE_WORKFLOW);

        for (String asset : List.of(
                "LoadBalancerPro-${version}.jar",
                "LoadBalancerPro-${version}-bom.json",
                "LoadBalancerPro-${version}-bom.xml",
                "LoadBalancerPro-${version}-SHA256SUMS.txt")) {
            assertTrue(evidence.contains(asset), "evidence should document release asset " + asset);
        }
        for (String asset : List.of(
                "LoadBalancerPro-${RELEASE_VERSION}.jar",
                "LoadBalancerPro-${RELEASE_VERSION}-bom.json",
                "LoadBalancerPro-${RELEASE_VERSION}-bom.xml",
                "LoadBalancerPro-${RELEASE_VERSION}-SHA256SUMS.txt")) {
            assertTrue(workflow.contains(asset), "workflow should produce release asset " + asset);
        }

        assertTrue(evidence.contains("Generating and verifying a SHA-256 checksum file"));
        assertTrue(workflow.contains("sha256sum -c"));
        assertTrue(evidence.contains("GitHub artifact attestations"));
        assertTrue(workflow.contains("actions/attest@v4.1.0"));
        assertTrue(workflow.contains("artifact-metadata: write"));
        assertTrue(workflow.contains("actions/upload-artifact@v7"));
        assertTrue(workflow.contains("retention-days: 90"));
    }

    @Test
    void supplyChainEvidenceListsCurrentMavenAndWorkflowControls() throws Exception {
        String evidence = read(SUPPLY_CHAIN);
        String pom = read(POM);
        String ci = read(CI_WORKFLOW);
        String codeql = read(CODEQL_WORKFLOW);
        String dockerfile = read(DOCKERFILE);

        for (String expected : List.of(
                "`maven-compiler-plugin` `3.15.0`",
                "`maven-surefire-plugin` `3.5.5`",
                "`exec-maven-plugin` `3.5.0`",
                "`jacoco-maven-plugin` `${jacoco.version}` (`0.8.13` at this evidence refresh)",
                "`maven-jar-plugin` `3.5.0`")) {
            assertTrue(evidence.contains(expected), "evidence should document " + expected);
        }
        for (String expected : List.of(
                "<version>3.15.0</version>",
                "<version>3.5.5</version>",
                "<version>3.5.0</version>",
                "<jacoco.version>0.8.13</jacoco.version>")) {
            assertTrue(pom.contains(expected), "pom should contain " + expected);
        }
        assertTrue(evidence.contains("Netty BOM"));
        assertTrue(pom.contains("<netty.version>4.2.13.Final</netty.version>"));
        assertTrue(ci.contains("cyclonedx-maven-plugin:2.9.1:makeAggregateBom"));
        assertTrue(ci.contains("aquasecurity/trivy-action@0.36.0"));
        assertTrue(ci.contains("actions/dependency-review-action@v5.0.0"));
        assertTrue(codeql.contains("name: CodeQL"));
        assertTrue(dockerfile.contains("@sha256:"));
        assertTrue(evidence.toLowerCase(Locale.ROOT).contains("digest-pinned")
                || evidence.toLowerCase(Locale.ROOT).contains("pinned by digest"));
    }

    @Test
    void reviewerDocsDistinguishReleaseFreeReviewFromSemanticTagPublication() throws Exception {
        String docs = read(HARDENING_GUIDE) + "\n" + read(TRUST_MAP) + "\n" + read(EXECUTIVE_SUMMARY);
        String normalized = docs.toLowerCase(Locale.ROOT);

        assertTrue(docs.contains("GitHub Release assets are published only by the separate semantic-tag "
                + "Release Artifacts workflow"));
        assertTrue(docs.contains("Release-free docs do not create tags, GitHub Releases, or release assets."));
        assertTrue(docs.contains("Semantic version tags can publish GitHub Release assets only through the "
                + "separate Release Artifacts workflow"));
        assertTrue(docs.contains("deterministic GitHub Release JAR/SBOM/checksum assets"));
        assertFalse(normalized.contains("production-grade"));
        assertFalse(normalized.contains("certified release"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
