package com.researchassistant.document.service;

import com.researchassistant.document.entity.Document;
import com.researchassistant.project.service.ProjectAuthorizationContext;

public record DocumentAuthorizationContext(
        Document document,
        ProjectAuthorizationContext projectContext
) {
}
