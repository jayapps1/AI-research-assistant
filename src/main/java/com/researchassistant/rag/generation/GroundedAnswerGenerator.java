package com.researchassistant.rag.generation;

import com.researchassistant.rag.evidence.EvidenceBundle;

public interface GroundedAnswerGenerator {

    default String providerName() {
        return "none";
    }

    default String modelName() {
        return "none";
    }

    boolean available();

    GeneratedAnswerDraft generate(EvidenceBundle evidenceBundle);
}
