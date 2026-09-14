package com.researchassistant.rag.entity;

public enum GroundedAnswerStatus {
    GENERATED,
    VERIFIED,
    REJECTED_UNGROUNDED,
    INSUFFICIENT_EVIDENCE
}
