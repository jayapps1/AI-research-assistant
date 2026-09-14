package com.researchassistant.rag.generation;

import org.springframework.stereotype.Component;

@Component
public class DisabledGroundedAnswerGenerator implements GroundedAnswerGenerator {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public GeneratedAnswerDraft generate(
            com.researchassistant.rag.evidence.EvidenceBundle evidenceBundle
    ) {
        throw new IllegalStateException("Grounded answer generation is disabled.");
    }
}
