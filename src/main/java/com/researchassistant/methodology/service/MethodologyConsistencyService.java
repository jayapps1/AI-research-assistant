package com.researchassistant.methodology.service;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentChunk;
import com.researchassistant.document.entity.DocumentPage;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.framework.entity.ConceptualFramework;
import com.researchassistant.framework.entity.ConceptualVariable;
import com.researchassistant.framework.entity.TheoreticalFramework;
import com.researchassistant.methodology.entity.Methodology;
import com.researchassistant.methodology.entity.SamplingPlan;
import com.researchassistant.methodology.entity.StudyPopulation;
import com.researchassistant.methodology.exception.MethodologyValidationException;
import com.researchassistant.researchdesign.entity.ResearchObjective;
import com.researchassistant.researchdesign.entity.ResearchProblem;
import com.researchassistant.researchdesign.entity.ResearchQuestion;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MethodologyConsistencyService {

    public void requireProblemInProject(ResearchProblem problem, UUID projectId) {
        if (problem == null) {
            return;
        }
        if (!problem.getProject().getId().equals(projectId)) {
            throw new MethodologyValidationException("Research problem belongs to another project.");
        }
    }

    public void requireObjectiveInProject(ResearchObjective objective, UUID projectId) {
        if (objective == null || !objective.getProject().getId().equals(projectId)) {
            throw new MethodologyValidationException("Objective belongs to another project.");
        }
    }

    public void requireQuestionInProject(ResearchQuestion question, UUID projectId) {
        if (question == null || !question.getProject().getId().equals(projectId)) {
            throw new MethodologyValidationException("Research question belongs to another project.");
        }
    }

    public void requireFrameworkInProject(ConceptualFramework framework, UUID projectId) {
        if (framework == null || !framework.getProject().getId().equals(projectId)) {
            throw new MethodologyValidationException("Conceptual framework belongs to another project.");
        }
    }

    public void requireTheoreticalFrameworkInProject(TheoreticalFramework framework, UUID projectId) {
        if (framework == null || !framework.getProject().getId().equals(projectId)) {
            throw new MethodologyValidationException("Theoretical framework belongs to another project.");
        }
    }

    public void requireVariableInFramework(ConceptualVariable variable, ConceptualFramework framework) {
        if (variable == null || !variable.getFramework().getId().equals(framework.getId())) {
            throw new MethodologyValidationException("Conceptual relationship variables must belong to the same framework.");
        }
    }

    public void requireMethodologyInProject(Methodology methodology, UUID projectId) {
        if (methodology == null || !methodology.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Methodology not found.");
        }
    }

    public void requirePopulationInMethodology(StudyPopulation population, Methodology methodology) {
        if (population == null || !population.getMethodology().getId().equals(methodology.getId())) {
            throw new MethodologyValidationException("Study population belongs to another methodology.");
        }
    }

    public void requireSamplingPlanInMethodology(SamplingPlan samplingPlan, Methodology methodology) {
        if (samplingPlan == null || !samplingPlan.getMethodology().getId().equals(methodology.getId())) {
            throw new MethodologyValidationException("Sampling plan belongs to another methodology.");
        }
    }

    public void requireDocumentEvidenceInProject(
            Document document,
            DocumentVersion version,
            DocumentPage page,
            DocumentChunk chunk,
            UUID projectId
    ) {
        if (document == null || !document.getProject().getId().equals(projectId)) {
            throw new MethodologyValidationException("Evidence document belongs to another project.");
        }
        if (version == null || !version.getDocument().getId().equals(document.getId())) {
            throw new MethodologyValidationException("Evidence version does not belong to the document.");
        }
        if (page != null && !page.getDocumentVersion().getId().equals(version.getId())) {
            throw new MethodologyValidationException("Evidence page does not belong to the version.");
        }
        if (chunk != null && !chunk.getDocumentVersion().getId().equals(version.getId())) {
            throw new MethodologyValidationException("Evidence chunk does not belong to the version.");
        }
    }
}
