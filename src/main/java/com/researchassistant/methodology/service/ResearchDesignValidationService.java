package com.researchassistant.methodology.service;

import com.researchassistant.cache.AppCacheNames;
import com.researchassistant.identity.entity.User;
import com.researchassistant.methodology.dto.MethodologyResponses.ResearchDesignValidationResult;
import com.researchassistant.methodology.repository.DataCollectionMethodRepository;
import com.researchassistant.methodology.repository.MethodologyRepository;
import com.researchassistant.methodology.repository.SamplingPlanRepository;
import com.researchassistant.methodology.repository.StudyPopulationRepository;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.researchdesign.entity.ResearchObjectiveType;
import com.researchassistant.researchdesign.repository.ResearchObjectiveRepository;
import com.researchassistant.researchdesign.repository.ResearchProblemRepository;
import com.researchassistant.researchdesign.repository.ResearchQuestionRepository;

import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ResearchDesignValidationService {
    private final ProjectAuthorizationService authorizationService;
    private final ResearchProblemRepository problemRepository;
    private final ResearchObjectiveRepository objectiveRepository;
    private final ResearchQuestionRepository questionRepository;
    private final MethodologyRepository methodologyRepository;
    private final StudyPopulationRepository populationRepository;
    private final SamplingPlanRepository samplingPlanRepository;
    private final DataCollectionMethodRepository methodRepository;

    public ResearchDesignValidationService(ProjectAuthorizationService authorizationService, ResearchProblemRepository problemRepository, ResearchObjectiveRepository objectiveRepository, ResearchQuestionRepository questionRepository, MethodologyRepository methodologyRepository, StudyPopulationRepository populationRepository, SamplingPlanRepository samplingPlanRepository, DataCollectionMethodRepository methodRepository) {
        this.authorizationService = authorizationService;
        this.problemRepository = problemRepository;
        this.objectiveRepository = objectiveRepository;
        this.questionRepository = questionRepository;
        this.methodologyRepository = methodologyRepository;
        this.populationRepository = populationRepository;
        this.samplingPlanRepository = samplingPlanRepository;
        this.methodRepository = methodRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = AppCacheNames.RESEARCH_DESIGN_VALIDATION,
            key = "'project:' + #projectId + ':user:' + #user.id + ':validation'"
    )
    public ResearchDesignValidationResult validate(UUID projectId, User user) {
        authorizationService.requireProjectViewer(projectId, user);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> info = new ArrayList<>();
        if (problemRepository.findAllByProjectId(projectId).isEmpty()) warnings.add("Research problem has not been defined.");
        if (!objectiveRepository.existsByProjectIdAndType(projectId, ResearchObjectiveType.GENERAL)) warnings.add("General objective has not been defined.");
        if (objectiveRepository.findAllByProjectId(projectId).stream().noneMatch(o -> o.getType() == ResearchObjectiveType.SPECIFIC)) warnings.add("Specific objectives have not been defined.");
        if (questionRepository.countByProjectId(projectId) == 0) warnings.add("Research questions have not been defined.");
        var activeMethodology = methodologyRepository.findByProjectIdAndStatus(projectId, com.researchassistant.methodology.entity.MethodologyStatus.ACTIVE);
        if (activeMethodology.isEmpty()) {
            errors.add("No active methodology has been selected.");
        } else {
            var methodology = activeMethodology.get();
            if (populationRepository.findAllByMethodologyId(methodology.getId()).isEmpty()) warnings.add("Study population has not been defined.");
            if (samplingPlanRepository.findAllByMethodologyId(methodology.getId()).isEmpty()) warnings.add("Sampling plan has not been defined.");
            if (methodRepository.countByMethodologyProjectId(projectId) == 0) warnings.add("Data collection methods have not been defined.");
        }
        if (errors.isEmpty() && warnings.isEmpty()) info.add("Research design has the core execution artifacts.");
        return new ResearchDesignValidationResult(errors.isEmpty(), errors, warnings, info);
    }
}
