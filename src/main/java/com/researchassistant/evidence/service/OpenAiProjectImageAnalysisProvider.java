package com.researchassistant.evidence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.evidence.dto.ProjectEvidenceDtos.ProjectImageAnalysisResult;
import com.researchassistant.evidence.entity.EvidenceAnalysisStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAiProjectImageAnalysisProvider implements ProjectImageAnalysisProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProjectImageAnalysisProvider.class);

    private final AiProperties properties;
    private final ChatModel chatModel;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    public OpenAiProjectImageAnalysisProvider(
            AiProperties properties,
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectProvider<Environment> environmentProvider,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.chatModel = chatModelProvider.getIfAvailable();
        this.environment = environmentProvider.getIfAvailable();
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() {
        return "OPENAI_VISION";
    }

    @Override
    public boolean isAvailable() {
        return properties.generation().enabled() && chatModel != null && openAiApiKeyPresent();
    }

    private boolean openAiApiKeyPresent() {
        return hasText(System.getenv("OPENAI_API_KEY"))
                || hasText(System.getProperty("OPENAI_API_KEY"))
                || (environment != null && hasText(environment.getProperty("spring.ai.openai.api-key")));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public ProjectImageAnalysisResult analyzeImage(
            byte[] imageBytes,
            String mimeType,
            String filename,
            String sectionTitle,
            String projectTitle,
            String projectDescription
    ) {
        if (!isAvailable()) {
            return new ProjectImageAnalysisResult(
                    EvidenceAnalysisStatus.UNAVAILABLE,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    "OpenAI multimodal vision provider is not available or configured. Manual description can be provided."
            );
        }

        try {
            String safeMime = (mimeType != null && mimeType.startsWith("image/")) ? mimeType : "image/png";
            Media media = Media.builder()
                    .mimeType(MimeTypeUtils.parseMimeType(safeMime))
                    .data(imageBytes)
                    .build();

            String systemPrompt = """
                You are an objective academic research assistant analyzing project evidence, software screenshots, diagrams, and interface figures for an academic report.
                Your task is to analyze STRICTLY what is visibly observable in the provided image.

                Analyze and produce structured JSON with these exact fields:
                {
                  "visibleSummary": "A concise, formal academic paragraph describing what is visibly shown on the screen/diagram (e.g. 'The figure shows the user dashboard interface...').",
                  "observableComponents": ["List of clearly visible UI components, buttons, form fields, table columns, or diagram nodes"],
                  "observableWorkflow": "Observable user actions or process flow indicated directly by visible elements",
                  "sectionRelevance": "Explanation of how this visible artifact supports the target section and objectives"
                }

                CRITICAL ACADEMIC INTEGRITY CONSTRAINTS:
                - Do NOT infer, assume, or fabricate invisible backend features, hidden architectures, encryption, or unobserved security mechanisms unless explicitly proven by visible labels.
                - Report ONLY observable details present in the image.
                - Never invent pass/fail status or claim untested functionality.
                - Respond with valid JSON only.
                """;

            String userPrompt = String.format(
                    "Target Section: %s\nProject Title: %s\nProject Context: %s\nImage Filename: %s\n\nPlease analyze the visible elements in this figure.",
                    sectionTitle != null ? sectionTitle : "System Results / Implementation",
                    projectTitle != null ? projectTitle : "Academic Project",
                    projectDescription != null ? projectDescription : "Project implementation and evaluation",
                    filename != null ? filename : "evidence.png"
            );

            SystemMessage sysMsg = new SystemMessage(systemPrompt);
            UserMessage userMsg = UserMessage.builder().text(userPrompt).media(media).build();

            String model = (properties.generation().model() != null && !properties.generation().model().isBlank())
                    ? properties.generation().model()
                    : "gpt-4o-mini";

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(model)
                    .temperature(0.2)
                    .maxTokens(1000)
                    .build();

            Prompt prompt = new Prompt(List.of(sysMsg, userMsg), options);
            ChatResponse response = chatModel.call(prompt);

            String content = response.getResult().getOutput().getText();

            if (content == null || content.isBlank()) {
                return new ProjectImageAnalysisResult(
                        EvidenceAnalysisStatus.FAILED,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        "Vision model returned empty response."
                );
            }

            return parseAnalysisResponse(content);

        } catch (Exception ex) {
            log.warn("Image analysis failed: {}", ex.getMessage());
            return new ProjectImageAnalysisResult(
                    EvidenceAnalysisStatus.FAILED,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    "Failed to analyze image: " + ex.getMessage()
            );
        }
    }

    private ProjectImageAnalysisResult parseAnalysisResponse(String text) {
        try {
            String jsonText = text.trim();
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7);
            } else if (jsonText.startsWith("```")) {
                jsonText = jsonText.substring(3);
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length() - 3);
            }
            jsonText = jsonText.trim();

            JsonNode root = objectMapper.readTree(jsonText);
            String summary = root.has("visibleSummary") ? root.get("visibleSummary").asText() : "";
            List<String> components = new ArrayList<>();
            if (root.has("observableComponents") && root.get("observableComponents").isArray()) {
                for (JsonNode n : root.get("observableComponents")) {
                    components.add(n.asText());
                }
            }
            String workflow = root.has("observableWorkflow") ? root.get("observableWorkflow").asText() : "";
            String relevance = root.has("sectionRelevance") ? root.get("sectionRelevance").asText() : "";

            return new ProjectImageAnalysisResult(
                    EvidenceAnalysisStatus.COMPLETED,
                    summary,
                    components,
                    workflow,
                    relevance,
                    text,
                    null
            );
        } catch (Exception ex) {
            // Fallback: use raw text as summary
            return new ProjectImageAnalysisResult(
                    EvidenceAnalysisStatus.COMPLETED,
                    text.trim(),
                    List.of(),
                    null,
                    null,
                    text,
                    null
            );
        }
    }
}
