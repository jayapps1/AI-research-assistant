package com.researchassistant.retrieval.service;

import com.researchassistant.retrieval.dto.DocumentRetrievalMode;
import com.researchassistant.retrieval.dto.DocumentRetrievalRequest;
import com.researchassistant.retrieval.dto.RetrievalSearchRequest;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RetrievalRequestFactory {

    public DocumentRetrievalRequest fromApi(
            UUID projectId,
            RetrievalSearchRequest request
    ) {
        boolean selected = request.documentIds() != null
                && !request.documentIds().isEmpty();
        return new DocumentRetrievalRequest(
                null,
                projectId,
                request.documentIds(),
                request.query(),
                request.limit() == null ? 0 : request.limit(),
                true,
                true,
                selected
                        ? DocumentRetrievalMode.SELECTED_DOCUMENTS
                        : DocumentRetrievalMode.PROJECT_ALL_DOCUMENTS
        );
    }
}
