package com.researchassistant.ai.orchestration.dto;

import java.util.List;

public record GeneratedResearchQuestionsDraft(
        String mainResearchQuestion,
        List<String> subQuestions,
        List<String> suggestedHypotheses
) {
}
