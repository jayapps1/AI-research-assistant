package com.researchassistant;

import com.researchassistant.document.config.DocumentProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DocumentProperties.class)
public class ResearchAssistantApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResearchAssistantApiApplication.class, args);
	}

}
