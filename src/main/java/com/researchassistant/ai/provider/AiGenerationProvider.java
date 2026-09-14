package com.researchassistant.ai.provider;

import com.researchassistant.ai.orchestration.AiTaskRequest;
import com.researchassistant.ai.orchestration.AiTaskResult;

public interface AiGenerationProvider {

    String providerName();

    String modelName();

    boolean available();

    <T> AiTaskResult<T> generate(AiTaskRequest request, Class<T> responseType);
}
