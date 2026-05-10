package com.richmond423.loadbalancerpro.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.core.CloudManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class EvidenceTrainingScorecardCliTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> EXPECTED_SCORECARDS = List.of(
            "audit-append-warn",
            "investigation-working-copy-warn",
            "receiver-redaction-warn",
            "regulated-handoff-fail",
            "regulated-handoff-pass",
            "strict-zero-drift-fail",
            "strict-zero-drift-pass");

    @TempDir
    Path tempDir;

    @Test
    void listTrainingScorecardsIsDeterministic() {
        CapturedRun first = runCli("--list-training-scorecards");
        CapturedRun second = runCli("--list-training-scorecards");

        assertEquals(0, first.result().exitCode());
        assertTrue(first.error().isBlank());
        assertEquals(first.output(), second.output());
        assertTrue(first.output().startsWith("Available evidence training scorecards:"));
        for (String scorecard : EXPECTED_SCORECARDS) {
            assertTrue(first.output().contains("- " + scorecard + ":"),
                    "list should include " + scorecard);
        }
    }

    @Test
    void printTrainingScorecardIsDeterministic() {
        CapturedRun first = runCli("--print-training-scorecard", "receiver-redaction-warn");
        CapturedRun second = runCli("--print-training-scorecard=receiver-redaction-warn");

        assertEquals(0, first.result().exitCode());
        assertEquals(first.output(), second.output());
        assertTrue(first.output().contains("Evidence training scorecard: receiver-redaction-warn"));
        assertTrue(first.output().contains("- expectedDecision: WARN"));
        assertTrue(first.output().contains("Receiver redaction changes require review"));
        assertTrue(first.output().contains("confirm redaction summary"));
    }

    @Test
    void perfectAnswersScoreOneHundredPercent() throws Exception {
        Path answers = writePerfectAnswers("operator-a");
        CapturedRun run = runCli("--grade-training-scorecard", answers.toString(),
                "--scorecard-format", "json");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.error().isBlank());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("1", report.path("scorecardVersion").asText());
        assertEquals("operator-a", report.path("operator").asText());
        assertEquals(7, report.path("totalExercises").asInt());
        assertEquals(70, report.path("totalScore").asInt());
        assertEquals(70, report.path("maxScore").asInt());
        assertEquals(100.0, report.path("percent").asDouble(), 0.0);
        assertTrue(report.path("passed").asBoolean());
        for (JsonNode exercise : report.path("perExercise")) {
            assertTrue(exercise.path("decisionCorrect").asBoolean());
            assertTrue(exercise.path("reasonMatched").asBoolean());
            assertTrue(exercise.path("actionMatched").asBoolean());
            assertEquals(10, exercise.path("score").asInt());
        }
    }

    @Test
    void wrongDecisionLosesDecisionPoints() throws Exception {
        Path answers = writePerfectAnswers("operator-a");
        ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(answers.toFile());
        for (JsonNode answer : root.path("answers")) {
            if ("strict-zero-drift-pass".equals(answer.path("exerciseName").asText())) {
                ((ObjectNode) answer).put("decision", "FAIL");
            }
        }
        Files.writeString(answers, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                StandardCharsets.UTF_8);

        CapturedRun run = runCli("--grade-training-scorecard", answers.toString(),
                "--scorecard-format", "json");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        JsonNode strictPass = exercise(report, "strict-zero-drift-pass");
        assertFalse(strictPass.path("decisionCorrect").asBoolean());
        assertEquals(5, strictPass.path("score").asInt());
        assertEquals(65, report.path("totalScore").asInt());
        assertEquals(92.9, report.path("percent").asDouble(), 0.0);
    }

    @Test
    void partialReasonAndActionEarnDeterministicPartialCredit() throws Exception {
        EvidenceTrainingScorecardService service = new EvidenceTrainingScorecardService();
        EvidenceTrainingScorecardService.ScorecardAnswers answers =
                new EvidenceTrainingScorecardService.ScorecardAnswers(
                        "operator-a",
                        List.of(new EvidenceTrainingScorecardService.OperatorAnswer(
                                "strict-zero-drift-fail",
                                "FAIL",
                                "checksum drift",
                                "block",
                                "Needs review")));

        EvidenceTrainingScorecardService.ScorecardGradeResult result =
                service.grade(answers, Optional.empty());
        EvidenceTrainingScorecardService.PerExerciseGrade strictFail = result.perExercise().stream()
                .filter(exercise -> "strict-zero-drift-fail".equals(exercise.exerciseName()))
                .findFirst()
                .orElseThrow();

        assertTrue(strictFail.decisionCorrect());
        assertFalse(strictFail.reasonMatched());
        assertFalse(strictFail.actionMatched());
        assertEquals(1, strictFail.reasonCredit());
        assertEquals(1, strictFail.actionCredit());
        assertEquals(7, strictFail.score());
    }

    @Test
    void failOnScoreBelowReturnsControlledNonZero() throws Exception {
        Path answers = writePerfectAnswers("operator-a");
        ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(answers.toFile());
        for (JsonNode answer : root.path("answers")) {
            if ("strict-zero-drift-pass".equals(answer.path("exerciseName").asText())) {
                ((ObjectNode) answer).put("decision", "FAIL");
            }
        }
        Files.writeString(answers, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                StandardCharsets.UTF_8);

        CapturedRun run = runCli("--grade-training-scorecard", answers.toString(),
                "--scorecard-format", "json",
                "--fail-on-score-below", "100");

        assertEquals(2, run.result().exitCode());
        assertTrue(run.error().isBlank());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals(92.9, report.path("percent").asDouble(), 0.0);
    }

    @Test
    void markdownGradingOutputIsDeterministic() throws Exception {
        Path answers = writePerfectAnswers("operator-a");

        CapturedRun first = runCli("--grade-training-scorecard", answers.toString());
        CapturedRun second = runCli("--grade-training-scorecard", answers.toString(),
                "--scorecard-format", "markdown");

        assertEquals(0, first.result().exitCode());
        assertEquals(first.output(), second.output());
        assertTrue(first.output().contains("# LoadBalancerPro Evidence Training Scorecard"));
        assertTrue(first.output().contains("| strict-zero-drift-pass | PASS | PASS | correct | 3 | 2 | 10/10 |"));
        assertTrue(first.output().contains("- Percent: 100.0"));
    }

    @Test
    void jsonGradingOutputIsDeterministic() throws Exception {
        Path answers = writePerfectAnswers("operator-a");

        CapturedRun first = runCli("--grade-training-scorecard", answers.toString(),
                "--scorecard-format", "json");
        CapturedRun second = runCli("--grade-training-scorecard", answers.toString(),
                "--scorecard-format", "json");

        assertEquals(0, first.result().exitCode());
        assertEquals(first.output(), second.output());
        JsonNode report = OBJECT_MAPPER.readTree(first.output());
        assertEquals(100.0, report.path("percent").asDouble(), 0.0);
    }

    @Test
    void scorecardOutputCanBeWrittenToFile() throws Exception {
        Path answers = writePerfectAnswers("operator-a");
        Path output = tempDir.resolve("scorecard.json");

        CapturedRun run = runCli("--grade-training-scorecard", answers.toString(),
                "--scorecard-format", "json",
                "--scorecard-output", output.toString());

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertEquals(100.0, OBJECT_MAPPER.readTree(output.toFile()).path("percent").asDouble(), 0.0);
    }

    @Test
    void malformedAnswersFileReturnsControlledError() throws Exception {
        Path answers = tempDir.resolve("malformed.json");
        Files.writeString(answers, "{ \"operator\" : \"operator-a\" }", StandardCharsets.UTF_8);

        CapturedRun run = runCli("--grade-training-scorecard", answers.toString());

        assertEquals(2, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("scorecard answers must include an answers array"));
    }

    @Test
    void unknownExerciseReturnsControlledError() throws Exception {
        Path answers = tempDir.resolve("unknown.json");
        Files.writeString(answers, """
                {
                  "answers" : [
                    {
                      "exerciseName" : "unknown-scorecard",
                      "decision" : "PASS",
                      "reason" : "No drift detected",
                      "action" : "continue handoff"
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);

        CapturedRun run = runCli("--grade-training-scorecard", answers.toString());

        assertEquals(2, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("unknown training scorecard exercise: unknown-scorecard"));
    }

    @Test
    void scorecardCommandsDoNotConstructCloudManager() throws Exception {
        Path answers = writePerfectAnswers("operator-a");
        try (MockedConstruction<CloudManager> mocked = Mockito.mockConstruction(CloudManager.class)) {
            CapturedRun list = runCli("--list-training-scorecards");
            CapturedRun print = runCli("--print-training-scorecard", "audit-append-warn");
            CapturedRun grade = runCli("--grade-training-scorecard", answers.toString());

            assertEquals(0, list.result().exitCode());
            assertEquals(0, print.result().exitCode());
            assertEquals(0, grade.result().exitCode());
            assertTrue(mocked.constructed().isEmpty());
        }
    }

    @Test
    void trainingScorecardFlagIsRecognized() {
        assertTrue(RemediationReportCli.isRequested(new String[]{"--list-training-scorecards"}));
        assertTrue(RemediationReportCli.isRequested(
                new String[]{"--print-training-scorecard", "strict-zero-drift-pass"}));
        assertTrue(RemediationReportCli.isRequested(
                new String[]{"--grade-training-scorecard=answers.json"}));
    }

    @Test
    void scorecardsStayAlignedWithPackagedExamples() throws Exception {
        EvidenceTrainingScorecardService scorecardService = new EvidenceTrainingScorecardService();
        EvidencePolicyExampleService exampleService = new EvidencePolicyExampleService();
        Map<String, EvidencePolicyExampleService.PolicyExample> examplesByName =
                exampleService.listExamples().stream()
                        .collect(Collectors.toMap(
                                EvidencePolicyExampleService.PolicyExample::name,
                                example -> example,
                                (left, right) -> left,
                                LinkedHashMap::new));

        for (EvidenceTrainingScorecardService.TrainingScorecard scorecard : scorecardService.listScorecards()) {
            EvidencePolicyExampleService.PolicyExample example = examplesByName.get(scorecard.exerciseName());
            assertTrue(example != null, "missing policy example for " + scorecard.exerciseName());
            assertEquals(example.templateName(), scorecard.templateName(), scorecard.exerciseName());
            assertEquals(example.expectedDecision(), scorecard.expectedDecision().name(), scorecard.exerciseName());
        }
        assertEquals(examplesByName.keySet(), scorecardService.listScorecards().stream()
                .map(EvidenceTrainingScorecardService.TrainingScorecard::exerciseName)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new)));
    }

    private Path writePerfectAnswers(String operator) throws Exception {
        EvidenceTrainingScorecardService service = new EvidenceTrainingScorecardService();
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("operator", operator);
        ArrayNode answers = root.putArray("answers");
        for (EvidenceTrainingScorecardService.TrainingScorecard scorecard : service.listScorecards()) {
            ObjectNode answer = answers.addObject();
            answer.put("exerciseName", scorecard.exerciseName());
            answer.put("decision", scorecard.expectedDecision().name());
            answer.put("reason", scorecard.expectedPrimaryReason());
            answer.put("action", scorecard.acceptableActions().get(0));
            answer.put("notes", scorecard.remediationNotes().get(0));
        }
        Path path = tempDir.resolve("answers-" + System.nanoTime() + ".json");
        Files.writeString(path, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                StandardCharsets.UTF_8);
        return path;
    }

    private static JsonNode exercise(JsonNode report, String exerciseName) {
        for (JsonNode exercise : report.path("perExercise")) {
            if (exerciseName.equals(exercise.path("exerciseName").asText())) {
                return exercise;
            }
        }
        throw new AssertionError("missing exercise " + exerciseName);
    }

    private CapturedRun runCli(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        RemediationReportCli.Result result = RemediationReportCli.run(args,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(error, true, StandardCharsets.UTF_8));
        return new CapturedRun(result, output.toString(StandardCharsets.UTF_8),
                error.toString(StandardCharsets.UTF_8));
    }

    private record CapturedRun(RemediationReportCli.Result result, String output, String error) {
    }
}
