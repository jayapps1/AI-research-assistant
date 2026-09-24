package com.researchassistant.rag.service;

import com.researchassistant.ai.config.AiProperties;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RagContextBudgetService {

    private static final int DEFAULT_SAFETY_MARGIN_TOKENS = 1_500;
    private static final int DEFAULT_SYSTEM_PROMPT_TOKENS = 450;
    private static final int DEFAULT_PROJECT_CONTEXT_TOKENS = 300;
    private static final int DEFAULT_SECTION_INSTRUCTION_TOKENS = 250;

    private final AiProperties aiProperties;
    private final TokenEstimator tokenEstimator;

    public RagContextBudgetService(AiProperties aiProperties, TokenEstimator tokenEstimator) {
        this.aiProperties = aiProperties;
        this.tokenEstimator = tokenEstimator;
    }

    public Budget calculate(String model, String userInstructions, String sectionInstructions, String previousAcceptedSections) {
        int contextWindow = contextWindow(model);
        int reservedOutputTokens = Math.max(512, aiProperties.generation().maxOutputTokens());
        int userInstructionTokens = tokenEstimator.estimate(userInstructions);
        int previousSectionTokens = tokenEstimator.estimate(previousAcceptedSections);
        int sectionInstructionTokens = DEFAULT_SECTION_INSTRUCTION_TOKENS + tokenEstimator.estimate(sectionInstructions);
        int nonEvidenceTokens = DEFAULT_SYSTEM_PROMPT_TOKENS
                + DEFAULT_PROJECT_CONTEXT_TOKENS
                + sectionInstructionTokens
                + userInstructionTokens
                + previousSectionTokens
                + DEFAULT_SAFETY_MARGIN_TOKENS;
        int maxEvidenceTokens = Math.max(0, contextWindow - reservedOutputTokens - nonEvidenceTokens);
        return new Budget(
                contextWindow,
                reservedOutputTokens,
                DEFAULT_SYSTEM_PROMPT_TOKENS,
                DEFAULT_PROJECT_CONTEXT_TOKENS,
                userInstructionTokens,
                sectionInstructionTokens,
                previousSectionTokens,
                DEFAULT_SAFETY_MARGIN_TOKENS,
                maxEvidenceTokens
        );
    }

    public int estimateEvidenceTokens(String evidenceText) {
        return tokenEstimator.estimate(evidenceText);
    }

    public int estimatePromptTokens(String promptText) {
        return tokenEstimator.estimate(promptText);
    }

    private int contextWindow(String model) {
        if (model == null || model.isBlank()) {
            return 128_000;
        }
        String normalized = model.toLowerCase(Locale.ROOT);
        if (normalized.contains("gpt-5") || normalized.contains("gpt-4.1") || normalized.contains("gpt-4o")) {
            return 128_000;
        }
        if (normalized.contains("gpt-4-turbo")) {
            return 128_000;
        }
        if (normalized.contains("gpt-4")) {
            return 8_192;
        }
        return 128_000;
    }

    public record Budget(
            int modelContextWindow,
            int reservedOutputTokens,
            int systemPromptTokens,
            int projectContextTokens,
            int userInstructionTokens,
            int sectionInstructionTokens,
            int previousAcceptedSectionTokens,
            int safetyMarginTokens,
            int maxEvidenceTokens
    ) {
    }
}
