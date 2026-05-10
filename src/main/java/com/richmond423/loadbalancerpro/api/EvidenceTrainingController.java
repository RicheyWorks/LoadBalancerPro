package com.richmond423.loadbalancerpro.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/evidence-training")
public class EvidenceTrainingController {
    private final EvidenceTrainingOnboardingService onboardingService;

    public EvidenceTrainingController(EvidenceTrainingOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/onboarding")
    public EvidenceTrainingOnboardingService.OnboardingSummary onboarding() {
        return onboardingService.onboarding();
    }

    @GetMapping("/templates")
    public List<EvidenceTrainingOnboardingService.PolicyTemplateSummary> templates() {
        return onboardingService.listTemplates();
    }

    @GetMapping("/examples")
    public List<EvidenceTrainingOnboardingService.PolicyExampleSummary> examples() {
        return onboardingService.listExamples();
    }

    @GetMapping("/scorecards")
    public List<EvidenceTrainingOnboardingService.TrainingScorecard> scorecards() {
        return onboardingService.listScorecards();
    }

    @GetMapping("/scorecards/{name}")
    public EvidenceTrainingOnboardingService.TrainingScorecard scorecard(@PathVariable("name") String name) {
        return onboardingService.scorecard(name);
    }

    @GetMapping("/scorecards/{name}/answer-template")
    public EvidenceTrainingOnboardingService.ScorecardAnswerTemplate answerTemplate(@PathVariable("name") String name) {
        return onboardingService.answerTemplate(name);
    }

    @PostMapping("/scorecards/grade")
    public EvidenceTrainingOnboardingService.ScorecardGradeResult grade(
            @RequestBody EvidenceTrainingOnboardingService.ScorecardAnswers answers,
            @RequestParam(name = "passingScore", required = false) Double passingScore) {
        return onboardingService.grade(answers, Optional.ofNullable(passingScore));
    }
}
