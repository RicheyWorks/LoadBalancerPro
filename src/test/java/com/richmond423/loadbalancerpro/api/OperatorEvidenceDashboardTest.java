package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OperatorEvidenceDashboardTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void operatorSummaryKeepsEvidenceLocalAndGeneratedOutputIgnored() throws Exception {
        mockMvc.perform(get("/api/enterprise-lab/operator-evidence-summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dashboardPath", is("/operator-evidence-dashboard.html")))
                .andExpect(jsonPath("$.reviewerSummaryApi",
                        is("/api/enterprise-lab/reviewer-summary")))
                .andExpect(jsonPath("$.evidencePaths.enterpriseLabRuns",
                        is("target/enterprise-lab-runs/")))
                .andExpect(jsonPath("$.evidencePaths.containerDryRunEvidence",
                        is("target/container-dry-run-evidence/")))
                .andExpect(jsonPath("$.evidencePaths.sourceControlBoundary",
                        is("ignored generated target output; not committed source")))
                .andExpect(jsonPath("$.commands", hasSize(5)))
                .andExpect(jsonPath("$.doesNotProve", hasSize(8)))
                .andExpect(jsonPath("$.safetyBoundaries[0]",
                        is("generated evidence under target/ remains ignored output")));
    }
}
