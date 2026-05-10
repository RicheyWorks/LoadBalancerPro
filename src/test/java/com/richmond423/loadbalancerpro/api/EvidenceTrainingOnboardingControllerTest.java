package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.core.CloudManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
class EvidenceTrainingOnboardingControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void onboardingSummaryIsDeterministicAndReadOnly() throws Exception {
        String first = mockMvc.perform(get("/api/evidence-training/onboarding"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.onboardingVersion", is("1")))
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.cloudMutation", is(false)))
                .andExpect(jsonPath("$.cloudManagerRequired", is(false)))
                .andExpect(jsonPath("$.apiServerRequiredForCli", is(false)))
                .andExpect(jsonPath("$.templates", hasSize(5)))
                .andExpect(jsonPath("$.examples", hasSize(7)))
                .andExpect(jsonPath("$.scorecards", hasSize(7)))
                .andExpect(jsonPath("$.sampleAnswerTemplate.answers[0].exerciseName",
                        is("receiver-redaction-warn")))
                .andExpect(jsonPath("$.safetyNotes[1]", is("Not certification.")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(get("/api/evidence-training/onboarding"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(first, second, "onboarding summary should not add timestamps or random ids");
        assertTrue(first.contains("POST /api/evidence-training/scorecards/grade"));
        assertTrue(first.contains("No cloud mutation."));
        assertTrue(first.contains("No CloudManager construction is required."));
    }

    @Test
    void templatesEndpointIsDeterministic() throws Exception {
        String first = mockMvc.perform(get("/api/evidence-training/templates"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].name", is("audit-append")))
                .andExpect(jsonPath("$[0].mode", is("ALLOWLIST")))
                .andExpect(jsonPath("$[4].name", is("strict-zero-drift")))
                .andExpect(jsonPath("$[4].mode", is("STRICT")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(get("/api/evidence-training/templates"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(first, second, "template listing should stay byte-stable for the same resources");
    }

    @Test
    void examplesAndScorecardsStayAlignedAndDeterministic() throws Exception {
        String examplesBody = mockMvc.perform(get("/api/evidence-training/examples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].name", is("audit-append-warn")))
                .andExpect(jsonPath("$[0].expectedDecision", is("WARN")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String scorecardsBody = mockMvc.perform(get("/api/evidence-training/scorecards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].exerciseName", is("audit-append-warn")))
                .andExpect(jsonPath("$[0].expectedDecision", is("WARN")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(examplesBody, mockMvc.perform(get("/api/evidence-training/examples"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertEquals(scorecardsBody, mockMvc.perform(get("/api/evidence-training/scorecards"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        Map<String, String> examplesByDecision = decisionsByName(examplesBody, "name");
        Map<String, String> scorecardsByDecision = decisionsByName(scorecardsBody, "exerciseName");
        assertEquals(examplesByDecision, scorecardsByDecision,
                "every packaged example should have a matching scorecard and expected decision");
    }

    @Test
    void scorecardDetailAndAnswerTemplateAreDeterministic() throws Exception {
        String detail = mockMvc.perform(get("/api/evidence-training/scorecards/receiver-redaction-warn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseName", is("receiver-redaction-warn")))
                .andExpect(jsonPath("$.templateName", is("receiver-redaction")))
                .andExpect(jsonPath("$.expectedDecision", is("WARN")))
                .andExpect(jsonPath("$.acceptableActions[0]", is("confirm redaction summary")))
                .andExpect(jsonPath("$.scoring.decisionPoints", is(5)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(detail, mockMvc.perform(get("/api/evidence-training/scorecards/receiver-redaction-warn"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        String template = mockMvc.perform(get(
                        "/api/evidence-training/scorecards/receiver-redaction-warn/answer-template"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operator", is("operator-label")))
                .andExpect(jsonPath("$.answers[0].exerciseName", is("receiver-redaction-warn")))
                .andExpect(jsonPath("$.answers[0].decision", is("WARN")))
                .andExpect(jsonPath("$.answers[0].action", is("confirm redaction summary")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(template, mockMvc.perform(get(
                        "/api/evidence-training/scorecards/receiver-redaction-warn/answer-template"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    @Test
    void gradeEndpointReturnsDeterministicPerfectScoreAndDoesNotConstructCloudManager() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(get("/api/evidence-training/onboarding"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cloudMutation", is(false)));
            mockMvc.perform(get("/api/evidence-training/scorecards/receiver-redaction-warn/answer-template"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answers[0].exerciseName", is("receiver-redaction-warn")));

            String first = mockMvc.perform(post("/api/evidence-training/scorecards/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(perfectAnswers()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.scorecardVersion", is("1")))
                    .andExpect(jsonPath("$.operator", is("operator-a")))
                    .andExpect(jsonPath("$.totalExercises", is(7)))
                    .andExpect(jsonPath("$.totalScore", is(70)))
                    .andExpect(jsonPath("$.maxScore", is(70)))
                    .andExpect(jsonPath("$.percent", is(100.0)))
                    .andExpect(jsonPath("$.passed", is(true)))
                    .andExpect(jsonPath("$.perExercise[0].exerciseName", is("audit-append-warn")))
                    .andExpect(jsonPath("$.perExercise[0].score", is(10)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String second = mockMvc.perform(post("/api/evidence-training/scorecards/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(perfectAnswers()))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertEquals(first, second, "grading should be deterministic for the same answers");
            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "training onboarding grading must not construct CloudManager");
        }
    }

    @Test
    void gradeEndpointAwardsDeterministicPartialReasonAndActionCredit() throws Exception {
        mockMvc.perform(post("/api/evidence-training/scorecards/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operator": "operator-a",
                                  "answers": [
                                    {
                                      "exerciseName": "audit-append-warn",
                                      "decision": "WARN",
                                      "reason": "audit anchor",
                                      "action": "verify audit",
                                      "notes": "partial practice answer"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore", is(7)))
                .andExpect(jsonPath("$.maxScore", is(70)))
                .andExpect(jsonPath("$.percent", is(10.0)))
                .andExpect(jsonPath("$.passed", is(false)))
                .andExpect(jsonPath("$.perExercise[0].decisionCorrect", is(true)))
                .andExpect(jsonPath("$.perExercise[0].reasonMatched", is(false)))
                .andExpect(jsonPath("$.perExercise[0].reasonCredit", is(1)))
                .andExpect(jsonPath("$.perExercise[0].actionMatched", is(false)))
                .andExpect(jsonPath("$.perExercise[0].actionCredit", is(1)))
                .andExpect(jsonPath("$.perExercise[0].score", is(7)))
                .andExpect(jsonPath("$.perExercise[1].actualDecision", is("MISSING")));
    }

    @Test
    void gradeEndpointCanUsePassingScoreOverrideWithoutWritingReportFiles() throws Exception {
        mockMvc.perform(post("/api/evidence-training/scorecards/grade?passingScore=101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfectAnswers()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percent", is(100.0)))
                .andExpect(jsonPath("$.passingScore", is(101.0)))
                .andExpect(jsonPath("$.passed", is(false)));
    }

    @Test
    void unknownScorecardReturnsControlledError() throws Exception {
        mockMvc.perform(get("/api/evidence-training/scorecards/missing-scorecard"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString(
                        "unknown training scorecard exercise: missing-scorecard")))
                .andExpect(jsonPath("$.path", is("/api/evidence-training/scorecards/missing-scorecard")));

        mockMvc.perform(post("/api/evidence-training/scorecards/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "exerciseName": "missing-scorecard",
                                      "decision": "PASS",
                                      "reason": "practice",
                                      "action": "continue handoff"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(
                        "unknown training scorecard exercise: missing-scorecard")))
                .andExpect(jsonPath("$.path", is("/api/evidence-training/scorecards/grade")));
    }

    @Test
    void malformedGradeRequestReturnsControlledError() throws Exception {
        mockMvc.perform(post("/api/evidence-training/scorecards/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operator\":\"operator-a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("answers array")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void openApiDocumentExposesEvidenceTrainingPaths() throws Exception {
        JsonNode docs = OBJECT_MAPPER.readTree(mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertFalse(docs.at("/paths/~1api~1evidence-training~1onboarding/get").isMissingNode());
        assertFalse(docs.at("/paths/~1api~1evidence-training~1templates/get").isMissingNode());
        assertFalse(docs.at("/paths/~1api~1evidence-training~1examples/get").isMissingNode());
        assertFalse(docs.at("/paths/~1api~1evidence-training~1scorecards/get").isMissingNode());
        assertFalse(docs.at("/paths/~1api~1evidence-training~1scorecards~1{name}/get").isMissingNode());
        assertFalse(docs.at("/paths/~1api~1evidence-training~1scorecards~1{name}~1answer-template/get")
                .isMissingNode());
        assertFalse(docs.at("/paths/~1api~1evidence-training~1scorecards~1grade/post").isMissingNode());
    }

    @Test
    void postmanCollectionJsonIsValidAndContainsEvidenceTrainingRequests() throws Exception {
        JsonNode collection = OBJECT_MAPPER.readTree(Files.readString(
                Path.of("postman/LoadBalancerPro.postman_collection.json"), StandardCharsets.UTF_8));
        assertEquals("LoadBalancerPro", collection.at("/info/name").asText());
        assertEquals("http://localhost:8080", collection.at("/variable/0/value").asText());

        JsonNode trainingFolder = null;
        for (JsonNode item : collection.path("item")) {
            if ("Evidence Training Onboarding".equals(item.path("name").asText())) {
                trainingFolder = item;
                break;
            }
        }
        assertTrue(trainingFolder != null, "Postman collection should include evidence training folder");
        assertEquals(7, trainingFolder.path("item").size());
        assertEquals("GET Onboarding Summary", trainingFolder.at("/item/0/name").asText());
        assertEquals("GET Policy Templates", trainingFolder.at("/item/1/name").asText());
        assertEquals("GET Policy Examples", trainingFolder.at("/item/2/name").asText());
        assertEquals("GET Training Scorecards", trainingFolder.at("/item/3/name").asText());
        assertEquals("GET Scorecard Detail", trainingFolder.at("/item/4/name").asText());
        assertEquals("GET Scorecard Answer Template", trainingFolder.at("/item/5/name").asText());
        assertEquals("POST Grade Scorecard Answers", trainingFolder.at("/item/6/name").asText());
        assertEquals("{{baseUrl}}/api/evidence-training/scorecards/grade",
                trainingFolder.at("/item/6/request/url/raw").asText());
    }

    @Test
    void docsMentionPostmanOnboardingLimitsAndCliContinuity() throws Exception {
        String postmanDocs = Files.readString(Path.of("docs/POSTMAN_EVIDENCE_TRAINING.md"),
                StandardCharsets.UTF_8);
        String normalizedPostmanDocs = postmanDocs.toLowerCase(Locale.ROOT);
        String runbook = Files.readString(Path.of("docs/OPERATIONS_RUNBOOK.md"), StandardCharsets.UTF_8);
        String cliDocs = Files.readString(Path.of("docs/REMEDIATION_REPORT_CLI.md"), StandardCharsets.UTF_8);

        assertTrue(postmanDocs.contains("/api/evidence-training/onboarding"));
        assertTrue(postmanDocs.contains("postman/LoadBalancerPro.postman_collection.json"));
        assertTrue(normalizedPostmanDocs.contains("not certification"));
        assertTrue(normalizedPostmanDocs.contains("not legal compliance proof"));
        assertTrue(normalizedPostmanDocs.contains("not identity proof"));
        assertTrue(normalizedPostmanDocs.contains("no cloud mutation"));
        assertTrue(postmanDocs.contains("API server is optional for CLI workflows"));
        assertTrue(runbook.contains("/api/evidence-training/onboarding"));
        assertTrue(cliDocs.contains("--grade-training-scorecard"));
        assertTrue(cliDocs.contains("/api/evidence-training/scorecards/grade"));
    }

    private static Map<String, String> decisionsByName(String body, String nameField) throws Exception {
        Map<String, String> decisions = new LinkedHashMap<>();
        for (JsonNode node : OBJECT_MAPPER.readTree(body)) {
            decisions.put(node.path(nameField).asText(), node.path("expectedDecision").asText());
        }
        return decisions;
    }

    private static String perfectAnswers() {
        return """
                {
                  "operator": "operator-a",
                  "answers": [
                    {
                      "exerciseName": "audit-append-warn",
                      "decision": "WARN",
                      "reason": "Audit anchor advanced after receiver verification",
                      "action": "verify audit append",
                      "notes": "Verify that the appended audit entry is expected receiver-side verification activity and record the review."
                    },
                    {
                      "exerciseName": "investigation-working-copy-warn",
                      "decision": "WARN",
                      "reason": "Working notes and report edits require review",
                      "action": "review warning",
                      "notes": "Review the working notes and report edits, then document why the investigation handoff can continue."
                    },
                    {
                      "exerciseName": "receiver-redaction-warn",
                      "decision": "WARN",
                      "reason": "Receiver redaction changes require review",
                      "action": "confirm redaction summary",
                      "notes": "Confirm the redaction summary and document why the redacted evidence changes are acceptable before continuing."
                    },
                    {
                      "exerciseName": "regulated-handoff-fail",
                      "decision": "FAIL",
                      "reason": "Core evidence bundle is missing",
                      "action": "investigate missing evidence",
                      "notes": "Investigate the missing bundle, block the handoff, and request a corrected evidence package."
                    },
                    {
                      "exerciseName": "regulated-handoff-pass",
                      "decision": "PASS",
                      "reason": "No drift detected",
                      "action": "continue handoff",
                      "notes": "No remediation required; attach the clean regulated handoff review to the ticket."
                    },
                    {
                      "exerciseName": "strict-zero-drift-fail",
                      "decision": "FAIL",
                      "reason": "Report checksum drift violates strict zero-drift policy",
                      "action": "block handoff",
                      "notes": "Block the handoff and ask the sender to resolve or justify the changed report before retrying."
                    },
                    {
                      "exerciseName": "strict-zero-drift-pass",
                      "decision": "PASS",
                      "reason": "No drift detected",
                      "action": "continue handoff",
                      "notes": "No remediation required; record the zero-drift result and continue the handoff."
                    }
                  ]
                }
                """;
    }
}
