package com.researchassistant.rag.scope;

import com.researchassistant.document.exception.InvalidDocumentOperationException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RetrievalScopeService {

    private final ProjectAuthorizationService projectAuthorizationService;
    private final RetrievalScopeRepository scopeRepository;

    public RetrievalScopeService(
            ProjectAuthorizationService projectAuthorizationService,
            RetrievalScopeRepository scopeRepository
    ) {
        this.projectAuthorizationService = projectAuthorizationService;
        this.scopeRepository = scopeRepository;
    }

    public RetrievalScope resolve(
            User user,
            UUID projectId,
            RetrievalScopeType scopeType,
            Set<UUID> requestedDocumentIds
    ) {
        ProjectAuthorizationContext context =
                projectAuthorizationService.requireProjectViewer(projectId, user);
        RetrievalScopeType effectiveType = scopeType == null
                ? RetrievalScopeType.PROJECT_ALL_DOCUMENTS
                : scopeType;
        List<RetrievalScopeDocument> documents;
        if (effectiveType == RetrievalScopeType.SELECTED_DOCUMENTS) {
            if (requestedDocumentIds == null || requestedDocumentIds.isEmpty()) {
                throw new InvalidDocumentOperationException(
                        "Selected document retrieval requires documentIds."
                );
            }
            documents = scopeRepository.findEligibleSelectedDocuments(
                    projectId,
                    requestedDocumentIds
            );
            Set<UUID> resolved = new LinkedHashSet<>(
                    documents.stream().map(RetrievalScopeDocument::documentId).toList()
            );
            if (resolved.size() != requestedDocumentIds.size()
                    || !resolved.containsAll(requestedDocumentIds)) {
                throw new InvalidDocumentOperationException(
                        "One or more selected documents are not available for retrieval."
                );
            }
        } else {
            documents = scopeRepository.findEligibleCurrentDocuments(projectId);
        }

        Map<UUID, UUID> versionByDocument = new LinkedHashMap<>();
        for (RetrievalScopeDocument document : documents) {
            versionByDocument.put(document.documentId(), document.documentVersionId());
        }
        return new RetrievalScope(
                context.project().getWorkspace().getId(),
                projectId,
                effectiveType,
                RetrievalVersionPolicy.CURRENT_VERSION_ONLY,
                List.copyOf(versionByDocument.keySet()),
                List.copyOf(versionByDocument.values()),
                versionByDocument,
                versionByDocument.size(),
                OffsetDateTime.now()
        );
    }
}
