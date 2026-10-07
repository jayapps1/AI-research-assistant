package com.researchassistant.evidence.service;

import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.analysis.repository.ResearchReportRepository;
import com.researchassistant.analysis.repository.ResearchReportSectionRepository;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.common.storage.*;
import com.researchassistant.evidence.dto.ProjectEvidenceDtos.*;
import com.researchassistant.evidence.entity.EvidenceAnalysisStatus;
import com.researchassistant.evidence.entity.EvidenceType;
import com.researchassistant.evidence.entity.ProjectEvidence;
import com.researchassistant.evidence.repository.ProjectEvidenceRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.project.service.ProjectAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ProjectEvidenceService {

    private static final long MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024; // 25 MB
    private static final Set<String> ALLOWED_IMAGE_MIMES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    );

    private final ProjectEvidenceRepository evidenceRepository;
    private final ResearchProjectRepository projectRepository;
    private final ResearchReportRepository reportRepository;
    private final ResearchReportSectionRepository sectionRepository;
    private final ObjectStorageService objectStorageService;
    private final StorageObjectMetadataService storageObjectMetadataService;
    private final ProjectEvidenceNumberingService numberingService;
    private final ProjectAuthorizationService authorizationService;

    public ProjectEvidenceService(
            ProjectEvidenceRepository evidenceRepository,
            ResearchProjectRepository projectRepository,
            ResearchReportRepository reportRepository,
            ResearchReportSectionRepository sectionRepository,
            ObjectStorageService objectStorageService,
            StorageObjectMetadataService storageObjectMetadataService,
            ProjectEvidenceNumberingService numberingService,
            ProjectAuthorizationService authorizationService
    ) {
        this.evidenceRepository = evidenceRepository;
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.sectionRepository = sectionRepository;
        this.objectStorageService = objectStorageService;
        this.storageObjectMetadataService = storageObjectMetadataService;
        this.numberingService = numberingService;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public ProjectEvidenceResponse uploadEvidence(
            UUID projectId,
            MultipartFile file,
            EvidenceType evidenceType,
            String caption,
            String description,
            UUID sectionId,
            String altText,
            User user
    ) {
        authorizationService.requireProjectMember(projectId, user);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file cannot be empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 25 MB limit.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_IMAGE_MIMES.contains(mimeType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Invalid file type: " + mimeType + ". Supported types: PNG, JPEG, WEBP, GIF, SVG.");
        }

        ResearchProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        ResearchReport report = reportRepository.findByProjectId(projectId).orElse(null);

        ResearchReportSection section = null;
        if (sectionId != null) {
            section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Report section not found: " + sectionId));
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file: " + e.getMessage());
        }

        UUID evidenceId = UUID.randomUUID();
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "evidence.png";
        String storageKey = "projects/" + projectId + "/evidence/" + evidenceId + "/" + originalFilename;

        StorageObjectEntity pendingObject = storageObjectMetadataService.createPending(
                user.getId(),
                project.getWorkspace().getId(),
                project.getId(),
                StorageObjectCategory.IMAGE,
                storageKey,
                originalFilename,
                originalFilename,
                mimeType,
                bytes.length,
                null
        );

        StoredObject stored;
        StorageObjectEntity availableObject;
        try {
            stored = objectStorageService.store(storageKey, new ByteArrayInputStream(bytes), mimeType);
            availableObject = storageObjectMetadataService.markAvailable(pendingObject.getId(), stored);
        } catch (RuntimeException ex) {
            storageObjectMetadataService.markFailed(pendingObject.getId(), "UPLOAD_FAILED", ex.getMessage());
            throw ex;
        }

        int nextOrder = (int) evidenceRepository.countByProjectId(projectId) + 1;

        ProjectEvidence evidence = new ProjectEvidence();
        evidence.setId(evidenceId);
        evidence.setProject(project);
        evidence.setWorkspace(project.getWorkspace());
        evidence.setReport(report);
        evidence.setSection(section);
        evidence.setStorageObject(availableObject);
        evidence.setStorageKey(storageKey);
        evidence.setOriginalFilename(originalFilename);
        evidence.setMimeType(mimeType);
        evidence.setFileSizeBytes(stored.sizeBytes());
        evidence.setEvidenceType(evidenceType != null ? evidenceType : EvidenceType.SCREENSHOT);
        evidence.setCaption(caption != null ? caption.trim() : null);
        evidence.setDescription(description != null ? description.trim() : null);
        evidence.setAltText(altText != null ? altText.trim() : null);
        evidence.setDisplayOrder(nextOrder);
        evidence.setCreatedBy(user);
        evidence.setAiAnalysisStatus(EvidenceAnalysisStatus.NOT_ANALYZED);

        ProjectEvidence saved = evidenceRepository.save(evidence);

        Map<UUID, String> labels = numberingService.computeDynamicLabelsForProject(projectId);
        String label = numberingService.resolveLabel(saved, labels);
        saved.setFigureLabel(label);

        return ProjectEvidenceResponse.from(saved, label);
    }

    @Transactional
    public ProjectEvidenceResponse createStructuredEvidence(
            UUID projectId,
            CreateStructuredEvidenceRequest request,
            User user
    ) {
        authorizationService.requireProjectMember(projectId, user);

        ResearchProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        ResearchReport report = reportRepository.findByProjectId(projectId).orElse(null);

        ResearchReportSection section = null;
        if (request.sectionId() != null) {
            section = sectionRepository.findById(request.sectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Report section not found: " + request.sectionId()));
        }

        int nextOrder = (int) evidenceRepository.countByProjectId(projectId) + 1;

        ProjectEvidence evidence = new ProjectEvidence();
        evidence.setId(UUID.randomUUID());
        evidence.setProject(project);
        evidence.setWorkspace(project.getWorkspace());
        evidence.setReport(report);
        evidence.setSection(section);
        evidence.setOriginalFilename(request.caption() != null ? request.caption() : "Structured Evidence");
        evidence.setMimeType("application/json");
        evidence.setFileSizeBytes(0);
        evidence.setEvidenceType(request.evidenceType());
        evidence.setCaption(request.caption());
        evidence.setDescription(request.description());
        evidence.setAltText(request.altText());
        evidence.setMetadataJson(request.metadataJson());
        evidence.setDisplayOrder(nextOrder);
        evidence.setCreatedBy(user);
        evidence.setAiAnalysisStatus(EvidenceAnalysisStatus.NOT_ANALYZED);

        ProjectEvidence saved = evidenceRepository.save(evidence);

        Map<UUID, String> labels = numberingService.computeDynamicLabelsForProject(projectId);
        String label = numberingService.resolveLabel(saved, labels);
        saved.setFigureLabel(label);

        return ProjectEvidenceResponse.from(saved, label);
    }

    @Transactional(readOnly = true)
    public List<ProjectEvidenceResponse> listProjectEvidence(
            UUID projectId,
            UUID sectionId,
            EvidenceType type,
            User user
    ) {
        authorizationService.requireProjectMember(projectId, user);

        List<ProjectEvidence> evidenceList;
        if (sectionId != null) {
            evidenceList = evidenceRepository.findAllBySectionIdOrderByDisplayOrderAscCreatedAtAsc(sectionId);
        } else if (type != null) {
            evidenceList = evidenceRepository.findAllByProjectIdAndEvidenceTypeOrderByDisplayOrderAscCreatedAtAsc(projectId, type);
        } else {
            evidenceList = evidenceRepository.findAllByProjectIdOrderByDisplayOrderAscCreatedAtAsc(projectId);
        }

        Map<UUID, String> labels = numberingService.computeDynamicLabelsForProject(projectId);

        return evidenceList.stream()
                .map(e -> ProjectEvidenceResponse.from(e, labels.get(e.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectEvidenceResponse getEvidence(UUID evidenceId, User user) {
        ProjectEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Project evidence not found: " + evidenceId));

        authorizationService.requireProjectMember(evidence.getProject().getId(), user);
        Map<UUID, String> labels = numberingService.computeDynamicLabelsForProject(evidence.getProject().getId());

        return ProjectEvidenceResponse.from(evidence, labels.get(evidence.getId()));
    }

    @Transactional
    public ProjectEvidenceResponse updateEvidence(
            UUID evidenceId,
            UpdateProjectEvidenceRequest request,
            User user
    ) {
        ProjectEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Project evidence not found: " + evidenceId));

        authorizationService.requireProjectMember(evidence.getProject().getId(), user);

        if (request.caption() != null) evidence.setCaption(request.caption().trim());
        if (request.description() != null) evidence.setDescription(request.description().trim());
        if (request.altText() != null) evidence.setAltText(request.altText().trim());
        if (request.evidenceType() != null) evidence.setEvidenceType(request.evidenceType());
        if (request.displayOrder() != null) evidence.setDisplayOrder(request.displayOrder());
        if (request.metadataJson() != null) evidence.setMetadataJson(request.metadataJson());

        if (request.sectionId() != null) {
            ResearchReportSection section = sectionRepository.findById(request.sectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + request.sectionId()));
            evidence.setSection(section);
            if (section.getChapter() != null && section.getChapter().getReport() != null) {
                evidence.setReport(section.getChapter().getReport());
            }
        }

        ProjectEvidence saved = evidenceRepository.save(evidence);
        Map<UUID, String> labels = numberingService.computeDynamicLabelsForProject(evidence.getProject().getId());

        return ProjectEvidenceResponse.from(saved, labels.get(saved.getId()));
    }

    @Transactional
    public void deleteEvidence(UUID evidenceId, User user) {
        ProjectEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Project evidence not found: " + evidenceId));

        authorizationService.requireProjectMember(evidence.getProject().getId(), user);

        if (evidence.getStorageKey() != null) {
            try {
                objectStorageService.delete(evidence.getStorageKey());
            } catch (Exception ignored) {}
        }

        evidenceRepository.delete(evidence);
    }

    @Transactional
    public List<ProjectEvidenceResponse> reorderEvidence(
            UUID projectId,
            List<UUID> evidenceIds,
            User user
    ) {
        authorizationService.requireProjectMember(projectId, user);

        List<ProjectEvidence> list = evidenceRepository.findAllByProjectIdOrderByDisplayOrderAscCreatedAtAsc(projectId);
        Map<UUID, ProjectEvidence> map = new HashMap<>();
        for (ProjectEvidence e : list) map.put(e.getId(), e);

        int order = 1;
        for (UUID id : evidenceIds) {
            ProjectEvidence e = map.get(id);
            if (e != null) {
                e.setDisplayOrder(order++);
                evidenceRepository.save(e);
            }
        }

        Map<UUID, String> labels = numberingService.computeDynamicLabelsForProject(projectId);
        return evidenceRepository.findAllByProjectIdOrderByDisplayOrderAscCreatedAtAsc(projectId).stream()
                .map(e -> ProjectEvidenceResponse.from(e, labels.get(e.getId())))
                .toList();
    }
}
