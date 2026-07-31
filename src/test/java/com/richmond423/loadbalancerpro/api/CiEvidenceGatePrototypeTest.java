package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
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
class CiEvidenceGatePrototypeTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void gateSummaryRemainsStaticLocalAndNotEnforced() throws Exception {
        mockMvc.perform(get("/api/enterprise-lab/ci-evidence-gate-summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.artifactKind", is("ci-evidence-gate-summary")))
                .andExpect(jsonPath("$.mode", is("prototype/local-review")))
                .andExpect(jsonPath("$.decision", is("READY_FOR_LOCAL_REVIEW")))
                .andExpect(jsonPath("$.enforcementStatus", is("NOT_ENFORCED")))
                .andExpect(jsonPath("$.apiPath",
                        is("/api/enterprise-lab/ci-evidence-gate-summary")))
                .andExpect(jsonPath("$.requiredEvidenceInputs", hasSize(8)))
                .andExpect(jsonPath("$.localEvidencePaths", hasSize(7)))
                .andExpect(jsonPath("$.localEvidencePaths[*]",
                        everyItem(startsWith("target/"))))
                .andExpect(jsonPath("$.readinessChecks", hasSize(5)))
                .andExpect(jsonPath("$.safetyBoundaries",
                        hasSize(8)));
    }
}
