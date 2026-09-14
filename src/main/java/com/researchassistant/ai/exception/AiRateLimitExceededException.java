package com.researchassistant.ai.exception;

public class AiRateLimitExceededException extends RuntimeException {

    public AiRateLimitExceededException(String message) {
        super(message);
    }
}
