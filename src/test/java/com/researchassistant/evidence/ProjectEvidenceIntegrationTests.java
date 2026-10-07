package com.researchassistant.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.IntegrationTestSupport;
import com.researchassistant.analysis.dto.AnalysisDtos.ReportResponse;
import com.researchassistant.analysis.dto.AnalysisDtos.ReportStructureResponse;
import com.researchassistant.analysis.entity.ResearchReportChapter;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.analysis.service.AnalysisWorkflowService;
import com.researchassistant.evidence.dto.ProjectEvidenceDtos.*;
import com.researchassistant.evidence.entity.EvidenceAnalysisStatus;
import com.researchassistant.evidence.entity.EvidenceType;
import com.researchassistant.evidence.entity.ProjectEvidence;
import com.researchassistant.evidence.repository.ProjectEvidenceRepository;
import com.researchassistant.identity.dto.CreateUserRequest;
import com.researchassistant.identity.dto.UserResponse;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.identity.service.UserService;
import com.researchassistant.project.dto.CreateResearchProjectRequest;
import com.researchassistant.project.dto.ResearchProjectResponse;
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.project.entity.AcademicWorkspaceType;
import com.researchassistant.project.service.ResearchProjectService;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.service.PersonalWorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Transactional
class ProjectEvidenceIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonalWorkspaceService personalWorkspaceService;

    @Autowired
    private ResearchProjectService projectService;

    @Autowired
    private AnalysisWorkflowService analysisWorkflowService;

    @Autowired
    private ProjectEvidenceRepository evidenceRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User primaryUser;
    private Workspace primaryWorkspace;
    private ResearchProjectResponse project;
    private ReportResponse report;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "lead-researcher-" + UUID.randomUUID() + "@university.edu";
        UserResponse created = userService.createUser(new CreateUserRequest(
                email, "Password@123", "Lead", "Researcher", "en"
        ));
        primaryUser = userRepository.findById(created.id()).orElseThrow();
        primaryWorkspace = personalWorkspaceService.ensurePersonalWorkspace(primaryUser);
        userToken = bearerToken(primaryUser.getId(), primaryUser.getEmail());

        Map<String, Object> body = Map.of(
                "workspaceId", primaryWorkspace.getId().toString(),
                "title", "Intelligent Drone Pest Detection System",
                "description", "Computer vision system for crop disease monitoring",
                "workspaceType", AcademicWorkspaceType.ACADEMIC_PROJECT.name(),
                "projectType", AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT.name(),
                "citationStyle", "APA_7"
        );
        CreateResearchProjectRequest projectReq = objectMapper.readValue(
                objectMapper.writeValueAsString(body),
                CreateResearchProjectRequest.class
        );
        project = projectService.createProject(primaryWorkspace.getId(), primaryUser, projectReq);
        report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);
    }

    @Test
    @DisplayName("Uploads real image file evidence, persists StorageObject, and computes dynamic Figure label")
    void uploadEvidenceImage_persistsStorageObjectAndComputesLabel() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dashboard_architecture.png",
                "image/png",
                "fake-png-binary-stream-data".getBytes()
        );

        String responseJson = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/projects/{projectId}/evidence/upload", project.id())
                        .file(file)
                        .param("evidenceType", "ARCHITECTURE_DIAGRAM")
                        .param("caption", "High-Level Pipeline Architecture")
                        .param("description", "Shows edge sensor data flow to cloud gateway")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.figureLabel").isNotEmpty())
                .andExpect(jsonPath("$.caption").value("High-Level Pipeline Architecture"))
                .andExpect(jsonPath("$.originalFilename").value("dashboard_architecture.png"))
                .andExpect(jsonPath("$.mimeType").value("image/png"))
                .andExpect(jsonPath("$.downloadUrl").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ProjectEvidenceResponse resp = objectMapper.readValue(responseJson, ProjectEvidenceResponse.class);
        assertThat(resp.id()).isNotNull();

        ProjectEvidence stored = evidenceRepository.findById(resp.id()).orElseThrow();
        assertThat(stored.getCaption()).isEqualTo("High-Level Pipeline Architecture");
        assertThat(stored.getStorageObject()).isNotNull();
        assertThat(stored.getOriginalFilename()).isEqualTo("dashboard_architecture.png");
    }

    @Test
    @DisplayName("Creates structured table evidence without file upload and formats Table label")
    void createStructuredTableEvidence_formatsTableLabel() throws Exception {
        CreateStructuredEvidenceRequest request = new CreateStructuredEvidenceRequest(
                EvidenceType.TABLE,
                "Confusion Matrix on Test Dataset",
                "Evaluates Precision and Recall across 500 leaf images",
                null,
                "Confusion Matrix Table",
                "{\"headers\":[\"Class\",\"Precision\",\"Recall\"],\"rows\":[[\"Blight\",\"0.94\",\"0.91\"],[\"Healthy\",\"0.98\",\"0.97\"]]}"
        );

        String responseJson = mockMvc.perform(post("/api/v1/projects/{projectId}/evidence/structured", project.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceType").value("TABLE"))
                .andExpect(jsonPath("$.figureLabel").value("Table"))
                .andExpect(jsonPath("$.caption").value("Confusion Matrix on Test Dataset"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ProjectEvidenceResponse resp = objectMapper.readValue(responseJson, ProjectEvidenceResponse.class);
        ProjectEvidence stored = evidenceRepository.findById(resp.id()).orElseThrow();
        assertThat(stored.getEvidenceType()).isEqualTo(EvidenceType.TABLE);
        assertThat(stored.getAltText()).isEqualTo("Confusion Matrix Table");
        assertThat(stored.getMetadataJson()).contains("headers");
    }

    @Test
    @DisplayName("Reorders evidence items and persists updated displayOrder")
    void reorderEvidence_updatesDisplayOrder() throws Exception {
        CreateStructuredEvidenceRequest item1 = new CreateStructuredEvidenceRequest(
                EvidenceType.SCREENSHOT, "Item 1", "Desc 1", null, null, null
        );
        CreateStructuredEvidenceRequest item2 = new CreateStructuredEvidenceRequest(
                EvidenceType.DIAGRAM, "Item 2", "Desc 2", null, null, null
        );

        String res1 = mockMvc.perform(post("/api/v1/projects/{projectId}/evidence/structured", project.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item1))
                        .header("Authorization", userToken))
                .andReturn().getResponse().getContentAsString();
        ProjectEvidenceResponse resp1 = objectMapper.readValue(res1, ProjectEvidenceResponse.class);

        String res2 = mockMvc.perform(post("/api/v1/projects/{projectId}/evidence/structured", project.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item2))
                        .header("Authorization", userToken))
                .andReturn().getResponse().getContentAsString();
        ProjectEvidenceResponse resp2 = objectMapper.readValue(res2, ProjectEvidenceResponse.class);

        // Reorder: put Item 2 first, then Item 1
        ReorderEvidenceRequest reorderReq = new ReorderEvidenceRequest(List.of(resp2.id(), resp1.id()));

        mockMvc.perform(post("/api/v1/projects/{projectId}/evidence/reorder", project.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reorderReq))
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(resp2.id().toString()))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                .andExpect(jsonPath("$[1].id").value(resp1.id().toString()))
                .andExpect(jsonPath("$[1].displayOrder").value(2));

        ProjectEvidence stored2 = evidenceRepository.findById(resp2.id()).orElseThrow();
        assertThat(stored2.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("Updates caption and description of an evidence item")
    void updateEvidence_updatesFieldsSafely() throws Exception {
        CreateStructuredEvidenceRequest item = new CreateStructuredEvidenceRequest(
                EvidenceType.SCREENSHOT, "Original Caption", "Original Description", null, null, null
        );
        String res = mockMvc.perform(post("/api/v1/projects/{projectId}/evidence/structured", project.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item))
                        .header("Authorization", userToken))
                .andReturn().getResponse().getContentAsString();
        ProjectEvidenceResponse resp = objectMapper.readValue(res, ProjectEvidenceResponse.class);

        UpdateProjectEvidenceRequest updateReq = new UpdateProjectEvidenceRequest(
                "Updated Caption",
                "Updated Description",
                null,
                "Updated Alt Text",
                EvidenceType.SYSTEM_OUTPUT,
                1,
                null
        );

        mockMvc.perform(put("/api/v1/projects/{projectId}/evidence/{evidenceId}", project.id(), resp.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq))
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caption").value("Updated Caption"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.evidenceType").value("SYSTEM_OUTPUT"));

        ProjectEvidence stored = evidenceRepository.findById(resp.id()).orElseThrow();
        assertThat(stored.getCaption()).isEqualTo("Updated Caption");
        assertThat(stored.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    @DisplayName("Deletes evidence item safely")
    void deleteEvidence_removesItem() throws Exception {
        CreateStructuredEvidenceRequest item = new CreateStructuredEvidenceRequest(
                EvidenceType.USER_NOTE, "Note to be deleted", null, null, null, null
        );
        String res = mockMvc.perform(post("/api/v1/projects/{projectId}/evidence/structured", project.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item))
                        .header("Authorization", userToken))
                .andReturn().getResponse().getContentAsString();
        ProjectEvidenceResponse resp = objectMapper.readValue(res, ProjectEvidenceResponse.class);

        mockMvc.perform(delete("/api/v1/projects/{projectId}/evidence/{evidenceId}", project.id(), resp.id())
                        .header("Authorization", userToken))
                .andExpect(status().isNoContent());

        assertThat(evidenceRepository.findById(resp.id())).isEmpty();
    }

    @Test
    @DisplayName("AI vision analysis gracefully returns UNAVAILABLE when vision model is unconfigured (Zero fake AI / No OpenAI calls)")
    void analyzeEvidence_returnsStructuredUnavailableFallback() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "system_flow.png",
                "image/png",
                "flowchart-bytes".getBytes()
        );

        String uploadRes = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/projects/{projectId}/evidence/upload", project.id())
                        .file(file)
                        .header("Authorization", userToken))
                .andReturn().getResponse().getContentAsString();
        ProjectEvidenceResponse uploaded = objectMapper.readValue(uploadRes, ProjectEvidenceResponse.class);

        // Perform AI analysis without OpenAI API key configured in test environment
        mockMvc.perform(post("/api/v1/projects/{projectId}/evidence/{evidenceId}/analyze", project.id(), uploaded.id())
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.containsString("unavailable or not configured")));

        ProjectEvidence stored = evidenceRepository.findById(uploaded.id()).orElseThrow();
        assertThat(stored.getAiAnalysisStatus()).isEqualTo(EvidenceAnalysisStatus.UNAVAILABLE);
    }

    @Test
    @DisplayName("List of Figures and List of Tables endpoints return structured front-matter items")
    void listOfFiguresAndTables_returnsReportFrontMatter() throws Exception {
        // Retrieve chapter 4 section from report structure
        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        UUID targetSectionId = structure.chapters().get(3).sections().get(0).id();

        CreateStructuredEvidenceRequest figReq = new CreateStructuredEvidenceRequest(
                EvidenceType.DIAGRAM, "Drone Circuit Schematic", "Circuit diagram", targetSectionId, null, null
        );
        CreateStructuredEvidenceRequest tblReq = new CreateStructuredEvidenceRequest(
                EvidenceType.TABLE, "Component Bill of Materials", "Hardware costs", targetSectionId, null, null
        );

        mockMvc.perform(post("/api/v1/projects/{projectId}/evidence/structured", project.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(figReq))
                        .header("Authorization", userToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/projects/{projectId}/evidence/structured", project.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tblReq))
                        .header("Authorization", userToken))
                .andExpect(status().isOk());

        // Test List of Figures
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/{reportId}/list-of-figures", project.id(), report.id())
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].caption").value("Drone Circuit Schematic"))
                .andExpect(jsonPath("$[0].figureNumber").value("Figure 3.1"));

        // Test List of Tables
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/{reportId}/list-of-tables", project.id(), report.id())
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].caption").value("Component Bill of Materials"))
                .andExpect(jsonPath("$[0].tableNumber").value("Table 3.1"));
    }
}
