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
		loadDotenv();
		SpringApplication.run(ResearchAssistantApiApplication.class, args);
	}

	private static void loadDotenv() {
		try {
			java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
			if (!java.nio.file.Files.exists(envPath)) {
				envPath = java.nio.file.Paths.get("..", ".env");
			}
			if (java.nio.file.Files.exists(envPath)) {
				for (String line : java.nio.file.Files.readAllLines(envPath, java.nio.charset.StandardCharsets.UTF_8)) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
						continue;
					}
					int eq = line.indexOf('=');
					String key = line.substring(0, eq).trim();
					String value = line.substring(eq + 1).trim();
					if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
						value = value.substring(1, value.length() - 1);
					} else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
						value = value.substring(1, value.length() - 1);
					}
					if (!key.isEmpty()) {
						System.setProperty(key, value);
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

}
