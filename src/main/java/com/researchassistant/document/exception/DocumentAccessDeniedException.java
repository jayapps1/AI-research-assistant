package com.researchassistant.document.exception;

public class DocumentAccessDeniedException extends RuntimeException {

    public DocumentAccessDeniedException(String message) {
        super(message);
    }
}
