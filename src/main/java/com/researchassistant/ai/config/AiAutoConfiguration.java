package com.researchassistant.ai.config;

import com.researchassistant.ai.provider.AiGenerationProvider;
import com.researchassistant.ai.provider.DisabledAiGenerationProvider;
import com.researchassistant.document.embedding.DisabledDocumentEmbeddingProvider;
import com.researchassistant.document.embedding.DocumentEmbeddingProvider;
import com.researchassistant.document.embedding.EmbeddingProperties;
import com.researchassistant.rag.generation.DisabledGroundedAnswerGenerator;
import com.researchassistant.rag.generation.GroundedAnswerGenerator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AiGenerationProvider.class)
    public AiGenerationProvider disabledAiGenerationProvider() {
        return new DisabledAiGenerationProvider();
    }

    @Bean
    @ConditionalOnMissingBean(DocumentEmbeddingProvider.class)
    public DocumentEmbeddingProvider disabledDocumentEmbeddingProvider(EmbeddingProperties properties) {
        return new DisabledDocumentEmbeddingProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean(GroundedAnswerGenerator.class)
    public GroundedAnswerGenerator disabledGroundedAnswerGenerator() {
        return new DisabledGroundedAnswerGenerator();
    }
}
