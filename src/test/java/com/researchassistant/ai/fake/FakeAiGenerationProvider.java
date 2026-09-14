package com.researchassistant.ai.fake;

import com.researchassistant.ai.orchestration.AiTaskRequest;
import com.researchassistant.ai.orchestration.AiTaskResult;
import com.researchassistant.ai.provider.AiGenerationProvider;
import com.researchassistant.ai.provider.AiProviderType;
import com.researchassistant.ai.usage.AiRequestStatus;
import com.researchassistant.rag.evidence.EvidenceBundle;
import com.researchassistant.rag.generation.GeneratedAnswerDraft;
import com.researchassistant.rag.generation.GeneratedCitation;
import com.researchassistant.rag.generation.GroundedAnswerGenerator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class FakeAiGenerationProvider implements AiGenerationProvider, GroundedAnswerGenerator {

    private boolean available = true;

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String providerName() {
        return "FAKE_OPENAI";
    }

    @Override
    public String modelName() {
        return "fake-gpt-4";
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> AiTaskResult<T> generate(AiTaskRequest request, Class<T> responseType) {
        if (!available) {
            throw new IllegalStateException("Fake provider unavailable");
        }
        return new AiTaskResult<>(
                UUID.randomUUID(),
                request.taskType(),
                AiProviderType.OPENAI,
                modelName(),
                AiRequestStatus.COMPLETED,
                null,
                100,
                50,
                150,
                250L,
                null,
                null,
                "fake-req-123",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of()
        );
    }

    @Override
    public GeneratedAnswerDraft generate(EvidenceBundle evidenceBundle) {
        return new GeneratedAnswerDraft(
                "This is a fake grounded answer based on supplied evidence E1.",
                List.of(new GeneratedCitation(1, "Fake claim supported by E1.")),
                providerName(),
                modelName(),
                100,
                50,
                150L,
                "COMPLETED"
        );
    }
}
