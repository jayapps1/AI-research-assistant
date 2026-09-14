package com.researchassistant.ai.orchestration.validation;

import com.researchassistant.ai.orchestration.dto.GeneratedFindingDraft;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GeneratedFindingValidator {

    public List<String> validate(GeneratedFindingDraft draft) {
        List<String> errors = new ArrayList<>();
        if (draft == null) {
            errors.add("Generated finding draft is null.");
            return errors;
        }
        if (draft.findingTitle() == null || draft.findingTitle().isBlank()) {
            errors.add("Finding title cannot be empty.");
        }
        if (draft.summaryText() == null || draft.summaryText().isBlank()) {
            errors.add("Summary text cannot be empty.");
        }
        return errors;
    }
}
