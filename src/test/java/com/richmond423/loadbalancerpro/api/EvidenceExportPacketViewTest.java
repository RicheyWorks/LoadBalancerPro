package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EvidenceExportPacketViewTest {
    private static final Path CONTROLLER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/"
                    + "EnterpriseLabEvidenceExportPacketController.java");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exportPacketApiKeepsBrowserLocalHandoffContract() throws Exception {
        mockMvc.perform(get("/api/enterprise-lab/evidence-export-packet"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dashboardPath", is("/evidence-export-packet.html")))
                .andExpect(jsonPath("$.timelinePath", is("/evidence-timeline.html")))
                .andExpect(jsonPath("$.packetSections", hasSize(11)))
                .andExpect(jsonPath("$.packetTemplateFields", hasSize(15)))
                .andExpect(jsonPath("$.evidencePaths.enterpriseLabRuns",
                        is("target/enterprise-lab-runs/")))
                .andExpect(jsonPath("$.evidencePaths.containerDryRunEvidence",
                        is("target/container-dry-run-evidence/")))
                .andExpect(jsonPath("$.doesNotProve", hasSize(8)))
                .andExpect(jsonPath("$.safetyBoundaries[0]",
                        is("human-readable handoff template only")))
                .andExpect(jsonPath("$.safetyBoundaries[1]",
                        is("no actual export file generation")));
    }

    @Test
    void controllerStillCannotCreateServerSideExportFiles() throws Exception {
        String source = Files.readString(CONTROLLER, StandardCharsets.UTF_8);
        for (String prohibited : List.of(
                "Files.write",
                "FileOutputStream",
                "ZipOutputStream",
                "createFile",
                "writeString",
                "ProcessBuilder",
                "Runtime.getRuntime",
                "System.getenv")) {
            assertFalse(source.contains(prohibited),
                    "server-side export behavior is forbidden: " + prohibited);
        }
    }
}
