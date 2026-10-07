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
import com.researchassistant.common.storage.ObjectStorageService;
import com.researchassistant.common.storage.StorageObjectCategory;
import com.researchassistant.common.storage.StorageObjectEntity;
import com.researchassistant.common.storage.StorageObjectMetadataService;
import com.researchassistant.common.storage.StoredObject;
import com.researchassistant.document.dto.DocumentResponse;
import com.researchassistant.document.entity.AcademicFileRole;
import com.researchassistant.document.entity.Document;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.document.service.DocumentService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.template.dto.AcademicTemplateDtos.*;
import com.researchassistant.template.exception.TemplateUploadException;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import com.researchassistant.workspace.service.WorkspaceAuthorizationService;
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
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final StorageObjectMetadataService storageObjectMetadataService;
    private final ObjectStorageService objectStorageService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
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
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ProjectAuthorizationService projectAuthorizationService,
            StorageObjectMetadataService storageObjectMetadataService,
            ObjectStorageService objectStorageService,
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
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.storageObjectMetadataService = storageObjectMetadataService;
        this.objectStorageService = objectStorageService;
        this.objectMapper = objectMapper;
    }

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
        this(guidelineRepository, extractionService, documentService, documentRepository, workspaceRepository,
                projectRepository, reportRepository, chapterRepository, sectionRepository, templateRepository,
                null, null, null, null, objectMapper);
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
        validateTemplateFile(file);
        Workspace workspace = workspaceAuthorizationService.requireActiveMembership(workspaceId, user).getWorkspace();

        ResearchProject project = null;
        if (projectId != null) {
            project = projectAuthorizationService.requireProjectEditor(projectId, user).project();
            if (!project.getWorkspace().getId().equals(workspaceId)) {
                throw new TemplateUploadException("TEMPLATE_ACCESS_DENIED", "Template upload workspace does not match the target project.");
            }
        }

        UUID guidelineId = UUID.randomUUID();
        byte[] bytes = readBytes(file);
        String originalName = safeFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "guideline");
        String mimeType = normalizeTemplateMime(file.getContentType(), originalName);
        String storageKey = "workspaces/%s/template-guidelines/%s/%s".formatted(workspaceId, guidelineId, UUID.randomUUID() + extension(originalName));
        StorageObjectEntity storageObject = storeStagedTemplate(user, workspaceId, projectId, storageKey, originalName, mimeType, bytes);

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
        String status = "EXTRACTED";
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            extracted = extractionService.extractFromStream(is, file.getOriginalFilename(), file.getContentType());
        } catch (Exception e) {
            log.warn("Template guideline uploaded but analysis failed for {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            extracted = extractionService.extractFromText("", file.getOriginalFilename());
            status = "TEMPLATE_PROCESSING_FAILED";
        }

        // 3. Determine version
        int nextVersion = 1;
        if (project != null) {
            nextVersion = guidelineRepository.findTopByProjectIdOrderByVersionDesc(projectId)
                    .map(g -> g.getVersion() + 1)
                    .orElse(1);
        }

        AcademicDocumentGuideline guideline = new AcademicDocumentGuideline();
        guideline.setId(guidelineId);
        guideline.setWorkspace(workspace);
        guideline.setProject(project);
        guideline.setDocument(doc);
        guideline.setStorageObject(storageObject);
        guideline.setOriginalFileName(originalName);
        guideline.setSource("UPLOADED");
        guideline.setVersion(nextVersion);
        guideline.setStatus(status);
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

    @Transactional(readOnly = true)
    public AcademicDocumentGuidelineResponse getGuideline(UUID guidelineId, User user) {
        AcademicDocumentGuideline guideline = guidelineRepository.findById(guidelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic guideline not found."));
        if (workspaceAuthorizationService != null) {
            workspaceAuthorizationService.requireActiveMembership(guideline.getWorkspace().getId(), user);
        }
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
        if (workspaceAuthorizationService != null) {
            workspaceAuthorizationService.requireActiveMembership(guideline.getWorkspace().getId(), user);
        }

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
        if (workspaceAuthorizationService != null) {
            workspaceAuthorizationService.requireActiveMembership(guideline.getWorkspace().getId(), user);
        }

        UUID projectId = targetProjectId != null ? targetProjectId : request.targetProjectId() != null ? request.targetProjectId() : (guideline.getProject() != null ? guideline.getProject().getId() : null);
        if (projectId == null) {
            throw new TemplateUploadException("TEMPLATE_UPLOAD_EXPIRED", "Target project ID is required to apply template guideline.");
        }

        ResearchProject project = projectAuthorizationService.requireProjectEditor(projectId, user).project();
        if (!project.getWorkspace().getId().equals(guideline.getWorkspace().getId())) {
            throw new TemplateUploadException("TEMPLATE_ACCESS_DENIED", "Template guideline does not belong to the target workspace.");
        }

        ExtractedAcademicTemplate template = fromJson(
                guideline.getApprovedStructureJson() != null ? guideline.getApprovedStructureJson() : guideline.getRawExtractionJson()
        );

        guideline.setStatus("APPROVED");
        guideline.setApprovedAt(OffsetDateTime.now());
        guideline.setApprovedByUser(user);
        guideline.setProject(project);
        if (guideline.getStorageObject() != null) {
            guideline.getStorageObject().setProjectId(project.getId());
        }

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
            ChapterTitleParts titleParts = parseChapterTitle(chDto.title());
            chapter.setTitle(titleParts.title());
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
        report.setRevisionNumber(report.getRevisionNumber() + 1);
        log.info("Applied approved academic guideline structure to project: {} (reportId={})", project.getTitle(), report.getId());
    }

    private ChapterTitleParts parseChapterTitle(String rawTitle) {
        String title = rawTitle == null ? "" : rawTitle.trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)^\\s*chapter\\s+(\\d+|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|[ivxlcdm]+)(?:(?:\\s*[:\\-.\\u2013\\u2014]\\s*|\\s+)(.+))?$")
                .matcher(title);
        if (!matcher.matches()) {
            return new ChapterTitleParts(title, null);
        }
        String cleanedTitle = (matcher.group(2) == null || matcher.group(2).isBlank()) ? title : matcher.group(2).trim();
        return new ChapterTitleParts(cleanedTitle, null);
    }

    private record ChapterTitleParts(String title, Integer chapterNumber) {}

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

    @Transactional(readOnly = true)
    public List<AcademicDocumentGuidelineResponse> listGuidelines(UUID workspaceId, UUID projectId, User user) {
        if (workspaceId == null && projectId == null) {
            return List.of();
        }
        if (projectId != null) {
            ResearchProject project = projectAuthorizationService.requireProjectViewer(projectId, user).project();
            workspaceId = project.getWorkspace().getId();
        }
        workspaceAuthorizationService.requireActiveMembership(workspaceId, user);
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

    private void validateTemplateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new TemplateUploadException("TEMPLATE_FILE_REQUIRED", "Template guideline file is required.");
        }
        if (file.getSize() > 50L * 1024L * 1024L) {
            throw new TemplateUploadException("TEMPLATE_FILE_TOO_LARGE", "Template guideline file must be 50 MB or smaller.");
        }
        normalizeTemplateMime(file.getContentType(), file.getOriginalFilename());
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new TemplateUploadException("TEMPLATE_UPLOAD_FAILED", "Template guideline could not be read.", e);
        }
    }

    private StorageObjectEntity storeStagedTemplate(User user, UUID workspaceId, UUID projectId, String storageKey,
            String originalFilename, String mimeType, byte[] bytes) {
        StorageObjectEntity pending = storageObjectMetadataService.createPending(
                user.getId(), workspaceId, projectId, StorageObjectCategory.TEMPLATE_GUIDELINE,
                storageKey, originalFilename, originalFilename, mimeType, bytes.length, null);
        try {
            StoredObject stored = objectStorageService.store(storageKey, new java.io.ByteArrayInputStream(bytes), mimeType);
            return storageObjectMetadataService.markAvailable(pending.getId(), stored);
        } catch (RuntimeException e) {
            storageObjectMetadataService.markFailed(pending.getId(), "TEMPLATE_STORAGE_FAILED", e.getMessage());
            throw new TemplateUploadException("TEMPLATE_STORAGE_FAILED", "Template guideline could not be stored.", e);
        }
    }

    private String normalizeTemplateMime(String mimeType, String filename) {
        String normalized = mimeType == null ? "" : mimeType.trim().toLowerCase(Locale.ROOT);
        String lowerName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if ("application/pdf".equals(normalized) || lowerName.endsWith(".pdf")) {
            return "application/pdf";
        }
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(normalized) || lowerName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        throw new TemplateUploadException("TEMPLATE_FILE_TYPE_UNSUPPORTED", "Template guideline must be a PDF or DOCX file.");
    }

    private String extension(String filename) {
        String name = filename == null ? "" : filename;
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            String ext = name.substring(dot).toLowerCase(Locale.ROOT);
            if (ext.matches("\\.[a-z0-9]{1,10}")) {
                return ext;
            }
        }
        return "";
    }

    private String safeFilename(String filename) {
        String name = filename == null ? "guideline" : filename.trim();
        name = name.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_");
        return name.isBlank() ? "guideline" : name;
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
