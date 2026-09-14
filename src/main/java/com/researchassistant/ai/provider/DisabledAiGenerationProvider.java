package com.researchassistant.ai.provider;

import com.researchassistant.ai.orchestration.AiTaskRequest;
import com.researchassistant.ai.orchestration.AiTaskResult;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(AiGenerationProvider.class)
public class DisabledAiGenerationProvider implements AiGenerationProvider {

    @Override
    public String providerName() {
        return "none";
    }

    @Override
    public String modelName() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public <T> AiTaskResult<T> generate(AiTaskRequest request, Class<T> responseType) {
        throw new IllegalStateException("AI generation provider is disabled or unavailable.");
    }
}
