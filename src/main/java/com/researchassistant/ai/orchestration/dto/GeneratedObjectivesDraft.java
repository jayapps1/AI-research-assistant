package com.researchassistant.ai.orchestration.dto;

import java.util.List;

public record GeneratedObjectivesDraft(
        String generalObjective,
        List<String> specificObjectives,
        String alignmentRationale
) {
}
