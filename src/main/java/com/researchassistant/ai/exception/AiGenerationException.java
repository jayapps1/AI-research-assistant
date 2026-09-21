package com.researchassistant.ai.exception;

public class AiGenerationException extends RuntimeException {

    private final String code;

    public AiGenerationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AiGenerationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
