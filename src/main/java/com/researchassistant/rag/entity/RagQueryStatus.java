package com.researchassistant.rag.entity;

public enum RagQueryStatus {
    RECEIVED,
    RETRIEVING,
    EVIDENCE_READY,
    GENERATING,
    COMPLETED,
    INSUFFICIENT_EVIDENCE,
    FAILED
}
