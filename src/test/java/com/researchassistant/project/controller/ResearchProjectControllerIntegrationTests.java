package com.researchassistant.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.identity.dto.AuthTokenResponse;
import com.researchassistant.identity.dto.CreateUserRequest;
import com.researchassistant.identity.dto.UserResponse;
import com.researchassistant.identity.service.UserService;
import com.researchassistant.project.dto.ResearchProjectResponse;
import com.researchassistant.project.entity.ProjectMembership;
import com.researchassistant.project.entity.ProjectMembershipStatus;
import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.entity.ResearchProjectStatus;
import com.researchassistant.project.repository.ProjectMembershipRepository;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.workspace.dto.WorkspaceResponse;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.entity.WorkspaceType;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Transactional
class ResearchProjectControllerIntegrationTests {

    private static final String PASSWORD = "correct-password-123";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserService userService;

    @Autowired
    private ResearchProjectRepository projectRepository;

    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;

    @Autowired
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Test
    void ownerAndAdminCanCreateProjectAndCreatorBecomesLead()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse admin = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String adminToken = login(admin.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(ownerToken);
        addWorkspaceMember(ownerToken, workspace.id(), admin.email(), "ADMIN")
                .andExpect(status().isCreated());

        ResearchProjectResponse ownerProject = createProject(
                ownerToken,
                workspace.id(),
                "Owner Project"
        );
        ResearchProjectResponse adminProject = createProject(
                adminToken,
                workspace.id(),
                "Admin Project"
        );

        assertProjectCreatorLead(ownerProject.id(), owner.id());
        assertProjectCreatorLead(adminProject.id(), admin.id());
        assertThat(projectRepository.findById(ownerProject.id()).orElseThrow()
                .getNextDocumentNumber()).isEqualTo(1L);
    }

