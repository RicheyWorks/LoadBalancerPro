package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class EvidenceTrainingScorecardService {
    private static final String SCORECARD_VERSION = "1";
    private static final double DEFAULT_PASSING_PERCENT = 80.0;
    private static final String RESOURCE_ROOT = "evidence-policies/scorecards/";
    private static final List<String> SCORECARD_RESOURCE_NAMES = List.of(
            "strict-zero-drift-pass.json",
            "strict-zero-drift-fail.json",
            "receiver-redaction-warn.json",
            "audit-append-warn.json",
            "regulated-handoff-pass.json",
            "regulated-handoff-fail.json",
            "investigation-working-copy-warn.json");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in", "into",
            "is", "it", "of", "on", "or", "the", "this", "to", "under", "with");
    private static final ObjectMapper SCORECARD_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    List<TrainingScorecard> listScorecards() throws IOException {
        List<TrainingScorecard> scorecards = new ArrayList<>();
        for (String resourceName : SCORECARD_RESOURCE_NAMES) {
            scorecards.add(readScorecardResource(resourceName));
        }
        return scorecards.stream()
                .sorted(Comparator.comparing(TrainingScorecard::exerciseName))
                .toList();
    }

    Optional<TrainingScorecard> findScorecard(String exerciseName) throws IOException {
        if (exerciseName == null || exerciseName.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(exerciseName);
        for (TrainingScorecard scorecard : listScorecards()) {
            if (normalize(scorecard.exerciseName()).equals(normalized)) {
                return Optional.of(scorecard);
            }
        }
        return Optional.empty();
    }

    String renderScorecardList() throws IOException {
        StringBuilder builder = new StringBuilder("Available evidence training scorecards:")
                .append(System.lineSeparator());
        for (TrainingScorecard scorecard : listScorecards()) {
            builder.append("- ")
                    .append(scorecard.exerciseName())
                    .append(": template=")
                    .append(scorecard.templateName())
                    .append(", expectedDecision=")
                    .append(scorecard.expectedDecision())
                    .append(" - ")
                    .append(scorecard.prompt())
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    String renderScorecard(String exerciseName) throws IOException {
        TrainingScorecard scorecard = requireScorecard(exerciseName);
        StringBuilder builder = new StringBuilder("Evidence training scorecard: ")
                .append(scorecard.exerciseName())
                .append(System.lineSeparator())
                .append("- template: ")
                .append(scorecard.templateName())
                .append(System.lineSeparator())
                .append("- expectedDecision: ")
                .append(scorecard.expectedDecision())
                .append(System.lineSeparator())
                .append("- prompt: ")
                .append(scorecard.prompt())
                .append(System.lineSeparator())
                .append("- expectedPrimaryReason: ")
                .append(scorecard.expectedPrimaryReason())
                .append(System.lineSeparator())
                .append("- acceptableActions:")
                .append(System.lineSeparator());
        for (String action : scorecard.acceptableActions()) {
            builder.append("  - ").append(action).append(System.lineSeparator());
        }
        builder.append("- remediationNotes:").append(System.lineSeparator());
        for (String note : scorecard.remediationNotes()) {
            builder.append("  - ").append(note).append(System.lineSeparator());
        }
        builder.append("- scoring: decision=")
                .append(scorecard.scoring().decisionPoints())
                .append(", reason=")
                .append(scorecard.scoring().reasonPoints())
                .append(", action=")
                .append(scorecard.scoring().actionPoints())
                .append(System.lineSeparator());
        return builder.toString();
    }

    ScorecardGradeResult grade(Path answersPath, Optional<Double> passingScoreOverride) throws IOException {
        Objects.requireNonNull(answersPath, "scorecard answers path cannot be null");
        ScorecardAnswers answers = readAnswers(answersPath);
        return grade(answers, passingScoreOverride);
    }

    ScorecardGradeResult grade(ScorecardAnswers answers, Optional<Double> passingScoreOverride) throws IOException {
        Objects.requireNonNull(answers, "scorecard answers cannot be null");
        List<TrainingScorecard> scorecards = listScorecards();
        Map<String, TrainingScorecard> scorecardsByName = new LinkedHashMap<>();
        for (TrainingScorecard scorecard : scorecards) {
            scorecardsByName.put(scorecard.exerciseName(), scorecard);
        }

        Map<String, OperatorAnswer> answersByExercise = answersByExercise(answers, scorecardsByName.keySet());
        List<PerExerciseGrade> perExercise = new ArrayList<>();
        int totalScore = 0;
        int maxScore = 0;
        for (TrainingScorecard scorecard : scorecards) {
            OperatorAnswer answer = answersByExercise.get(scorecard.exerciseName());
            PerExerciseGrade grade = gradeExercise(scorecard, answer);
            perExercise.add(grade);
            totalScore += grade.score();
            maxScore += grade.maxScore();
        }
        double percent = maxScore == 0 ? 0.0 : roundPercent((totalScore * 100.0) / maxScore);
        double passingScore = passingScoreOverride.orElse(DEFAULT_PASSING_PERCENT);
        return new ScorecardGradeResult(
                SCORECARD_VERSION,
                blankToNull(answers.operator()),
                scorecards.size(),
                totalScore,
                maxScore,
                percent,
                percent >= passingScore,
                passingScore,
                perExercise);
    }

    String renderJson(ScorecardGradeResult result) throws IOException {
        return SCORECARD_MAPPER.writeValueAsString(result) + System.lineSeparator();
    }

    String renderMarkdown(ScorecardGradeResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# LoadBalancerPro Evidence Training Scorecard")
                .append(System.lineSeparator())
                .append(System.lineSeparator());
        builder.append("- Scorecard version: ").append(result.scorecardVersion()).append(System.lineSeparator());
        if (result.operator() != null) {
            builder.append("- Operator: ").append(escapeMarkdown(result.operator())).append(System.lineSeparator());
        }
        builder.append("- Total exercises: ").append(result.totalExercises()).append(System.lineSeparator());
        builder.append("- Total score: ").append(result.totalScore()).append(System.lineSeparator());
        builder.append("- Max score: ").append(result.maxScore()).append(System.lineSeparator());
        builder.append("- Percent: ").append(formatPercent(result.percent())).append(System.lineSeparator());
        builder.append("- Passing score: ").append(formatPercent(result.passingScore())).append(System.lineSeparator());
        builder.append("- Passed: ").append(result.passed()).append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("## Exercise Results").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("| Exercise | Expected | Actual | Decision | Reason Credit | Action Credit | Score | Feedback |")
                .append(System.lineSeparator());
        builder.append("| --- | --- | --- | --- | --- | --- | --- | --- |").append(System.lineSeparator());
        for (PerExerciseGrade exercise : result.perExercise()) {
            builder.append("| ")
                    .append(escapeMarkdown(exercise.exerciseName()))
                    .append(" | ")
                    .append(exercise.expectedDecision())
                    .append(" | ")
                    .append(escapeMarkdown(exercise.actualDecision()))
                    .append(" | ")
                    .append(exercise.decisionCorrect() ? "correct" : "review")
                    .append(" | ")
                    .append(exercise.reasonCredit())
                    .append(" | ")
                    .append(exercise.actionCredit())
                    .append(" | ")
                    .append(exercise.score())
                    .append("/")
                    .append(exercise.maxScore())
                    .append(" | ")
                    .append(escapeMarkdown(exercise.feedback()))
                    .append(" |")
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    int exitCode(ScorecardGradeResult result, Optional<Double> failOnScoreBelow) {
        Objects.requireNonNull(result, "scorecard grade result cannot be null");
        return failOnScoreBelow.isPresent() && result.percent() < failOnScoreBelow.get() ? 2 : 0;
    }

    private ScorecardAnswers readAnswers(Path answersPath) throws IOException {
        try {
            JsonNode root = SCORECARD_MAPPER.readTree(answersPath.toFile());
            JsonNode answerNodes = root.path("answers");
            if (!answerNodes.isArray()) {
                throw new IllegalArgumentException("scorecard answers must include an answers array");
            }
            List<OperatorAnswer> answers = new ArrayList<>();
            for (JsonNode answerNode : answerNodes) {
                answers.add(new OperatorAnswer(
                        text(answerNode, "exerciseName", null),
                        text(answerNode, "decision", null),
                        text(answerNode, "reason", null),
                        text(answerNode, "action", null),
                        text(answerNode, "notes", null)));
            }
            return new ScorecardAnswers(text(root, "operator", null), answers);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException("failed to read scorecard answers " + answersPath + ": " + safeMessage(e), e);
        }
    }

    private Map<String, OperatorAnswer> answersByExercise(
            ScorecardAnswers answers,
            Set<String> knownExerciseNames) {
        Map<String, OperatorAnswer> answersByExercise = new LinkedHashMap<>();
        for (OperatorAnswer answer : answers.answers()) {
            if (answer.exerciseName() == null || answer.exerciseName().isBlank()) {
                throw new IllegalArgumentException("scorecard answer exerciseName is required");
            }
            String matchedName = knownExerciseNames.stream()
                    .filter(name -> normalize(name).equals(normalize(answer.exerciseName())))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "unknown training scorecard exercise: " + answer.exerciseName()));
            if (answersByExercise.containsKey(matchedName)) {
                throw new IllegalArgumentException("duplicate training scorecard answer: " + matchedName);
            }
            answersByExercise.put(matchedName, answer);
        }
        return answersByExercise;
    }

    private PerExerciseGrade gradeExercise(TrainingScorecard scorecard, OperatorAnswer answer) {
        int maxScore = scorecard.scoring().maxScore();
        if (answer == null) {
            return new PerExerciseGrade(
                    scorecard.exerciseName(),
                    scorecard.expectedDecision(),
                    "MISSING",
                    false,
                    false,
                    0,
                    false,
                    0,
                    0,
                    maxScore,
                    "No answer supplied; expected " + scorecard.expectedDecision()
                            + " with action " + scorecard.acceptableActions().get(0) + ".");
        }

        ScorecardDecision actualDecision = ScorecardDecision.parse(answer.decision());
        boolean decisionCorrect = actualDecision == scorecard.expectedDecision();
        int decisionCredit = decisionCorrect ? scorecard.scoring().decisionPoints() : 0;
        int reasonCredit = scoreText(answer.reason(), scorecard.expectedPrimaryReason(),
                scorecard.scoring().reasonPoints());
        int actionCredit = scoreAction(answer.action(), scorecard.acceptableActions(),
                scorecard.scoring().actionPoints());
        boolean reasonMatched = reasonCredit == scorecard.scoring().reasonPoints();
        boolean actionMatched = actionCredit == scorecard.scoring().actionPoints();
        int score = decisionCredit + reasonCredit + actionCredit;
        return new PerExerciseGrade(
                scorecard.exerciseName(),
                scorecard.expectedDecision(),
                actualDecision.name(),
                decisionCorrect,
                reasonMatched,
                reasonCredit,
                actionMatched,
                actionCredit,
                score,
                maxScore,
                feedback(scorecard, decisionCorrect, reasonMatched, actionMatched));
    }

    private static int scoreText(String actual, String expected, int maxPoints) {
        if (maxPoints <= 0 || actual == null || actual.isBlank()) {
            return 0;
        }
        String normalizedActual = normalizedPhrase(actual);
        String normalizedExpected = normalizedPhrase(expected);
        if (normalizedActual.equals(normalizedExpected)
                || normalizedActual.contains(normalizedExpected)) {
            return maxPoints;
        }
        Set<String> expectedTokens = significantTokens(expected);
        Set<String> actualTokens = significantTokens(actual);
        if (expectedTokens.isEmpty() || actualTokens.isEmpty()) {
            return 0;
        }
        int matchedTokens = 0;
        for (String token : expectedTokens) {
            if (actualTokens.contains(token)) {
                matchedTokens++;
            }
        }
        return Math.min(maxPoints, (int) Math.round(maxPoints * (matchedTokens / (double) expectedTokens.size())));
    }

    private static int scoreAction(String actual, List<String> acceptableActions, int maxPoints) {
        if (maxPoints <= 0 || actual == null || actual.isBlank()) {
            return 0;
        }
        String normalizedActual = normalizedPhrase(actual);
        for (String action : acceptableActions) {
            if (normalizedPhrase(action).equals(normalizedActual)) {
                return maxPoints;
            }
        }
        int bestCredit = 0;
        for (String action : acceptableActions) {
            bestCredit = Math.max(bestCredit, scoreText(actual, action, maxPoints));
        }
        return bestCredit;
    }

    private static String feedback(
            TrainingScorecard scorecard,
            boolean decisionCorrect,
            boolean reasonMatched,
            boolean actionMatched) {
        List<String> feedback = new ArrayList<>();
        feedback.add(decisionCorrect
                ? "Decision matched expected " + scorecard.expectedDecision()
                : "Expected decision " + scorecard.expectedDecision());
        feedback.add(reasonMatched
                ? "Reason matched expected primary reason"
                : "Expected reason: " + scorecard.expectedPrimaryReason());
        feedback.add(actionMatched
                ? "Action matched acceptable action"
                : "Acceptable action: " + scorecard.acceptableActions().get(0));
        if (!scorecard.remediationNotes().isEmpty()) {
            feedback.add("Expected note: " + scorecard.remediationNotes().get(0));
        }
        return String.join("; ", feedback) + ".";
    }

    private TrainingScorecard requireScorecard(String exerciseName) throws IOException {
        return findScorecard(exerciseName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "unknown training scorecard exercise: " + exerciseName));
    }

    private static TrainingScorecard readScorecardResource(String resourceName) throws IOException {
        try (InputStream input = resourceStream(RESOURCE_ROOT + resourceName)) {
            JsonNode root = SCORECARD_MAPPER.readTree(input);
            Scoring scoring = new Scoring(
                    intValue(root.path("scoring"), "decisionPoints"),
                    intValue(root.path("scoring"), "reasonPoints"),
                    intValue(root.path("scoring"), "actionPoints"));
            return new TrainingScorecard(
                    requiredText(root, "exerciseName"),
                    requiredText(root, "templateName"),
                    ScorecardDecision.parse(requiredText(root, "expectedDecision")),
                    requiredText(root, "expectedPrimaryReason"),
                    requiredText(root, "prompt"),
                    stringList(root.path("acceptableActions"), "acceptableActions"),
                    stringList(root.path("remediationNotes"), "remediationNotes"),
                    scoring);
        }
    }

    private static InputStream resourceStream(String resourcePath) throws IOException {
        InputStream input = EvidenceTrainingScorecardService.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IOException("missing evidence training scorecard resource: " + resourcePath);
        }
        return input;
    }

    private static int intValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.canConvertToInt() || value.asInt() < 0) {
            throw new IllegalArgumentException("scorecard scoring " + field + " must be a non-negative integer");
        }
        return value.asInt();
    }

    private static String requiredText(JsonNode node, String field) {
        String value = text(node, field, null);
        if (value == null) {
            throw new IllegalArgumentException("scorecard " + field + " is required");
        }
        return value;
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : fallback;
    }

    private static List<String> stringList(JsonNode node, String field) {
        if (!node.isArray()) {
            throw new IllegalArgumentException("scorecard " + field + " must be an array");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            if (!value.isTextual() || value.asText().isBlank()) {
                throw new IllegalArgumentException("scorecard " + field + " entries must be text");
            }
            values.add(value.asText().trim());
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("scorecard " + field + " must not be empty");
        }
        return values;
    }

    private static Set<String> significantTokens(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalizedPhrase(value).split(" ")) {
            if (!token.isBlank() && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String normalizedPhrase(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String normalize(String value) {
        return Objects.requireNonNull(value, "value cannot be null")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String escapeMarkdown(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace(System.lineSeparator(), " ");
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static double roundPercent(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    enum ScorecardFormat {
        MARKDOWN,
        JSON;

        static ScorecardFormat parse(String value) {
            String normalized = Objects.requireNonNull(value, "scorecard format is required")
                    .trim()
                    .toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "markdown", "md" -> MARKDOWN;
                case "json" -> JSON;
                default -> throw new IllegalArgumentException("scorecard format must be markdown or json");
            };
        }
    }

    enum ScorecardDecision {
        PASS,
        WARN,
        FAIL;

        static ScorecardDecision parse(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("scorecard decision is required");
            }
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            return switch (normalized) {
                case "PASS" -> PASS;
                case "WARN" -> WARN;
                case "FAIL" -> FAIL;
                default -> throw new IllegalArgumentException("scorecard decision must be PASS, WARN, or FAIL");
            };
        }
    }

    record TrainingScorecard(
            String exerciseName,
            String templateName,
            ScorecardDecision expectedDecision,
            String expectedPrimaryReason,
            String prompt,
            List<String> acceptableActions,
            List<String> remediationNotes,
            Scoring scoring) {

        TrainingScorecard {
            Objects.requireNonNull(exerciseName, "exercise name cannot be null");
            Objects.requireNonNull(templateName, "template name cannot be null");
            Objects.requireNonNull(expectedDecision, "expected decision cannot be null");
            Objects.requireNonNull(expectedPrimaryReason, "expected primary reason cannot be null");
            Objects.requireNonNull(prompt, "prompt cannot be null");
            acceptableActions = acceptableActions == null ? List.of() : List.copyOf(acceptableActions);
            remediationNotes = remediationNotes == null ? List.of() : List.copyOf(remediationNotes);
            Objects.requireNonNull(scoring, "scoring cannot be null");
        }
    }

    record Scoring(int decisionPoints, int reasonPoints, int actionPoints) {
        Scoring {
            if (decisionPoints < 0 || reasonPoints < 0 || actionPoints < 0) {
                throw new IllegalArgumentException("scorecard points cannot be negative");
            }
        }

        int maxScore() {
            return decisionPoints + reasonPoints + actionPoints;
        }
    }

    record ScorecardAnswers(String operator, List<OperatorAnswer> answers) {
        ScorecardAnswers {
            answers = answers == null ? List.of() : List.copyOf(answers);
        }
    }

    record OperatorAnswer(
            String exerciseName,
            String decision,
            String reason,
            String action,
            String notes) {
    }

    record ScorecardGradeResult(
            String scorecardVersion,
            String operator,
            int totalExercises,
            int totalScore,
            int maxScore,
            double percent,
            boolean passed,
            double passingScore,
            List<PerExerciseGrade> perExercise) {

        ScorecardGradeResult {
            perExercise = perExercise == null ? List.of() : List.copyOf(perExercise);
        }
    }

    record PerExerciseGrade(
            String exerciseName,
            ScorecardDecision expectedDecision,
            String actualDecision,
            boolean decisionCorrect,
            boolean reasonMatched,
            int reasonCredit,
            boolean actionMatched,
            int actionCredit,
            int score,
            int maxScore,
            String feedback) {
    }
}
