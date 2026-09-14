package com.researchassistant.ai.orchestration.validation;

import com.researchassistant.ai.orchestration.dto.GeneratedObjectivesDraft;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GeneratedObjectiveValidator {

    public List<String> validate(GeneratedObjectivesDraft draft) {
        List<String> errors = new ArrayList<>();
        if (draft == null) {
            errors.add("Generated objectives draft is null.");
            return errors;
        }
        if (draft.generalObjective() == null || draft.generalObjective().isBlank()) {
            errors.add("General objective cannot be empty.");
        }
        if (draft.specificObjectives() == null || draft.specificObjectives().isEmpty()) {
            errors.add("Specific objectives list cannot be empty.");
        }
        return errors;
    }
}
