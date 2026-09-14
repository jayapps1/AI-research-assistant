package com.researchassistant.rag.generation;

import com.researchassistant.rag.evidence.EvidenceBundle;

public interface GroundedAnswerGenerator {

    boolean available();

    GeneratedAnswerDraft generate(EvidenceBundle evidenceBundle);
}
