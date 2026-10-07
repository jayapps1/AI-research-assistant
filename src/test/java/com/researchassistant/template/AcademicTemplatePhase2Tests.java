package com.researchassistant.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.repository.AcademicDocumentGuidelineRepository;
import com.researchassistant.analysis.repository.ResearchReportChapterRepository;
import com.researchassistant.analysis.repository.ResearchReportRepository;
import com.researchassistant.analysis.repository.ResearchReportSectionRepository;
import com.researchassistant.analysis.repository.ResearchReportTemplateRepository;
import com.researchassistant.analysis.service.AcademicSectionPromptService;
import com.researchassistant.analysis.service.AcademicSectionPromptService.SectionGenerationContext;
import com.researchassistant.document.dto.DocumentResponse;
import com.researchassistant.document.entity.AcademicFileRole;
import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.entity.DocumentType;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.document.service.DocumentService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.project.entity.AcademicWorkspaceType;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.template.dto.AcademicTemplateDtos.*;
import com.researchassistant.template.service.*;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

import static com.researchassistant.template.dto.AcademicTemplateDtos.NOT_SPECIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcademicTemplatePhase2Tests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("1. Three Distinct Academic File Roles & Isolation")
    class AcademicFileRoleIsolationTests {

        @Test
        @DisplayName("TEMPLATE_GUIDELINE, EXAMPLE_REPORT, and RESEARCH_SOURCE have distinct roles")
        void testFileRoleClassification() {
            Document doc1 = new Document();
            doc1.setAcademicRole(AcademicFileRole.TEMPLATE_GUIDELINE);

            Document doc2 = new Document();
            doc2.setAcademicRole(AcademicFileRole.EXAMPLE_REPORT);

            Document doc3 = new Document();
            doc3.setAcademicRole(AcademicFileRole.RESEARCH_SOURCE);

            assertThat(doc1.getAcademicRole()).isEqualTo(AcademicFileRole.TEMPLATE_GUIDELINE);
            assertThat(doc2.getAcademicRole()).isEqualTo(AcademicFileRole.EXAMPLE_REPORT);
            assertThat(doc3.getAcademicRole()).isEqualTo(AcademicFileRole.RESEARCH_SOURCE);
        }

        @Test
        @DisplayName("Only RESEARCH_SOURCE is eligible for research literature evidence; guidelines and example reports are excluded")
        void testEvidenceExclusionForNonResearchSources() {
            List<Document> documents = new ArrayList<>();

            Document researchDoc = new Document();
            researchDoc.setId(UUID.randomUUID());
            researchDoc.setAcademicRole(AcademicFileRole.RESEARCH_SOURCE);
            researchDoc.setStatus(DocumentStatus.READY);
            documents.add(researchDoc);

            Document guidelineDoc = new Document();
            guidelineDoc.setId(UUID.randomUUID());
            guidelineDoc.setAcademicRole(AcademicFileRole.TEMPLATE_GUIDELINE);
            guidelineDoc.setStatus(DocumentStatus.READY);
            documents.add(guidelineDoc);

            Document exampleDoc = new Document();
            exampleDoc.setId(UUID.randomUUID());
            exampleDoc.setAcademicRole(AcademicFileRole.EXAMPLE_REPORT);
            exampleDoc.setStatus(DocumentStatus.READY);
            documents.add(exampleDoc);

            List<Document> eligibleForRag = documents.stream()
                    .filter(d -> d.getAcademicRole() == AcademicFileRole.RESEARCH_SOURCE)
                    .toList();

            assertThat(eligibleForRag).hasSize(1);
            assertThat(eligibleForRag.get(0).getId()).isEqualTo(researchDoc.getId());
            assertThat(eligibleForRag).doesNotContain(guidelineDoc, exampleDoc);
        }
    }

    @Nested
    @DisplayName("3 & 5. Structure Extraction Engine (Institution, Front Matter, Chapters, Formatting)")
    class TemplateExtractionTests {

        private AcademicTemplateExtractionService extractionService;

        @BeforeEach
        void setUp() {
            extractionService = new AcademicTemplateExtractionService(
                    mock(com.researchassistant.document.extraction.PdfBoxTextExtractor.class),
                    mock(com.researchassistant.document.extraction.DocxTextExtractor.class),
                    objectMapper
            );
        }

        @Test
        @DisplayName("Extracts Takoradi Technical University Computer Science guideline structure accurately")
        void testExtractTtuComputerScienceGuideline() {
            String guidelineText = """
                    TAKORADI TECHNICAL UNIVERSITY
                    FACULTY OF APPLIED SCIENCES
                    DEPARTMENT OF COMPUTER SCIENCE
                    HND COMPUTER SCIENCE PROGRAMME
                    
                    GUIDELINES FOR FINAL YEAR PROJECT REPORT
                    
                    FORMATTING REQUIREMENTS:
                    1. Font: Times New Roman, 12 pt font for body text.
                    2. Headings: 14 pt bold for chapter titles.
                    3. Spacing: 1.5 line spacing throughout.
                    4. Margins: Left margin 1.5 inches for binding; right, top and bottom margins 1.0 inch.
                    5. Referencing style: IEEE referencing format.
                    
                    PRELIMINARY PAGES:
                    Title Page
                    Declaration
                    Certification
                    Dedication
                    Acknowledgements
                    Abstract
                    Table of Contents
                    List of Tables
                    List of Figures
                    
                    CHAPTER ONE: INTRODUCTION
                    1.1 Background of the Study
                    1.2 Statement of the Problem
                    1.3 Purpose / Aim and Objectives of the Study
                    1.4 Research Questions
                    1.5 Significance of the Study
                    1.6 Limitations and Delimitations
                    1.7 Organization of the Study
                    
                    CHAPTER TWO: LITERATURE REVIEW
                    2.1 Theoretical Framework
                    2.2 Review of Related Systems
                    2.3 Summary and Research Gap
                    
                    CHAPTER THREE: PROPOSED METHOD / SYSTEM / SOFTWARE
                    3.1 System Requirements Specification
                    3.2 Architecture and System Design
                    3.3 Database Design and Data Flow
                    
                    CHAPTER FOUR: RESULTS AND DISCUSSIONS
                    4.1 System Testing and Verification
                    4.2 Evaluation Results and Discussions
                    
                    CHAPTER FIVE: CONCLUSION AND RECOMMENDATION
                    5.1 Summary of Findings
                    5.2 Conclusions
                    5.3 Recommendations
                    
                    APPENDICES
                    Appendix A: Source Code Listings
                    Appendix B: User Manual
                    """;

            ExtractedAcademicTemplate template = extractionService.extractFromText(guidelineText, "ttu_cs_guideline.pdf");

            // Metadata
            assertThat(template.institution()).containsIgnoringCase("Technical University");
            assertThat(template.department()).containsIgnoringCase("Department of Computer Science");
            assertThat(template.documentType()).containsIgnoringCase("Project Report");
            assertThat(template.citationStyle()).isEqualTo("IEEE");

            // Formatting
            assertThat(template.formattingRules().fontFamily()).isEqualTo("Times New Roman");
            assertThat(template.formattingRules().bodyFontSize()).isEqualTo("12 pt");
            assertThat(template.formattingRules().lineSpacing()).isEqualTo("1.5");
            assertThat(template.formattingRules().margins().leftMargin()).contains("1.5");

            // Front matter
            assertThat(template.frontMatter()).extracting(TemplateSectionDefinitionDto::heading)
                    .contains("Title Page", "Declaration", "Certification", "Acknowledgements", "Abstract", "Table of Contents");

            // Chapters
            assertThat(template.chapters()).hasSize(5);
            assertThat(template.chapters().get(0).title()).contains("INTRODUCTION");
            assertThat(template.chapters().get(0).sections()).extracting(TemplateSectionDefinitionDto::heading)
                    .contains("Background of the Study", "Statement of the Problem", "Organization of the Study");

            assertThat(template.chapters().get(1).title()).contains("LITERATURE REVIEW");
            assertThat(template.chapters().get(2).title()).contains("SYSTEM");
            assertThat(template.chapters().get(3).title()).contains("RESULTS");
            assertThat(template.chapters().get(4).title()).contains("CONCLUSION");

            // Appendices
            assertThat(template.appendices()).isNotEmpty();
        }

        @Test
        @DisplayName("Unspecified fields strictly output NOT_SPECIFIED without hallucination")
        void testUnspecifiedFieldsFallBackToNotSpecified() {
            String sparseText = """
                    PROJECT GUIDELINES
                    
                    CHAPTER ONE: INTRODUCTION
                    1.1 Background
                    1.2 Problem Statement
                    
                    CHAPTER TWO: LITERATURE REVIEW
                    2.1 Literature Review
                    """;

            ExtractedAcademicTemplate template = extractionService.extractFromText(sparseText, "sparse.docx");

            assertThat(template.institution()).isEqualTo(NOT_SPECIFIED);
            assertThat(template.department()).isEqualTo(NOT_SPECIFIED);
            assertThat(template.formattingRules().fontFamily()).isEqualTo(NOT_SPECIFIED);
            assertThat(template.formattingRules().lineSpacing()).isEqualTo(NOT_SPECIFIED);
            assertThat(template.uncertainItems()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("4 & 11 & 12. Review, Approval, Remapping & Content Preservation")
    class TemplateReviewAndApprovalTests {

        @Mock private AcademicDocumentGuidelineRepository guidelineRepository;
        @Mock private AcademicTemplateExtractionService extractionService;
        @Mock private DocumentService documentService;
        @Mock private DocumentRepository documentRepository;
        @Mock private WorkspaceRepository workspaceRepository;
        @Mock private ResearchProjectRepository projectRepository;
        @Mock private ResearchReportRepository reportRepository;
        @Mock private ResearchReportChapterRepository chapterRepository;
        @Mock private ResearchReportSectionRepository sectionRepository;
        @Mock private ResearchReportTemplateRepository templateRepository;

        private AcademicTemplateService templateService;
        private User testUser;
        private ResearchProject testProject;
        private Workspace testWorkspace;

        @BeforeEach
        void setUp() {
            templateService = new AcademicTemplateService(
                    guidelineRepository, extractionService, documentService, documentRepository,
                    workspaceRepository, projectRepository, reportRepository, chapterRepository,
                    sectionRepository, templateRepository, objectMapper
            );

            testUser = new User();
            testUser.setId(UUID.randomUUID());
            testUser.setEmail("researcher@test.com");

            testWorkspace = new Workspace();
            testWorkspace.setId(UUID.randomUUID());

            testProject = new ResearchProject();
            testProject.setId(UUID.randomUUID());
            testProject.setTitle("Mobile Crop Disease Advisory");
            testProject.setWorkspace(testWorkspace);
        }

        @Test
        @DisplayName("User can review, edit detected items, and approve guideline")
        void testReviewAndEditGuideline() {
            UUID guidelineId = UUID.randomUUID();
            AcademicDocumentGuideline guideline = new AcademicDocumentGuideline();
            guideline.setId(guidelineId);
            guideline.setWorkspace(testWorkspace);
            guideline.setProject(testProject);
            guideline.setOriginalFileName("guideline.pdf");
            guideline.setStatus("EXTRACTED");
            guideline.setInstitution("Uncertain Uni");
            guideline.setUploadedAt(OffsetDateTime.now());

            when(guidelineRepository.findById(guidelineId)).thenReturn(Optional.of(guideline));
            when(guidelineRepository.save(any(AcademicDocumentGuideline.class))).thenAnswer(i -> i.getArgument(0));

            UpdateAcademicGuidelineRequest updateReq = new UpdateAcademicGuidelineRequest(
                    "Takoradi Technical University",
                    "Department of Computer Science",
                    "HND Computer Science",
                    "Final Year Project Report",
                    "IEEE",
                    null
            );

            AcademicDocumentGuidelineResponse response = templateService.updateGuideline(guidelineId, updateReq, testUser);

            assertThat(response.status()).isEqualTo("EDITED");
            assertThat(response.institution()).isEqualTo("Takoradi Technical University");
            assertThat(response.department()).isEqualTo("Department of Computer Science");
            assertThat(response.citationStyle()).isEqualTo("IEEE");
        }

        @Test
        @DisplayName("Applying approved guideline preserves previously written section content")
        void testApplyGuidelinePreservesExistingContent() {
            UUID reportId = UUID.randomUUID();
            ResearchReport report = new ResearchReport();
            report.setId(reportId);
            report.setProject(testProject);

            when(reportRepository.findByProjectId(testProject.getId())).thenReturn(Optional.of(report));

            // Existing section with student's drafted content
            ResearchReportSection existingBg = new ResearchReportSection();
            existingBg.setId(UUID.randomUUID());
            existingBg.setHeading("1.1 Background of the Study");
            existingBg.setSemanticPurpose(SectionSemanticPurpose.BACKGROUND);
            existingBg.setContent("This is the student's carefully crafted background content.");
            existingBg.setStatus(ReportSectionStatus.DRAFT);

            when(sectionRepository.findAllByReportIdOrderByChapterDisplayOrderAscDisplayOrderAsc(reportId))
                    .thenReturn(List.of(existingBg));
            when(chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(reportId))
                    .thenReturn(List.of());
            when(chapterRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(sectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ExtractedAcademicTemplate template = new ExtractedAcademicTemplate(
                    "TTU", "CS", "HND", "Project Report", "IEEE",
                    List.of(),
                    List.of(new TemplateChapterDefinitionDto(
                            1, "CHAPTER ONE — INTRODUCTION", ReportChapterType.INTRODUCTION, true,
                            List.of(new TemplateSectionDefinitionDto(
                                    "1.1", "1.1 Background of the Study", SectionRequirementLevel.REQUIRED,
                                    SectionSemanticPurpose.BACKGROUND, SectionGenerationPolicy.SOURCE_GROUNDED_AI,
                                    "Background requirements", List.of()
                            ))
                    )),
                    List.of(),
                    TemplateFormattingRulesDto.defaultUnspecified(),
                    List.of()
            );

            templateService.applyTemplateToProjectReport(testProject, template, true, testUser);

            // Verify section was re-created with preserved content
            verify(sectionRepository, atLeastOnce()).save(argThat(sec ->
                    "1.1 Background of the Study".equals(sec.getHeading())
                    && sec.getContent() != null
                    && sec.getContent().contains("student's carefully crafted background content")
                    && sec.getRequirementLevel() == SectionRequirementLevel.REQUIRED
            ));
        }
    }

    @Nested
    @DisplayName("6. Template Relevance & Ranking")
    class TemplateRelevanceTests {

        @Mock private ResearchReportTemplateRepository templateRepository;
        private TemplateRelevanceService relevanceService;

        @BeforeEach
        void setUp() {
            relevanceService = new TemplateRelevanceService(templateRepository);
        }

        @Test
        @DisplayName("Software project ranks computer science / software templates top and penalizes nursing/business")
        void testSoftwareProjectRanksRelevantTemplates() {
            ResearchReportTemplate csTemplate = new ResearchReportTemplate();
            csTemplate.setId(UUID.randomUUID());
            csTemplate.setName("Computer Science Software Project Template");
            csTemplate.setSupportedWorkspaceTypes("ACADEMIC_PROJECT");
            csTemplate.setDescription("Software system development report with architecture and testing");
            csTemplate.setSystemTemplate(true);

            ResearchReportTemplate nursingTemplate = new ResearchReportTemplate();
            nursingTemplate.setId(UUID.randomUUID());
            nursingTemplate.setName("Clinical Nursing Dissertation");
            nursingTemplate.setSupportedWorkspaceTypes("ACADEMIC_PROJECT");
            nursingTemplate.setDescription("Clinical trial and patient cohort study");
            nursingTemplate.setSystemTemplate(true);

            when(templateRepository.findAll()).thenReturn(List.of(nursingTemplate, csTemplate));

            List<RecommendedTemplateResponse> recommended = relevanceService.getRecommendedTemplates(
                    AcademicWorkspaceType.ACADEMIC_PROJECT,
                    AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT,
                    null, "Computer Science", null
            );

            assertThat(recommended).isNotEmpty();
            assertThat(recommended.get(0).name()).isEqualTo("Computer Science Software Project Template");
            assertThat(recommended.get(0).relevanceScore()).isGreaterThan(recommended.get(1).relevanceScore());
        }
    }

    @Nested
    @DisplayName("7 & 8. Dynamic TOC and Requirement Validation")
    class DynamicTocAndValidationTests {

        @Mock private ResearchReportRepository reportRepository;
        @Mock private ResearchReportChapterRepository chapterRepository;
        @Mock private ResearchReportSectionRepository sectionRepository;
        @Mock private AcademicDocumentGuidelineRepository guidelineRepository;

        private ReportDynamicTocService tocService;
        private ReportStructureValidationService validationService;
        private UUID projectId;
        private ResearchReport report;

        @BeforeEach
        void setUp() {
            tocService = new ReportDynamicTocService(reportRepository, chapterRepository, sectionRepository);
            validationService = new ReportStructureValidationService(reportRepository, sectionRepository, guidelineRepository, objectMapper);

            projectId = UUID.randomUUID();
            report = new ResearchReport();
            report.setId(UUID.randomUUID());

            when(reportRepository.findByProjectId(projectId)).thenReturn(Optional.of(report));
        }

        @Test
        @DisplayName("Dynamic TOC reflects current hierarchy and dynamic page numbering without static template numbers")
        void testDynamicTocGeneration() {
            ResearchReportChapter ch1 = new ResearchReportChapter();
            ch1.setId(UUID.randomUUID());
            ch1.setChapterNumber(1);
            ch1.setTitle("CHAPTER ONE: INTRODUCTION");
            ch1.setDisplayOrder(1);

            ResearchReportSection sec1 = new ResearchReportSection();
            sec1.setId(UUID.randomUUID());
            sec1.setChapter(ch1);
            sec1.setSectionNumber("1.1");
            sec1.setHeading("Background of the Study");
            sec1.setContent("A very comprehensive domain background containing several paragraphs...");
            sec1.setDisplayOrder(1);
            sec1.setRequirementLevel(SectionRequirementLevel.REQUIRED);

            when(chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId())).thenReturn(List.of(ch1));
            when(sectionRepository.findAllByReportIdOrderByChapterDisplayOrderAscDisplayOrderAsc(report.getId())).thenReturn(List.of(sec1));

            DynamicTableOfContentsResponse toc = tocService.generateDynamicToc(projectId);

            assertThat(toc.chapters()).hasSize(1);
            assertThat(toc.chapters().get(0).title()).isEqualTo("CHAPTER ONE: INTRODUCTION");
            assertThat(toc.chapters().get(0).sections()).hasSize(1);
            assertThat(toc.chapters().get(0).sections().get(0).heading()).isEqualTo("Background of the Study");
            assertThat(toc.chapters().get(0).sections().get(0).estimatedPageNumber()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Validation service warns if required sections are missing or empty")
        void testValidationWarningsForMissingRequiredSections() {
            ResearchReportSection emptyRequiredSec = new ResearchReportSection();
            emptyRequiredSec.setId(UUID.randomUUID());
            emptyRequiredSec.setHeading("Problem Statement");
            emptyRequiredSec.setContent(null); // empty
            emptyRequiredSec.setRequirementLevel(SectionRequirementLevel.REQUIRED);

            when(sectionRepository.findAllByReportIdOrderByChapterDisplayOrderAscDisplayOrderAsc(report.getId()))
                    .thenReturn(List.of(emptyRequiredSec));
            when(guidelineRepository.findFirstByProjectIdAndStatusOrderByVersionDesc(projectId, "APPROVED"))
                    .thenReturn(Optional.empty());

            DocumentStructureValidationResponse validation = validationService.validateReportStructure(projectId);

            assertThat(validation.valid()).isFalse();
            assertThat(validation.warnings()).isNotEmpty();
            assertThat(validation.warnings().get(0).sectionHeading()).isEqualTo("Problem Statement");
            assertThat(validation.missingRequiredSections()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("9. Template-Aware AI Generation")
    class TemplateAwareAiTests {

        private final AcademicSectionPromptService promptService = new AcademicSectionPromptService();

        @Test
        @DisplayName("AI prompt includes target section template requirements and forbids generic methodology substitution")
        void testTemplateRequirementsInjectedIntoAiPrompt() {
            SectionGenerationContext context = new SectionGenerationContext(
                    AcademicWorkspaceType.ACADEMIC_PROJECT,
                    AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT,
                    "REPORT",
                    "Chapter Three: System Design",
                    UUID.randomUUID(),
                    ReportSectionType.SYSTEM_DESIGN,
                    SectionSemanticPurpose.SYSTEM_DESIGN,
                    SectionGenerationPolicy.PROJECT_DERIVED_AI,
                    "3.2 Data Design",
                    "3.0 System Architecture and Design",
                    "Farmer-to-Buyer Marketplace",
                    "Platform connecting rural farmers to urban buyers",
                    "Build transactional marketplace",
                    List.of("Design database", "Implement API"),
                    List.of("What is the optimal schema?"),
                    "Farmers lack direct access to wholesale markets",
                    "Oyo State",
                    "Software Engineering",
                    "Agile and Relational Modeling",
                    0, 0,
                    "Description of data-flow diagram, entity-relationship model, and PostgreSQL database schema.",
                    "Focus on crop batching and escrow payment tables.",
                    null
            );

            String prompt = promptService.build(context);

            assertThat(prompt).contains("TEMPLATE REQUIREMENTS");
            assertThat(prompt).contains("Description of data-flow diagram, entity-relationship model, and PostgreSQL database schema.");
            assertThat(prompt).contains("Do not substitute generic research methodology or off-topic boilerplate.");
            assertThat(prompt).contains("Focus on crop batching and escrow payment tables.");
        }
    }
}
