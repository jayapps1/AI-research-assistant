package com.researchassistant.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.repository.AcademicDocumentGuidelineRepository;
import static com.researchassistant.template.dto.AcademicTemplateDtos.NOT_SPECIFIED;
import com.researchassistant.analysis.repository.ResearchReportChapterRepository;
import com.researchassistant.analysis.repository.ResearchReportRepository;
import com.researchassistant.analysis.repository.ResearchReportSectionRepository;
import com.researchassistant.analysis.repository.ResearchReportTemplateRepository;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.document.dto.DocumentResponse;
import com.researchassistant.document.entity.AcademicFileRole;
import com.researchassistant.document.entity.Document;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.document.service.DocumentService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.template.dto.AcademicTemplateDtos.*;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class AcademicTemplateService {

    private static final Logger log = LoggerFactory.getLogger(AcademicTemplateService.class);

    private final AcademicDocumentGuidelineRepository guidelineRepository;
    private final AcademicTemplateExtractionService extractionService;
    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ResearchProjectRepository projectRepository;
    private final ResearchReportRepository reportRepository;
    private final ResearchReportChapterRepository chapterRepository;
    private final ResearchReportSectionRepository sectionRepository;
    private final ResearchReportTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public AcademicTemplateService(
            AcademicDocumentGuidelineRepository guidelineRepository,
            AcademicTemplateExtractionService extractionService,
            DocumentService documentService,
            DocumentRepository documentRepository,
            WorkspaceRepository workspaceRepository,
            ResearchProjectRepository projectRepository,
            ResearchReportRepository reportRepository,
            ResearchReportChapterRepository chapterRepository,
            ResearchReportSectionRepository sectionRepository,
            ResearchReportTemplateRepository templateRepository,
            ObjectMapper objectMapper
    ) {
        this.guidelineRepository = guidelineRepository;
        this.extractionService = extractionService;
        this.documentService = documentService;
        this.documentRepository = documentRepository;
        this.workspaceRepository = workspaceRepository;
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.chapterRepository = chapterRepository;
        this.sectionRepository = sectionRepository;
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Uploads guideline document with TEMPLATE_GUIDELINE role and extracts structure.
     */
    @Transactional
    public AcademicDocumentGuidelineResponse uploadAndAnalyzeGuideline(
            UUID workspaceId,
            UUID projectId,
            MultipartFile file,
            User user
    ) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));

        ResearchProject project = null;
        if (projectId != null) {
            project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
        }

        // 1. Upload document with TEMPLATE_GUIDELINE role if projectId is present
        Document doc = null;
        if (project != null) {
            DocumentResponse docResponse = documentService.uploadDocument(
                    projectId, user, file, file.getOriginalFilename(), AcademicFileRole.TEMPLATE_GUIDELINE
            );
            doc = documentRepository.findById(docResponse.id()).orElse(null);
        }

        // 2. Extract structure
        ExtractedAcademicTemplate extracted;
        try (InputStream is = file.getInputStream()) {
            extracted = extractionService.extractFromStream(is, file.getOriginalFilename(), file.getContentType());
        } catch (Exception e) {
            log.error("Failed to analyze uploaded guideline {}: {}", file.getOriginalFilename(), e.getMessage());
            extracted = extractionService.extractFromText("", file.getOriginalFilename());
        }

        // 3. Determine version
        int nextVersion = 1;
        if (project != null) {
            nextVersion = guidelineRepository.findTopByProjectIdOrderByVersionDesc(projectId)
                    .map(g -> g.getVersion() + 1)
                    .orElse(1);
        }

        AcademicDocumentGuideline guideline = new AcademicDocumentGuideline();
        guideline.setWorkspace(workspace);
        guideline.setProject(project);
        guideline.setDocument(doc);
        guideline.setOriginalFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "guideline.pdf");
        guideline.setSource("UPLOADED");
        guideline.setVersion(nextVersion);
        guideline.setStatus("EXTRACTED");
        guideline.setInstitution(extracted.institution());
        guideline.setDepartment(extracted.department());
        guideline.setProgramme(extracted.programme());
        guideline.setDocumentType(extracted.documentType());
        guideline.setCitationStyle(extracted.citationStyle());
        guideline.setRawExtractionJson(toJson(extracted));
        guideline.setApprovedStructureJson(toJson(extracted));
        guideline.setFormattingRulesJson(toJson(extracted.formattingRules()));
        guideline.setUncertainItemsJson(toJson(extracted.uncertainItems()));
        guideline.setUploadedAt(OffsetDateTime.now());

        AcademicDocumentGuideline saved = guidelineRepository.save(guideline);
        return mapToResponse(saved, extracted);
    }

    /**
     * Retrieves guideline by ID.
     */
    @Transactional(readOnly = true)
    public AcademicDocumentGuidelineResponse getGuideline(UUID guidelineId) {
        AcademicDocumentGuideline guideline = guidelineRepository.findById(guidelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic guideline not found."));
        ExtractedAcademicTemplate template = fromJson(
                guideline.getApprovedStructureJson() != null ? guideline.getApprovedStructureJson() : guideline.getRawExtractionJson()
        );
        return mapToResponse(guideline, template);
    }

    /**
     * User review & edit of extracted template before approval.
     */
    @Transactional
    public AcademicDocumentGuidelineResponse updateGuideline(
            UUID guidelineId,
            UpdateAcademicGuidelineRequest request,
            User user
    ) {
        AcademicDocumentGuideline guideline = guidelineRepository.findById(guidelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic guideline not found."));

        if (request.institution() != null) guideline.setInstitution(request.institution());
        if (request.department() != null) guideline.setDepartment(request.department());
        if (request.programme() != null) guideline.setProgramme(request.programme());
        if (request.documentType() != null) guideline.setDocumentType(request.documentType());
        if (request.citationStyle() != null) guideline.setCitationStyle(request.citationStyle());

        ExtractedAcademicTemplate updatedStructure = request.templateStructure();
        if (updatedStructure != null) {
            guideline.setApprovedStructureJson(toJson(updatedStructure));
            if (updatedStructure.formattingRules() != null) {
                guideline.setFormattingRulesJson(toJson(updatedStructure.formattingRules()));
            }
            if (updatedStructure.uncertainItems() != null) {
                guideline.setUncertainItemsJson(toJson(updatedStructure.uncertainItems()));
            }
        }
        guideline.setStatus("EDITED");
        AcademicDocumentGuideline saved = guidelineRepository.save(guideline);
        return mapToResponse(saved, updatedStructure);
    }

    /**
     * Approves guideline and applies the structure to the project report.
     */
    @Transactional
    public AcademicDocumentGuidelineResponse approveAndApplyGuideline(
            UUID guidelineId,
            UUID targetProjectId,
            ApproveGuidelineRequest request,
            User user
    ) {
        AcademicDocumentGuideline guideline = guidelineRepository.findById(guidelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic guideline not found."));

        UUID projectId = targetProjectId != null ? targetProjectId : (guideline.getProject() != null ? guideline.getProject().getId() : null);
        if (projectId == null) {
            throw new IllegalArgumentException("Target project ID is required to apply template guideline.");
        }

        ResearchProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        ExtractedAcademicTemplate template = fromJson(
                guideline.getApprovedStructureJson() != null ? guideline.getApprovedStructureJson() : guideline.getRawExtractionJson()
        );

        guideline.setStatus("APPROVED");
        guideline.setApprovedAt(OffsetDateTime.now());
        guideline.setApprovedByUser(user);
        guideline.setProject(project);

        if (request.applyToProject()) {
            applyTemplateToProjectReport(project, template, request.preserveExistingContent(), user);
        }

        AcademicDocumentGuideline saved = guidelineRepository.save(guideline);
        return mapToResponse(saved, template);
    }

    /**
     * Converts approved template structure into Report Chapters, Sections, Subsections, and
     * persists them with requirement levels and template guidance.
     */
    @Transactional
    public void applyTemplateToProjectReport(
            ResearchProject project,
            ExtractedAcademicTemplate template,
            boolean preserveExistingContent,
            User user
    ) {
        ResearchReport report = reportRepository.findByProjectId(project.getId())
                .orElseGet(() -> {
                    ResearchReport newReport = new ResearchReport();
                    newReport.setProject(project);
                    newReport.setTitle(project.getTitle() + " - Report");
                    newReport.setStatus(ResearchReportStatus.DRAFT);
                    newReport.setCreatedBy(user);
                    return reportRepository.save(newReport);
                });

        // Map existing content by heading and semantic purpose to preserve user work
        Map<String, String> existingContentByHeading = new HashMap<>();
        Map<SectionSemanticPurpose, String> existingContentByPurpose = new HashMap<>();

        if (preserveExistingContent) {
            List<ResearchReportSection> existingSections = sectionRepository.findAllByReportIdOrderByChapterDisplayOrderAscDisplayOrderAsc(report.getId());
            for (ResearchReportSection sec : existingSections) {
                if (sec.getContent() != null && !sec.getContent().isBlank()) {
                    existingContentByHeading.put(sec.getHeading().toLowerCase(Locale.ROOT).trim(), sec.getContent());
                    if (sec.getSemanticPurpose() != null) {
                        existingContentByPurpose.put(sec.getSemanticPurpose(), sec.getContent());
                    }
                }
            }
        }

        // Delete existing chapters and sections safely
        List<ResearchReportChapter> existingChapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId());
        for (ResearchReportChapter ch : existingChapters) {
            sectionRepository.deleteAllByChapterId(ch.getId());
        }
        chapterRepository.deleteAllByReportId(report.getId());

        int chapterOrder = 1;

        // 1. Front Matter Chapter (PRELIMINARY)
        if (!template.frontMatter().isEmpty()) {
            ResearchReportChapter prelimChapter = new ResearchReportChapter();
            prelimChapter.setReport(report);
            prelimChapter.setType(ReportChapterType.PRELIMINARY);
            prelimChapter.setTitle("PRELIMINARY PAGES");
            prelimChapter.setChapterNumber(null);
            prelimChapter.setDisplayOrder(chapterOrder++);
            prelimChapter.setRequired(true);
            prelimChapter.setSystemDefined(true);
            ResearchReportChapter savedPrelim = chapterRepository.save(prelimChapter);

            int secOrder = 1;
            for (TemplateSectionDefinitionDto fm : template.frontMatter()) {
                ResearchReportSection sec = new ResearchReportSection();
                sec.setChapter(savedPrelim);
                sec.setSectionNumber(fm.sectionNumber());
                sec.setHeading(fm.heading());
                sec.setType(ReportSectionType.CUSTOM);
                sec.setSemanticPurpose(fm.semanticPurpose());
                sec.setGenerationPolicy(fm.generationPolicy());
                sec.setRequirementLevel(fm.requirementLevel());
                sec.setTemplateGuidance(fm.description());
                sec.setDisplayOrder(secOrder++);
                sec.setRequired(fm.requirementLevel() == SectionRequirementLevel.REQUIRED);
                sec.setSystemDefined(true);
                sec.setAiEnabled(fm.generationPolicy() != SectionGenerationPolicy.DETERMINISTIC);
                sec.setCreatedBy(user);
                sec.setUpdatedBy(user);
                sectionRepository.save(sec);
            }
        }

        // 2. Main Chapters & Sections
        for (TemplateChapterDefinitionDto chDto : template.chapters()) {
            ResearchReportChapter chapter = new ResearchReportChapter();
            chapter.setReport(report);
            chapter.setType(chDto.type());
            chapter.setTitle(chDto.title());
            chapter.setChapterNumber(chDto.chapterNumber());
            chapter.setDisplayOrder(chapterOrder++);
            chapter.setRequired(chDto.required());
            chapter.setSystemDefined(true);
            ResearchReportChapter savedChapter = chapterRepository.save(chapter);

            int secOrder = 1;
            for (TemplateSectionDefinitionDto secDto : chDto.sections()) {
                persistSectionHierarchy(savedChapter, null, secDto, secOrder++, existingContentByHeading, existingContentByPurpose, user);
            }
        }

        // 3. Appendices Chapter
        if (!template.appendices().isEmpty()) {
            ResearchReportChapter appChapter = new ResearchReportChapter();
            appChapter.setReport(report);
            appChapter.setType(ReportChapterType.APPENDICES);
            appChapter.setTitle("APPENDICES");
            appChapter.setChapterNumber(null);
            appChapter.setDisplayOrder(chapterOrder++);
            appChapter.setRequired(false);
            appChapter.setSystemDefined(true);
            ResearchReportChapter savedApp = chapterRepository.save(appChapter);

            int secOrder = 1;
            for (TemplateSectionDefinitionDto appSec : template.appendices()) {
                persistSectionHierarchy(savedApp, null, appSec, secOrder++, existingContentByHeading, existingContentByPurpose, user);
            }
        }

        // Update Project citation style if detected
        if (template.citationStyle() != null && !template.citationStyle().equals(NOT_SPECIFIED)) {
            try {
                CitationStyle style = CitationStyle.valueOf(template.citationStyle().toUpperCase(Locale.ROOT));
                project.setCitationStyle(style);
            } catch (Exception ignored) {}
        }

        projectRepository.save(project);
        log.info("Applied approved academic guideline structure to project: {} (reportId={})", project.getTitle(), report.getId());
    }

    private void persistSectionHierarchy(
            ResearchReportChapter chapter,
            ResearchReportSection parent,
            TemplateSectionDefinitionDto secDto,
            int displayOrder,
            Map<String, String> existingContentByHeading,
            Map<SectionSemanticPurpose, String> existingContentByPurpose,
            User user
    ) {
        ResearchReportSection sec = new ResearchReportSection();
        sec.setChapter(chapter);
        sec.setParentSection(parent);
        sec.setSectionNumber(secDto.sectionNumber());
        sec.setHeading(secDto.heading());
        sec.setType(ReportSectionType.CUSTOM);
        sec.setSemanticPurpose(secDto.semanticPurpose());
        sec.setGenerationPolicy(secDto.generationPolicy());
        sec.setRequirementLevel(secDto.requirementLevel());
        sec.setTemplateGuidance(secDto.description());
        sec.setDisplayOrder(displayOrder);
        sec.setRequired(secDto.requirementLevel() == SectionRequirementLevel.REQUIRED);
        sec.setSystemDefined(true);
        sec.setAiEnabled(secDto.generationPolicy() != SectionGenerationPolicy.DETERMINISTIC);
        sec.setCreatedBy(user);
        sec.setUpdatedBy(user);

        // Preserve previous user writing if matching heading or semantic purpose found
        String preserved = existingContentByHeading.get(secDto.heading().toLowerCase(Locale.ROOT).trim());
        if (preserved == null && secDto.semanticPurpose() != null) {
            preserved = existingContentByPurpose.get(secDto.semanticPurpose());
        }
        if (preserved != null && !preserved.isBlank()) {
            sec.setContent(preserved);
            sec.setStatus(ReportSectionStatus.DRAFT);
        }

        ResearchReportSection savedSec = sectionRepository.save(sec);

        // Subsections
        int subOrder = 1;
        for (TemplateSectionDefinitionDto subDto : secDto.subsections()) {
            persistSectionHierarchy(chapter, savedSec, subDto, subOrder++, existingContentByHeading, existingContentByPurpose, user);
        }
    }

    @Transactional(readOnly = true)
    public List<AcademicDocumentGuidelineResponse> listGuidelines(UUID workspaceId, UUID projectId) {
        List<AcademicDocumentGuideline> list = projectId != null
                ? guidelineRepository.findAllByProjectIdOrderByUploadedAtDesc(projectId)
                : guidelineRepository.findAllByWorkspaceIdOrderByUploadedAtDesc(workspaceId);

        return list.stream()
                .map(g -> mapToResponse(g, fromJson(g.getApprovedStructureJson() != null ? g.getApprovedStructureJson() : g.getRawExtractionJson())))
                .toList();
    }

    private AcademicDocumentGuidelineResponse mapToResponse(AcademicDocumentGuideline g, ExtractedAcademicTemplate tpl) {
        return new AcademicDocumentGuidelineResponse(
                g.getId(),
                g.getWorkspace().getId(),
                g.getProject() != null ? g.getProject().getId() : null,
                g.getDocument() != null ? g.getDocument().getId() : null,
                g.getOriginalFileName(),
                g.getSource(),
                g.getVersion(),
                g.getStatus(),
                g.getInstitution(),
                g.getDepartment(),
                g.getProgramme(),
                g.getDocumentType(),
                g.getCitationStyle(),
                tpl,
                g.getUploadedAt().toString(),
                g.getApprovedAt() != null ? g.getApprovedAt().toString() : null,
                g.getApprovedByUser() != null ? g.getApprovedByUser().getFullName() : null
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private ExtractedAcademicTemplate fromJson(String json) {
        if (json == null || json.isBlank()) {
            return extractionService.extractFromText("", "guideline");
        }
        try {
            return objectMapper.readValue(json, ExtractedAcademicTemplate.class);
        } catch (Exception e) {
            return extractionService.extractFromText("", "guideline");
        }
    }
}
