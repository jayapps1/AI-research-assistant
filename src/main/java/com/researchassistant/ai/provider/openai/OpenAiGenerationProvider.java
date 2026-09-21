package com.researchassistant.ai.provider.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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

import com.researchassistant.ai.usage.AiProviderBudgetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "app.ai.generation.provider", havingValue = "openai")
public class OpenAiGenerationProvider implements AiGenerationProvider, GroundedAnswerGenerator {

    private static final Logger log = LoggerFactory.getLogger(OpenAiGenerationProvider.class);

    private final AiProperties properties;
    private final ChatModel chatModel;
    private final GroundingPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final AiProviderBudgetService budgetService;

    public OpenAiGenerationProvider(
            AiProperties properties,
            ObjectProvider<ChatModel> chatModel,
            GroundingPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            AiProviderBudgetService budgetService
    ) {
        this.properties = properties;
        this.chatModel = chatModel.getIfAvailable();
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.budgetService = budgetService;
    }

    @Override
    public String providerName() {
        return AiProviderType.OPENAI.name();
    }

    @Override
    public String modelName() {
        return properties.generation().model() == null || properties.generation().model().isBlank()
                ? "gpt-4o-mini"
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

        budgetService.checkBudgetBeforeCall(providerName(), modelName(), 2000, properties.generation().maxOutputTokens());

        long startTime = System.currentTimeMillis();
        OffsetDateTime startedAt = OffsetDateTime.now();

        try {
            OpenAiChatOptions options = buildChatOptions();

            Prompt prompt = new Prompt(request.promptText(), options);
            ChatResponse response = chatModel.call(prompt);

            long latencyMs = System.currentTimeMillis() - startTime;
            OffsetDateTime completedAt = OffsetDateTime.now();

            Integer inputTokens = null;
            Integer outputTokens = null;
            Integer totalTokens = null;
            Integer cachedInputTokens = null;

            var metadata = response.getMetadata();
            if (metadata != null) {
                var usage = metadata.getUsage();
                if (usage != null) {
                    inputTokens = usage.getPromptTokens();
                    outputTokens = usage.getCompletionTokens();
                    totalTokens = usage.getTotalTokens();
                    if (usage.getCacheReadInputTokens() != null) {
                        cachedInputTokens = Math.toIntExact(usage.getCacheReadInputTokens());
                    }
                }
            }

            String text = response.getResult() != null
                    ? response.getResult().getOutput().getText()
                    : "";
            T parsed = parseResponse(text, responseType, inputTokens, outputTokens, latencyMs, request.evidenceBundle());

            return AiTaskResult.success(
                    UUID.randomUUID(),
                    request.taskType(),
                    AiProviderType.OPENAI,
                    modelName(),
                    parsed,
                    inputTokens,
                    outputTokens,
                    totalTokens,
                    cachedInputTokens,
                    latencyMs,
                    null,
                    startedAt,
                    completedAt,
                    Collections.emptyList()
            );
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            if (e instanceof com.researchassistant.ai.exception.AiDevelopmentBudgetExceededException budgetEx) {
                return AiTaskResult.failure(
                        UUID.randomUUID(),
                        request.taskType(),
                        AiProviderType.OPENAI,
                        modelName(),
                        "AI_DEVELOPMENT_BUDGET_EXCEEDED",
                        budgetEx.getMessage(),
                        latencyMs,
                        startedAt,
                        OffsetDateTime.now(),
                        List.of(budgetEx.getMessage()),
                        budgetEx
                );
            }
            ProviderErrorDetails providerError = extractProviderError(e);
            SafeErrorDetails safeError = resolveSafeError(e);
            String correlationId = MDC.get("requestId");

            log.error(
                    "OpenAI generation failed: provider=OPENAI model={} exception={} httpStatus={} providerCode={} providerParam={} providerRequestId={} correlationId={} safeProviderMessage={}",
                    modelName(),
                    e.getClass().getName(),
                    providerError.httpStatus(),
                    providerError.providerCode(),
                    providerError.parameter(),
                    providerError.providerRequestId(),
                    correlationId,
                    providerError.safeMessage()
            );

            return AiTaskResult.failure(
                    UUID.randomUUID(),
                    request.taskType(),
                    AiProviderType.OPENAI,
                    modelName(),
                    safeError.code(),
                    safeError.code(),
                    latencyMs,
                    startedAt,
                    OffsetDateTime.now(),
                    List.of(safeError.message()),
                    e
            );
        }
    }

