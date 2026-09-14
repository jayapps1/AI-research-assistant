package com.researchassistant.ai.orchestration;

import com.researchassistant.rag.evidence.EvidenceBundle;
import com.researchassistant.rag.scope.RetrievalScope;

import java.util.UUID;

public record AiTaskRequest(
        AiTaskType taskType,
        UUID userId,
        UUID workspaceId,
        UUID projectId,
        String queryOrInstruction,
        String promptText,
        RetrievalScope retrievalScope,
        EvidenceBundle evidenceBundle,
        Class<?> expectedOutputType,
        boolean externalResearchContentEnabled
) {
}
