package com.researchassistant.project.exception;

import com.researchassistant.common.exception.ResourceNotFoundException;

public class ResearchProjectNotFoundException extends ResourceNotFoundException {

    public ResearchProjectNotFoundException() {
        super("Research project not found.");
    }
}
