package com.researchassistant.ai.provider.openai;

import com.researchassistant.ai.orchestration.AiTaskRequest;
import com.researchassistant.ai.orchestration.AiTaskResult;
import com.researchassistant.ai.orchestration.AiTaskType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest(properties = {
        "app.ai.generation.enabled=true",
        "app.ai.generation.provider=openai",
        "app.ai.generation.model=${AI_GENERATION_MODEL:gpt-4o-mini}",
        "spring.ai.model.chat=openai",
        "spring.autoconfigure.exclude="
})
public class OpenAiSmokeRunner {

    @Autowired(required = false)
    private OpenAiGenerationProvider provider;

    @Test
    void executeMinimalSmokeTest() {
        if (provider == null) {
            System.out.println("SMOKE_TEST_RESULT: provider is null");
            return;
        }
        System.out.println("SMOKE_TEST_PROVIDER: " + provider.providerName());
        System.out.println("SMOKE_TEST_MODEL: " + provider.modelName());
        System.out.println("SMOKE_TEST_AVAILABLE: " + provider.available());

        AiTaskRequest request = new AiTaskRequest(
                AiTaskType.GROUNDED_QA,
                UUID.randomUUID(),
                null,
                null,
                "Reply only with: connected",
                "Reply only with: connected",
                null,
                null,
                String.class,
                false
        );

        AiTaskResult<String> result = provider.generate(request, String.class);
        System.out.println("SMOKE_TEST_STATUS: " + result.status());
        System.out.println("SMOKE_TEST_FAILURE_CODE: " + result.failureCode());
        System.out.println("SMOKE_TEST_FAILURE_CATEGORY: " + result.failureCategory());
        System.out.println("SMOKE_TEST_TEXT: " + result.result());
        System.out.println("SMOKE_TEST_INPUT_TOKENS: " + result.inputTokens());
        System.out.println("SMOKE_TEST_OUTPUT_TOKENS: " + result.outputTokens());
        System.out.println("SMOKE_TEST_LATENCY_MS: " + result.latencyMs());
        if (result.cause() != null) {
            System.out.println("SMOKE_TEST_CAUSE_CLASS: " + result.cause().getClass().getName());
            System.out.println("SMOKE_TEST_CAUSE_MESSAGE: " + result.cause().getMessage());
        }
    }
}
