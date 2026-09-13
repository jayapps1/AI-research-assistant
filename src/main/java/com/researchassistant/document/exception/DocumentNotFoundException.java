package com.researchassistant.document.exception;

import com.researchassistant.common.exception.ResourceNotFoundException;

public class DocumentNotFoundException extends ResourceNotFoundException {

    public DocumentNotFoundException() {
        super("Document not found.");
    }
}
