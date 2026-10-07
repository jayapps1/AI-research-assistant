package com.researchassistant.evidence.service;

import com.researchassistant.evidence.dto.ProjectEvidenceDtos.ProjectImageAnalysisResult;

public interface ProjectImageAnalysisProvider {

    boolean isAvailable();

    String providerName();

    ProjectImageAnalysisResult analyzeImage(
            byte[] imageBytes,
            String mimeType,
            String filename,
            String sectionTitle,
            String projectTitle,
            String projectDescription
    );
}
