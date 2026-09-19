package com.researchassistant;

import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.admin.SuperAdminSeedProperties;
import com.researchassistant.cache.AppCacheProperties;
import com.researchassistant.collaboration.service.CollaborationProperties;
import com.researchassistant.document.config.DocumentProperties;
import com.researchassistant.document.embedding.EmbeddingProperties;
import com.researchassistant.operations.backup.BackupProperties;
import com.researchassistant.rag.config.RagProperties;
import com.researchassistant.retrieval.service.RetrievalProperties;

import com.researchassistant.subscription.SubscriptionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
		DocumentProperties.class,
		AiProperties.class,
		EmbeddingProperties.class,
		AppCacheProperties.class,
		BackupProperties.class,
		CollaborationProperties.class,
		RagProperties.class,
		RetrievalProperties.class,
		SuperAdminSeedProperties.class,
		SubscriptionProperties.class
})
public class ResearchAssistantApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResearchAssistantApiApplication.class, args);
	}

}
