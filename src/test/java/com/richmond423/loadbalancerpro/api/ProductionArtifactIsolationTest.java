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

import org.junit.jupiter.api.Test;

class ProductionArtifactIsolationTest {
    private static final Path MAIN_JAVA = Path.of("src", "main", "java");
    private static final Path TEST_JAVA = Path.of("src", "test", "java");
    private static final Path CLI = Path.of(
            "com", "richmond423", "loadbalancerpro", "cli");
    private static final Path LAB = Path.of(
            "com", "richmond423", "loadbalancerpro", "lab");
    private static final Path GUI = Path.of(
            "com", "richmond423", "loadbalancerpro", "gui");

    private static final List<Path> PROOF_TOOL_SOURCES = List.of(
            CLI.resolve("EnterpriseLabAllocationProofCommand.java"),
            CLI.resolve("EnterpriseLabDurableRecoveryProofCommand.java"),
            CLI.resolve("EnterpriseLabEvidenceOwnershipProofCommand.java"),
            CLI.resolve("EnterpriseLabExperimentProofCommand.java"),
            CLI.resolve("EnterpriseLabIndependentSupervisorProofCommand.java"),
            LAB.resolve("EnterpriseLabAllocationProofReport.java"),
            LAB.resolve("EnterpriseLabAllocationProofRunner.java"),
            LAB.resolve("EnterpriseLabAllocationProofStateHolder.java"),
            LAB.resolve("EnterpriseLabDurableRecoveryProofReport.java"),
            LAB.resolve("EnterpriseLabDurableRecoveryProofRunner.java"),
            LAB.resolve("EnterpriseLabEvidenceOwnershipProofReport.java"),
            LAB.resolve("EnterpriseLabEvidenceOwnershipProofRunner.java"),
            LAB.resolve("EnterpriseLabExperimentProofExporter.java"),
            LAB.resolve("EnterpriseLabExperimentProofReport.java"),
            LAB.resolve("EnterpriseLabExperimentProofRunner.java"),
            LAB.resolve("EnterpriseLabIndependentSupervisorProofReport.java"),
            LAB.resolve("EnterpriseLabIndependentSupervisorProofRunner.java"));

    @Test
    void allFiveProofFamiliesLiveOnlyInTestSource() {
        for (Path source : PROOF_TOOL_SOURCES) {
            assertFalse(Files.exists(MAIN_JAVA.resolve(source)),
                    source + " must not remain in production source");
            assertTrue(Files.exists(TEST_JAVA.resolve(source)),
                    source + " must remain executable from test/tool source");
        }
        assertTrue(Files.exists(TEST_JAVA.resolve(
                CLI.resolve("EnterpriseLabProofToolsApplication.java"))));
    }

    @Test
    void productionApplicationHasNoProofDispatch() throws Exception {
        String application = read(MAIN_JAVA.resolve(Path.of(
                "com", "richmond423", "loadbalancerpro", "api",
                "LoadBalancerApiApplication.java")));
        assertFalse(application.contains("ProofCommand"));
        assertFalse(application.contains("--enterprise-lab-")
                && application.contains("-proof"));
    }

    @Test
    void javaFxDesktopSourceAndDependencyAreAbsentFromTheProject() throws Exception {
        String pom = read(Path.of("pom.xml"));
        assertFalse(pom.contains("<javafx.version>"));
        assertFalse(pom.contains("<groupId>org.openjfx</groupId>"));
        assertFalse(pom.contains("<artifactId>javafx-controls</artifactId>"));
        assertTrue(pom.contains("<id>lab</id>"));
        assertTrue(pom.contains("<finalName>${project.artifactId}-${project.version}-lab</finalName>"));
        assertTrue(pom.contains(
                "<mainClass>com.richmond423.loadbalancerpro.cli.LabToolsApplication</mainClass>"));
        assertTrue(pom.contains(
                "<include>com/richmond423/loadbalancerpro/api/proxy/**</include>"));
        assertTrue(pom.contains(
                "<excludeGroupIds>software.amazon.awssdk,com.google.code.gson,io.projectreactor</excludeGroupIds>"));
        assertFalse(pom.contains("com/richmond423/loadbalancerpro/gui/LoadBalancerGUI"));
        assertFalse(pom.contains("com/richmond423/loadbalancerpro/gui/ServerTableRow"));
        assertFalse(pom.contains("<excludeGroupIds>org.openjfx</excludeGroupIds>"));

        List<Path> javaFxImporters = new ArrayList<>();
        try (var sources = Files.walk(MAIN_JAVA)) {
            sources.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return read(path).contains("import javafx");
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .map(MAIN_JAVA::relativize)
                    .forEach(javaFxImporters::add);
        }
        assertEquals(List.of(), javaFxImporters.stream().sorted().toList());

        for (String retiredSource : List.of(
                "LoadBalancerGUI.java",
                "ServerTableRow.java",
                "GuiConfig.java",
                "ConfigLoader.java",
                "CliArgsParser.java",
                "CliArgs.java",
                "AddServerCommand.java",
                "FailServerCommand.java")) {
            assertFalse(Files.exists(MAIN_JAVA.resolve(GUI).resolve(retiredSource)),
                    retiredSource + " must be removed with the desktop simulator");
        }
        assertTrue(Files.exists(MAIN_JAVA.resolve(GUI).resolve("Command.java")),
                "CloudManager compatibility contract must remain");
        assertFalse(Files.exists(Path.of("src", "main", "java", "gui", "messages.properties")));
        assertFalse(Files.exists(Path.of("src", "main", "resources", "gui", "messages.properties")));
    }

    @Test
    void ciRunsProofToolsAndRejectsForbiddenJarContent() throws Exception {
        String workflow = read(Path.of(".github", "workflows", "ci.yml"));
        String releaseWorkflow = read(Path.of(
                ".github", "workflows", "release-artifacts.yml"));
        assertTrue(workflow.contains("Run test-scope independent-supervisor proof"));
        assertTrue(workflow.contains(
                "scripts/smoke/enterprise-lab-independent-supervisor-proof.ps1"));
        assertTrue(workflow.contains("EnterpriseLabAllocationProofRunnerTest"));
        assertTrue(workflow.contains("EnterpriseLabExperimentProofRunnerTest"));
        assertTrue(workflow.contains("EnterpriseLabEvidenceOwnershipProofCommandTest"));
        assertTrue(workflow.contains("EnterpriseLabDurableRecoveryProofCommandTest"));
        assertTrue(workflow.contains("BOOT-INF/lib/javafx"));
        assertTrue(workflow.contains("(cli|demo|gui|lab)"));
        assertTrue(workflow.contains("ServerMonitor"));
        assertTrue(workflow.contains("mvn -B -P lab -DskipTests package"));
        assertTrue(workflow.contains("resolve-executable-jar.sh --lab"));
        assertTrue(workflow.contains("-DincludeProvidedScope=false"));
        assertTrue(workflow.contains("mvn -B -DskipTests package"));
        assertTrue(releaseWorkflow.contains("Release artifact contains lab-only classes or dependencies."));
        assertTrue(releaseWorkflow.contains("-DincludeProvidedScope=false"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
