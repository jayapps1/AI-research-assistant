package com.researchassistant.document.service;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.exception.DocumentAccessDeniedException;
import com.researchassistant.document.exception.DocumentNotFoundException;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ProjectMembership;
import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Document authorization boundary.
 *
 * <p>Document UUIDs are never sufficient for access. This service
 * resolves the owning project and delegates to project authorization,
 * which in turn proves ACTIVE parent workspace membership before any
 * document metadata or storage object is exposed.</p>
 */
@Service
@Transactional(readOnly = true)
public class DocumentAuthorizationService {

    private final DocumentRepository documentRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    public DocumentAuthorizationService(
            DocumentRepository documentRepository,
            ProjectAuthorizationService projectAuthorizationService
    ) {
        this.documentRepository = documentRepository;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    public DocumentAuthorizationContext requireDocumentViewer(
            UUID documentId,
            User user
    ) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(DocumentNotFoundException::new);
        ProjectAuthorizationContext projectContext =
                projectAuthorizationService.requireProjectViewer(
                        document.getProject().getId(),
                        user
                );
        return new DocumentAuthorizationContext(document, projectContext);
    }

    public DocumentAuthorizationContext requireDocumentEditor(
            UUID documentId,
            User user
    ) {
        DocumentAuthorizationContext context =
                requireDocumentViewer(documentId, user);

        if (projectAuthorizationService.isWorkspaceAdmin(
                context.projectContext().workspaceMembership()
        ) || context.projectContext().projectMembership()
                .map(ProjectMembership::getRole)
                .filter(role -> role == ProjectRole.LEAD
                        || role == ProjectRole.EDITOR)
                .isPresent()) {
            return context;
        }

        throw new DocumentAccessDeniedException(
                "Document editor access is required."
        );
    }

    public DocumentAuthorizationContext requireDocumentLeadOrWorkspaceAdmin(
            UUID documentId,
            User user
    ) {
        DocumentAuthorizationContext context =
                requireDocumentViewer(documentId, user);

        if (projectAuthorizationService.isWorkspaceAdmin(
                context.projectContext().workspaceMembership()
        ) || context.projectContext().projectMembership()
                .map(ProjectMembership::getRole)
                .filter(role -> role == ProjectRole.LEAD)
                .isPresent()) {
            return context;
        }

        throw new DocumentAccessDeniedException(
                "Document lead access is required."
        );
    }
}
