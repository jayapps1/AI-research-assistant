package com.researchassistant.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.document.dto.DocumentResponse;
import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.entity.DocumentProcessingJobType;
import com.researchassistant.document.entity.DocumentProcessingStatus;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.document.entity.DocumentVersionStatus;
import com.researchassistant.document.repository.DocumentProcessingJobRepository;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.document.repository.DocumentVersionRepository;
import com.researchassistant.identity.dto.AuthTokenResponse;
import com.researchassistant.identity.dto.CreateUserRequest;
import com.researchassistant.identity.dto.UserResponse;
import com.researchassistant.identity.service.UserService;
import com.researchassistant.project.dto.ResearchProjectResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.document.max-upload-size-bytes=64",
        "app.document.storage.local-directory=./target/test-documents"
})
@AutoConfigureMockMvc
@Transactional
class DocumentControllerIntegrationTests {

    private static final String PASSWORD = "correct-password-123";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserService userService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository versionRepository;

    @Autowired
    private DocumentProcessingJobRepository jobRepository;

    @Autowired
    private ResearchProjectRepository projectRepository;

    @Autowired
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Test
    void documentNumbersArePermanentAndProjectScoped()
            throws Exception {

        UserResponse owner = createUser();
        String token = login(owner.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(token);
        ResearchProjectResponse firstProject =
                createProject(token, workspace.id(), "First");
        ResearchProjectResponse secondProject =
                createProject(token, workspace.id(), "Second");

        DocumentResponse first = uploadDocument(
                token,
                firstProject.id(),
                "first.pdf",
                "application/pdf",
                "one",
                null
        );
        DocumentResponse second = uploadDocument(
                token,
                firstProject.id(),
                "second.pdf",
                "application/pdf",
                "two",
                null
        );
        DocumentResponse otherProject = uploadDocument(
                token,
                secondProject.id(),
                "other.pdf",
                "application/pdf",
                "other",
                null
        );

        archiveDocument(token, first.id()).andExpect(status().isOk());

        DocumentResponse third = uploadDocument(
                token,
                firstProject.id(),
                "third.pdf",
                "application/pdf",
                "three",
                null
        );

        assertThat(first.documentCode()).isEqualTo("DOC-001");
        assertThat(second.documentCode()).isEqualTo("DOC-002");
        assertThat(third.documentCode()).isEqualTo("DOC-003");
        assertThat(otherProject.documentCode()).isEqualTo("DOC-001");
        assertThat(projectRepository.findById(firstProject.id()).orElseThrow()
                .getNextDocumentNumber()).isEqualTo(4L);
    }

    @Test
    void uploadStoresVersionMetadataChecksumAndQueuedProcessingJob()
            throws Exception {

        UserResponse owner = createUser();
        String token = login(owner.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(token);
        ResearchProjectResponse project =
                createProject(token, workspace.id(), "Metadata");

        DocumentResponse document = uploadDocument(
                token,
                project.id(),
                "..\\unsafe.pdf",
                "application/pdf",
                "hello",
                null
        );

        DocumentVersion version = versionRepository
                .findByDocumentIdAndVersionNumber(document.id(), 1)
                .orElseThrow();

        assertThat(version.getOriginalFilename()).doesNotContain("\\", "/");
        assertThat(version.getStorageKey()).doesNotContain("unsafe.pdf");
        assertThat(version.getStorageKey()).contains("workspace/");
        assertThat(version.getFileSizeBytes()).isEqualTo(5L);
        assertThat(version.getChecksumSha256()).isEqualTo(sha256("hello"));
        assertThat(version.getStatus())
                .isEqualTo(DocumentVersionStatus.PROCESSING);

        List<DocumentProcessingJob> jobs =
                jobRepository.findAllByDocumentVersionDocumentIdOrderByQueuedAtDesc(
                        document.id()
                );

        assertThat(jobs).hasSize(1);
        assertThat(jobs.getFirst().getType())
                .isEqualTo(DocumentProcessingJobType.INGESTION);
        assertThat(jobs.getFirst().getStatus())
                .isEqualTo(DocumentProcessingStatus.QUEUED);
    }

    @Test
    void leadAndEditorCanUploadButViewerAndUnrelatedUsersCannot()
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
                createProject(ownerToken, workspace.id(), "Auth");
        addProjectMember(ownerToken, project.id(), editor.email(), "EDITOR")
                .andExpect(status().isCreated());
        addProjectMember(ownerToken, project.id(), viewer.email(), "VIEWER")
                .andExpect(status().isCreated());

        uploadDocumentAction(ownerToken, project.id(), file("lead.pdf", "application/pdf", "a"), null)
                .andExpect(status().isCreated());
        uploadDocumentAction(editorToken, project.id(), file("editor.txt", "text/plain", "b"), null)
                .andExpect(status().isCreated());
        uploadDocumentAction(viewerToken, project.id(), file("viewer.pdf", "application/pdf", "c"), null)
                .andExpect(status().isForbidden());
        uploadDocumentAction(unrelatedToken, project.id(), file("bad.pdf", "application/pdf", "d"), null)
                .andExpect(status().isNotFound());
    }

    @Test
    void suspendedWorkspaceMembershipBlocksDocumentAccess()
            throws Exception {

        UserResponse owner = createUser();
        UserResponse editor = createUser();
        String ownerToken = login(owner.email()).accessToken();
        String editorToken = login(editor.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(ownerToken);
        addWorkspaceMember(ownerToken, workspace.id(), editor.email(), "MEMBER")
                .andExpect(status().isCreated());
        ResearchProjectResponse project =
                createProject(ownerToken, workspace.id(), "Boundary");
        addProjectMember(ownerToken, project.id(), editor.email(), "EDITOR")
                .andExpect(status().isCreated());
        DocumentResponse document = uploadDocument(
                editorToken,
                project.id(),
                "allowed.pdf",
                "application/pdf",
                "ok",
                null
        );

        WorkspaceMembership membership =
                workspaceMembershipRepository.findByWorkspaceIdAndUserId(
                        workspace.id(),
                        editor.id()
                ).orElseThrow();
        membership.setStatus(WorkspaceMembershipStatus.SUSPENDED);

        mockMvc.perform(get("/api/v1/documents/{documentId}", document.id())
                        .header("Authorization", "Bearer " + editorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsupportedAndOversizedUploadsAreRejectedBeforeAllocation()
            throws Exception {

        UserResponse owner = createUser();
        String token = login(owner.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(token);
        ResearchProjectResponse project =
                createProject(token, workspace.id(), "Validation");

        uploadDocumentAction(token, project.id(), file("bad.bin", "application/octet-stream", "bad"), null)
                .andExpect(status().isUnsupportedMediaType());
        uploadDocumentAction(token, project.id(), file("big.pdf", "application/pdf", "x".repeat(128)), null)
                .andExpect(status().isPayloadTooLarge());

        assertThat(projectRepository.findById(project.id()).orElseThrow()
                .getNextDocumentNumber()).isEqualTo(1L);
    }

    @Test
    void newVersionPreservesDocumentCodeAndSupersedesOldVersion()
            throws Exception {

        UserResponse owner = createUser();
        String token = login(owner.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(token);
        ResearchProjectResponse project =
                createProject(token, workspace.id(), "Versions");
        DocumentResponse document = uploadDocument(
                token,
                project.id(),
                "v1.pdf",
                "application/pdf",
                "v1",
                "Versioned"
        );

        DocumentResponse updated = uploadVersion(
                token,
                document.id(),
                "v2.pdf",
                "application/pdf",
                "v2"
        );

        assertThat(updated.documentCode()).isEqualTo(document.documentCode());
        assertThat(updated.currentVersion().versionNumber()).isEqualTo(2);
        assertThat(documentRepository.findById(document.id()).orElseThrow()
                .getNextVersionNumber()).isEqualTo(3);
        assertThat(versionRepository
                .findByDocumentIdAndVersionNumber(document.id(), 1)
                .orElseThrow()
                .getStatus()).isEqualTo(DocumentVersionStatus.SUPERSEDED);

        mockMvc.perform(get("/api/v1/documents/{documentId}/versions",
                        document.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNumber").value(2))
                .andExpect(jsonPath("$[1].versionNumber").value(1));
    }

    @Test
    void archiveRestoreAndDownloadDoNotExposeStoragePath()
            throws Exception {

        UserResponse owner = createUser();
        String token = login(owner.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(token);
        ResearchProjectResponse project =
                createProject(token, workspace.id(), "Archive");
        DocumentResponse document = uploadDocument(
                token,
                project.id(),
                "download.pdf",
                "application/pdf",
                "body",
                null
        );

        archiveDocument(token, document.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mockMvc.perform(post("/api/v1/documents/{documentId}/restore",
                        document.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
        mockMvc.perform(get("/api/v1/documents/{documentId}/download",
                        document.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("download.pdf")
                ))
                .andExpect(header().string(
                        "Content-Disposition",
                        not(containsString("target"))
                ));
    }

    @Test
    void retryCreatesNewProcessingJobWithoutNewVersion()
            throws Exception {

        UserResponse owner = createUser();
        String token = login(owner.email()).accessToken();
        WorkspaceResponse workspace = createWorkspace(token);
        ResearchProjectResponse project =
                createProject(token, workspace.id(), "Retry");
        DocumentResponse document = uploadDocument(
                token,
                project.id(),
                "retry.pdf",
                "application/pdf",
                "retry",
                null
        );

        DocumentVersion version = versionRepository
                .findByDocumentIdAndVersionNumber(document.id(), 1)
                .orElseThrow();
        version.setStatus(DocumentVersionStatus.FAILED);
        documentRepository.findById(document.id()).orElseThrow()
                .setStatus(DocumentStatus.FAILED);

        mockMvc.perform(post("/api/v1/documents/{documentId}/processing/retry",
                        document.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptNumber").value(2))
                .andExpect(jsonPath("$.status").value("QUEUED"));

        assertThat(versionRepository
                .findAllByDocumentIdOrderByVersionNumberDesc(document.id()))
                .hasSize(1);
        assertThat(jobRepository
                .findAllByDocumentVersionDocumentIdOrderByQueuedAtDesc(
                        document.id()
                ))
                .hasSize(2);
    }

    private UserResponse createUser() {
        String email = "document-test-" + UUID.randomUUID() + "@example.com";
        return userService.createUser(new CreateUserRequest(
                email,
                PASSWORD,
                "Document",
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
                                "title", title
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

    private DocumentResponse uploadDocument(
            String accessToken,
            UUID projectId,
            String filename,
            String mimeType,
            String content,
            String title
    ) throws Exception {
        String responseJson = uploadDocumentAction(
                accessToken,
                projectId,
                file(filename, mimeType, content),
                title
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(responseJson, DocumentResponse.class);
    }

    private DocumentResponse uploadVersion(
            String accessToken,
            UUID documentId,
            String filename,
            String mimeType,
            String content
    ) throws Exception {
        String responseJson = mockMvc.perform(multipart(
                        "/api/v1/documents/{documentId}/versions",
                        documentId
                )
                        .file(file(filename, mimeType, content))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(responseJson, DocumentResponse.class);
    }

    private ResultActions uploadDocumentAction(
            String accessToken,
            UUID projectId,
            MockMultipartFile file,
            String title
    ) throws Exception {
        var request = multipart(
                "/api/v1/projects/{projectId}/documents",
                projectId
        )
                .file(file)
                .header("Authorization", "Bearer " + accessToken);
        if (title != null) {
            request.param("title", title);
        }
        return mockMvc.perform(request);
    }

    private ResultActions archiveDocument(String accessToken, UUID documentId)
            throws Exception {
        return mockMvc.perform(post("/api/v1/documents/{documentId}/archive",
                        documentId)
                        .header("Authorization", "Bearer " + accessToken));
    }

    private MockMultipartFile file(
            String filename,
            String mimeType,
            String content
    ) {
        return new MockMultipartFile(
                "file",
                filename,
                mimeType,
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }
}
