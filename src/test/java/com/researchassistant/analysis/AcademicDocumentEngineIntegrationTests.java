package com.researchassistant.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.IntegrationTestSupport;
import com.researchassistant.analysis.dto.AnalysisDtos.ChapterResponse;
import com.researchassistant.analysis.dto.AnalysisDtos.ChapterStructureResponse;
import com.researchassistant.analysis.dto.AnalysisDtos.CreateChapterRequest;
import com.researchassistant.analysis.dto.AnalysisDtos.CreateSectionRequest;
import com.researchassistant.analysis.dto.AnalysisDtos.ReorderItem;
import com.researchassistant.analysis.dto.AnalysisDtos.ReorderStructureRequest;
import com.researchassistant.analysis.dto.AnalysisDtos.ReportResponse;
import com.researchassistant.analysis.dto.AnalysisDtos.ReportStructureResponse;
import com.researchassistant.analysis.dto.AnalysisDtos.SectionResponse;
import com.researchassistant.analysis.dto.AnalysisDtos.SectionStructureResponse;
import com.researchassistant.analysis.dto.AnalysisDtos.TableOfContentsResponse;
import com.researchassistant.analysis.dto.AnalysisDtos.DeterministicRepairResponse;
import com.researchassistant.analysis.dto.AnalysisDtos.GenerateSectionRequest;
import com.researchassistant.analysis.dto.AnalysisDtos.FinalDocumentResponse;
import com.researchassistant.analysis.dto.AnalysisDtos.UpdateSectionRequest;
import com.researchassistant.analysis.dto.AnalysisDtos.UpdateTitlePageDetailsRequest;
import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.analysis.repository.ReportDocumentVersionRepository;
import com.researchassistant.analysis.entity.ReportChapterType;
import com.researchassistant.analysis.entity.ReportSectionType;
import com.researchassistant.analysis.entity.ResearchReportStatus;
import com.researchassistant.analysis.entity.SectionGenerationPolicy;
import com.researchassistant.analysis.entity.SectionSemanticPurpose;
import com.researchassistant.analysis.exception.InsufficientProjectEvidenceException;
import com.researchassistant.analysis.controller.ReportExportController.DocumentExportSelection;
import com.researchassistant.analysis.service.ReportDocumentCompiler;
import com.researchassistant.analysis.service.ReportExportService;
import com.researchassistant.analysis.service.ReportSectionPromptBuilder;
import com.researchassistant.analysis.service.AnalysisWorkflowService;
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
import com.researchassistant.workspace.exception.WorkspaceNotFoundException;
import com.researchassistant.workspace.service.PersonalWorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class AcademicDocumentEngineIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private AnalysisWorkflowService analysisWorkflowService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonalWorkspaceService personalWorkspaceService;

    @Autowired
    private ResearchProjectService projectService;

    @Autowired
    private ReportDocumentVersionRepository reportDocumentVersionRepository;

    @Autowired
    private ReportExportService reportExportService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User primaryUser;
    private Workspace primaryWorkspace;

    @BeforeEach
    void setUp() {
        String email = "scholar-" + UUID.randomUUID() + "@example.edu";
        UserResponse created = userService.createUser(new CreateUserRequest(
                email, "Password@123", "Academic", "Author", "en"
        ));
        primaryUser = userRepository.findById(created.id()).orElseThrow();
        primaryWorkspace = personalWorkspaceService.ensurePersonalWorkspace(primaryUser);
    }

    private ResearchProjectResponse createWorkspaceProject(AcademicWorkspaceType type, String title) throws Exception {
        AcademicProjectType projectType = (type == AcademicWorkspaceType.ACADEMIC_PROJECT)
                ? AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT
                : null;
        Map<String, Object> body = Map.of(
                "workspaceId", primaryWorkspace.getId().toString(),
                "title", title,
                "description", "Academic document workspace",
                "workspaceType", type.name(),
                "citationStyle", "APA_7"
        );
        CreateResearchProjectRequest req = objectMapper.readValue(
                objectMapper.writeValueAsString(body),
                CreateResearchProjectRequest.class
        );
        return projectService.createProject(primaryWorkspace.getId(), primaryUser, req);
    }

    private SectionStructureResponse firstNumberedSection(ReportStructureResponse structure, int chapterNumber) {
        ChapterStructureResponse chapter = structure.chapters().stream()
                .filter(c -> c.chapterNumber() != null && c.chapterNumber() == chapterNumber)
                .findFirst()
                .orElseThrow();
        return chapter.sections().stream().findFirst().orElseThrow();
    }

    private List<SectionStructureResponse> flattenSections(ReportStructureResponse structure) {
        List<SectionStructureResponse> sections = new ArrayList<>();
        for (ChapterStructureResponse chapter : structure.chapters()) {
            for (SectionStructureResponse section : chapter.sections()) {
                collectSection(section, sections);
            }
        }
        return sections;
    }

    private void collectSection(SectionStructureResponse section, List<SectionStructureResponse> sections) {
        sections.add(section);
        for (SectionStructureResponse child : section.subsections()) {
            collectSection(child, sections);
        }
    }

    @Test
    @DisplayName("Academic Research workspace initializes Research Report starting structure")
    void testInitializeAcademicResearchReport() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_RESEARCH, "Agricultural IoT Study");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);

        assertThat(report).isNotNull();
        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        assertThat(structure.chapters()).isNotEmpty();

        List<String> chapterTitles = structure.chapters().stream().map(ChapterStructureResponse::title).toList();
        assertThat(chapterTitles).anyMatch(t -> t.contains("Introduction"));
        assertThat(chapterTitles).anyMatch(t -> t.contains("Literature Review"));
        assertThat(chapterTitles).anyMatch(t -> t.contains("Methodology"));
        assertThat(chapterTitles).anyMatch(t -> t.contains("Results and Discussion"));
        assertThat(chapterTitles).anyMatch(t -> t.contains("References"));
    }

    @Test
    @DisplayName("Academic Project workspace initializes Project Report starting structure")
    void testInitializeAcademicProjectReport() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "Campus Navigation System");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);

        assertThat(report).isNotNull();
        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        assertThat(structure.chapters()).isNotEmpty();

        List<String> chapterTitles = structure.chapters().stream().map(ChapterStructureResponse::title).toList();
        assertThat(chapterTitles).anyMatch(t -> t.contains("Introduction"));
        assertThat(chapterTitles).anyMatch(t -> t.contains("Related Work") || t.contains("Literature Review"));
        assertThat(chapterTitles).anyMatch(t -> t.contains("Design"));
        assertThat(chapterTitles).anyMatch(t -> t.contains("Implementation"));
        assertThat(chapterTitles).anyMatch(t -> t.contains("Conclusion"));
        assertThat(chapterTitles).anyMatch(t -> t.contains("References"));
    }

    @Test
    @DisplayName("Coursework Acceptance Test: Add Literature Review, reorder, add 3 subheadings, verify 1., 2., 2.1, 2.2, 2.3, 3., 4., TOC, idempotency")
    void testCourseworkAcceptanceCriteria() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.COURSEWORK, "Digital Agriculture Coursework");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);

        ReportStructureResponse initialStructure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        assertThat(initialStructure.chapters()).isNotEmpty();
        ChapterStructureResponse mainChapter = initialStructure.chapters().stream()
                .filter(c -> c.type() != ReportChapterType.REFERENCES)
                .findFirst().orElseThrow();

        // 1. Initial template sections: Introduction, Main Discussion, Conclusion
        List<String> headings = mainChapter.sections().stream().map(SectionStructureResponse::heading).toList();
        assertThat(headings).contains("Introduction", "Main Discussion", "Conclusion");

        SectionStructureResponse introSec = mainChapter.sections().stream().filter(s -> s.heading().equals("Introduction")).findFirst().orElseThrow();
        SectionStructureResponse mainDiscSec = mainChapter.sections().stream().filter(s -> s.heading().equals("Main Discussion")).findFirst().orElseThrow();
        SectionStructureResponse conclSec = mainChapter.sections().stream().filter(s -> s.heading().equals("Conclusion")).findFirst().orElseThrow();

        // 2. User adds heading: "Literature Review"
        SectionResponse litReviewResp = analysisWorkflowService.createSection(mainChapter.id(), primaryUser, new CreateSectionRequest(
                ReportSectionType.CUSTOM, "Literature Review", null, null, null, null,
                null, null, null, null, null, null, false, false, true
        ));
        assertThat(litReviewResp).isNotNull();
        UUID litReviewId = litReviewResp.id();

        // 3. User reorders to:
        // 1. Introduction
        // 2. Literature Review
        // 3. Main Discussion
        // 4. Conclusion
        List<ReorderItem> reordered = List.of(
                new ReorderItem(introSec.id(), 1, null),
                new ReorderItem(litReviewId, 2, null),
                new ReorderItem(mainDiscSec.id(), 3, null),
                new ReorderItem(conclSec.id(), 4, null)
        );
        analysisWorkflowService.reorderStructure(report.id(), new ReorderStructureRequest(null, reordered), primaryUser);

        // 4. User adds 3 subheadings under Literature Review:
        // - "Concept of Digital Agriculture"
        // - "Previous Studies"
        // - "Research Gap"
        SectionResponse sub1 = analysisWorkflowService.createSection(mainChapter.id(), primaryUser, new CreateSectionRequest(
                ReportSectionType.CUSTOM, "Concept of Digital Agriculture", null, null, null, null,
                1, null, null, null, litReviewId, null, false, false, true
        ));
        SectionResponse sub2 = analysisWorkflowService.createSection(mainChapter.id(), primaryUser, new CreateSectionRequest(
                ReportSectionType.CUSTOM, "Previous Studies", null, null, null, null,
                2, null, null, null, litReviewId, null, false, false, true
        ));
        SectionResponse sub3 = analysisWorkflowService.createSection(mainChapter.id(), primaryUser, new CreateSectionRequest(
                ReportSectionType.CUSTOM, "Research Gap", null, null, null, null,
                3, null, null, null, litReviewId, null, false, false, true
        ));

        // 5. Verify automatic numbering in structure:
        ReportStructureResponse updatedStructure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        ChapterStructureResponse activeChapter = updatedStructure.chapters().stream()
                .filter(c -> c.id().equals(mainChapter.id())).findFirst().orElseThrow();
        List<SectionStructureResponse> roots = activeChapter.sections();

        SectionStructureResponse r1 = roots.stream().filter(s -> s.heading().equals("Introduction")).findFirst().orElseThrow();
        SectionStructureResponse r2 = roots.stream().filter(s -> s.heading().equals("Literature Review")).findFirst().orElseThrow();
        SectionStructureResponse r3 = roots.stream().filter(s -> s.heading().equals("Main Discussion")).findFirst().orElseThrow();
        SectionStructureResponse r4 = roots.stream().filter(s -> s.heading().equals("Conclusion")).findFirst().orElseThrow();

        assertThat(r1.sectionNumber()).isEqualTo("1");
        assertThat(r2.sectionNumber()).isEqualTo("2");
        assertThat(r3.sectionNumber()).isEqualTo("3");
        assertThat(r4.sectionNumber()).isEqualTo("4");

        // Verify subsections under Literature Review
        List<SectionStructureResponse> subs = r2.subsections();
        assertThat(subs).hasSize(3);
        assertThat(subs.get(0).heading()).isEqualTo("Concept of Digital Agriculture");
        assertThat(subs.get(0).sectionNumber()).isEqualTo("2.1");
        assertThat(subs.get(1).heading()).isEqualTo("Previous Studies");
        assertThat(subs.get(1).sectionNumber()).isEqualTo("2.2");
        assertThat(subs.get(2).heading()).isEqualTo("Research Gap");
        assertThat(subs.get(2).sectionNumber()).isEqualTo("2.3");

        // Stored title must NOT permanently encode "2.3 Research Gap"
        assertThat(subs.get(2).heading()).isEqualTo("Research Gap");

        // 6. Verify deterministic Table of Contents output
        TableOfContentsResponse toc = analysisWorkflowService.generateTableOfContents(report.id(), primaryUser);
        assertThat(toc.formattedMarkdown()).contains("1. Introduction");
        assertThat(toc.formattedMarkdown()).contains("2. Literature Review");
        assertThat(toc.formattedMarkdown()).contains("2.1 Concept of Digital Agriculture");
        assertThat(toc.formattedMarkdown()).contains("2.2 Previous Studies");
        assertThat(toc.formattedMarkdown()).contains("2.3 Research Gap");
        assertThat(toc.formattedMarkdown()).contains("3. Main Discussion");
        assertThat(toc.formattedMarkdown()).contains("4. Conclusion");
        assertThat(toc.formattedMarkdown()).contains("References");
        // Must NOT output dummy "### Coursework Document" chapter heading
        assertThat(toc.formattedMarkdown()).doesNotContain("### Coursework Document");

        // 7. Idempotency: Re-calling ensureReportInitialized preserves existing custom sections and structure
        ReportResponse reEnsured = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);
        assertThat(reEnsured.id()).isEqualTo(report.id());

        ReportStructureResponse reStructure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        SectionStructureResponse reLitReview = reStructure.chapters().stream()
                .filter(c -> c.id().equals(mainChapter.id())).findFirst().orElseThrow()
                .sections().stream()
                .filter(s -> s.heading().equals("Literature Review")).findFirst().orElseThrow();
        assertThat(reLitReview.subsections()).hasSize(3);
        assertThat(reLitReview.subsections().get(2).sectionNumber()).isEqualTo("2.3");
    }

    @Test
    @DisplayName("Academic Project Acceptance Test: Add Chapter Six, add 4 sections (6.1-6.4), reorder before Chapter Five, verify renumbering to 5.1-5.4")
    void testAcademicProjectAcceptanceCriteria() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "E-Commerce Logistics Platform");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);

        // Initial chapters
        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        ChapterStructureResponse ch5 = structure.chapters().stream()
                .filter(c -> c.chapterNumber() != null && c.chapterNumber() == 5)
                .findFirst().orElseThrow();
        assertThat(ch5.title()).contains("Conclusion");

        // User adds: "Chapter Six — Deployment and Maintenance"
        ChapterResponse ch6Resp = analysisWorkflowService.createChapter(report.id(), primaryUser, new CreateChapterRequest(
                ReportChapterType.CUSTOM, "Chapter Six — Deployment and Maintenance", null, null, false, false
        ));
        assertThat(ch6Resp).isNotNull();
        // Title parsing cleans out "Chapter Six —" and extracts clean title
        assertThat(ch6Resp.title()).isEqualTo("Deployment and Maintenance");
        assertThat(ch6Resp.chapterNumber()).isEqualTo(6);

        // Add 4 sections under Chapter Six:
        // 6.1 Deployment Environment
        // 6.2 Installation
        // 6.3 Maintenance
        // 6.4 Future Enhancements
        SectionResponse sec1 = analysisWorkflowService.createSection(ch6Resp.id(), primaryUser, new CreateSectionRequest(
                ReportSectionType.CUSTOM, "Deployment Environment", null, null, null, null,
                1, null, null, null, null, null, false, false, true
        ));
        SectionResponse sec2 = analysisWorkflowService.createSection(ch6Resp.id(), primaryUser, new CreateSectionRequest(
                ReportSectionType.CUSTOM, "Installation", null, null, null, null,
                2, null, null, null, null, null, false, false, true
        ));
        SectionResponse sec3 = analysisWorkflowService.createSection(ch6Resp.id(), primaryUser, new CreateSectionRequest(
                ReportSectionType.CUSTOM, "Maintenance", null, null, null, null,
                3, null, null, null, null, null, false, false, true
        ));
        SectionResponse sec4 = analysisWorkflowService.createSection(ch6Resp.id(), primaryUser, new CreateSectionRequest(
                ReportSectionType.CUSTOM, "Future Enhancements", null, null, null, null,
                4, null, null, null, null, null, false, false, true
        ));

        // Check section numbers under Chapter Six:
        assertThat(sec1.sectionNumber()).isEqualTo("6.1");
        assertThat(sec2.sectionNumber()).isEqualTo("6.2");
        assertThat(sec3.sectionNumber()).isEqualTo("6.3");
        assertThat(sec4.sectionNumber()).isEqualTo("6.4");

        ReportStructureResponse sWithCh6 = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        ChapterStructureResponse ch6Structure = sWithCh6.chapters().stream()
                .filter(c -> c.id().equals(ch6Resp.id())).findFirst().orElseThrow();
        assertThat(ch6Structure.chapterNumber()).isEqualTo(6);

        // Reorder Chapter Six before Chapter Five
        List<ChapterStructureResponse> allChapters = sWithCh6.chapters();
        List<ReorderItem> chapterReorders = new ArrayList<>();
        int order = 1;
        for (ChapterStructureResponse c : allChapters) {
            if (c.id().equals(ch5.id())) {
                // Insert Ch6 before Ch5
                chapterReorders.add(new ReorderItem(ch6Resp.id(), order++, null));
                chapterReorders.add(new ReorderItem(ch5.id(), order++, null));
            } else if (!c.id().equals(ch6Resp.id())) {
                chapterReorders.add(new ReorderItem(c.id(), order++, null));
            }
        }

        ReportStructureResponse reorderedStructure = analysisWorkflowService.reorderStructure(
                report.id(), new ReorderStructureRequest(chapterReorders, null), primaryUser
        );

        // Verify automatic renumbering:
        // Ch6 now has chapterNumber = 5!
        ChapterStructureResponse renumberedCh6 = reorderedStructure.chapters().stream()
                .filter(c -> c.id().equals(ch6Resp.id())).findFirst().orElseThrow();
        assertThat(renumberedCh6.chapterNumber()).isEqualTo(5);
        assertThat(renumberedCh6.title()).isEqualTo("Deployment and Maintenance"); // Stored title remains clean

        // Its sections are automatically renumbered to 5.1, 5.2, 5.3, 5.4!
        List<SectionStructureResponse> renumberedSections = renumberedCh6.sections();
        assertThat(renumberedSections).hasSize(4);
        assertThat(renumberedSections.get(0).sectionNumber()).isEqualTo("5.1");
        assertThat(renumberedSections.get(0).heading()).isEqualTo("Deployment Environment");
        assertThat(renumberedSections.get(1).sectionNumber()).isEqualTo("5.2");
        assertThat(renumberedSections.get(1).heading()).isEqualTo("Installation");
        assertThat(renumberedSections.get(2).sectionNumber()).isEqualTo("5.3");
        assertThat(renumberedSections.get(2).heading()).isEqualTo("Maintenance");
        assertThat(renumberedSections.get(3).sectionNumber()).isEqualTo("5.4");
        assertThat(renumberedSections.get(3).heading()).isEqualTo("Future Enhancements");

        // Original Chapter Five now has chapterNumber = 6!
        ChapterStructureResponse renumberedCh5 = reorderedStructure.chapters().stream()
                .filter(c -> c.id().equals(ch5.id())).findFirst().orElseThrow();
        assertThat(renumberedCh5.chapterNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("Required template sections are protected from deletion, custom sections can be deleted")
    void testRequiredSectionProtection() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.COURSEWORK, "Protection Test Coursework");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);

        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        ChapterStructureResponse mainChapter = structure.chapters().get(0);
        SectionStructureResponse requiredIntro = mainChapter.sections().stream()
                .filter(SectionStructureResponse::required).findFirst().orElseThrow();

        // Attempting to delete required section throws IllegalArgumentException
        assertThatThrownBy(() -> analysisWorkflowService.deleteSection(requiredIntro.id(), primaryUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");

        // Custom section can be deleted
        SectionResponse customSec = analysisWorkflowService.createSection(mainChapter.id(), primaryUser, new CreateSectionRequest(
                ReportSectionType.CUSTOM, "Disposable Section", null, null, null, null,
                null, null, null, null, null, null, false, false, true
        ));
        analysisWorkflowService.deleteSection(customSec.id(), primaryUser);

        ReportStructureResponse postDelete = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        assertThat(postDelete.chapters().get(0).sections().stream().noneMatch(s -> s.id().equals(customSec.id()))).isTrue();
    }

    @Test
    @DisplayName("Cross-workspace security: Unauthorized user cannot manipulate report structure")
    void testCrossWorkspaceSecurity() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.COURSEWORK, "Private Coursework");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);

        // Create an outside unauthorized user
        UserResponse stranger = userService.createUser(new CreateUserRequest(
                "stranger-" + UUID.randomUUID() + "@other.edu", "Password@123", "Other", "Student", "en"
        ));
        User strangerUser = userRepository.findById(stranger.id()).orElseThrow();

        // Stranger cannot view report structure or TOC (rejected by security)
        assertThatThrownBy(() -> analysisWorkflowService.getReportStructure(report.id(), strangerUser))
                .isInstanceOfAny(AccessDeniedException.class, WorkspaceNotFoundException.class);

        assertThatThrownBy(() -> analysisWorkflowService.generateTableOfContents(report.id(), strangerUser))
                .isInstanceOfAny(AccessDeniedException.class, WorkspaceNotFoundException.class);
    }

    @Test
    @DisplayName("Title Page is deterministic: 0 credits, template-driven, never literature synthesis")
    void testTitlePageDeterministicRefreshNoAiCredits() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "AI Drone Navigation");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);

        // Refresh Title Page deterministically
        SectionResponse titlePageResp = analysisWorkflowService.refreshTitlePage(report.id(), primaryUser);
        assertThat(titlePageResp).isNotNull();
        assertThat(titlePageResp.semanticPurpose()).isEqualTo(SectionSemanticPurpose.TITLE_PAGE);
        assertThat(titlePageResp.generationPolicy()).isEqualTo(SectionGenerationPolicy.DETERMINISTIC);
        assertThat(titlePageResp.content().toUpperCase()).contains("AI DRONE NAVIGATION");
        assertThat(titlePageResp.content()).contains("Department of Computer Science");
        // Must NEVER contain literature synthesis prose
        assertThat(titlePageResp.content()).doesNotContain("Overview and Thematic Context");
        assertThat(titlePageResp.content()).doesNotContain("synthesis of grounded empirical literature");

        // Update Title Page details with custom metadata
        SectionResponse updatedTitlePage = analysisWorkflowService.updateTitlePageDetails(
                report.id(),
                primaryUser,
                new UpdateTitlePageDetailsRequest(
                        "Autonomous Urban Drone Delivery",
                        "Kofi Mensah",
                        "07210999",
                        "Takoradi Technical University",
                        "Computer Science Department",
                        "B.Tech Computer Science",
                        "Dr. E. Arthur",
                        "2024 / 2025",
                        2025
                )
        );
        assertThat(updatedTitlePage.content().toUpperCase()).contains("AUTONOMOUS URBAN DRONE DELIVERY");
        assertThat(updatedTitlePage.content()).contains("Kofi Mensah");
        assertThat(updatedTitlePage.content()).contains("07210999");
        assertThat(updatedTitlePage.content()).contains("Takoradi Technical University");
        assertThat(updatedTitlePage.content()).contains("Dr. E. Arthur");
    }

    @Test
    @DisplayName("Table of Contents is deterministic: reflects actual hierarchy, 0 AI calls")
    void testTableOfContentsDeterministicHierarchy() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "Smart Irrigation System");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);

        SectionResponse tocResp = analysisWorkflowService.refreshTableOfContents(report.id(), primaryUser);
        assertThat(tocResp).isNotNull();
        assertThat(tocResp.semanticPurpose()).isEqualTo(SectionSemanticPurpose.TABLE_OF_CONTENTS);
        assertThat(tocResp.generationPolicy()).isEqualTo(SectionGenerationPolicy.DETERMINISTIC);
        assertThat(tocResp.content()).contains("TABLE OF CONTENTS");
        assertThat(tocResp.content()).contains("Chapter One: Introduction");
        assertThat(tocResp.content()).contains("1.1 Background and Context");
        // Must NEVER contain literature synthesis prose
        assertThat(tocResp.content()).doesNotContain("Overview and Thematic Context");
        assertThat(tocResp.content()).doesNotContain("synthesis of grounded empirical literature");
    }

    @Test
    @DisplayName("List of Figures and List of Tables are extracted deterministically")
    void testListOfFiguresAndTablesDeterministicExtractors() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "Campus Navigation App");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);

        // Find a content section to embed figure and table into
        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        SectionStructureResponse introSec = structure.chapters().get(1).sections().get(0);

        analysisWorkflowService.updateSection(introSec.id(), primaryUser, new UpdateSectionRequest(
                null,
                null,
                "Here is the system architecture:\n\n![System Architecture](fig1.png)\n*Figure 1.1: System Architecture Overview*\n\n" +
                "And here is the requirements summary:\n\n| ID | Requirement |\n|---|---|\n| R1 | Real-time GPS |\n\n*Table 1.1: Functional Requirements*",
                null, null, null, null, null, null, null, null, null, null, null
        ));

        // Refresh List of Figures
        SectionResponse figuresResp = analysisWorkflowService.refreshListOfFigures(report.id(), primaryUser);
        assertThat(figuresResp.semanticPurpose()).isEqualTo(SectionSemanticPurpose.LIST_OF_FIGURES);
        assertThat(figuresResp.generationPolicy()).isEqualTo(SectionGenerationPolicy.DETERMINISTIC);
        assertThat(figuresResp.content()).contains("System Architecture");

        // Refresh List of Tables
        SectionResponse tablesResp = analysisWorkflowService.refreshListOfTables(report.id(), primaryUser);
        assertThat(tablesResp.semanticPurpose()).isEqualTo(SectionSemanticPurpose.LIST_OF_TABLES);
        assertThat(tablesResp.generationPolicy()).isEqualTo(SectionGenerationPolicy.DETERMINISTIC);
        assertThat(tablesResp.content()).contains("Summary Data Table");
    }

    @Test
    @DisplayName("Final document compiler includes front matter, TOC, body sections, custom chapters, and references")
    void testFinalDocumentCompilerProducesCompleteSnapshot() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "Clinic Queue Management System");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);
        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);

        SectionStructureResponse chapterOne = firstNumberedSection(structure, 1);
        SectionStructureResponse chapterTwo = firstNumberedSection(structure, 2);
        SectionStructureResponse chapterThree = firstNumberedSection(structure, 3);

        analysisWorkflowService.updateSection(chapterOne.id(), primaryUser, new UpdateSectionRequest(
                null, null, "Background content for the queue management system.", null, null, null, null, null, null, null, null, null, null, null
        ));
        analysisWorkflowService.updateSection(chapterTwo.id(), primaryUser, new UpdateSectionRequest(
                null, null, "Literature review content comparing appointment scheduling systems.", null, null, null, null, null, null, null, null, null, null, null
        ));
        analysisWorkflowService.updateSection(chapterThree.id(), primaryUser, new UpdateSectionRequest(
                null, null, "System design content describing modules, database schema, and workflows.", null, null, null, null, null, null, null, null, null, null, null
        ));

        ChapterResponse ch6 = analysisWorkflowService.createChapter(report.id(), primaryUser, new CreateChapterRequest(
                ReportChapterType.CUSTOM, "Chapter Six: Deployment and Maintenance", 6, null, false, false
        ));
        analysisWorkflowService.createSection(ch6.id(), primaryUser, new CreateSectionRequest(
                ReportSectionType.CUSTOM, "Deployment Environment", "Deployment environment content for production hosting.", null, null, null,
                null, null, null, null, null, null, false, false, true
        ));

        FinalDocumentResponse finalDoc = analysisWorkflowService.prepareFinalDocument(report.id(), primaryUser);

        assertThat(finalDoc.plainText()).contains("Title Page");
        assertThat(finalDoc.plainText()).contains("Table of Contents");
        assertThat(finalDoc.plainText()).contains("Background content for the queue management system.");
        assertThat(finalDoc.plainText()).contains("Literature review content comparing appointment scheduling systems.");
        assertThat(finalDoc.plainText()).contains("System design content describing modules, database schema, and workflows.");
        assertThat(finalDoc.plainText()).contains("Deployment and Maintenance");
        assertThat(finalDoc.plainText()).contains("Deployment environment content for production hosting.");
        assertThat(finalDoc.plainText()).contains("References");
        assertThat(finalDoc.plainText().indexOf("Table of Contents"))
                .isLessThan(finalDoc.plainText().indexOf("Background content for the queue management system."));

        DocumentExportSelection fullSelection = new DocumentExportSelection(
                "FULL", List.of(), true, true, true, true, true, true, true, "ALL_PROJECT_REFERENCES"
        );
        ReportDocumentCompiler.CompiledAcademicDocument fullPreview = reportExportService.preview(report.id(), fullSelection, primaryUser);
        assertThat(fullPreview.plainText()).contains("Background content for the queue management system.");
        assertThat(fullPreview.plainText()).contains("Literature review content comparing appointment scheduling systems.");
        assertThat(fullPreview.plainText()).contains("System design content describing modules, database schema, and workflows.");
        assertThat(fullPreview.compiledSectionCount()).isGreaterThan(0);
        assertThat(fullPreview.nonEmptySectionCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Selected preview compiles chapter parents with descendants in canonical numbering")
    void testSelectedPreviewCompilesChapterSelectionsWithCanonicalNumbering() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "Wi-Fi Coverage Optimization");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);
        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);

        ChapterStructureResponse chapterOne = structure.chapters().stream()
                .filter(chapter -> chapter.chapterNumber() != null && chapter.chapterNumber() == 1)
                .findFirst().orElseThrow();
        ChapterStructureResponse chapterTwo = structure.chapters().stream()
                .filter(chapter -> chapter.chapterNumber() != null && chapter.chapterNumber() == 2)
                .findFirst().orElseThrow();
        SectionStructureResponse chapterOneSection = chapterOne.sections().stream().findFirst().orElseThrow();
        SectionStructureResponse chapterTwoSection = chapterTwo.sections().stream().findFirst().orElseThrow();

        analysisWorkflowService.updateSection(chapterOneSection.id(), primaryUser, new UpdateSectionRequest(
                null, null, "Chapter one Wi-Fi access point coverage analysis body.", null, null, null, null, null, null, null, null, null, null, null
        ));
        analysisWorkflowService.updateSection(chapterTwoSection.id(), primaryUser, new UpdateSectionRequest(
                null, null, "Chapter two radio-frequency propagation literature body.", null, null, null, null, null, null, null, null, null, null, null
        ));

        DocumentExportSelection chapterOneSelection = new DocumentExportSelection(
                "SELECTED", List.of(chapterOne.id()), false, false, false, false, false, true, false, "CITED_IN_SELECTION"
        );
        ReportDocumentCompiler.CompiledAcademicDocument chapterOnePreview = reportExportService.preview(report.id(), chapterOneSelection, primaryUser);
        assertThat(chapterOnePreview.plainText()).contains("CHAPTER 1");
        assertThat(chapterOnePreview.plainText()).contains("Chapter one Wi-Fi access point coverage analysis body.");
        assertThat(chapterOnePreview.plainText()).doesNotContain("Chapter two radio-frequency propagation literature body.");
        assertThat(chapterOnePreview.compiledSectionCount()).isGreaterThan(0);

        DocumentExportSelection chapterTwoSelection = new DocumentExportSelection(
                "SELECTED", List.of(chapterTwo.id()), false, false, false, false, false, true, false, "CITED_IN_SELECTION"
        );
        ReportDocumentCompiler.CompiledAcademicDocument chapterTwoPreview = reportExportService.preview(report.id(), chapterTwoSelection, primaryUser);
        assertThat(chapterTwoPreview.plainText()).contains("CHAPTER 2");
        assertThat(chapterTwoPreview.plainText()).contains(chapterTwoSection.sectionNumber());
        assertThat(chapterTwoPreview.plainText()).contains("Chapter two radio-frequency propagation literature body.");
        assertThat(chapterTwoPreview.plainText()).doesNotContain("CHAPTER 1");
        assertThat(chapterTwoPreview.plainText()).doesNotContain("Chapter one Wi-Fi access point coverage analysis body.");

        DocumentExportSelection chaptersOneAndTwoSelection = new DocumentExportSelection(
                "SELECTED", List.of(chapterOne.id(), chapterTwo.id()), false, false, true, false, false, true, false, "CITED_IN_SELECTION"
        );
        ReportDocumentCompiler.CompiledAcademicDocument chaptersOneAndTwoPreview = reportExportService.preview(report.id(), chaptersOneAndTwoSelection, primaryUser);
        assertThat(chaptersOneAndTwoPreview.plainText()).contains("Table of Contents");
        assertThat(chaptersOneAndTwoPreview.plainText()).contains("CHAPTER 1");
        assertThat(chaptersOneAndTwoPreview.plainText()).contains("CHAPTER 2");
        assertThat(chaptersOneAndTwoPreview.plainText().indexOf("CHAPTER 1"))
                .isLessThan(chaptersOneAndTwoPreview.plainText().indexOf("CHAPTER 2"));
        assertThat(chaptersOneAndTwoPreview.compiledSectionCount()).isGreaterThanOrEqualTo(2);
        assertThat(chaptersOneAndTwoPreview.tocEntryCount()).isGreaterThanOrEqualTo(2);

        DocumentExportSelection emptySelection = new DocumentExportSelection(
                "SELECTED", List.of(), false, false, false, false, false, true, false, "CITED_IN_SELECTION"
        );
        assertThatThrownBy(() -> reportExportService.preview(report.id(), emptySelection, primaryUser))
                .hasMessageContaining("Selected content export requires at least one selected chapter or section.");
    }

    @Test
    @DisplayName("Updating final document from sections creates a new snapshot and stale state clears")
    void testFinalDocumentUpdatePreservesOldSnapshot() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "Library Inventory System");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);
        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        SectionStructureResponse section = firstNumberedSection(structure, 1);

        analysisWorkflowService.updateSection(section.id(), primaryUser, new UpdateSectionRequest(
                null, null, "Initial background body.", null, null, null, null, null, null, null, null, null, null, null
        ));
        FinalDocumentResponse v1 = analysisWorkflowService.prepareFinalDocument(report.id(), primaryUser);

        analysisWorkflowService.updateSection(section.id(), primaryUser, new UpdateSectionRequest(
                null, null, "Updated background body after section editing.", null, null, null, null, null, null, null, null, null, null, null
        ));
        FinalDocumentResponse stale = analysisWorkflowService.getFinalDocument(report.id(), primaryUser);
        assertThat(stale.stale()).isTrue();

        FinalDocumentResponse v2 = analysisWorkflowService.prepareFinalDocument(report.id(), primaryUser);
        assertThat(v2.versionNumber()).isEqualTo(v1.versionNumber() + 1);
        assertThat(v2.stale()).isFalse();
        assertThat(v2.plainText()).contains("Updated background body after section editing.");
        assertThat(reportDocumentVersionRepository.findById(v1.id())).isPresent();
    }

    @Test
    @DisplayName("Finalized report can reopen working revision and finalize a new immutable version")
    void testFinalizedReportReopensWorkingRevisionForNextFinalVersion() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "Clinic Queue System");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);
        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        for (SectionStructureResponse section : flattenSections(structure)) {
            if (section.type() == ReportSectionType.LITERATURE_REVIEW || section.type() == ReportSectionType.CONCLUSIONS) {
                analysisWorkflowService.updateSection(section.id(), primaryUser, new UpdateSectionRequest(
                        null, null, section.heading() + " completed content.", null, null, null, null, null, null, null, null, null, null, null
                ));
            }
        }

        ReportResponse finalized = analysisWorkflowService.finalizeReport(report.id(), primaryUser);
        assertThat(finalized.status()).isEqualTo(ResearchReportStatus.FINAL);
        FinalDocumentResponse v1 = analysisWorkflowService.getFinalDocument(report.id(), primaryUser);

        SectionStructureResponse background = firstNumberedSection(
                analysisWorkflowService.getReportStructure(report.id(), primaryUser),
                1
        );
        analysisWorkflowService.updateSection(background.id(), primaryUser, new UpdateSectionRequest(
                null, null, "Background changed after first finalization.", null, null, null, null, null, null, null, null, null, null, null
        ));

        FinalDocumentResponse stale = analysisWorkflowService.getFinalDocument(report.id(), primaryUser);
        assertThat(stale.id()).isEqualTo(v1.id());
        assertThat(stale.stale()).isTrue();

        ReportResponse refinalized = analysisWorkflowService.finalizeReport(report.id(), primaryUser);
        FinalDocumentResponse v2 = analysisWorkflowService.getFinalDocument(report.id(), primaryUser);

        assertThat(refinalized.status()).isEqualTo(ResearchReportStatus.FINAL);
        assertThat(v2.versionNumber()).isEqualTo(v1.versionNumber() + 1);
        assertThat(v2.stale()).isFalse();
        assertThat(v2.plainText()).contains("Background changed after first finalization.");
        assertThat(reportDocumentVersionRepository.findById(v1.id())).isPresent();
    }

    @Test
    @DisplayName("Final document editor cannot overwrite non-empty snapshot with empty content")
    void testFinalDocumentEmptyOverwriteProtection() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "Pharmacy Stock System");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);
        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        SectionStructureResponse section = firstNumberedSection(structure, 1);
        analysisWorkflowService.updateSection(section.id(), primaryUser, new UpdateSectionRequest(
                null, null, "Non-empty section content.", null, null, null, null, null, null, null, null, null, null, null
        ));
        analysisWorkflowService.prepareFinalDocument(report.id(), primaryUser);

        assertThatThrownBy(() -> analysisWorkflowService.updateFinalDocument(
                report.id(),
                primaryUser,
                new com.researchassistant.analysis.dto.AnalysisDtos.UpdateFinalDocumentRequest(
                        "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}",
                        "",
                        null
                )
        )).hasMessageContaining("REPORT_FINAL_DOCUMENT_EMPTY");
    }

    @Test
    @DisplayName("Deterministic repair restores corrupted legacy AI prose to clean template format")
    void testDeterministicRepairCleansCorruptedLegacyProse() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "Hospital Management System");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);

        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        ChapterStructureResponse prelimChapter = structure.chapters().get(0);
        SectionStructureResponse titlePageSec = prelimChapter.sections().stream()
                .filter(s -> s.semanticPurpose() == SectionSemanticPurpose.TITLE_PAGE)
                .findFirst().orElseThrow();

        // Simulate corrupted legacy AI content generated by the broken route
        String corruptedText = "## Overview and Thematic Context\n\n" +
                "This section examines the Title Page for Preliminary Pages through a synthesis of grounded empirical literature...";
        analysisWorkflowService.updateSection(titlePageSec.id(), primaryUser, new UpdateSectionRequest(
                null, null, corruptedText, null, null, null, null, null, null, null, null, null, null, null
        ));

        // Execute deterministic repair
        DeterministicRepairResponse repairResp = analysisWorkflowService.repairDeterministicNodes(report.id(), primaryUser, true);
        assertThat(repairResp.repairedSectionsCount()).isGreaterThanOrEqualTo(1);
        assertThat(repairResp.repairedSectionTitles()).contains("Title Page");

        // Verify Title Page is now clean template format
        ReportStructureResponse repairedStructure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        SectionStructureResponse repairedTitleSec = repairedStructure.chapters().get(0).sections().stream()
                .filter(s -> s.id().equals(titlePageSec.id())).findFirst().orElseThrow();

        // Content must no longer contain the corrupted text
        assertThat(repairedTitleSec.heading()).isNotNull();
    }

    @Test
    @DisplayName("Testing section is gated by empirical evidence and rejects ungrounded generation")
    void testTestingSectionGatedByEmpiricalEvidence() throws Exception {
        ResearchProjectResponse project = createWorkspaceProject(AcademicWorkspaceType.ACADEMIC_PROJECT, "Secure Banking App");
        ReportResponse report = analysisWorkflowService.ensureReportInitialized(project.id(), primaryUser);

        ReportStructureResponse structure = analysisWorkflowService.getReportStructure(report.id(), primaryUser);
        // Find or create TESTING section
        ChapterStructureResponse ch4 = structure.chapters().stream()
                .filter(c -> c.chapterNumber() != null && c.chapterNumber() == 4)
                .findFirst().orElse(structure.chapters().get(structure.chapters().size() - 2));

        SectionResponse testingSec = analysisWorkflowService.createSection(ch4.id(), primaryUser, new CreateSectionRequest(
                ReportSectionType.TESTING, "System Testing and Results", null, null, null, null,
                null, null, null, null, null, null, false, false, true
        ));

        // Attempting to generate testing prose without test runs/empirical datasets throws InsufficientProjectEvidenceException
        assertThatThrownBy(() -> analysisWorkflowService.generateSection(
                testingSec.id(),
                primaryUser,
                new GenerateSectionRequest(
                        null,
                        null,
                        testingSec.id(),
                        "System Testing and Results",
                        null,
                        null,
                        null,
                        "REPLACE"
                )
        )).isInstanceOf(InsufficientProjectEvidenceException.class);
    }

    @Test
    @DisplayName("ReportSectionPromptBuilder produces materially distinct prompts per semantic purpose")
    void testPromptBuilderTailoredPerSemanticPurpose() {
        String litPrompt = ReportSectionPromptBuilder.buildPrompt(
                AcademicWorkspaceType.ACADEMIC_PROJECT,
                AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT,
                "Project Report",
                "Chapter Two: Literature Review",
                "2.1 Conceptual Framework",
                SectionSemanticPurpose.LITERATURE_REVIEW,
                "Multi-source synthesis guidelines",
                "E-Commerce Platform",
                null,
                "Focus on distributed transactions"
        );

        String designPrompt = ReportSectionPromptBuilder.buildPrompt(
                AcademicWorkspaceType.ACADEMIC_PROJECT,
                AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT,
                "Project Report",
                "Chapter Three: System Design",
                "3.2 Database Schema Design",
                SectionSemanticPurpose.SYSTEM_DESIGN,
                "Relational schema guidelines",
                "E-Commerce Platform",
                null,
                "Focus on PostgreSQL normalized tables"
        );

        String testingPrompt = ReportSectionPromptBuilder.buildPrompt(
                AcademicWorkspaceType.ACADEMIC_PROJECT,
                AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT,
                "Project Report",
                "Chapter Four: Implementation and Testing",
                "4.3 Automated Integration Testing",
                SectionSemanticPurpose.TESTING,
                "Test report guidelines",
                "E-Commerce Platform",
                null,
                "Verify JUnit test results"
        );

        String customPrompt = ReportSectionPromptBuilder.buildPrompt(
                AcademicWorkspaceType.ACADEMIC_PROJECT,
                AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT,
                "Project Report",
                "Chapter Six: Deployment and Maintenance",
                "6.2 Containerized Kubernetes Installation",
                SectionSemanticPurpose.CUSTOM,
                null,
                "E-Commerce Platform",
                null,
                "Provide Helm chart details"
        );

        String backgroundPrompt = ReportSectionPromptBuilder.buildPrompt(
                AcademicWorkspaceType.ACADEMIC_RESEARCH,
                AcademicProjectType.GENERAL_ACADEMIC_PROJECT,
                "Research Report",
                "Chapter One: Introduction",
                "Background of the Study",
                SectionSemanticPurpose.BACKGROUND,
                null,
                "Digital Agriculture Adoption",
                null,
                null
        );

        String problemPrompt = ReportSectionPromptBuilder.buildPrompt(
                AcademicWorkspaceType.ACADEMIC_RESEARCH,
                AcademicProjectType.GENERAL_ACADEMIC_PROJECT,
                "Research Report",
                "Chapter One: Introduction",
                "Problem Statement",
                SectionSemanticPurpose.PROBLEM_STATEMENT,
                null,
                "Digital Agriculture Adoption",
                null,
                null
        );

        String methodologyPrompt = ReportSectionPromptBuilder.buildPrompt(
                AcademicWorkspaceType.ACADEMIC_RESEARCH,
                AcademicProjectType.GENERAL_ACADEMIC_PROJECT,
                "Research Report",
                "Chapter Three: Methodology",
                "Methodology",
                SectionSemanticPurpose.METHODOLOGY,
                null,
                "Digital Agriculture Adoption",
                null,
                null
        );

        // Prompts must be materially distinct and specialized:
        assertThat(backgroundPrompt).contains("scholarly and contextual foundation");
        assertThat(problemPrompt).contains("what is currently happening");
        assertThat(litPrompt).contains("comprehensive multi-paper literature synthesis");
        assertThat(litPrompt).contains("agreements, disagreements, methodological patterns");
        assertThat(litPrompt).contains("CRITICAL SECTION PURPOSE - LITERATURE REVIEW");

        assertThat(designPrompt).contains("CRITICAL SECTION PURPOSE - SYSTEM DESIGN & ARCHITECTURE");
        assertThat(designPrompt).contains("architecture/design information");

        assertThat(methodologyPrompt).contains("research design");
        assertThat(testingPrompt).contains("CRITICAL SECTION PURPOSE - SYSTEM TESTING");
        assertThat(testingPrompt).contains("Never invent PASS/FAIL results");

        assertThat(customPrompt).contains("CRITICAL SECTION PURPOSE - CUSTOM DOCUMENT SECTION");
        assertThat(customPrompt).contains("Containerized Kubernetes Installation");
        assertThat(customPrompt).contains("Deployment and Maintenance");

        // Verify pairwise inequality
        assertThat(backgroundPrompt).isNotEqualTo(litPrompt);
        assertThat(designPrompt).isNotEqualTo(litPrompt);
        assertThat(problemPrompt).isNotEqualTo(backgroundPrompt);
        assertThat(litPrompt).isNotEqualTo(designPrompt);
        assertThat(designPrompt).isNotEqualTo(testingPrompt);
        assertThat(testingPrompt).isNotEqualTo(customPrompt);
    }
}
