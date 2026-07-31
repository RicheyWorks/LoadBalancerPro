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
class EvidenceTimelineViewTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void timelineApiKeepsOrderedEvidenceAndBoundaries() throws Exception {
        mockMvc.perform(get("/api/enterprise-lab/evidence-timeline"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dashboardPath", is("/evidence-timeline.html")))
                .andExpect(jsonPath("$.operatorEvidenceSummaryApi",
                        is("/api/enterprise-lab/operator-evidence-summary")))
                .andExpect(jsonPath("$.reviewerSummaryApi",
                        is("/api/enterprise-lab/reviewer-summary")))
                .andExpect(jsonPath("$.evidenceStages", hasSize(8)))
                .andExpect(jsonPath("$.evidenceStages[0].label", is("Source readiness docs")))
                .andExpect(jsonPath("$.runTemplateFields", hasSize(10)))
                .andExpect(jsonPath("$.doesNotProve", hasSize(8)))
                .andExpect(jsonPath("$.safetyBoundaries", hasSize(4)));
    }
}