    private OpenAiChatOptions buildChatOptions() {
        String model = modelName();
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        builder.model(model);
        builder.maxCompletionTokens(configuredMaxOutputTokens());
        if (supportsTemperature(model)) {
            builder.temperature(properties.generation().temperature());
        }
        return builder.build();
    }

    private int configuredMaxOutputTokens() {
        int configured = properties.generation().maxOutputTokens();
        return configured > 0 ? configured : 4096;
    }

    private boolean supportsTemperature(String model) {
        if (model == null || model.isBlank()) {
            return true;
        }
        return !model.trim().toLowerCase(java.util.Locale.ROOT).startsWith("gpt-5");
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
                : (result.failureCategory() != null ? result.failureCategory() : "AI generation failed");
        String failureCode = result.failureCode() != null ? result.failureCode()
                : (result.failureCategory() != null ? result.failureCategory() : "AI_GENERATION_FAILED");
        throw new com.researchassistant.ai.exception.AiGenerationException(failureCode, failureReason, result.cause());
    }

    private String cleanGeneratedText(String rawText) {
        if (rawText == null) {
            return "";
        }
        String cleanText = rawText.trim();
        if (cleanText.startsWith("```json")) {
            cleanText = cleanText.substring(7);
            if (cleanText.endsWith("```")) {
                cleanText = cleanText.substring(0, cleanText.length() - 3).trim();
            }
        } else if (cleanText.startsWith("```")) {
            cleanText = cleanText.substring(3);
            if (cleanText.endsWith("```")) {
                cleanText = cleanText.substring(0, cleanText.length() - 3).trim();
            }
        }
        return cleanText;
    }

    @SuppressWarnings("unchecked")
    private <T> T parseResponse(String rawText, Class<T> responseType, Integer inputTokens, Integer outputTokens, long latencyMs, EvidenceBundle evidenceBundle) {
        String cleanText = cleanGeneratedText(rawText);

        try {
            return objectMapper.readValue(cleanText, responseType);
        } catch (Exception e) {
            if (responseType == GeneratedAnswerDraft.class) {
                java.util.List<com.researchassistant.rag.generation.GeneratedCitation> citations = new java.util.ArrayList<>();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[?E(\\d+)]?").matcher(rawText != null ? rawText : "");
                java.util.Set<Integer> seen = new java.util.HashSet<>();
                java.util.Set<Integer> validOrdinals = evidenceBundle != null
                        ? evidenceBundle.items().stream().map(com.researchassistant.rag.evidence.EvidenceItem::evidenceOrdinal).collect(java.util.stream.Collectors.toSet())
                        : null;

                while (m.find()) {
                    try {
                        int ordinal = Integer.parseInt(m.group(1));
                        if ((validOrdinals == null || validOrdinals.contains(ordinal)) && seen.add(ordinal)) {
                            citations.add(new com.researchassistant.rag.generation.GeneratedCitation(ordinal, "Evidence reference E" + ordinal));
                        }
                    } catch (NumberFormatException ignored) {}
                }
                return (T) new GeneratedAnswerDraft(
                        rawText,
                        citations,
                        providerName(),
                        modelName(),
                        inputTokens,
                        outputTokens,
                        latencyMs,
                        "stop"
                );
            }
            if (responseType == String.class) {
                return (T) rawText;
            }
            throw new com.researchassistant.ai.exception.AiGenerationException("AI_RESPONSE_INVALID", "Failed to parse structured AI response.", e);
        }
    }

