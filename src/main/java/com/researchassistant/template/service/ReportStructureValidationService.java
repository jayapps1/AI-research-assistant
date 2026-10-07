package com.researchassistant.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.analysis.entity.AcademicDocumentGuideline;
import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.analysis.entity.SectionRequirementLevel;
import com.researchassistant.analysis.entity.SectionSemanticPurpose;
import com.researchassistant.analysis.repository.AcademicDocumentGuidelineRepository;
import com.researchassistant.analysis.repository.ResearchReportRepository;
import com.researchassistant.analysis.repository.ResearchReportSectionRepository;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.template.dto.AcademicTemplateDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReportStructureValidationService {

    private final ResearchReportRepository reportRepository;
    private final ResearchReportSectionRepository sectionRepository;
    private final AcademicDocumentGuidelineRepository guidelineRepository;
    private final ObjectMapper objectMapper;

    public ReportStructureValidationService(
            ResearchReportRepository reportRepository,
            ResearchReportSectionRepository sectionRepository,
            AcademicDocumentGuidelineRepository guidelineRepository,
            ObjectMapper objectMapper
    ) {
        this.reportRepository = reportRepository;
        this.sectionRepository = sectionRepository;
        this.guidelineRepository = guidelineRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates whether current project sections fulfill the template requirements.
     * Required items produce actionable validation warnings without blocking modification.
     */
    @Transactional(readOnly = true)
    public DocumentStructureValidationResponse validateReportStructure(UUID projectId) {
        ResearchReport report = reportRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found for project: " + projectId));

        List<ResearchReportSection> currentSections = sectionRepository.findAllByReportIdOrderByChapterDisplayOrderAscDisplayOrderAsc(report.getId());
        Set<String> presentHeadings = new HashSet<>();
        Set<SectionSemanticPurpose> presentPurposes = new HashSet<>();

        for (ResearchReportSection sec : currentSections) {
            presentHeadings.add(sec.getHeading().toLowerCase(Locale.ROOT).trim());
            if (sec.getSemanticPurpose() != null) {
                presentPurposes.add(sec.getSemanticPurpose());
            }
        }

        List<ValidationWarning> warnings = new ArrayList<>();
        int requiredSectionsCount = 0;
        int presentRequiredCount = 0;

        // 1. Check against approved guideline if one exists
        Optional<AcademicDocumentGuideline> guidelineOpt = guidelineRepository.findFirstByProjectIdAndStatusOrderByVersionDesc(projectId, "APPROVED");
        if (guidelineOpt.isPresent()) {
            ExtractedAcademicTemplate template = parseTemplate(guidelineOpt.get());
            if (template != null) {
                // Check front matter
                for (TemplateSectionDefinitionDto fm : template.frontMatter()) {
                    if (fm.requirementLevel() == SectionRequirementLevel.REQUIRED) {
                        requiredSectionsCount++;
                        boolean present = presentHeadings.contains(fm.heading().toLowerCase(Locale.ROOT).trim())
                                || (fm.semanticPurpose() != null && presentPurposes.contains(fm.semanticPurpose()));
                        if (present) {
                            presentRequiredCount++;
                        } else {
                            warnings.add(new ValidationWarning(
                                    "MISSING_REQUIRED_FRONT_MATTER",
                                    "Required front-matter section '" + fm.heading() + "' is missing from the document.",
                                    fm.heading(),
                                    "PRELIMINARY PAGES",
                                    fm.requirementLevel()
                            ));
                        }
                    }
                }

                // Check chapters
                for (TemplateChapterDefinitionDto ch : template.chapters()) {
                    for (TemplateSectionDefinitionDto sec : ch.sections()) {
                        if (sec.requirementLevel() == SectionRequirementLevel.REQUIRED) {
                            requiredSectionsCount++;
                            boolean present = presentHeadings.contains(sec.heading().toLowerCase(Locale.ROOT).trim())
                                    || (sec.semanticPurpose() != null && presentPurposes.contains(sec.semanticPurpose()));
                            if (present) {
                                presentRequiredCount++;
                            } else {
                                warnings.add(new ValidationWarning(
                                    "MISSING_REQUIRED_SECTION",
                                    "Required section '" + sec.heading() + "' (" + (sec.sectionNumber() != null ? sec.sectionNumber() : "") + ") is missing from " + ch.title() + ".",
                                    sec.heading(),
                                    ch.title(),
                                    sec.requirementLevel()
                                ));
                            }
                        }
                    }
                }
            }
        } else {
            // General validation from current section requirement levels
            for (ResearchReportSection sec : currentSections) {
                if (sec.getRequirementLevel() == SectionRequirementLevel.REQUIRED) {
                    requiredSectionsCount++;
                    if (sec.getContent() != null && !sec.getContent().isBlank()) {
                        presentRequiredCount++;
                    } else {
                        warnings.add(new ValidationWarning(
                                "EMPTY_REQUIRED_SECTION",
                                "Required section '" + sec.getHeading() + "' is currently empty.",
                                sec.getHeading(),
                                sec.getChapter() != null ? sec.getChapter().getTitle() : "Document",
                                sec.getRequirementLevel()
                        ));
                    }
                }
            }
        }

        int missingRequired = requiredSectionsCount - presentRequiredCount;
        return new DocumentStructureValidationResponse(
                warnings.isEmpty(),
                warnings,
                currentSections.size(),
                requiredSectionsCount,
                presentRequiredCount,
                Math.max(0, missingRequired)
        );
    }

    private ExtractedAcademicTemplate parseTemplate(AcademicDocumentGuideline g) {
        String json = g.getApprovedStructureJson() != null ? g.getApprovedStructureJson() : g.getRawExtractionJson();
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, ExtractedAcademicTemplate.class);
        } catch (Exception e) {
            return null;
        }
    }
}
