package com.researchassistant.ai.orchestration.dto;

import java.util.List;

public record GeneratedQuestionnaireDraft(
        String instrumentTitle,
        String instructions,
        List<QuestionItem> items
) {
    public record QuestionItem(
            String itemText,
            String constructName,
            String responseFormat,
            List<String> options
    ) {
    }
}
