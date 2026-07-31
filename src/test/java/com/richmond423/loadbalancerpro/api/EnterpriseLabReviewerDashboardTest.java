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
class EnterpriseLabReviewerDashboardTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void reviewerSummaryRemainsDeterministicAndBounded() throws Exception {
        mockMvc.perform(get("/api/enterprise-lab/reviewer-summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posture.enterpriseLabReady", is(true)))
                .andExpect(jsonPath("$.posture.reviewerReadyEnterpriseLab", is(true)))
                .andExpect(jsonPath("$.posture.productionCertified", is(false)))
                .andExpect(jsonPath("$.posture.enterpriseProductionReady", is(false)))
                .andExpect(jsonPath("$.boundaries.noRegistryPublishClaim", is(true)))
                .andExpect(jsonPath("$.boundaries.noContainerSigningClaim", is(true)))
                .andExpect(jsonPath("$.boundaries.noLiveCloudValidationClaim", is(true)))
                .andExpect(jsonPath("$.boundaries.noRealTenantProofClaim", is(true)))
                .andExpect(jsonPath("$.ciArtifact.name",
                        is("container-dry-run-evidence-no-publish-no-sign")))
                .andExpect(jsonPath("$.ciArtifact.proves", hasSize(4)))
                .andExpect(jsonPath("$.ciArtifact.doesNotProve", hasSize(5)))
                .andExpect(jsonPath("$.dashboard.pagePath", is("/enterprise-lab-reviewer.html")));
    }
}
