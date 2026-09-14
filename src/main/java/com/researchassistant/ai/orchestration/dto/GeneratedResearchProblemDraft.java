package com.researchassistant.ai.orchestration.dto;

import java.util.List;

public record GeneratedResearchProblemDraft(
        String problemStatement,
        String backgroundContext,
        String researchGap,
        String significance,
        List<String> keyVariablesOrConstructs
) {
}
