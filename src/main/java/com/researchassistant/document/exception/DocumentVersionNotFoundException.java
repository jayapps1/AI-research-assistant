package com.researchassistant.document.exception;

import com.researchassistant.common.exception.ResourceNotFoundException;

public class DocumentVersionNotFoundException extends ResourceNotFoundException {

    public DocumentVersionNotFoundException() {
        super("Document version not found.");
    }
}
