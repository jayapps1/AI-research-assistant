package com.researchassistant.collaboration.service;

import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.repository.*;
import com.researchassistant.collaboration.entity.CollaborationArtifactType;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.document.entity.Document;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.framework.repository.ConceptualFrameworkRepository;
import com.researchassistant.framework.repository.TheoreticalFrameworkRepository;
import com.researchassistant.instruments.entity.ResearchInstrument;
import com.researchassistant.instruments.repository.ResearchInstrumentRepository;
import com.researchassistant.methodology.entity.Methodology;
import com.researchassistant.methodology.repository.MethodologyRepository;
import com.researchassistant.project.exception.InvalidProjectOperationException;
import com.researchassistant.researchdesign.entity.ResearchProblem;
import com.researchassistant.researchdesign.repository.ResearchProblemRepository;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CollaborationArtifactResolver {
    private final ResearchProblemRepository problemRepository;
    private final MethodologyRepository methodologyRepository;
    private final ResearchInstrumentRepository instrumentRepository;
    private final DocumentRepository documentRepository;
    private final ResearchReportRepository reportRepository;
    private final ResearchReportChapterRepository chapterRepository;
    private final ResearchReportSectionRepository sectionRepository;
    private final ResearchFindingRepository findingRepository;
    private final FindingDiscussionRepository discussionRepository;
    private final ResearchConclusionRepository conclusionRepository;
    private final ResearchRecommendationRepository recommendationRepository;
    private final ConceptualFrameworkRepository conceptualFrameworkRepository;
    private final TheoreticalFrameworkRepository theoreticalFrameworkRepository;

    public CollaborationArtifactResolver(
            ResearchProblemRepository problemRepository,
            MethodologyRepository methodologyRepository,
            ResearchInstrumentRepository instrumentRepository,
            DocumentRepository documentRepository,
            ResearchReportRepository reportRepository,
            ResearchReportChapterRepository chapterRepository,
            ResearchReportSectionRepository sectionRepository,
            ResearchFindingRepository findingRepository,
            FindingDiscussionRepository discussionRepository,
            ResearchConclusionRepository conclusionRepository,
            ResearchRecommendationRepository recommendationRepository,
            ConceptualFrameworkRepository conceptualFrameworkRepository,
            TheoreticalFrameworkRepository theoreticalFrameworkRepository
    ) {
        this.problemRepository = problemRepository;
        this.methodologyRepository = methodologyRepository;
        this.instrumentRepository = instrumentRepository;
        this.documentRepository = documentRepository;
        this.reportRepository = reportRepository;
        this.chapterRepository = chapterRepository;
        this.sectionRepository = sectionRepository;
        this.findingRepository = findingRepository;
        this.discussionRepository = discussionRepository;
        this.conclusionRepository = conclusionRepository;
        this.recommendationRepository = recommendationRepository;
        this.conceptualFrameworkRepository = conceptualFrameworkRepository;
        this.theoreticalFrameworkRepository = theoreticalFrameworkRepository;
    }

    public ArtifactRevision requireArtifact(UUID projectId, CollaborationArtifactType type, UUID artifactId) {
        if (type == CollaborationArtifactType.OTHER) {
            return new ArtifactRevision(projectId, artifactId, 1);
        }
        UUID ownerProjectId = switch (type) {
            case RESEARCH_PROBLEM -> problemRepository.findById(artifactId).map(ResearchProblem::getProject).map(p -> p.getId()).orElseThrow(() -> notFound(type));
            case METHODOLOGY -> methodologyRepository.findById(artifactId).map(Methodology::getProject).map(p -> p.getId()).orElseThrow(() -> notFound(type));
            case QUESTIONNAIRE, INTERVIEW_GUIDE, FOCUS_GROUP_GUIDE, OBSERVATION_CHECKLIST -> instrumentRepository.findById(artifactId).map(ResearchInstrument::getProject).map(p -> p.getId()).orElseThrow(() -> notFound(type));
            case DOCUMENT -> documentRepository.findById(artifactId).map(Document::getProject).map(p -> p.getId()).orElseThrow(() -> notFound(type));
            case REPORT -> reportRepository.findById(artifactId).map(ResearchReport::getProject).map(p -> p.getId()).orElseThrow(() -> notFound(type));
            case REPORT_CHAPTER -> chapterRepository.findById(artifactId).map(c -> c.getReport().getProject().getId()).orElseThrow(() -> notFound(type));
            case REPORT_SECTION -> sectionRepository.findById(artifactId).map(s -> s.getChapter().getReport().getProject().getId()).orElseThrow(() -> notFound(type));
            case FINDING -> findingRepository.findById(artifactId).map(ResearchFinding::getProject).map(p -> p.getId()).orElseThrow(() -> notFound(type));
            case DISCUSSION -> discussionRepository.findById(artifactId).map(FindingDiscussion::getProject).map(p -> p.getId()).orElseThrow(() -> notFound(type));
            case CONCLUSION -> conclusionRepository.findById(artifactId).map(ResearchConclusion::getProject).map(p -> p.getId()).orElseThrow(() -> notFound(type));
            case RECOMMENDATION -> recommendationRepository.findById(artifactId).map(ResearchRecommendation::getProject).map(p -> p.getId()).orElseThrow(() -> notFound(type));
            case CONCEPTUAL_FRAMEWORK -> conceptualFrameworkRepository.findById(artifactId).map(f -> f.getProject().getId()).orElseThrow(() -> notFound(type));
            case THEORETICAL_FRAMEWORK -> theoreticalFrameworkRepository.findById(artifactId).map(f -> f.getProject().getId()).orElseThrow(() -> notFound(type));
            default -> throw new InvalidProjectOperationException("Artifact type is not yet linkable.");
        };
        if (!projectId.equals(ownerProjectId)) {
            throw new InvalidProjectOperationException("Artifact does not belong to this project.");
        }
        return new ArtifactRevision(projectId, artifactId, revision(type, artifactId));
    }

    public int revision(CollaborationArtifactType type, UUID artifactId) {
        return switch (type) {
            case METHODOLOGY -> methodologyRepository.findById(artifactId).map(Methodology::getRevisionNumber).orElse(1);
            case REPORT -> reportRepository.findById(artifactId).map(ResearchReport::getRevisionNumber).orElse(1);
            case REPORT_SECTION -> sectionRepository.findById(artifactId).map(ResearchReportSection::getRevisionNumber).orElse(1);
            case FINDING -> findingRepository.findById(artifactId).map(ResearchFinding::getRevisionNumber).orElse(1);
            case DISCUSSION -> discussionRepository.findById(artifactId).map(FindingDiscussion::getRevisionNumber).orElse(1);
            case CONCLUSION -> conclusionRepository.findById(artifactId).map(ResearchConclusion::getRevisionNumber).orElse(1);
            case RECOMMENDATION -> recommendationRepository.findById(artifactId).map(ResearchRecommendation::getRevisionNumber).orElse(1);
            case CONCEPTUAL_FRAMEWORK -> conceptualFrameworkRepository.findById(artifactId).map(f -> f.getRevisionNumber()).orElse(1);
            case THEORETICAL_FRAMEWORK -> theoreticalFrameworkRepository.findById(artifactId).map(f -> f.getRevisionNumber()).orElse(1);
            default -> 1;
        };
    }

    private ResourceNotFoundException notFound(CollaborationArtifactType type) {
        return new ResourceNotFoundException(type + " not found.");
    }

    public record ArtifactRevision(UUID projectId, UUID artifactId, int revision) {
    }
}
