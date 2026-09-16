package com.researchassistant.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.identity.dto.AuthTokenResponse;
import com.researchassistant.identity.dto.CreateUserRequest;
import com.researchassistant.identity.dto.UserResponse;
import com.researchassistant.identity.service.UserService;
import com.researchassistant.project.dto.ResearchProjectResponse;
import com.researchassistant.workspace.dto.WorkspaceResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

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
class DashboardProjectionIntegrationTests {

    private static final String PASSWORD = "secure-password-123";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserService userService;

    @Test
    void userDashboardAndWorkspaceDashboardReturnAggregates() throws Exception {
        String email = "dash-" + UUID.randomUUID() + "@example.com";
        UserResponse user = userService.createUser(new CreateUserRequest(email, PASSWORD, "Marie", "Curie", "en"));
        String token = authenticate(email, PASSWORD);

        // Fetch user dashboard
        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.greeting", org.hamcrest.Matchers.containsString("Marie")))
                .andExpect(jsonPath("$.currentWorkspace.name").value("Marie's Workspace"))
                .andExpect(jsonPath("$.activeProjectCount").value(0))
                .andExpect(jsonPath("$.openTaskCount").value(0))
                .andExpect(jsonPath("$.documentCount").value(0));

        // Get personal workspace
        String wsJson = mockMvc.perform(get("/api/v1/workspaces/personal")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        WorkspaceResponse workspace = objectMapper.readValue(wsJson, WorkspaceResponse.class);

        // Fetch workspace dashboard
        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/dashboard", workspace.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspace.id").value(workspace.id().toString()))
                .andExpect(jsonPath("$.currentUserRole").value("OWNER"))
                .andExpect(jsonPath("$.memberCount").value(1));

        // Create a project in this workspace
        String projJson = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/projects", workspace.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Radiation Studies in Oncology",
                                  "description": "Empirical investigation of isotope decay"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        ResearchProjectResponse project = objectMapper.readValue(projJson, ResearchProjectResponse.class);

        // Project dashboard
        mockMvc.perform(get("/api/v1/projects/{projectId}/dashboard", project.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.id").value(project.id().toString()))
                .andExpect(jsonPath("$.currentUserRole").value("LEAD"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.documents.total").value(0))
                .andExpect(jsonPath("$.tasks.total").value(0))
                .andExpect(jsonPath("$.researchProgress.totalStages").value(18));

        // Research progress endpoint
        mockMvc.perform(get("/api/v1/projects/{projectId}/research-progress", project.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(project.id().toString()))
                .andExpect(jsonPath("$.totalStages").value(18))
                .andExpect(jsonPath("$.stages[0].key").value("problem"))
                .andExpect(jsonPath("$.stages[0].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.nextIncompleteStage").value("Research Problem"));

        // Global my tasks and my documents endpoints
        mockMvc.perform(get("/api/v1/tasks/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/documents/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
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
