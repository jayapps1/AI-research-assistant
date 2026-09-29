package com.researchassistant.conversation.exception;

public class ConversationAccessDeniedException extends RuntimeException {

    public ConversationAccessDeniedException(String message) {
        super(message);
    }
}
