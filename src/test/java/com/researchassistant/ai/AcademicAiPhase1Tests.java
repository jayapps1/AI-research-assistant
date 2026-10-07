package com.researchassistant.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.ai.exception.AiGenerationException;
import com.researchassistant.ai.orchestration.AiTaskRequest;
import com.researchassistant.ai.orchestration.AiTaskResult;
import com.researchassistant.ai.orchestration.AiTaskType;
import com.researchassistant.ai.provider.AiProviderType;
import com.researchassistant.ai.provider.openai.OpenAiGenerationProvider;
import com.researchassistant.ai.usage.AiProviderBudgetService;
import com.researchassistant.analysis.dto.AnalysisDtos.GeneratedDraftResponse;
import com.researchassistant.analysis.entity.ReportSectionType;
import com.researchassistant.analysis.entity.SectionGenerationPolicy;
import com.researchassistant.analysis.entity.SectionSemanticPurpose;
import com.researchassistant.analysis.exception.InsufficientProjectEvidenceException;
import com.researchassistant.analysis.repository.AnalysisResultRepository;
import com.researchassistant.analysis.repository.AnalysisRunRepository;
import com.researchassistant.analysis.repository.ResearchConclusionRepository;
import com.researchassistant.analysis.repository.ResearchFindingRepository;
import com.researchassistant.analysis.repository.ResearchRecommendationRepository;
import com.researchassistant.analysis.repository.ResearchReportCitationRepository;
import com.researchassistant.analysis.repository.ResearchReportSectionRepository;
import com.researchassistant.analysis.service.AcademicSectionGenerationService;
import com.researchassistant.analysis.service.AcademicSectionGenerationService.AcademicSectionGenerationContext;
import com.researchassistant.analysis.service.AcademicSectionPromptService;
import com.researchassistant.analysis.service.AcademicSectionPromptService.SectionGenerationContext;
import com.researchassistant.analysis.service.ReportMarkdownRenderer;
import com.researchassistant.analysis.service.ReportRichTextService;
import com.researchassistant.document.chunk.ChunkHygieneService;
import com.researchassistant.document.entity.ChunkSemanticType;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.literature.repository.LiteratureMatrixRepository;
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.project.entity.AcademicWorkspaceType;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.rag.citation.CitationVerificationService;
import com.researchassistant.rag.evidence.EvidenceBundle;
import com.researchassistant.rag.evidence.EvidenceBundleService;
import com.researchassistant.rag.evidence.EvidenceItem;
import com.researchassistant.rag.generation.GeneratedAnswerDraft;
import com.researchassistant.rag.generation.GeneratedCitation;
import com.researchassistant.rag.generation.GroundingPromptBuilder;
import com.researchassistant.rag.repository.RagQueryEvidenceRepository;
import com.researchassistant.rag.service.RagContextBudgetService;
import com.researchassistant.rag.service.RagQueryService;
import com.researchassistant.reference.service.ProjectReferenceRegistryService;
import com.researchassistant.security.audit.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcademicAiPhase1Tests {

    @Nested
    @DisplayName("A & B & C: Provider Authentication Failure & No Fallback Text")
    class ProviderAuthenticationAndNoFallbackTests {

        @Mock
        private ChatModel chatModel;

        @Mock
        private Environment environment;

        private OpenAiGenerationProvider provider;

        @BeforeEach
        void setUp() {
            AiProperties aiProps = new AiProperties(
                    new AiProperties.Generation(true, AiProviderType.OPENAI, "gpt-4o-mini", 0.2, 4096, Duration.ofSeconds(60)),
                    null, null, null, null, null
            );

            ObjectProvider<ChatModel> chatModelProvider = mock(ObjectProvider.class);
            when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);

            ObjectProvider<Environment> envProvider = mock(ObjectProvider.class);
            when(envProvider.getIfAvailable()).thenReturn(environment);

            GroundingPromptBuilder promptBuilder = new GroundingPromptBuilder();
            ObjectMapper objectMapper = new ObjectMapper();
            AiProviderBudgetService budgetService = mock(AiProviderBudgetService.class);
            RagContextBudgetService contextBudgetService = mock(RagContextBudgetService.class);

            provider = new OpenAiGenerationProvider(
                    aiProps,
                    chatModelProvider,
                    promptBuilder,
                    objectMapper,
                    budgetService,
                    contextBudgetService,
                    envProvider
            );
        }

        @Test
        @DisplayName("When API key is unauthenticated, provider fails clearly with AI_PROVIDER_AUTHENTICATION_FAILED and never generates fallback")
        void testAuthenticationFailureMappingAndNoFallback() {
            when(environment.getProperty("spring.ai.openai.api-key")).thenReturn(null);

            EvidenceBundle bundle = new EvidenceBundle(
                    "Literature Review",
                    null,
                    List.of(new EvidenceItem(
                            UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                            "DOC-001", "Digital Agriculture in Practice", UUID.randomUUID(), 1, 1, 1,
                            "Agricultural digitalization increases market transparency.",
                            0.92, 0.95, 0.94, null, 1
                    )),
                    1, 1, 1, 1, 1000, 100, 0,
                    "HYBRID", "gpt-4o-mini", null, OffsetDateTime.now()
            );

            assertThatThrownBy(() -> provider.generate(bundle))
                    .isInstanceOf(AiGenerationException.class)
                    .satisfies(ex -> {
                        AiGenerationException aiEx = (AiGenerationException) ex;
                        assertThat(aiEx.getCode()).isEqualTo("AI_PROVIDER_AUTHENTICATION_FAILED");
                    });
        }

        @Test
        @DisplayName("When ChatModel throws 401 Unauthorized at runtime, provider maps to AI_PROVIDER_AUTHENTICATION_FAILED")
        void testRuntimeChatModel401MapsToAuthenticationFailed() {
            when(environment.getProperty("spring.ai.openai.api-key")).thenReturn("sk-test-key");
            when(chatModel.call(any(Prompt.class)))
                    .thenThrow(new RuntimeException("401 Unauthorized: Invalid API key provided"));

            EvidenceBundle bundle = new EvidenceBundle(
                    "Literature Review",
                    null,
                    List.of(new EvidenceItem(
                            UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                            "DOC-001", "Digital Agriculture in Practice", UUID.randomUUID(), 1, 1, 1,
                            "Agricultural digitalization increases market transparency.",
                            0.92, 0.95, 0.94, null, 1
                    )),
                    1, 1, 1, 1, 1000, 100, 0,
                    "HYBRID", "gpt-4o-mini", null, OffsetDateTime.now()
            );

            assertThatThrownBy(() -> provider.generate(bundle))
                    .isInstanceOf(AiGenerationException.class)
                    .satisfies(ex -> {
                        AiGenerationException aiEx = (AiGenerationException) ex;
                        assertThat(aiEx.getCode()).isEqualTo("AI_PROVIDER_AUTHENTICATION_FAILED");
                    });
        }

        @Test
        @DisplayName("Structured task request returns structured AI_PROVIDER_AUTHENTICATION_FAILED failure")
        void testStructuredTaskAuthenticationFailure() {
            when(environment.getProperty("spring.ai.openai.api-key")).thenReturn(null);

            AiTaskRequest task = new AiTaskRequest(
                    AiTaskType.GENERAL_CONVERSATION,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Summarize findings",
                    "academic research context",
                    null,
                    null,
                    String.class,
                    false
            );

            AiTaskResult<String> result = provider.generate(task, String.class);
            assertThat(result.failureCode()).isEqualTo("AI_PROVIDER_AUTHENTICATION_FAILED");
            assertThat(result.result()).isNull();
        }
    }

    @Nested
    @DisplayName("E & J: Section-Specific Prompting & Professional Writing Quality")
    class SectionSpecificPromptTests {

        private final AcademicSectionPromptService promptService = new AcademicSectionPromptService();

        @Test
        @DisplayName("Background prompt moves from broad context toward problem and forbids canned heading")
        void testBackgroundPrompt() {
            SectionGenerationContext ctx = new SectionGenerationContext(
                    AcademicWorkspaceType.ACADEMIC_RESEARCH,
                    AcademicProjectType.RESEARCH_BASED_PROJECT,
                    "REPORT",
                    "Chapter 1: Introduction",
                    UUID.randomUUID(),
                    ReportSectionType.BACKGROUND,
                    SectionSemanticPurpose.BACKGROUND,
                    SectionGenerationPolicy.SOURCE_GROUNDED_AI,
                    "1.1 Background of the Study",
                    null,
                    "Digital Agriculture Adoption Among Smallholder Farmers",
                    "A study on barriers to adoption",
                    "To examine technology adoption factors",
                    List.of("Identify socio-economic factors", "Assess infrastructure barriers"),
                    List.of("What factors drive adoption?"),
                    "Smallholder farmers face low adoption rates.",
                    "Oyo State, Nigeria",
                    "Empirical Quantitative Survey",
                    "Survey of 300 farmers",
                    0, 0, null, null, null
            );

            String prompt = promptService.build(ctx);

            assertThat(prompt).contains("Digital Agriculture Adoption Among Smallholder Farmers");
            assertThat(prompt).contains("broad contextual setting and scholarly foundation toward the specific project and problem");
            assertThat(prompt).doesNotContain("Contextual Foundation and Domain Background\n");
        }

        @Test
        @DisplayName("Problem Statement prompt requires evidence and forbids writing another literature review")
        void testProblemStatementPrompt() {
            SectionGenerationContext ctx = new SectionGenerationContext(
                    AcademicWorkspaceType.ACADEMIC_RESEARCH,
                    AcademicProjectType.RESEARCH_BASED_PROJECT,
                    "REPORT",
                    "Chapter 1: Introduction",
                    UUID.randomUUID(),
                    ReportSectionType.PROBLEM_STATEMENT,
                    SectionSemanticPurpose.PROBLEM_STATEMENT,
                    SectionGenerationPolicy.SOURCE_GROUNDED_AI,
                    "1.2 Problem Statement",
                    null,
                    "Digital Agriculture Adoption",
                    "Project description",
                    "Examine barriers",
                    List.of("Obj 1"),
                    List.of("Q 1"),
                    "Farmers lack localized digital advisory systems.",
                    null, null, null, 0, 0, null, null, null
            );

            String prompt = promptService.build(ctx);

            assertThat(prompt).contains("actual problem context");
            assertThat(prompt).contains("Do not write another literature review");
            assertThat(prompt).contains("practical or research gap");
            assertThat(prompt).contains("Farmers lack localized digital advisory systems");
        }

        @Test
        @DisplayName("Literature Review prompt demands cross-source synthesis and forbids junk")
        void testLiteratureReviewPrompt() {
            SectionGenerationContext ctx = new SectionGenerationContext(
                    AcademicWorkspaceType.ACADEMIC_RESEARCH,
                    AcademicProjectType.RESEARCH_BASED_PROJECT,
                    "REPORT",
                    "Chapter 2: Literature Review",
                    UUID.randomUUID(),
                    ReportSectionType.LITERATURE_REVIEW,
                    SectionSemanticPurpose.LITERATURE_REVIEW,
                    SectionGenerationPolicy.SOURCE_GROUNDED_AI,
                    "2.1 Digital Platforms in Agricultural Extension",
                    null,
                    "Digital Agriculture Adoption",
                    "Project description",
                    "Examine barriers",
                    List.of("Obj 1"),
                    List.of("Q 1"),
                    null, null, null, null, 0, 0, null, null, null
            );

            String prompt = promptService.build(ctx);

            assertThat(prompt).contains("Synthesize across multiple sources into thematic academic narrative");
            assertThat(prompt).contains("compare findings, methods, agreements, disagreements, and gaps");
            assertThat(prompt).contains("Do not summarize sources one-by-one");
            assertThat(prompt).contains("Do not repeat the project title in every paragraph");
        }

        @Test
        @DisplayName("System Design prompt relies on actual project design without forced literature")
        void testSystemDesignPrompt() {
            SectionGenerationContext ctx = new SectionGenerationContext(
                    AcademicWorkspaceType.ACADEMIC_RESEARCH,
                    AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT,
                    "REPORT",
                    "Chapter 3: System Architecture",
                    UUID.randomUUID(),
                    ReportSectionType.SYSTEM_DESIGN,
                    SectionSemanticPurpose.SYSTEM_DESIGN,
                    SectionGenerationPolicy.PROJECT_DERIVED_AI,
                    "3.1 Architecture Overview",
                    null,
                    "AI Agritech System",
                    "Spring Boot and React platform",
                    "Build advisory system",
                    List.of("Develop frontend", "Develop backend"),
                    List.of("How to architect scalable backend?"),
                    null, null, null, null, 0, 0, null, null, null
            );

            String prompt = promptService.build(ctx);

            assertThat(prompt).contains("actual project requirements, system context, architectural design");
            assertThat(prompt).contains("Do not force literature synthesis");
        }

        @Test
        @DisplayName("Custom section uses actual heading, chapter, and instructions")
        void testCustomSectionPrompt() {
            SectionGenerationContext ctx = new SectionGenerationContext(
                    AcademicWorkspaceType.ACADEMIC_RESEARCH,
                    AcademicProjectType.GENERAL_ACADEMIC_PROJECT,
                    "REPORT",
                    "Chapter 4: Implementation",
                    UUID.randomUUID(),
                    ReportSectionType.CUSTOM,
                    SectionSemanticPurpose.CUSTOM,
                    SectionGenerationPolicy.CONTEXTUAL_AI,
                    "4.3 Smart Contract Security Considerations",
                    "4.0 Implementation Details",
                    "Blockchain Agri-Supply Chain",
                    "Supply chain tracking",
                    "Aim",
                    List.of(),
                    List.of(),
                    null, null, null, null, 0, 0, null,
                    "Focus specifically on reentrancy attack mitigations.",
                    null
            );

            String prompt = promptService.build(ctx);

            assertThat(prompt).contains("4.3 Smart Contract Security Considerations");
            assertThat(prompt).contains("Focus specifically on reentrancy attack mitigations.");
        }
    }

    @Nested
    @DisplayName("L: Section Evidence Policy & Gating")
    class EvidenceGatingTests {

        @Test
        @DisplayName("Testing section generation requires actual test execution records")
        void testTestingEvidenceGating() {
            AcademicSectionPromptService promptService = new AcademicSectionPromptService();
            RagQueryService ragQueryService = mock(RagQueryService.class);
            ProjectAuthorizationService authorizationService = mock(ProjectAuthorizationService.class);
            ResearchReportSectionRepository sectionRepository = mock(ResearchReportSectionRepository.class);
            AnalysisRunRepository runRepository = mock(AnalysisRunRepository.class);
            ResearchFindingRepository findingRepository = mock(ResearchFindingRepository.class);
            AnalysisResultRepository resultRepository = mock(AnalysisResultRepository.class);
            ResearchConclusionRepository conclusionRepository = mock(ResearchConclusionRepository.class);
            ResearchRecommendationRepository recommendationRepository = mock(ResearchRecommendationRepository.class);
            DocumentRepository documentRepository = mock(DocumentRepository.class);
            ResearchReportCitationRepository citationRepository = mock(ResearchReportCitationRepository.class);
            RagQueryEvidenceRepository ragEvidenceRepository = mock(RagQueryEvidenceRepository.class);
            ProjectReferenceRegistryService referenceRegistryService = mock(ProjectReferenceRegistryService.class);
            LiteratureMatrixRepository literatureMatrixRepository = mock(LiteratureMatrixRepository.class);
            ReportMarkdownRenderer markdownRenderer = mock(ReportMarkdownRenderer.class);
            ReportRichTextService richTextService = mock(ReportRichTextService.class);
            PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
            SecurityAuditService auditService = mock(SecurityAuditService.class);

            UUID projectId = UUID.randomUUID();
            when(runRepository.countByProjectId(projectId)).thenReturn(0L);

            AcademicSectionGenerationService service = new AcademicSectionGenerationService(
                    promptService, ragQueryService, authorizationService, sectionRepository,
                    runRepository, findingRepository, resultRepository, conclusionRepository,
                    recommendationRepository, documentRepository, citationRepository,
                    ragEvidenceRepository, referenceRegistryService, literatureMatrixRepository,
                    markdownRenderer, richTextService, transactionManager, auditService
            );

            AcademicSectionGenerationContext context = new AcademicSectionGenerationContext(
                    null,
                    AcademicWorkspaceType.ACADEMIC_RESEARCH,
                    projectId,
                    AcademicProjectType.RESEARCH_BASED_PROJECT,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    ReportSectionType.TESTING,
                    SectionSemanticPurpose.TESTING,
                    SectionGenerationPolicy.PROJECT_EVIDENCE_REQUIRED,
                    "5.1 System Testing",
                    null,
                    "Chapter 5",
                    "Project",
                    "Desc",
                    null,
                    List.of(),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    0,
                    0,
                    null,
                    "ALL_PROJECT_DOCUMENTS",
                    Set.of(),
                    null,
                    10,
                    "REPLACE"
            );

            assertThatThrownBy(() -> service.generate(context, null))
                    .isInstanceOf(InsufficientProjectEvidenceException.class)
                    .hasMessageContaining("Testing section generation requires actual test execution records");
        }
    }

    @Nested
    @DisplayName("G: Retrieval Quality & Chunk Hygiene")
    class ChunkHygieneTests {

        private final ChunkHygieneService hygieneService = new ChunkHygieneService();

        @Test
        @DisplayName("Classifies metadata, headers, references, and body chunks accurately")
        void testChunkClassification() {
            assertThat(hygieneService.classify("Available online at www.sciencedirect.com\nComputers and Electronics in Agriculture", 1, 1))
                    .isEqualTo(ChunkSemanticType.FOOTER_HEADER);

            assertThat(hygieneService.classify("Page 12 of 35", 12, 1))
                    .isEqualTo(ChunkSemanticType.FOOTER_HEADER);

            assertThat(hygieneService.classify("Article history:\nReceived 12 May 2023\nAccepted 4 August 2023\nKeywords: Agriculture, ICT", 1, 1))
                    .isEqualTo(ChunkSemanticType.METADATA);

            assertThat(hygieneService.classify("References\n[1] Adebayo, K. (2022). Digital extension systems. J. Agr. Sci, 14(2), 45-56.", 20, 1))
                    .isEqualTo(ChunkSemanticType.REFERENCES);

            assertThat(hygieneService.classify("Smallholder farmers who utilized mobile advisory platforms reported a 23% yield improvement compared to the control group.", 5, 2))
                    .isEqualTo(ChunkSemanticType.BODY);
        }

        @Test
        @DisplayName("Excludes non-substantive chunks from evidence")
        void testEvidenceSubstantiveCheck() {
            assertThat(hygieneService.isSubstantiveEvidence(ChunkSemanticType.FOOTER_HEADER)).isFalse();
            assertThat(hygieneService.isSubstantiveEvidence(ChunkSemanticType.METADATA)).isFalse();
            assertThat(hygieneService.isSubstantiveEvidence(ChunkSemanticType.REFERENCES)).isFalse();

            assertThat(hygieneService.isSubstantiveEvidence(ChunkSemanticType.BODY)).isTrue();
            assertThat(hygieneService.isSubstantiveEvidence(ChunkSemanticType.TABLE)).isTrue();
            assertThat(hygieneService.isSubstantiveEvidence(ChunkSemanticType.FIGURE_CAPTION)).isTrue();
        }

        @Test
        @DisplayName("Sanitizes junk artifacts while preserving substantive academic content")
        void testSanitizeEvidenceText() {
            String dirtyText = "Available online at www.sciencedirect.com\n"
                    + "Computers and Electronics in Agriculture 180 (2021) 105898\n"
                    + "https://doi.org/10.1016/j.compag.2020.105898\n"
                    + "0168-1699/© 2020 Elsevier B.V. All rights reserved.\n"
                    + "Precision agriculture tools significantly reduce fertilizer application variability across diverse soil topologies.";

            String cleaned = hygieneService.sanitizeEvidenceText(dirtyText);

            assertThat(cleaned).doesNotContain("Available online");
            assertThat(cleaned).doesNotContain("doi.org");
            assertThat(cleaned).doesNotContain("All rights reserved");
            assertThat(cleaned).contains("Precision agriculture tools significantly reduce fertilizer application variability");
        }
    }

    @Nested
    @DisplayName("H: Display Title Kept Separate From Scholarly Title")
    class FilenameHygieneTests {

        private final EvidenceBundleService bundleService = new EvidenceBundleService(null, null, null, null);

        @Test
        @DisplayName("Preserves logical display titles even when they look filename-derived")
        void testCleanDocumentTitle() {
            assertThat(bundleService.cleanDocumentTitle("Exploring_the_impact_of_agricultural_digitalizatio.pdf"))
                    .isEqualTo("Exploring_the_impact_of_agricultural_digitalizatio.pdf");
            assertThat(bundleService.cleanDocumentTitle("impact_of_ict_in_agriculture.docx"))
                    .isEqualTo("impact_of_ict_in_agriculture.docx");
            assertThat(bundleService.cleanDocumentTitle("digital_farming_2023.PDF"))
                    .isEqualTo("digital_farming_2023.PDF");
            assertThat(bundleService.cleanDocumentTitle("Exploring_the_impact_of_agricultural_digitalizatio"))
                    .isEqualTo("Exploring_the_impact_of_agricultural_digitalizatio");

            // Real titles should be retained
            assertThat(bundleService.cleanDocumentTitle("Exploring the Impact of Agricultural Digitalization on Smallholder Income"))
                    .isEqualTo("Exploring the Impact of Agricultural Digitalization on Smallholder Income");
        }
    }

    @Nested
    @DisplayName("I & F: Citation Verification and Multi-Paper Synthesis Pipeline")
    class CitationAndSynthesisPipelineTests {

        private final CitationVerificationService verificationService = new CitationVerificationService();

        @Test
        @DisplayName("Verifies valid citations mapped to evidence items and rejects fabricated ones")
        void testCitationVerification() {
            GeneratedAnswerDraft draft = new GeneratedAnswerDraft(
                    "Agricultural digitalization improves market access [E1], while financial constraints hinder adoption [E2].",
                    List.of(
                            new GeneratedCitation(1, "[E1]"),
                            new GeneratedCitation(2, "[E2]")
                    ),
                    "openai", "gpt-4o-mini", 100, 50, 200L, "stop"
            );

            com.researchassistant.rag.entity.RagQueryEvidence ev1 = new com.researchassistant.rag.entity.RagQueryEvidence();
            ev1.setEvidenceOrdinal(1);
            ev1.setDocumentCode("DOC-001");

            com.researchassistant.rag.entity.RagQueryEvidence ev2 = new com.researchassistant.rag.entity.RagQueryEvidence();
            ev2.setEvidenceOrdinal(2);
            ev2.setDocumentCode("DOC-002");

            var result = verificationService.verify(draft, List.of(ev1, ev2));
            assertThat(result.verified()).isTrue();
            assertThat(result.errors()).isEmpty();

            // Fabricated citation test
            GeneratedAnswerDraft hallucinatedDraft = new GeneratedAnswerDraft(
                    "Digital tools solve all productivity issues [E99].",
                    List.of(new GeneratedCitation(99, "[E99]")),
                    "openai", "gpt-4o-mini", 100, 50, 200L, "stop"
            );

            var failureResult = verificationService.verify(hallucinatedDraft, List.of(ev1, ev2));
            assertThat(failureResult.verified()).isFalse();
            assertThat(failureResult.errors()).contains("Citation references evidence that was not supplied.");
            assertThat(failureResult.invalidCitationMarkers()).contains("[E99]");
            assertThat(failureResult.missingCitationOrdinals()).contains(99);
        }
    }
}
