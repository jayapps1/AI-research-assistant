package com.researchassistant.rag.scope;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RetrievalScopeType {
    PROJECT_ALL_DOCUMENTS,
    SELECTED_DOCUMENTS;

    @JsonCreator
    public static RetrievalScopeType fromString(String value) {
        if (value == null || value.isBlank()) {
            return PROJECT_ALL_DOCUMENTS;
        }
        String normalized = value.trim().toUpperCase();
        if ("ALL_PROJECT_DOCUMENTS".equals(normalized) || "PROJECT_ALL_DOCUMENTS".equals(normalized)) {
            return PROJECT_ALL_DOCUMENTS;
        }
        if ("SELECTED_DOCUMENTS".equals(normalized)) {
            return SELECTED_DOCUMENTS;
        }
        throw new IllegalArgumentException("Unknown retrieval scope type: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
