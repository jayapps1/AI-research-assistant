package com.researchassistant.ai.controller;

import com.researchassistant.ai.orchestration.AiTaskRequest;
import com.researchassistant.ai.orchestration.AiTaskResult;
import com.researchassistant.ai.orchestration.AiTaskType;
import com.researchassistant.ai.provider.AiGenerationProvider;
import com.researchassistant.ai.usage.AiRequestStatus;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/diagnostics")
public class AiDiagnosticsController {

    private static final String SMOKE_PROMPT = "Reply exactly with: connected";

    private final ObjectProvider<AiGenerationProvider> generationProvider;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AiDiagnosticsController(
            ObjectProvider<AiGenerationProvider> generationProvider,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.generationProvider = generationProvider;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/smoke-test")
    @ResponseStatus(HttpStatus.OK)
    public AiSmokeTestResponse smokeTest(Authentication authentication) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        AiGenerationProvider provider = generationProvider.getIfAvailable();
        if (provider == null || !provider.available()) {
            return new AiSmokeTestResponse(
                    UUID.randomUUID(),
                    provider == null ? "NONE" : provider.providerName(),
                    provider == null ? "none" : provider.modelName(),
                    false,
                    null,
                    null,
                    "FAILED",
                    "AI_PROVIDER_NOT_CONFIGURED"
            );
        }
        AiTaskRequest request = new AiTaskRequest(
                AiTaskType.ACADEMIC_WRITING_REVIEW,
                user.getId(),
                null,
                null,
                SMOKE_PROMPT,
                SMOKE_PROMPT,
                null,
                null,
                String.class,
                false
        );
        AiTaskResult<String> result = provider.generate(request, String.class);
        String safeStatus = result.status() == AiRequestStatus.COMPLETED ? "COMPLETED" : "FAILED";
        boolean connected = result.status() == AiRequestStatus.COMPLETED
                && result.result() != null
                && "connected".equalsIgnoreCase(result.result().trim());
        return new AiSmokeTestResponse(
                UUID.randomUUID(),
                result.provider().name(),
                result.model(),
                connected,
                result.inputTokens(),
                result.outputTokens(),
                safeStatus,
                result.failureCode()
        );
    }

    public record AiSmokeTestResponse(
            UUID id,
            String provider,
            String model,
            boolean success,
            Integer inputTokens,
            Integer outputTokens,
            String safeProviderStatus,
            String failureCode
    ) {
    }
}