    private Integer extractHttpStatus(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof org.springframework.web.client.HttpStatusCodeException httpEx) {
                return httpEx.getStatusCode().value();
            }
            if (current instanceof org.springframework.web.client.RestClientResponseException restEx) {
                return restEx.getStatusCode().value();
            }
            String msg = current.getMessage();
            if (msg != null) {
                if (msg.contains("401")) return 401;
                if (msg.contains("403")) return 403;
                if (msg.contains("404")) return 404;
                if (msg.contains("429")) return 429;
                if (msg.contains("400")) return 400;
                if (msg.contains("500")) return 500;
                if (msg.contains("502")) return 502;
                if (msg.contains("503")) return 503;
                if (msg.contains("504")) return 504;
            }
            current = current.getCause();
        }
        return null;
    }

    private String extractProviderCode(Throwable throwable) {
        ProviderErrorDetails details = extractProviderError(throwable);
        if (details.providerCode() != null) {
            return details.providerCode();
        }
        Throwable current = throwable;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("invalid_api_key")) return "invalid_api_key";
                if (lower.contains("model_not_found")) return "model_not_found";
                if (lower.contains("insufficient_quota")) return "insufficient_quota";
                if (lower.contains("rate_limit_exceeded")) return "rate_limit_exceeded";
                if (lower.contains("context_length_exceeded")) return "context_length_exceeded";
            }
            current = current.getCause();
        }
        return null;
    }

    private ProviderErrorDetails extractProviderError(Throwable throwable) {
        Integer httpStatus = extractHttpStatus(throwable);
        String providerRequestId = null;
        String providerCode = null;
        String parameter = null;
        String safeMessage = null;

        Throwable current = throwable;
        while (current != null) {
            if (current instanceof com.openai.errors.OpenAIServiceException openAiEx) {
                httpStatus = openAiEx.statusCode();
                providerCode = firstNonBlank(providerCode, openAiEx.code().orElse(null));
                providerCode = firstNonBlank(providerCode, openAiEx.type().orElse(null));
                parameter = firstNonBlank(parameter, openAiEx.param().orElse(null));
                ProviderErrorDetails parsed = parseOpenAiErrorBody(openAiEx.body().toString());
                providerCode = firstNonBlank(providerCode, parsed.providerCode());
                parameter = firstNonBlank(parameter, parsed.parameter());
                safeMessage = firstNonBlank(safeMessage, parsed.safeMessage());
                if (providerRequestId == null) {
                    providerRequestId = firstOpenAiHeader(openAiEx, "x-request-id");
                }
            }
            if (current instanceof org.springframework.web.client.RestClientResponseException restEx) {
                httpStatus = restEx.getStatusCode().value();
                if (providerRequestId == null) {
                    providerRequestId = firstHeader(restEx, "x-request-id");
                }
                ProviderErrorDetails parsed = parseOpenAiErrorBody(restEx.getResponseBodyAsString());
                providerCode = firstNonBlank(providerCode, parsed.providerCode());
                parameter = firstNonBlank(parameter, parsed.parameter());
                safeMessage = firstNonBlank(safeMessage, parsed.safeMessage());
            }

            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                ProviderErrorDetails parsed = parseOpenAiErrorBody(message);
                providerCode = firstNonBlank(providerCode, parsed.providerCode());
                parameter = firstNonBlank(parameter, parsed.parameter());
                safeMessage = firstNonBlank(safeMessage, parsed.safeMessage());
                providerCode = firstNonBlank(providerCode, extractQuotedField(message, "code"));
                parameter = firstNonBlank(parameter, extractQuotedField(message, "param"));
                safeMessage = firstNonBlank(safeMessage, extractQuotedField(message, "message"));
                safeMessage = firstNonBlank(safeMessage, message);
                if (parameter == null && message.toLowerCase(java.util.Locale.ROOT).contains("temperature")) {
                    parameter = "temperature";
                }
                if (parameter == null) {
                    parameter = extractInvalidParameterName(message);
                }
            }
            current = current.getCause();
        }

        return new ProviderErrorDetails(
                httpStatus,
                providerCode,
                parameter,
                sanitizeProviderMessage(safeMessage),
                providerRequestId
        );
    }

    private ProviderErrorDetails parseOpenAiErrorBody(String body) {
        if (body == null || body.isBlank()) {
            return ProviderErrorDetails.empty();
        }
        int jsonStart = body.indexOf('{');
        int jsonEnd = body.lastIndexOf('}');
        if (jsonStart < 0 || jsonEnd < jsonStart) {
            return ProviderErrorDetails.empty();
        }
        String json = body.substring(jsonStart, jsonEnd + 1);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode error = root.path("error");
            if (error.isMissingNode() || error.isNull()) {
                error = root;
            }
            return new ProviderErrorDetails(
                    null,
                    textOrNull(error.path("code")),
                    textOrNull(error.path("param")),
                    textOrNull(error.path("message")),
                    null
            );
        } catch (Exception ignored) {
            return ProviderErrorDetails.empty();
        }
    }

    private String firstHeader(org.springframework.web.client.RestClientResponseException ex, String name) {
        if (ex.getResponseHeaders() == null) {
            return null;
        }
        return ex.getResponseHeaders().getFirst(name);
    }

    private String firstOpenAiHeader(com.openai.errors.OpenAIServiceException ex, String name) {
        List<String> values = ex.headers().values(name);
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    private String extractQuotedField(String message, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractInvalidParameterName(String message) {
        Pattern pattern = Pattern.compile("Invalid '([^']+)'");
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String firstNonBlank(String existing, String candidate) {
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return candidate != null && !candidate.isBlank() ? candidate : null;
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private String sanitizeProviderMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String singleLine = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return singleLine.length() <= 500 ? singleLine : singleLine.substring(0, 500);
    }

    private SafeErrorDetails resolveSafeError(Throwable throwable) {
        ProviderErrorDetails providerError = extractProviderError(throwable);
        Integer httpStatus = providerError.httpStatus();
        String providerCode = providerError.providerCode();
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                sb.append(" ").append(current.getMessage().toLowerCase());
            }
            current = current.getCause();
        }
        String msg = sb.toString();

        if ((httpStatus != null && httpStatus == 401) || "invalid_api_key".equals(providerCode) || msg.contains("401") || msg.contains("unauthorized")) {
            return new SafeErrorDetails("AI_PROVIDER_AUTHENTICATION_FAILED", "The AI provider configuration could not be authenticated.");
        }
        if ((httpStatus != null && httpStatus == 403) || msg.contains("403") || msg.contains("forbidden") || msg.contains("access denied")) {
            return new SafeErrorDetails("AI_PROVIDER_ACCESS_DENIED", "Access to the AI provider was denied. Please check key, project, or model permissions.");
        }
        if ("insufficient_quota".equals(providerCode) || msg.contains("insufficient_quota") || msg.contains("quota") || msg.contains("billing")) {
            return new SafeErrorDetails("AI_PROVIDER_QUOTA_EXHAUSTED", "The AI provider currently has insufficient API quota.");
        }
        if ((httpStatus != null && httpStatus == 429) || "rate_limit_exceeded".equals(providerCode) || msg.contains("429") || msg.contains("rate limit") || msg.contains("rate_limit")) {
            return new SafeErrorDetails("AI_PROVIDER_RATE_LIMITED", "AI provider rate limit exceeded. Please try again later.");
        }
        if ((httpStatus != null && httpStatus == 404) || "model_not_found".equals(providerCode) || (msg.contains("model") && (msg.contains("not found") || msg.contains("does not exist") || msg.contains("not permitted") || msg.contains("permission")))) {
            return new SafeErrorDetails("AI_MODEL_UNAVAILABLE", "The configured AI model is not available.");
        }
        if ((httpStatus != null && httpStatus == 400) || msg.contains("400") || msg.contains("bad request") || msg.contains("invalid request") || msg.contains("invalid_request_error")) {
            return new SafeErrorDetails("AI_PROVIDER_REQUEST_INVALID", "The AI request was rejected by the provider as invalid.");
        }
        if ((httpStatus != null && httpStatus == 504) || msg.contains("timeout") || msg.contains("timed out") || throwable instanceof java.util.concurrent.TimeoutException || throwable instanceof java.net.SocketTimeoutException) {
            return new SafeErrorDetails("AI_PROVIDER_TIMEOUT", "AI provider request timed out. Please try again.");
        }
        if (throwable instanceof java.net.ConnectException || msg.contains("connection refused") || msg.contains("failed to connect") || msg.contains("connection reset")) {
            return new SafeErrorDetails("AI_PROVIDER_UNAVAILABLE", "Unable to connect to the AI provider. Please check network connectivity.");
        }
        return new SafeErrorDetails("AI_GENERATION_FAILED", "An error occurred while communicating with the AI service.");
    }

    private record ProviderErrorDetails(
            Integer httpStatus,
            String providerCode,
            String parameter,
            String safeMessage,
            String providerRequestId
    ) {
        static ProviderErrorDetails empty() {
            return new ProviderErrorDetails(null, null, null, null, null);
        }
    }

    private record SafeErrorDetails(String code, String message) {}
}
