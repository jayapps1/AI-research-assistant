package com.researchassistant;

import com.researchassistant.document.config.DocumentProperties;
import com.researchassistant.document.embedding.EmbeddingProperties;
import com.researchassistant.rag.config.RagProperties;
import com.researchassistant.retrieval.service.RetrievalProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		DocumentProperties.class,
		EmbeddingProperties.class,
		RagProperties.class,
		RetrievalProperties.class
})
public class ResearchAssistantApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResearchAssistantApiApplication.class, args);
	}

}
