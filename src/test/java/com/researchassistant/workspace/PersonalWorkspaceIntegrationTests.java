package com.researchassistant.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.identity.dto.AuthTokenResponse;
import com.researchassistant.identity.dto.CreateUserRequest;
import com.researchassistant.identity.dto.UserResponse;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.identity.service.UserService;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.entity.WorkspaceRole;
import com.researchassistant.workspace.entity.WorkspaceType;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import com.researchassistant.workspace.service.PersonalWorkspaceService;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Transactional
class PersonalWorkspaceIntegrationTests {

    private static final String PASSWORD = "secure-password-123";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMembershipRepository membershipRepository;

    @Autowired
    private PersonalWorkspaceService personalWorkspaceService;

    @Test
    void newUserRegistrationAutomaticallyProvisionsPersonalWorkspace() {
        String email = "auto-" + UUID.randomUUID() + "@example.com";
        UserResponse user = userService.createUser(new CreateUserRequest(
                email, PASSWORD, "Grace", "Hopper", "en"
        ));

        assertThat(user).isNotNull();
        var workspaces = workspaceRepository.findAllByOwnerIdAndTypeAndStatus(
                user.id(), WorkspaceType.PERSONAL, com.researchassistant.workspace.entity.WorkspaceStatus.ACTIVE
        );

        assertThat(workspaces).hasSize(1);
        Workspace personal = workspaces.get(0);
        assertThat(personal.getName()).isEqualTo("Grace's Workspace");

        var membership = membershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                personal.getId(), user.id(), WorkspaceMembershipStatus.ACTIVE
        );
        assertThat(membership).isPresent();
        assertThat(membership.get().getRole()).isEqualTo(WorkspaceRole.OWNER);
    }

    @Test
    void ensurePersonalWorkspaceIsIdempotent() {
        String email = "idempotent-" + UUID.randomUUID() + "@example.com";
        UserResponse userResp = userService.createUser(new CreateUserRequest(
                email, PASSWORD, "Ada", "Lovelace", "en"
        ));

        User user = userRepository.findById(userResp.id()).orElseThrow();

        // Repeated ensure calls return the exact same personal workspace
        Workspace first = personalWorkspaceService.ensurePersonalWorkspace(user);
        Workspace second = personalWorkspaceService.ensurePersonalWorkspace(user);

        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(workspaceRepository.findAllByOwnerIdAndTypeAndStatus(
                user.getId(), WorkspaceType.PERSONAL, com.researchassistant.workspace.entity.WorkspaceStatus.ACTIVE
        )).hasSize(1);
    }

    @Test
    void ensurePersonalWorkspaceApiEndpointReturnsWorkspace() throws Exception {
        String email = "api-pw-" + UUID.randomUUID() + "@example.com";
        userService.createUser(new CreateUserRequest(email, PASSWORD, "Alan", "Turing", "en"));
        String token = authenticate(email, PASSWORD);

        mockMvc.perform(post("/api/v1/workspaces/personal/ensure")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PERSONAL"))
                .andExpect(jsonPath("$.currentUserRole").value("OWNER"))
                .andExpect(jsonPath("$.name").value("Alan's Workspace"));

        mockMvc.perform(get("/api/v1/workspaces/personal")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PERSONAL"))
                .andExpect(jsonPath("$.currentUserRole").value("OWNER"));
    }

    private String authenticate(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, AuthTokenResponse.class).accessToken();
    }
}
