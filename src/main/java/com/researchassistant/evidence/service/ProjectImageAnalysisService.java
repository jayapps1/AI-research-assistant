package com.researchassistant.evidence.service;

import com.researchassistant.common.storage.ObjectStorageService;
import com.researchassistant.common.storage.StorageObject;
import com.researchassistant.evidence.dto.ProjectEvidenceDtos.ProjectImageAnalysisResult;
import com.researchassistant.evidence.entity.EvidenceAnalysisStatus;
import com.researchassistant.evidence.entity.ProjectEvidence;
import com.researchassistant.evidence.repository.ProjectEvidenceRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectImageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ProjectImageAnalysisService.class);

    private final ProjectEvidenceRepository evidenceRepository;
    private final ObjectStorageService objectStorageService;
    private final ProjectAuthorizationService authorizationService;
    private final List<ProjectImageAnalysisProvider> providers;

    public ProjectImageAnalysisService(
            ProjectEvidenceRepository evidenceRepository,
            ObjectStorageService objectStorageService,
            ProjectAuthorizationService authorizationService,
            List<ProjectImageAnalysisProvider> providers
    ) {
        this.evidenceRepository = evidenceRepository;
        this.objectStorageService = objectStorageService;
        this.authorizationService = authorizationService;
        this.providers = providers;
    }

    /**
     * Checks if any multimodal image analysis provider is available.
     */
    public boolean isImageAnalysisAvailable() {
        return providers.stream().anyMatch(ProjectImageAnalysisProvider::isAvailable);
    }

    @Transactional
    public ProjectImageAnalysisResult analyzeEvidence(UUID evidenceId, User user) {
        ProjectEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Project evidence not found: " + evidenceId));

        authorizationService.requireProjectMember(evidence.getProject().getId(), user);

        ProjectImageAnalysisProvider provider = providers.stream()
                .filter(ProjectImageAnalysisProvider::isAvailable)
                .findFirst()
                .orElse(null);

        // Strict Requirement 6: NO FAKE IMAGE ANALYSIS
        if (provider == null) {
            String msg = "Multimodal image analysis provider is unavailable or not configured. You can provide an image description manually.";
            evidence.setAiAnalysisStatus(EvidenceAnalysisStatus.UNAVAILABLE);
            evidence.setAiAnalysisError(msg);
            evidenceRepository.save(evidence);
            return new ProjectImageAnalysisResult(
                    EvidenceAnalysisStatus.UNAVAILABLE,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    msg
            );
        }

        if (evidence.getStorageKey() == null) {
            String msg = "No image file attached to this evidence.";
            evidence.setAiAnalysisStatus(EvidenceAnalysisStatus.FAILED);
            evidence.setAiAnalysisError(msg);
            evidenceRepository.save(evidence);
            return new ProjectImageAnalysisResult(
                    EvidenceAnalysisStatus.FAILED,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    msg
            );
        }

        try {
            StorageObject storageObject = objectStorageService.open(evidence.getStorageKey());
            byte[] bytes;
            try (InputStream is = storageObject.contentStream()) {
                bytes = is.readAllBytes();
            }

            String sectionTitle = evidence.getSection() != null ? evidence.getSection().getHeading() : null;
            String projectTitle = evidence.getProject().getTitle();
            String projectDesc = evidence.getProject().getDescription();

            evidence.setAiAnalysisStatus(EvidenceAnalysisStatus.ANALYZING);
            evidenceRepository.save(evidence);

            ProjectImageAnalysisResult result = provider.analyzeImage(
                    bytes,
                    evidence.getMimeType(),
                    evidence.getOriginalFilename(),
                    sectionTitle,
                    projectTitle,
                    projectDesc
            );

            if (result.status() == EvidenceAnalysisStatus.COMPLETED) {
                evidence.setAiVisualAnalysis(result.visibleSummary());
                evidence.setAiAnalysisStatus(EvidenceAnalysisStatus.COMPLETED);
                evidence.setAiAnalysisError(null);
            } else {
                evidence.setAiAnalysisStatus(result.status());
                evidence.setAiAnalysisError(result.errorMessage());
            }

            evidenceRepository.save(evidence);
            return result;

        } catch (Exception ex) {
            log.error("Failed to execute image analysis for evidence {}: {}", evidenceId, ex.getMessage(), ex);
            String err = "Error analyzing image: " + ex.getMessage();
            evidence.setAiAnalysisStatus(EvidenceAnalysisStatus.FAILED);
            evidence.setAiAnalysisError(err);
            evidenceRepository.save(evidence);
            return new ProjectImageAnalysisResult(
                    EvidenceAnalysisStatus.FAILED,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    err
            );
        }
    }
}
