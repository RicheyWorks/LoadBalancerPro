package com.richmond423.loadbalancerpro.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
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

@Service
public class EvidenceTrainingOnboardingService {
    private static final String ONBOARDING_VERSION = "1";
    private static final String SCORECARD_VERSION = "1";
    private static final double DEFAULT_PASSING_PERCENT = 80.0;
    private static final String TEMPLATE_ROOT = "evidence-policies/templates/";
    private static final String EXAMPLE_ROOT = "evidence-policies/examples/";
    private static final String SCORECARD_ROOT = "evidence-policies/scorecards/";

    private static final List<TemplateSource> TEMPLATE_SOURCES = List.of(
            new TemplateSource(
                    "strict-zero-drift",
                    "Final sender/receiver equality check; any unclassified drift fails.",
                    TEMPLATE_ROOT + "strict-zero-drift.json"),
            new TemplateSource(
                    "receiver-redaction",
                    "Receiver-side redaction handoff; redaction summaries are expected and redacted outputs warn.",
                    TEMPLATE_ROOT + "receiver-redaction.json"),
            new TemplateSource(
                    "audit-append",
                    "Receiver verification flow where audit log anchors may advance after handoff.",
                    TEMPLATE_ROOT + "audit-append.json"),
            new TemplateSource(
                    "regulated-handoff",
                    "Strict regulated evidence handoff profile with only documented summaries allowed.",
                    TEMPLATE_ROOT + "regulated-handoff.json"),
            new TemplateSource(
                    "investigation-working-copy",
                    "Active investigation profile that permits working notes while preserving core evidence failures.",
                    TEMPLATE_ROOT + "investigation-working-copy.json"));

    private static final List<ExampleSource> EXAMPLE_SOURCES = List.of(
            new ExampleSource(
                    "strict-zero-drift-pass",
                    "strict-zero-drift",
                    "PASS",
                    "Identical sender and receiver catalogs for a final zero-drift handoff.",
                    EXAMPLE_ROOT + "strict-zero-drift-pass/expected-decision.json"),
            new ExampleSource(
                    "strict-zero-drift-fail",
                    "strict-zero-drift",
                    "FAIL",
                    "A strict handoff where report checksum drift stops the transfer.",
                    EXAMPLE_ROOT + "strict-zero-drift-fail/expected-decision.json"),
            new ExampleSource(
                    "receiver-redaction-warn",
                    "receiver-redaction",
                    "WARN",
                    "Receiver-side redaction summary plus redacted evidence changes that need review.",
                    EXAMPLE_ROOT + "receiver-redaction-warn/expected-decision.json"),
            new ExampleSource(
                    "audit-append-warn",
                    "audit-append",
                    "WARN",
                    "Receiver audit log anchor advancement after local verification.",
                    EXAMPLE_ROOT + "audit-append-warn/expected-decision.json"),
            new ExampleSource(
                    "regulated-handoff-pass",
                    "regulated-handoff",
                    "PASS",
                    "Strict packaged review profile with no drift.",
                    EXAMPLE_ROOT + "regulated-handoff-pass/expected-decision.json"),
            new ExampleSource(
                    "regulated-handoff-fail",
                    "regulated-handoff",
                    "FAIL",
                    "Missing core bundle evidence under the regulated handoff profile.",
                    EXAMPLE_ROOT + "regulated-handoff-fail/expected-decision.json"),
            new ExampleSource(
                    "investigation-working-copy-warn",
                    "investigation-working-copy",
                    "WARN",
                    "Active investigation handoff with working notes and reviewed report edits.",
                    EXAMPLE_ROOT + "investigation-working-copy-warn/expected-decision.json"));

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

    private final ObjectMapper objectMapper;

    public EvidenceTrainingOnboardingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OnboardingSummary onboarding() {
        return new OnboardingSummary(
                ONBOARDING_VERSION,
                true,
                false,
                false,
                false,
                workflows(),
                List.of(
                        "GET /api/evidence-training/onboarding",
                        "GET /api/evidence-training/templates",
                        "GET /api/evidence-training/examples",
                        "GET /api/evidence-training/scorecards",
                        "GET /api/evidence-training/scorecards/{name}",
                        "GET /api/evidence-training/scorecards/{name}/answer-template",
                        "POST /api/evidence-training/scorecards/grade"),
                listTemplates(),
                listExamples(),
                listScorecards(),
                answerTemplate("receiver-redaction-warn"),
                safetyNotes());
    }

    public List<PolicyTemplateSummary> listTemplates() {
        return TEMPLATE_SOURCES.stream()
                .map(this::templateSummary)
                .sorted(Comparator.comparing(PolicyTemplateSummary::name))
                .toList();
    }

    public List<PolicyExampleSummary> listExamples() {
        return EXAMPLE_SOURCES.stream()
                .map(this::exampleSummary)
                .sorted(Comparator.comparing(PolicyExampleSummary::name))
                .toList();
    }

    public List<TrainingScorecard> listScorecards() {
        List<TrainingScorecard> scorecards = new ArrayList<>();
        for (String resourceName : SCORECARD_RESOURCE_NAMES) {
            scorecards.add(readScorecardResource(resourceName));
        }
        return scorecards.stream()
                .sorted(Comparator.comparing(TrainingScorecard::exerciseName))
                .toList();
    }

