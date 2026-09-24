package com.researchassistant.rag.generation;

import com.researchassistant.rag.evidence.EvidenceBundle;
import com.researchassistant.rag.citation.CitationVerificationResult;

public interface GroundedAnswerGenerator {

    default String providerName() {
        return "none";
    }

    default String modelName() {
        return "none";
    }

    boolean available();

    GeneratedAnswerDraft generate(EvidenceBundle evidenceBundle);

    default boolean supportsCitationRepair() {
        return false;
    }

    default GeneratedAnswerDraft repairCitations(
            EvidenceBundle evidenceBundle,
            GeneratedAnswerDraft draft,
            CitationVerificationResult verification
    ) {
        return draft;
    }
}
