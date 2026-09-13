package com.researchassistant.project.service;

import com.researchassistant.project.entity.ProjectMembership;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.workspace.entity.WorkspaceMembership;

import java.util.Optional;

public record ProjectAuthorizationContext(
        ResearchProject project,
        WorkspaceMembership workspaceMembership,
        Optional<ProjectMembership> projectMembership
) {
}
