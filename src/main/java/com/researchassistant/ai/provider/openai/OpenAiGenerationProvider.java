package com.researchassistant.ai.provider.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.ai.orchestration.AiTaskRequest;
import com.researchassistant.ai.orchestration.AiTaskResult;
import com.researchassistant.ai.provider.AiGenerationProvider;
import com.researchassistant.ai.provider.AiProviderType;
import com.researchassistant.ai.usage.AiRequestStatus;
import com.researchassistant.rag.evidence.EvidenceBundle;
import com.researchassistant.rag.generation.GeneratedAnswerDraft;
import com.researchassistant.rag.generation.GroundedAnswerGenerator;
import com.researchassistant.rag.generation.GroundingPromptBuilder;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.ai.generation.provider", havingValue = "openai")
public class OpenAiGenerationProvider implements AiGenerationProvider, GroundedAnswerGenerator {

    private final AiProperties properties;
    private final ChatModel chatModel;
    private final GroundingPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public OpenAiGenerationProvider(
            AiProperties properties,
            ObjectProvider<ChatModel> chatModel,
            GroundingPromptBuilder promptBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.chatModel = chatModel.getIfAvailable();
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() {
        return AiProviderType.OPENAI.name();
    }

    @Override
    public String modelName() {
        return properties.generation().model() == null || properties.generation().model().isBlank()
                ? "gpt-5.6-luna"
                : properties.generation().model();
    }

    @Override
    public boolean available() {
        return properties.generation().enabled() && chatModel != null;
    }

    @Override
    public <T> AiTaskResult<T> generate(AiTaskRequest request, Class<T> responseType) {
        if (!available()) {
            throw new IllegalStateException("OpenAI generation provider is disabled or unavailable.");
        }
        long startTime = System.currentTimeMillis();
        OffsetDateTime startedAt = OffsetDateTime.now();

        try {
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(modelName())
                    .temperature(properties.generation().temperature())
                    .maxTokens(properties.generation().maxOutputTokens())
                    .build();

            Prompt prompt = new Prompt(request.promptText(), options);
            ChatResponse response = chatModel.call(prompt);

            long latencyMs = System.currentTimeMillis() - startTime;
            OffsetDateTime completedAt = OffsetDateTime.now();

            String text = response.getResult().getOutput().getText();
            T parsed = objectMapper.readValue(text, responseType);

            Integer inputTokens = response.getMetadata() != null && response.getMetadata().getUsage() != null
                    ? Math.toIntExact(response.getMetadata().getUsage().getPromptTokens()) : null;
            Integer outputTokens = response.getMetadata() != null && response.getMetadata().getUsage() != null
                    ? Math.toIntExact(response.getMetadata().getUsage().getCompletionTokens()) : null;
            Integer totalTokens = response.getMetadata() != null && response.getMetadata().getUsage() != null
                    ? Math.toIntExact(response.getMetadata().getUsage().getTotalTokens()) : null;

            return new AiTaskResult<>(
                    UUID.randomUUID(),
                    request.taskType(),
                    AiProviderType.OPENAI,
                    modelName(),
                    AiRequestStatus.COMPLETED,
                    parsed,
                    inputTokens,
                    outputTokens,
                    totalTokens,
                    latencyMs,
                    null,
                    null,
                    null,
                    startedAt,
                    completedAt,
                    List.of()
            );
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            SafeErrorDetails safeError = resolveSafeError(e);
            return new AiTaskResult<>(
                    UUID.randomUUID(),
                    request.taskType(),
                    AiProviderType.OPENAI,
                    modelName(),
                    AiRequestStatus.FAILED,
                    null,
                    null,
                    null,
                    null,
                    latencyMs,
                    safeError.code(),
                    safeError.message(),
                    null,
                    startedAt,
                    OffsetDateTime.now(),
                    List.of(safeError.message())
            );
        }
    }

    @Override
    public GeneratedAnswerDraft generate(EvidenceBundle evidenceBundle) {
        String promptText = promptBuilder.build(evidenceBundle);
        AiTaskRequest taskRequest = new AiTaskRequest(
                com.researchassistant.ai.orchestration.AiTaskType.GROUNDED_QA,
                null,
                evidenceBundle.scope() != null ? evidenceBundle.scope().workspaceId() : null,
                evidenceBundle.scope() != null ? evidenceBundle.scope().projectId() : null,
                evidenceBundle.query(),
                promptText,
                evidenceBundle.scope(),
                evidenceBundle,
                GeneratedAnswerDraft.class,
                properties.privacy().externalResearchContentEnabled()
        );
        AiTaskResult<GeneratedAnswerDraft> result = generate(taskRequest, GeneratedAnswerDraft.class);
        if (result.status() == AiRequestStatus.COMPLETED && result.result() != null) {
            return result.result();
        }
        String failureReason = !result.warnings().isEmpty()
                ? String.join(", ", result.warnings())
                : result.failureCode() != null ? result.failureCode() : "AI generation failed";
        return new GeneratedAnswerDraft(
                "Unable to generate answer: " + failureReason,
                List.of(),
                providerName(),
                modelName(),
                result.inputTokens(),
                result.outputTokens(),
                result.latencyMs(),
                result.failureCode() != null ? result.failureCode() : "FAILED"
        );
    }

    private SafeErrorDetails resolveSafeError(Throwable throwable) {
        String msg = throwable != null && throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";
        if (msg.contains("401") || msg.contains("unauthorized") || msg.contains("invalid_api_key") || msg.contains("authentication")) {
            return new SafeErrorDetails("AI_PROVIDER_AUTHENTICATION_FAILED", "AI provider authentication failed. Please check provider configuration.");
        }
        if (msg.contains("429") || msg.contains("rate_limit") || msg.contains("insufficient_quota") || msg.contains("quota")) {
            return new SafeErrorDetails("AI_PROVIDER_RATE_LIMITED", "AI provider rate limit or quota exceeded. Please try again later.");
        }
        if (msg.contains("timeout") || msg.contains("timed out") || throwable instanceof java.util.concurrent.TimeoutException) {
            return new SafeErrorDetails("AI_PROVIDER_TIMEOUT", "AI provider request timed out. Please try again.");
        }
        if (msg.contains("model") && (msg.contains("not found") || msg.contains("does not exist") || msg.contains("not permitted") || msg.contains("permission"))) {
            return new SafeErrorDetails("AI_MODEL_UNAVAILABLE", "Configured AI model is currently unavailable.");
        }
        return new SafeErrorDetails("AI_PROVIDER_ERROR", "An error occurred while communicating with the AI service.");
    }

    private record SafeErrorDetails(String code, String message) {}
}
