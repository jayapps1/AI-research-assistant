package com.researchassistant.ai.config;

import com.researchassistant.ai.provider.AiGenerationProvider;
import com.researchassistant.ai.provider.openai.OpenAiGenerationProvider;
import com.researchassistant.rag.generation.GroundedAnswerGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OpenAiConfigurationDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(OpenAiConfigurationDiagnostics.class);

    private final AiProperties aiProperties;
    private final Environment environment;
    private final ObjectProvider<OpenAiGenerationProvider> openAiGenerationProvider;
    private final ObjectProvider<ChatModel> chatModel;
    private final ObjectProvider<AiGenerationProvider> aiGenerationProvider;
    private final ObjectProvider<GroundedAnswerGenerator> groundedAnswerGenerator;

    public OpenAiConfigurationDiagnostics(
            AiProperties aiProperties,
            Environment environment,
            ObjectProvider<OpenAiGenerationProvider> openAiGenerationProvider,
            ObjectProvider<ChatModel> chatModel,
            ObjectProvider<AiGenerationProvider> aiGenerationProvider,
            ObjectProvider<GroundedAnswerGenerator> groundedAnswerGenerator
    ) {
        this.aiProperties = aiProperties;
        this.environment = environment;
        this.openAiGenerationProvider = openAiGenerationProvider;
        this.chatModel = chatModel;
        this.aiGenerationProvider = aiGenerationProvider;
        this.groundedAnswerGenerator = groundedAnswerGenerator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void report() {
        boolean apiKeyPresent = openAiApiKeyPresent();
        boolean openAiProviderPresent = openAiGenerationProvider.getIfAvailable() != null;
        boolean chatModelPresent = chatModel.getIfAvailable() != null;
        AiGenerationProvider selectedGenerationProvider = aiGenerationProvider.getIfAvailable();
        GroundedAnswerGenerator selectedGroundedGenerator = groundedAnswerGenerator.getIfAvailable();
        String configuredProvider = environment.getProperty("app.ai.generation.provider");
        String configuredModel = environment.getProperty("app.ai.generation.model");
        String springAiModelChat = environment.getProperty("spring.ai.model.chat");
        log.info(
                "AI generation diagnostics: activeProfiles={} AI_GENERATION_ENABLED={} app.ai.generation.provider={} app.ai.generation.provider.bound={} app.ai.generation.model={} SPRING_AI_MODEL_CHAT={} OPENAI_API_KEY present={} OpenAiGenerationProvider bean present={} ChatModel bean present={} AiGenerationProvider selected={} GroundedAnswerGenerator selected={}",
                environment.getActiveProfiles(),
                aiProperties.generation().enabled(),
                configuredProvider,
                aiProperties.generation().provider(),
                configuredModel,
                springAiModelChat,
                apiKeyPresent,
                openAiProviderPresent,
                chatModelPresent,
                selectedGenerationProvider == null ? "none" : selectedGenerationProvider.getClass().getName(),
                selectedGroundedGenerator == null ? "none" : selectedGroundedGenerator.getClass().getName()
        );
        if ("OPENAI".equals(configuredProvider)) {
            log.error("AI generation provider is configured as OPENAI; use exact lowercase app.ai.generation.provider=openai so the OpenAiGenerationProvider condition matches.");
        }
        if ("OPENAI".equals(springAiModelChat)) {
            log.error("Spring AI chat model is configured as OPENAI; use exact lowercase spring.ai.model.chat=openai so OpenAI ChatModel auto-configuration matches.");
        }
        if (aiProperties.generation().enabled() && !apiKeyPresent) {
            log.warn("AI generation is enabled but OPENAI_API_KEY is not present; OpenAI generation will remain unavailable.");
        }
        if (aiProperties.generation().enabled() && !chatModelPresent) {
            log.warn("AI generation is enabled but no Spring AI ChatModel bean is present; check spring.ai.model.chat and Spring AI OpenAI auto-configuration.");
        }
        if (aiProperties.generation().enabled() && !openAiProviderPresent) {
            log.warn("AI generation is enabled but OpenAiGenerationProvider is not present; check app.ai.generation.provider resolves exactly to openai.");
        }
    }

    private boolean openAiApiKeyPresent() {
        return hasText(System.getenv("OPENAI_API_KEY"))
                || hasText(System.getProperty("OPENAI_API_KEY"))
                || hasText(environment.getProperty("spring.ai.openai.api-key"));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
