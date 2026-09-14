package com.researchassistant.ai.orchestration.validation;

import com.researchassistant.ai.orchestration.dto.GeneratedQuestionnaireDraft;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GeneratedQuestionnaireValidator {

    public List<String> validate(GeneratedQuestionnaireDraft draft) {
        List<String> errors = new ArrayList<>();
        if (draft == null) {
            errors.add("Generated questionnaire draft is null.");
            return errors;
        }
        if (draft.instrumentTitle() == null || draft.instrumentTitle().isBlank()) {
            errors.add("Instrument title cannot be empty.");
        }
        if (draft.items() == null || draft.items().isEmpty()) {
            errors.add("Questionnaire items list cannot be empty.");
        }
        return errors;
    }
}
