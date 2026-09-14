package com.researchassistant.collaboration.event;

import com.researchassistant.collaboration.entity.CollaborationArtifactType;
import com.researchassistant.collaboration.entity.ProjectActivityType;

import java.util.UUID;

public record ProjectCollaborationEvent(
        UUID projectId,
        UUID actorUserId,
        ProjectActivityType type,
        CollaborationArtifactType artifactType,
        UUID artifactId
) {
}
