package com.researchassistant.collaboration.service;

import com.researchassistant.collaboration.dto.CollaborationDtos.ProjectActivityResponse;
import com.researchassistant.collaboration.entity.CollaborationArtifactType;
import com.researchassistant.collaboration.entity.ProjectActivity;
import com.researchassistant.collaboration.entity.ProjectActivityType;
import com.researchassistant.collaboration.event.ProjectCollaborationEvent;
import com.researchassistant.collaboration.repository.ProjectActivityRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.project.service.ProjectAuthorizationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProjectActivityService {
    private final ProjectActivityRepository repository;
    private final ResearchProjectRepository projectRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ApplicationEventPublisher eventPublisher;

    public ProjectActivityService(ProjectActivityRepository repository, ResearchProjectRepository projectRepository, ProjectAuthorizationService authorizationService, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void record(ResearchProject project, User actor, ProjectActivityType type, CollaborationArtifactType artifactType, UUID artifactId, String safeSummary) {
        ProjectActivity activity = new ProjectActivity();
        activity.setProject(project);
        activity.setActor(actor);
        activity.setType(type);
        activity.setArtifactType(artifactType);
        activity.setArtifactId(artifactId);
        activity.setSafeSummary(safeSummary == null ? null : safeSummary.substring(0, Math.min(500, safeSummary.length())));
        repository.save(activity);
        eventPublisher.publishEvent(new ProjectCollaborationEvent(
                project.getId(),
                actor == null ? null : actor.getId(),
                type,
                artifactType,
                artifactId
        ));
    }

    @Transactional(readOnly = true)
    public Page<ProjectActivityResponse> list(UUID projectId, User user, UUID memberUserId, ProjectActivityType type, CollaborationArtifactType artifactType, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, user);
        Page<ProjectActivity> page;
        if (memberUserId != null) {
            page = repository.findAllByProjectIdAndActorIdOrderByOccurredAtDesc(projectId, memberUserId, pageable);
        } else if (type != null) {
            page = repository.findAllByProjectIdAndTypeOrderByOccurredAtDesc(projectId, type, pageable);
        } else if (artifactType != null) {
            page = repository.findAllByProjectIdAndArtifactTypeOrderByOccurredAtDesc(projectId, artifactType, pageable);
        } else {
            page = repository.findAllByProjectIdOrderByOccurredAtDesc(projectId, pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional
    public void record(UUID projectId, User actor, ProjectActivityType type, CollaborationArtifactType artifactType, UUID artifactId, String safeSummary) {
        ResearchProject project = projectRepository.getReferenceById(projectId);
        record(project, actor, type, artifactType, artifactId, safeSummary);
    }

    private ProjectActivityResponse toResponse(ProjectActivity activity) {
        return new ProjectActivityResponse(
                activity.getId(),
                activity.getProject().getId(),
                activity.getActor() == null ? null : activity.getActor().getId(),
                activity.getType(),
                activity.getArtifactType(),
                activity.getArtifactId(),
                activity.getSafeSummary(),
                activity.getMetadataJson(),
                activity.getOccurredAt()
        );
    }
}
