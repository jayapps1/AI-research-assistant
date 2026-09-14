package com.researchassistant.rag.exception;

public class RagAccessDeniedException extends RuntimeException {

    public RagAccessDeniedException(String message) {
        super(message);
    }
}