    public TrainingScorecard scorecard(String exerciseName) {
        String normalized = normalize(exerciseName, "scorecard exercise name");
        return listScorecards().stream()
                .filter(scorecard -> normalize(scorecard.exerciseName(), "scorecard exercise name").equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "unknown training scorecard exercise: " + exerciseName));
    }

    public ScorecardAnswerTemplate answerTemplate(String exerciseName) {
        TrainingScorecard scorecard = scorecard(exerciseName);
        OperatorAnswer answer = new OperatorAnswer(
                scorecard.exerciseName(),
                scorecard.expectedDecision().name(),
                scorecard.expectedPrimaryReason(),
                scorecard.acceptableActions().get(0),
                scorecard.remediationNotes().get(0));
        return new ScorecardAnswerTemplate("operator-label", List.of(answer));
    }

    public ScorecardGradeResult grade(ScorecardAnswers answers, Optional<Double> passingScoreOverride) {
        if (answers == null || answers.answers() == null) {
            throw new IllegalArgumentException("scorecard answers must include an answers array");
        }
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
        double passingScore = (passingScoreOverride == null ? Optional.<Double>empty() : passingScoreOverride)
                .orElse(DEFAULT_PASSING_PERCENT);
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

    private PolicyTemplateSummary templateSummary(TemplateSource source) {
        JsonNode root = resourceJson(source.resourcePath());
        return new PolicyTemplateSummary(
                source.name(),
                source.description(),
                text(root, "policyVersion", "unknown"),
                text(root, "mode", "unknown"),
                text(root, "defaultSeverity", "unknown"),
                root.path("rules").isArray() ? root.path("rules").size() : 0,
                source.resourcePath());
    }

    private PolicyExampleSummary exampleSummary(ExampleSource source) {
        JsonNode root = resourceJson(source.expectedDecisionPath());
        return new PolicyExampleSummary(
                source.name(),
                text(root, "template", source.templateName()),
                text(root, "expectedDecision", source.expectedDecision()),
                source.description(),
                intValue(root, "expectedFailCount"),
                intValue(root, "expectedWarnCount"),
                intValue(root, "expectedInfoCount"),
                intValue(root, "expectedIgnoredCount"),
                intValue(root, "expectedUnclassifiedCount"),
                stringList(root.path("expectedChangedPaths")),
                stringList(root.path("expectedChangeTypes")),
                source.expectedDecisionPath());
    }

    private TrainingScorecard readScorecardResource(String resourceName) {
        JsonNode root = resourceJson(SCORECARD_ROOT + resourceName);
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

    private Map<String, OperatorAnswer> answersByExercise(
            ScorecardAnswers answers,
            Set<String> knownExerciseNames) {
        Map<String, OperatorAnswer> answersByExercise = new LinkedHashMap<>();
        for (OperatorAnswer answer : answers.answers()) {
            if (answer == null) {
                throw new IllegalArgumentException("scorecard answer entry is required");
            }
            if (answer.exerciseName() == null || answer.exerciseName().isBlank()) {
                throw new IllegalArgumentException("scorecard answer exerciseName is required");
            }
            String matchedName = knownExerciseNames.stream()
                    .filter(name -> normalize(name, "scorecard exercise name")
                            .equals(normalize(answer.exerciseName(), "scorecard exercise name")))
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

    private JsonNode resourceJson(String resourcePath) {
        try (InputStream input = EvidenceTrainingOnboardingService.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("missing evidence training resource: " + resourcePath);
            }
            return objectMapper.readTree(input);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read evidence training resource: " + resourcePath, e);
        }
    }

    private static List<WorkflowSummary> workflows() {
        return List.of(
                new WorkflowSummary(
                        "Browse onboarding summary",
                        "GET",
                        "/api/evidence-training/onboarding",
                        "Read templates, examples, scorecards, answer-template shape, and safety notes.",
                        "Evidence Training Onboarding / GET Onboarding Summary",
                        null),
                new WorkflowSummary(
                        "List packaged policy templates",
                        "GET",
                        "/api/evidence-training/templates",
                        "java -jar target/LoadBalancerPro-2.4.2.jar --list-policy-templates",
                        "Evidence Training Onboarding / GET Policy Templates",
                        null),
                new WorkflowSummary(
                        "List packaged policy examples",
                        "GET",
                        "/api/evidence-training/examples",
                        "java -jar target/LoadBalancerPro-2.4.2.jar --list-policy-examples",
                        "Evidence Training Onboarding / GET Policy Examples",
                        null),
                new WorkflowSummary(
                        "List training scorecards",
                        "GET",
                        "/api/evidence-training/scorecards",
                        "java -jar target/LoadBalancerPro-2.4.2.jar --list-training-scorecards",
                        "Evidence Training Onboarding / GET Training Scorecards",
                        null),
                new WorkflowSummary(
                        "Get scorecard answer template",
                        "GET",
                        "/api/evidence-training/scorecards/{name}/answer-template",
                        "java -jar target/LoadBalancerPro-2.4.2.jar --print-training-scorecard <name>",
                        "Evidence Training Onboarding / GET Scorecard Answer Template",
                        null),
                new WorkflowSummary(
                        "Grade scorecard answers",
                        "POST",
                        "/api/evidence-training/scorecards/grade",
                        "java -jar target/LoadBalancerPro-2.4.2.jar --grade-training-scorecard scorecard-answers.json",
                        "Evidence Training Onboarding / POST Grade Scorecard Answers",
                        "Deterministic grading only; no report file is written by the API."));
    }

    private static List<String> safetyNotes() {
        return List.of(
                "Local/operator training aid only.",
                "Not certification.",
                "Not legal compliance proof.",
                "Not identity proof.",
                "No cloud mutation.",
                "No CloudManager construction is required.",
                "The API server is optional for CLI workflows.");
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

    private static String normalize(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim().toLowerCase(Locale.ROOT);
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

    private static int intValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.canConvertToInt() ? value.asInt() : 0;
    }

    private static List<String> stringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            if (value.isTextual() && !value.asText().isBlank()) {
                values.add(value.asText().trim());
            }
        }
        return values;
    }

    private static List<String> stringList(JsonNode node, String field) {
        if (!node.isArray()) {
            throw new IllegalArgumentException("scorecard " + field + " must be an array");
        }
        List<String> values = stringList(node);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("scorecard " + field + " must not be empty");
        }
        return values;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static double roundPercent(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public enum ScorecardDecision {
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

    public record OnboardingSummary(
            String onboardingVersion,
            boolean readOnly,
            boolean cloudMutation,
            boolean cloudManagerRequired,
            boolean apiServerRequiredForCli,
            List<WorkflowSummary> workflows,
            List<String> endpoints,
            List<PolicyTemplateSummary> templates,
            List<PolicyExampleSummary> examples,
            List<TrainingScorecard> scorecards,
            ScorecardAnswerTemplate sampleAnswerTemplate,
            List<String> safetyNotes) {
        public OnboardingSummary {
            workflows = workflows == null ? List.of() : List.copyOf(workflows);
            endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
            templates = templates == null ? List.of() : List.copyOf(templates);
            examples = examples == null ? List.of() : List.copyOf(examples);
            scorecards = scorecards == null ? List.of() : List.copyOf(scorecards);
            safetyNotes = safetyNotes == null ? List.of() : List.copyOf(safetyNotes);
        }
    }

    public record WorkflowSummary(
            String name,
            String method,
            String path,
            String cliEquivalent,
            String postmanRequest,
            String note) {
    }

    public record PolicyTemplateSummary(
            String name,
            String description,
            String policyVersion,
            String mode,
            String defaultSeverity,
            int ruleCount,
            String resourcePath) {
    }

    public record PolicyExampleSummary(
            String name,
            String templateName,
            String expectedDecision,
            String description,
            int expectedFailCount,
            int expectedWarnCount,
            int expectedInfoCount,
            int expectedIgnoredCount,
            int expectedUnclassifiedCount,
            List<String> expectedChangedPaths,
            List<String> expectedChangeTypes,
            String expectedDecisionResourcePath) {
        public PolicyExampleSummary {
            expectedChangedPaths = expectedChangedPaths == null ? List.of() : List.copyOf(expectedChangedPaths);
            expectedChangeTypes = expectedChangeTypes == null ? List.of() : List.copyOf(expectedChangeTypes);
        }
    }

    public record TrainingScorecard(
            String exerciseName,
            String templateName,
            ScorecardDecision expectedDecision,
            String expectedPrimaryReason,
            String prompt,
            List<String> acceptableActions,
            List<String> remediationNotes,
            Scoring scoring) {
        public TrainingScorecard {
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

    public record Scoring(int decisionPoints, int reasonPoints, int actionPoints) {
        public Scoring {
            if (decisionPoints < 0 || reasonPoints < 0 || actionPoints < 0) {
                throw new IllegalArgumentException("scorecard points cannot be negative");
            }
        }

        int maxScore() {
            return decisionPoints + reasonPoints + actionPoints;
        }
    }

    public record ScorecardAnswerTemplate(String operator, List<OperatorAnswer> answers) {
        public ScorecardAnswerTemplate {
            answers = answers == null ? List.of() : List.copyOf(answers);
        }
    }

    public record ScorecardAnswers(String operator, List<OperatorAnswer> answers) {
    }

    public record OperatorAnswer(
            String exerciseName,
            String decision,
            String reason,
            String action,
            String notes) {
    }

    public record ScorecardGradeResult(
            String scorecardVersion,
            String operator,
            int totalExercises,
            int totalScore,
            int maxScore,
            double percent,
            boolean passed,
            double passingScore,
            List<PerExerciseGrade> perExercise) {
        public ScorecardGradeResult {
            perExercise = perExercise == null ? List.of() : List.copyOf(perExercise);
        }
    }

    public record PerExerciseGrade(
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

    private record TemplateSource(String name, String description, String resourcePath) {
    }

    private record ExampleSource(
            String name,
            String templateName,
            String expectedDecision,
            String description,
            String expectedDecisionPath) {
    }
}
