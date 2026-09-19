package com.researchassistant.workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.identity.dto.AuthTokenResponse;
import com.researchassistant.identity.dto.CreateUserRequest;
import com.researchassistant.identity.dto.UserResponse;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.identity.service.UserService;
import com.researchassistant.workspace.dto.WorkspaceResponse;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.entity.WorkspaceRole;
import com.researchassistant.workspace.entity.WorkspaceStatus;
import com.researchassistant.workspace.entity.WorkspaceType;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;
import com.researchassistant.workspace.repository.WorkspaceRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
class WorkspaceControllerIntegrationTests {

    private static final String PASSWORD = "correct-password-123";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMembershipRepository membershipRepository;

    @Autowired
    private com.researchassistant.subscription.WorkspaceSubscriptionRepository subscriptionRepository;

    @Autowired
    private com.researchassistant.subscription.EntitlementService entitlementService;

    @Test
    void authenticatedUserCreatesWorkspaceAndBecomesOwner()
            throws Exception {

        UserResponse user = createUser();
        String token = login(user.email()).accessToken();

        WorkspaceResponse workspace = createWorkspace(
                token,
                "Research Lab",
                WorkspaceType.ORGANIZATION
        );

        assertThat(workspace.name()).isEqualTo("Research Lab");
        assertThat(workspace.type()).isEqualTo(WorkspaceType.ORGANIZATION);
        assertThat(workspace.status()).isEqualTo(WorkspaceStatus.ACTIVE);
        assertThat(workspace.ownerId()).isEqualTo(user.id());
        assertThat(workspace.currentUserRole()).isEqualTo(WorkspaceRole.OWNER);

        WorkspaceMembership ownerMembership =
                membershipRepository.findByWorkspaceIdAndUserId(
                        workspace.id(),
                        user.id()
                ).orElseThrow();

        assertThat(ownerMembership.getRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(ownerMembership.getStatus())
                .isEqualTo(WorkspaceMembershipStatus.ACTIVE);
        assertThat(ownerMembership.getWorkspace().getOwner().getId())
                .isEqualTo(ownerMembership.getUser().getId());
    }

    @Test
    void unauthenticatedWorkspaceCreationIsRejected()
            throws Exception {

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Research Lab",
                                "type", "PERSONAL"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void workspaceListReturnsOnlyActiveMemberships()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse member = createUser();
        UserResponse unrelated = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String memberToken = login(member.email()).accessToken();

        WorkspaceResponse visible = createWorkspace(
                ownerToken,
                "Visible",
                WorkspaceType.ORGANIZATION
        );
        addMember(ownerToken, visible.id(), member.email(), "MEMBER")
                .andExpect(status().isCreated());

        WorkspaceResponse removed = createWorkspace(
                ownerToken,
                "Removed",
                WorkspaceType.ORGANIZATION
        );
        addMember(ownerToken, removed.id(), member.email(), "MEMBER")
                .andExpect(status().isCreated());
        removeMember(ownerToken, removed.id(), member.id())
                .andExpect(status().isNoContent());

        createWorkspace(
                login(unrelated.email()).accessToken(),
                "Unrelated",
                WorkspaceType.ORGANIZATION
        );

        mockMvc.perform(get("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(visible.id())).exists())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(removed.id())).doesNotExist())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void workspaceDetailRequiresActiveMembership()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse member = createUser();
        UserResponse unrelated = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String memberToken = login(member.email()).accessToken();
        String unrelatedToken = login(unrelated.email()).accessToken();

        WorkspaceResponse workspace = createWorkspace(
                ownerToken,
                "Detail",
                WorkspaceType.ORGANIZATION
        );
        addMember(ownerToken, workspace.id(), member.email(), "MEMBER")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", workspace.id())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workspace.id().toString()));

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", workspace.id())
                        .header("Authorization", "Bearer " + unrelatedToken))
                .andExpect(status().isNotFound());

        membershipRepository.findByWorkspaceIdAndUserId(
                        workspace.id(),
                        member.id()
                )
                .orElseThrow()
                .setStatus(WorkspaceMembershipStatus.SUSPENDED);

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", workspace.id())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerAndAdminCanUpdateWorkspaceButMemberCannot()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse admin = createUser();
        UserResponse member = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String adminToken = login(admin.email()).accessToken();
        String memberToken = login(member.email()).accessToken();

        WorkspaceResponse workspace = createWorkspace(
                ownerToken,
                "Before",
                WorkspaceType.ORGANIZATION
        );
        addMember(ownerToken, workspace.id(), admin.email(), "ADMIN")
                .andExpect(status().isCreated());
        addMember(ownerToken, workspace.id(), member.email(), "MEMBER")
                .andExpect(status().isCreated());

        updateWorkspace(ownerToken, workspace.id(), "Owner Update")
                .andExpect(status().isOk());
        updateWorkspace(adminToken, workspace.id(), "Admin Update")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin Update"));
        updateWorkspace(memberToken, workspace.id(), "Member Update")
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanArchiveWorkspace()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse admin = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String adminToken = login(admin.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(
                ownerToken,
                "Archive",
                WorkspaceType.ORGANIZATION
        );
        addMember(ownerToken, workspace.id(), admin.email(), "ADMIN")
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/archive",
                        workspace.id())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/archive",
                        workspace.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void membershipRulesAreEnforced()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse admin = createUser();
        UserResponse member = createUser();
        UserResponse secondMember = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String adminToken = login(admin.email()).accessToken();
        String memberToken = login(member.email()).accessToken();

        WorkspaceResponse workspace = createWorkspace(
                ownerToken,
                "Members",
                WorkspaceType.ORGANIZATION
        );

        addMember(ownerToken, workspace.id(), admin.email(), "ADMIN")
                .andExpect(status().isCreated());
        addMember(ownerToken, workspace.id(), admin.email(), "ADMIN")
                .andExpect(status().isConflict());
        addMember(memberToken, workspace.id(), secondMember.email(), "MEMBER")
                .andExpect(status().isNotFound());
        addMember(adminToken, workspace.id(), secondMember.email(), "OWNER")
                .andExpect(status().isConflict());
        addMember(adminToken, workspace.id(), secondMember.email(), "ADMIN")
                .andExpect(status().isConflict());
        addMember(adminToken, workspace.id(), secondMember.email(), "MEMBER")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members",
                        workspace.id())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound());

        addMember(ownerToken, workspace.id(), member.email(), "MEMBER")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members",
                        workspace.id())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
    }

    @Test
    void onlyOwnerCanChangeRolesAndCannotDemoteOwner()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse member = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String memberToken = login(member.email()).accessToken();

        WorkspaceResponse workspace = createWorkspace(
                ownerToken,
                "Roles",
                WorkspaceType.ORGANIZATION
        );
        addMember(ownerToken, workspace.id(), member.email(), "MEMBER")
                .andExpect(status().isCreated());

        changeRole(memberToken, workspace.id(), member.id(), "ADMIN")
                .andExpect(status().isForbidden());
        changeRole(ownerToken, workspace.id(), member.id(), "OWNER")
                .andExpect(status().isConflict());
        changeRole(ownerToken, workspace.id(), owner.id(), "MEMBER")
                .andExpect(status().isConflict());
        changeRole(ownerToken, workspace.id(), member.id(), "ADMIN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        WorkspaceMembership ownerMembership =
                membershipRepository.findByWorkspaceIdAndUserId(
                        workspace.id(),
                        owner.id()
                ).orElseThrow();
        assertThat(ownerMembership.getRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(workspaceRepository.findById(workspace.id()).orElseThrow()
                .getOwner()
                .getId()).isEqualTo(ownerMembership.getUser().getId());
    }

    @Test
    void memberRemovalIsLogicalAndRevokesAccess()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse admin = createUser();
        UserResponse member = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String adminToken = login(admin.email()).accessToken();
        String memberToken = login(member.email()).accessToken();

        WorkspaceResponse workspace = createWorkspace(
                ownerToken,
                "Removal",
                WorkspaceType.ORGANIZATION
        );
        addMember(ownerToken, workspace.id(), admin.email(), "ADMIN")
                .andExpect(status().isCreated());
        addMember(ownerToken, workspace.id(), member.email(), "MEMBER")
                .andExpect(status().isCreated());

        removeMember(adminToken, workspace.id(), owner.id())
                .andExpect(status().isConflict());
        removeMember(ownerToken, workspace.id(), owner.id())
                .andExpect(status().isConflict());
        removeMember(adminToken, workspace.id(), member.id())
                .andExpect(status().isNoContent());

        WorkspaceMembership removed =
                membershipRepository.findByWorkspaceIdAndUserId(
                        workspace.id(),
                        member.id()
                ).orElseThrow();

        assertThat(removed.getStatus())
                .isEqualTo(WorkspaceMembershipStatus.REMOVED);

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", workspace.id())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrationAutomaticallyProvisionsPersonalWorkspaceWithOwnerAndFreePlan() {
        UserResponse user = createUser();

        var personalWsOpt = workspaceRepository.findFirstByOwnerIdAndTypeAndStatus(
                user.id(),
                WorkspaceType.PERSONAL,
                WorkspaceStatus.ACTIVE
        );

        assertThat(personalWsOpt).isPresent();
        var personalWs = personalWsOpt.get();
        assertThat(personalWs.getName()).isEqualTo("Workspace's Workspace");

        WorkspaceMembership membership = membershipRepository
                .findByWorkspaceIdAndUserId(personalWs.getId(), user.id())
                .orElseThrow();
        assertThat(membership.getRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(membership.getStatus()).isEqualTo(WorkspaceMembershipStatus.ACTIVE);

        var subscriptionOpt = subscriptionRepository.findActiveSubscription(personalWs.getId());
        assertThat(subscriptionOpt).isPresent();
        var subscription = subscriptionOpt.get();
        assertThat(subscription.getPlan().getCode()).isEqualTo("FREE");
        assertThat(subscription.getStatus()).isEqualTo(com.researchassistant.subscription.WorkspaceSubscriptionStatus.ACTIVE);
        assertThat(subscription.getAccessSource()).isEqualTo(com.researchassistant.subscription.SubscriptionAccessSource.FREE_DEFAULT);
    }

    @Test
    void creatingDuplicatePersonalWorkspaceIsRejected() throws Exception {
        UserResponse user = createUser();
        String token = login(user.email()).accessToken();

        mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Second Personal",
                                "type", "PERSONAL"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void workspaceBillingReportsFreeAccessWithoutRequiringPaystack() throws Exception {
        UserResponse user = createUser();
        String token = login(user.email()).accessToken();

        var personalWs = workspaceRepository.findFirstByOwnerIdAndTypeAndStatus(
                user.id(),
                WorkspaceType.PERSONAL,
                WorkspaceStatus.ACTIVE
        ).orElseThrow();

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/billing/subscription", personalWs.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("FREE"))
                .andExpect(jsonPath("$.price").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accessSource").value("FREE_DEFAULT"))
                .andExpect(jsonPath("$.isComplimentary").value(false));
    }

    @Test
    void projectCreationWorksOnFreeWorkspaceBelowLimitAndCreatorIsLead() throws Exception {
        UserResponse user = createUser();
        String token = login(user.email()).accessToken();

        var personalWs = workspaceRepository.findFirstByOwnerIdAndTypeAndStatus(
                user.id(),
                WorkspaceType.PERSONAL,
                WorkspaceStatus.ACTIVE
        ).orElseThrow();

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/projects", personalWs.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Grounded Clinical Study",
                                "description", "Investigating biomarkers"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Grounded Clinical Study"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentUserRole").value("LEAD"));
    }

    @Test
    void projectCreationBlockedWhenFreeQuotaExhaustedWith429() throws Exception {
        UserResponse user = createUser();
        String token = login(user.email()).accessToken();

        var personalWs = workspaceRepository.findFirstByOwnerIdAndTypeAndStatus(
                user.id(),
                WorkspaceType.PERSONAL,
                WorkspaceStatus.ACTIVE
        ).orElseThrow();

        // Create 5 projects (FREE quota limit is 5)
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/projects", personalWs.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "title", "Research Project #" + i,
                                    "description", "Initial study " + i
                            ))))
                    .andExpect(status().isCreated());
        }

        // 6th project exceeds the FREE project creation limit of 5
        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/projects", personalWs.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Research Project Exceeding Quota",
                                "description", "Should be rejected"
                        ))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("QUOTA_EXCEEDED"))
                .andExpect(jsonPath("$.metadata.feature").value("PROJECT_CREATION"))
                .andExpect(jsonPath("$.metadata.limit").value("5"));
    }

    private UserResponse createUser() {
        String email = "workspace-test-" + UUID.randomUUID()
                + "@example.com";
        return userService.createUser(new CreateUserRequest(
                email,
                PASSWORD,
                "Workspace",
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

    private WorkspaceResponse createWorkspace(
            String accessToken,
            String name,
            WorkspaceType type
    ) throws Exception {
        String responseJson = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "type", type.name()
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseJson, WorkspaceResponse.class);
    }

    private org.springframework.test.web.servlet.ResultActions addMember(
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

    private org.springframework.test.web.servlet.ResultActions updateWorkspace(
            String accessToken,
            UUID workspaceId,
            String name
    ) throws Exception {
        return mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}",
                        workspaceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name
                        ))));
    }

    private org.springframework.test.web.servlet.ResultActions changeRole(
            String accessToken,
            UUID workspaceId,
            UUID userId,
            String role
    ) throws Exception {
        return mockMvc.perform(patch(
                        "/api/v1/workspaces/{workspaceId}/members/{userId}/role",
                        workspaceId,
                        userId
                )
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "role", role
                        ))));
    }

    private org.springframework.test.web.servlet.ResultActions removeMember(
            String accessToken,
            UUID workspaceId,
            UUID userId
    ) throws Exception {
        return mockMvc.perform(delete(
                        "/api/v1/workspaces/{workspaceId}/members/{userId}",
                        workspaceId,
                        userId
                )
                        .header("Authorization", "Bearer " + accessToken));
    }
}