    @Test
    void workspaceMemberCannotCreateProjectAndApiCannotOverrideCounter()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse member = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String memberToken = login(member.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(ownerToken);
        addWorkspaceMember(ownerToken, workspace.id(), member.email(), "MEMBER")
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/projects",
                        workspace.id())
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workspaceId", workspace.id(),
                                "title", "Blocked"
                        ))))
                .andExpect(status().isForbidden());

        String responseJson = mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/projects",
                        workspace.id()
                )
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "%s",
                                  "title": "Counter Project",
                                  "nextDocumentNumber": 999
                                }
                                """.formatted(workspace.id())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", not(hasKey("nextDocumentNumber"))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResearchProjectResponse project = objectMapper.readValue(
                responseJson,
                ResearchProjectResponse.class
        );

        assertThat(projectRepository.findById(project.id()).orElseThrow()
                .getNextDocumentNumber()).isEqualTo(1L);
    }

    @Test
    void projectListingRespectsWorkspaceAndProjectMembership()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse admin = createUser();
        UserResponse member = createUser();
        UserResponse noProjectMember = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String adminToken = login(admin.email()).accessToken();
        String memberToken = login(member.email()).accessToken();
        String noProjectToken = login(noProjectMember.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(ownerToken);
        addWorkspaceMember(ownerToken, workspace.id(), admin.email(), "ADMIN")
                .andExpect(status().isCreated());
        addWorkspaceMember(ownerToken, workspace.id(), member.email(), "MEMBER")
                .andExpect(status().isCreated());
        addWorkspaceMember(ownerToken, workspace.id(), noProjectMember.email(), "MEMBER")
                .andExpect(status().isCreated());

        ResearchProjectResponse visible = createProject(
                ownerToken,
                workspace.id(),
                "Visible"
        );
        createProject(ownerToken, workspace.id(), "Other");
        addProjectMember(ownerToken, visible.id(), member.email(), "VIEWER")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects",
                        workspace.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects",
                        workspace.id())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects",
                        workspace.id())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(visible.id().toString()));

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects",
                        workspace.id())
                        .header("Authorization", "Bearer " + noProjectToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        setWorkspaceMembershipStatus(
                workspace.id(),
                member.id(),
                WorkspaceMembershipStatus.REMOVED
        );

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects",
                        workspace.id())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void projectDetailRequiresProjectAccessAndActiveWorkspaceMembership()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse editor = createUser();
        UserResponse viewer = createUser();
        UserResponse unrelated = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String editorToken = login(editor.email()).accessToken();
        String viewerToken = login(viewer.email()).accessToken();
        String unrelatedToken = login(unrelated.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(ownerToken);
        addWorkspaceMember(ownerToken, workspace.id(), editor.email(), "MEMBER")
                .andExpect(status().isCreated());
        addWorkspaceMember(ownerToken, workspace.id(), viewer.email(), "MEMBER")
                .andExpect(status().isCreated());

        ResearchProjectResponse project =
                createProject(ownerToken, workspace.id(), "Detail");
        addProjectMember(ownerToken, project.id(), editor.email(), "EDITOR")
                .andExpect(status().isCreated());
        addProjectMember(ownerToken, project.id(), viewer.email(), "VIEWER")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/{projectId}", project.id())
                        .header("Authorization", "Bearer " + editorToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/projects/{projectId}", project.id())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/projects/{projectId}", project.id())
                        .header("Authorization", "Bearer " + unrelatedToken))
                .andExpect(status().isNotFound());

        setWorkspaceMembershipStatus(
                workspace.id(),
                viewer.id(),
                WorkspaceMembershipStatus.SUSPENDED
        );

        mockMvc.perform(get("/api/v1/projects/{projectId}", project.id())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerAdminAndLeadCanUpdateButViewerCannot()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse admin = createUser();
        UserResponse lead = createUser();
        UserResponse viewer = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String adminToken = login(admin.email()).accessToken();
        String leadToken = login(lead.email()).accessToken();
        String viewerToken = login(viewer.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(ownerToken);
        addWorkspaceMember(ownerToken, workspace.id(), admin.email(), "ADMIN")
                .andExpect(status().isCreated());
        addWorkspaceMember(ownerToken, workspace.id(), lead.email(), "MEMBER")
                .andExpect(status().isCreated());
        addWorkspaceMember(ownerToken, workspace.id(), viewer.email(), "MEMBER")
                .andExpect(status().isCreated());
        ResearchProjectResponse project =
                createProject(ownerToken, workspace.id(), "Update");
        addProjectMember(ownerToken, project.id(), lead.email(), "LEAD")
                .andExpect(status().isCreated());
        addProjectMember(ownerToken, project.id(), viewer.email(), "VIEWER")
                .andExpect(status().isCreated());

        updateProject(ownerToken, project.id(), "Owner Update")
                .andExpect(status().isOk());
        updateProject(adminToken, project.id(), "Admin Update")
                .andExpect(status().isOk());
        updateProject(leadToken, project.id(), "Lead Update")
                .andExpect(status().isOk());
        updateProject(viewerToken, project.id(), "Viewer Update")
                .andExpect(status().isForbidden());

        ResearchProject stored = projectRepository.findById(project.id())
                .orElseThrow();
        assertThat(stored.getNextDocumentNumber()).isEqualTo(1L);
    }

    @Test
    void statusTransitionsDoNotResetDocumentCounter()
            throws Exception {

        UserResponse owner = createUser();
        String ownerToken = login(owner.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(ownerToken);
        ResearchProjectResponse project =
                createProject(ownerToken, workspace.id(), "Lifecycle");

        mockMvc.perform(post("/api/v1/projects/{projectId}/activate",
                        project.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(post("/api/v1/projects/{projectId}/complete",
                        project.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(post("/api/v1/projects/{projectId}/archive",
                        project.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mockMvc.perform(post("/api/v1/projects/{projectId}/activate",
                        project.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict());

        ResearchProject stored = projectRepository.findById(project.id())
                .orElseThrow();
        assertThat(stored.getNextDocumentNumber()).isEqualTo(1L);
        assertThat(stored.getStatus()).isEqualTo(ResearchProjectStatus.ARCHIVED);
    }

    @Test
    void projectMembershipRulesAreEnforced()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse lead = createUser();
        UserResponse editor = createUser();
        UserResponse viewer = createUser();
        UserResponse outsideWorkspace = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String leadToken = login(lead.email()).accessToken();
        String viewerToken = login(viewer.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(ownerToken);
        addWorkspaceMember(ownerToken, workspace.id(), lead.email(), "MEMBER")
                .andExpect(status().isCreated());
        addWorkspaceMember(ownerToken, workspace.id(), editor.email(), "MEMBER")
                .andExpect(status().isCreated());
        addWorkspaceMember(ownerToken, workspace.id(), viewer.email(), "MEMBER")
                .andExpect(status().isCreated());
        ResearchProjectResponse project =
                createProject(ownerToken, workspace.id(), "Membership");
        addProjectMember(ownerToken, project.id(), lead.email(), "LEAD")
                .andExpect(status().isCreated());

        addProjectMember(leadToken, project.id(), editor.email(), "EDITOR")
                .andExpect(status().isCreated());
        addProjectMember(leadToken, project.id(), viewer.email(), "VIEWER")
                .andExpect(status().isCreated());
        addProjectMember(leadToken, project.id(), outsideWorkspace.email(), "VIEWER")
                .andExpect(status().isConflict());
        addProjectMember(leadToken, project.id(), editor.email(), "EDITOR")
                .andExpect(status().isConflict());
        addProjectMember(leadToken, project.id(), viewer.email(), "LEAD")
                .andExpect(status().isConflict());
        addProjectMember(viewerToken, project.id(), outsideWorkspace.email(), "VIEWER")
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/projects/{projectId}/members",
                        project.id())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
    }

    @Test
    void removalRevokesProjectAccessButKeepsWorkspaceMembership()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse viewer = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String viewerToken = login(viewer.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(ownerToken);
        addWorkspaceMember(ownerToken, workspace.id(), viewer.email(), "MEMBER")
                .andExpect(status().isCreated());
        ResearchProjectResponse project =
                createProject(ownerToken, workspace.id(), "Removal");
        addProjectMember(ownerToken, project.id(), viewer.email(), "VIEWER")
                .andExpect(status().isCreated());

        removeProjectMember(ownerToken, project.id(), viewer.id())
                .andExpect(status().isNoContent());

        ProjectMembership membership =
                projectMembershipRepository.findByProjectIdAndUserId(
                        project.id(),
                        viewer.id()
                ).orElseThrow();
        assertThat(membership.getStatus())
                .isEqualTo(ProjectMembershipStatus.REMOVED);

        WorkspaceMembership workspaceMembership =
                workspaceMembershipRepository.findByWorkspaceIdAndUserId(
                        workspace.id(),
                        viewer.id()
                ).orElseThrow();
        assertThat(workspaceMembership.getStatus())
                .isEqualTo(WorkspaceMembershipStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/projects/{projectId}", project.id())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void finalLeadCannotBeRemovedOrDemoted()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse lead = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String leadToken = login(lead.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(ownerToken);
        addWorkspaceMember(ownerToken, workspace.id(), lead.email(), "MEMBER")
                .andExpect(status().isCreated());
        ResearchProjectResponse project =
                createProject(ownerToken, workspace.id(), "Lead Invariant");
        addProjectMember(ownerToken, project.id(), lead.email(), "LEAD")
                .andExpect(status().isCreated());
        removeProjectMember(ownerToken, project.id(), owner.id())
                .andExpect(status().isNoContent());

        changeProjectRole(ownerToken, project.id(), lead.id(), "VIEWER")
                .andExpect(status().isConflict());
        removeProjectMember(leadToken, project.id(), lead.id())
                .andExpect(status().isConflict());

        long activeLeads =
                projectMembershipRepository.countByProjectIdAndRoleAndStatus(
                        project.id(),
                        ProjectRole.LEAD,
                        ProjectMembershipStatus.ACTIVE
                );

        assertThat(activeLeads).isEqualTo(1L);
    }

    private void assertProjectCreatorLead(UUID projectId, UUID creatorId) {
        ProjectMembership membership =
                projectMembershipRepository.findByProjectIdAndUserId(
                        projectId,
                        creatorId
                ).orElseThrow();
        assertThat(membership.getRole()).isEqualTo(ProjectRole.LEAD);
        assertThat(membership.getStatus())
                .isEqualTo(ProjectMembershipStatus.ACTIVE);
    }

    private UserResponse createUser() {
        String email = "project-test-" + UUID.randomUUID() + "@example.com";
        return userService.createUser(new CreateUserRequest(
                email,
                PASSWORD,
                "Project",
                "Test",
                "en"
        ));
    }

    private AuthTokenResponse login(String email) throws Exception {
        String responseJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(responseJson, AuthTokenResponse.class);
    }

    private WorkspaceResponse createWorkspace(String accessToken)
            throws Exception {
        String responseJson = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Workspace " + UUID.randomUUID(),
                                "type", WorkspaceType.ORGANIZATION.name()
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(responseJson, WorkspaceResponse.class);
    }

    private ResearchProjectResponse createProject(
            String accessToken,
            UUID workspaceId,
            String title
    ) throws Exception {
        String responseJson = mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/projects",
                        workspaceId
                )
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workspaceId", workspaceId,
                                "title", title,
                                "description", "Description"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(
                responseJson,
                ResearchProjectResponse.class
        );
    }

    private ResultActions addWorkspaceMember(
            String accessToken,
            UUID workspaceId,
            String email,
            String role
    ) throws Exception {
        return mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/members",
                        workspaceId
                )
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "role", role
                        ))));
    }

    private ResultActions addProjectMember(
            String accessToken,
            UUID projectId,
            String email,
            String role
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/projects/{projectId}/members",
                        projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "role", role
                        ))));
    }

    private ResultActions updateProject(
            String accessToken,
            UUID projectId,
            String title
    ) throws Exception {
        return mockMvc.perform(patch("/api/v1/projects/{projectId}", projectId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "%s",
                          "nextDocumentNumber": 50
                        }
                        """.formatted(title)));
    }

    private ResultActions changeProjectRole(
            String accessToken,
            UUID projectId,
            UUID userId,
            String role
    ) throws Exception {
        return mockMvc.perform(patch(
                        "/api/v1/projects/{projectId}/members/{userId}/role",
                        projectId,
                        userId
                )
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "role", role
                        ))));
    }

    private ResultActions removeProjectMember(
            String accessToken,
            UUID projectId,
            UUID userId
    ) throws Exception {
        return mockMvc.perform(delete(
                        "/api/v1/projects/{projectId}/members/{userId}",
                        projectId,
                        userId
                )
                        .header("Authorization", "Bearer " + accessToken));
    }

    @Test
    void createProject_withoutWorkspaceIdInBody_succeedsAndDefaultsToPathWorkspace()
            throws Exception {
        UserResponse owner = createUser();
        String ownerToken = login(owner.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(ownerToken);

        String title = "Path Only Project " + UUID.randomUUID();
        String responseJson = mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/projects",
                        workspace.id()
                )
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Created with path workspace only"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.workspaceId").value(workspace.id().toString()))
                .andExpect(jsonPath("$.currentUserRole").value("LEAD"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResearchProjectResponse project = objectMapper.readValue(
                responseJson,
                ResearchProjectResponse.class
        );

        ResearchProject entity = projectRepository.findById(project.id()).orElseThrow();
        assertThat(entity.getNextDocumentNumber()).isEqualTo(1L);
        assertThat(entity.getStatus()).isEqualTo(ResearchProjectStatus.DRAFT);
        assertThat(entity.getCreatedBy().getId()).isEqualTo(owner.id());

        // Test GET /api/v1/projects/mine
        mockMvc.perform(get("/api/v1/projects/mine")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(project.id().toString()))
                .andExpect(jsonPath("$.content[0].title").value(title));

        // Test filtered GET /api/v1/workspaces/{workspaceId}/projects with q and status
        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects", workspace.id())
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("q", "Path Only")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(project.id().toString()));

        // Filter that does not match
        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects", workspace.id())
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("q", "NonExistentTerm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private void setWorkspaceMembershipStatus(
            UUID workspaceId,
            UUID userId,
            WorkspaceMembershipStatus status
    ) {
        WorkspaceMembership membership =
                workspaceMembershipRepository.findByWorkspaceIdAndUserId(
                        workspaceId,
                        userId
                ).orElseThrow();
        membership.setStatus(status);
    }
}
