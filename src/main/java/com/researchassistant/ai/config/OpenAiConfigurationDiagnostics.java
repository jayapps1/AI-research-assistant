package com.researchassistant.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OpenAiConfigurationDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(OpenAiConfigurationDiagnostics.class);

    private final AiProperties aiProperties;
    private final Environment environment;

    public OpenAiConfigurationDiagnostics(AiProperties aiProperties, Environment environment) {
        this.aiProperties = aiProperties;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void report() {
        log.info("OPENAI_API_KEY present = {}", openAiApiKeyPresent() ? "YES" : "NO");
        log.info("generation enabled = {}", aiProperties.generation().enabled() ? "YES" : "NO");
        log.info("generation provider = {}", aiProperties.generation().provider());
        log.info("generation model = {}", aiProperties.generation().model());
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
