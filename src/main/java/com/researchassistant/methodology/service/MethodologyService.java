package com.researchassistant.methodology.service;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.methodology.dto.MethodologyRequests.*;
import com.researchassistant.methodology.dto.MethodologyResponses.*;
import com.researchassistant.methodology.entity.*;
import com.researchassistant.methodology.repository.*;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.rag.exception.RagCapabilityUnavailableException;
import com.researchassistant.researchdesign.entity.ResearchProblem;
import com.researchassistant.researchdesign.repository.ResearchProblemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MethodologyService {
    private final ProjectAuthorizationService authorizationService;
    private final MethodologyRepository methodologyRepository;
    private final StudyPopulationRepository populationRepository;
    private final SamplingPlanRepository samplingPlanRepository;
    private final SampleSizeCalculationRepository calculationRepository;
    private final DataCollectionMethodRepository methodRepository;
    private final ResearchProblemRepository problemRepository;
    private final MethodologyConsistencyService consistencyService;
    private final SampleSizeCalculator calculator;

    public MethodologyService(ProjectAuthorizationService authorizationService, MethodologyRepository methodologyRepository, StudyPopulationRepository populationRepository, SamplingPlanRepository samplingPlanRepository, SampleSizeCalculationRepository calculationRepository, DataCollectionMethodRepository methodRepository, ResearchProblemRepository problemRepository, MethodologyConsistencyService consistencyService, SampleSizeCalculator calculator) {
        this.authorizationService = authorizationService;
        this.methodologyRepository = methodologyRepository;
        this.populationRepository = populationRepository;
        this.samplingPlanRepository = samplingPlanRepository;
        this.calculationRepository = calculationRepository;
        this.methodRepository = methodRepository;
        this.problemRepository = problemRepository;
        this.consistencyService = consistencyService;
        this.calculator = calculator;
    }

    @Transactional
    public MethodologyResponse create(UUID projectId, User user, CreateMethodologyRequest request) {
        ProjectAuthorizationContext context = authorizationService.requireProjectEditor(projectId, user);
        Methodology methodology = new Methodology();
        methodology.setProject(context.project());
        if (request.researchProblemId() != null) {
            ResearchProblem problem = problemRepository.findById(request.researchProblemId()).orElseThrow(() -> new ResourceNotFoundException("Research problem not found."));
            consistencyService.requireProblemInProject(problem, projectId);
            methodology.setResearchProblem(problem);
        }
        methodology.setTitle(request.title());
        methodology.setApproach(request.approach() == null ? ResearchApproach.UNSPECIFIED : request.approach());
        methodology.setDesignType(request.designType() == null ? ResearchDesignType.UNSPECIFIED : request.designType());
        methodology.setDesignDescription(request.designDescription());
        methodology.setStudySetting(request.studySetting());
        methodology.setStudyPeriod(request.studyPeriod());
        methodology.setRationale(request.rationale());
        methodology.setOrigin(origin(request.origin()));
        methodology.setCreatedBy(user);
        return response(methodologyRepository.save(methodology));
    }

    @Transactional(readOnly = true)
    public List<MethodologyResponse> list(UUID projectId, User user) {
        authorizationService.requireProjectViewer(projectId, user);
        return methodologyRepository.findAllByProjectIdOrderByRevisionNumberDesc(projectId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public MethodologyResponse get(UUID id, User user) {
        Methodology methodology = methodology(id);
        authorizationService.requireProjectViewer(methodology.getProject().getId(), user);
        return response(methodology);
    }

    @Transactional
    public MethodologyResponse update(UUID id, User user, CreateMethodologyRequest request) {
        Methodology methodology = methodology(id);
        authorizationService.requireProjectEditor(methodology.getProject().getId(), user);
        if (request.title() != null) methodology.setTitle(request.title());
        if (request.approach() != null) methodology.setApproach(request.approach());
        if (request.designType() != null) methodology.setDesignType(request.designType());
        if (request.designDescription() != null) methodology.setDesignDescription(request.designDescription());
        if (request.studySetting() != null) methodology.setStudySetting(request.studySetting());
        if (request.studyPeriod() != null) methodology.setStudyPeriod(request.studyPeriod());
        if (request.rationale() != null) methodology.setRationale(request.rationale());
        if (request.origin() != null) methodology.setOrigin(request.origin());
        return response(methodology);
    }

    @Transactional
    public MethodologyResponse activate(UUID id, User user) {
        Methodology methodology = methodology(id);
        authorizationService.requireProjectEditor(methodology.getProject().getId(), user);
        methodologyRepository.findByProjectIdAndStatus(methodology.getProject().getId(), MethodologyStatus.ACTIVE)
                .filter(active -> !active.getId().equals(methodology.getId()))
                .ifPresent(active -> active.setStatus(MethodologyStatus.SUPERSEDED));
        methodology.setStatus(MethodologyStatus.ACTIVE);
        return response(methodology);
    }

    @Transactional
    public PopulationResponse createPopulation(UUID methodologyId, User user, CreatePopulationRequest request) {
        Methodology methodology = methodology(methodologyId);
        authorizationService.requireProjectEditor(methodology.getProject().getId(), user);
        StudyPopulation population = new StudyPopulation();
        population.setMethodology(methodology);
        population.setTargetPopulationDescription(request.targetPopulationDescription());
        population.setTargetPopulationSize(request.targetPopulationSize());
        population.setAccessiblePopulationDescription(request.accessiblePopulationDescription());
        population.setAccessiblePopulationSize(request.accessiblePopulationSize());
        population.setInclusionCriteria(request.inclusionCriteria());
        population.setExclusionCriteria(request.exclusionCriteria());
        population.setGeographicScope(request.geographicScope());
        population.setDemographicCharacteristics(request.demographicCharacteristics());
        population.setOrigin(origin(request.origin()));
        return populationResponse(populationRepository.save(population));
    }

    @Transactional
    public PopulationResponse updatePopulation(UUID id, User user, CreatePopulationRequest request) {
        StudyPopulation population = population(id);
        authorizationService.requireProjectEditor(population.getMethodology().getProject().getId(), user);
        if (request.targetPopulationDescription() != null) population.setTargetPopulationDescription(request.targetPopulationDescription());
        if (request.targetPopulationSize() != null) population.setTargetPopulationSize(request.targetPopulationSize());
        if (request.accessiblePopulationDescription() != null) population.setAccessiblePopulationDescription(request.accessiblePopulationDescription());
        if (request.accessiblePopulationSize() != null) population.setAccessiblePopulationSize(request.accessiblePopulationSize());
        if (request.origin() != null) population.setOrigin(request.origin());
        return populationResponse(population);
    }

    @Transactional
    public SamplingPlanResponse createSamplingPlan(UUID methodologyId, User user, CreateSamplingPlanRequest request) {
        Methodology methodology = methodology(methodologyId);
        authorizationService.requireProjectEditor(methodology.getProject().getId(), user);
        SamplingPlan plan = new SamplingPlan();
        plan.setMethodology(methodology);
        if (request.populationId() != null) {
            StudyPopulation population = population(request.populationId());
            consistencyService.requirePopulationInMethodology(population, methodology);
            plan.setPopulation(population);
        }
        plan.setApproach(request.approach() == null ? SamplingApproach.OTHER : request.approach());
        plan.setTechnique(request.technique() == null ? SamplingTechnique.OTHER : request.technique());
        plan.setPlannedSampleSize(request.plannedSampleSize());
        plan.setRationale(request.rationale());
        plan.setSamplingFrame(request.samplingFrame());
        plan.setRecruitmentStrategy(request.recruitmentStrategy());
        plan.setOrigin(origin(request.origin()));
        return samplingResponse(samplingPlanRepository.save(plan));
    }

    @Transactional
    public SamplingPlanResponse updateSamplingPlan(UUID id, User user, CreateSamplingPlanRequest request) {
        SamplingPlan plan = samplingPlan(id);
        authorizationService.requireProjectEditor(plan.getMethodology().getProject().getId(), user);
        if (request.populationId() != null) {
            StudyPopulation population = population(request.populationId());
            consistencyService.requirePopulationInMethodology(population, plan.getMethodology());
            plan.setPopulation(population);
        }
        if (request.approach() != null) plan.setApproach(request.approach());
        if (request.technique() != null) plan.setTechnique(request.technique());
        if (request.plannedSampleSize() != null) plan.setPlannedSampleSize(request.plannedSampleSize());
        if (request.rationale() != null) plan.setRationale(request.rationale());
        return samplingResponse(plan);
    }

    @Transactional
    public SampleSizeCalculationResponse calculate(UUID samplingPlanId, User user, CalculateSampleSizeRequest request) {
        SamplingPlan plan = samplingPlan(samplingPlanId);
        authorizationService.requireProjectEditor(plan.getMethodology().getProject().getId(), user);
        SampleSizeCalculationResponse calculated = calculator.calculate(UUID.randomUUID(), request);
        SampleSizeCalculation entity = new SampleSizeCalculation();
        entity.setId(calculated.id());
        entity.setSamplingPlan(plan);
        entity.setMethod(calculated.method());
        entity.setPopulationSize(calculated.populationSize());
        entity.setConfidenceLevel(calculated.confidenceLevel());
        entity.setMarginOfError(calculated.marginOfError());
        entity.setEstimatedProportion(calculated.estimatedProportion());
        entity.setDesignEffect(calculated.designEffect());
        entity.setExpectedResponseRate(calculated.expectedResponseRate());
        entity.setInitialSampleSize(calculated.initialSampleSize());
        entity.setAdjustedSampleSize(calculated.adjustedSampleSize());
        entity.setFormulaDescription(calculated.formulaDescription());
        entity.setAssumptions(calculated.assumptions());
        entity.setOrigin(ContentOrigin.USER);
        entity.setCreatedBy(user);
        calculationRepository.save(entity);
        return calculated;
    }

    @Transactional(readOnly = true)
    public List<SampleSizeCalculationResponse> calculations(UUID samplingPlanId, User user) {
        SamplingPlan plan = samplingPlan(samplingPlanId);
        authorizationService.requireProjectViewer(plan.getMethodology().getProject().getId(), user);
        return calculationRepository.findAllBySamplingPlanIdOrderByCreatedAtDesc(samplingPlanId).stream().map(this::calculationResponse).toList();
    }

    @Transactional
    public DataCollectionMethodResponse createDataCollectionMethod(UUID methodologyId, User user, CreateDataCollectionMethodRequest request) {
        Methodology methodology = methodology(methodologyId);
        authorizationService.requireProjectEditor(methodology.getProject().getId(), user);
        DataCollectionMethod method = new DataCollectionMethod();
        method.setMethodology(methodology);
        method.setType(request.type() == null ? DataCollectionMethodType.OTHER : request.type());
        method.setName(request.name().trim());
        method.setDescription(request.description());
        method.setRationale(request.rationale());
        method.setSourceType(request.sourceType() == null ? DataSourceType.PRIMARY : request.sourceType());
        method.setAdministrationMode(request.administrationMode());
        method.setSetting(request.setting());
        method.setTiming(request.timing());
        method.setPrimaryMethod(request.primaryMethod());
        method.setDisplayOrder(request.displayOrder() == null ? 1 : request.displayOrder());
        method.setOrigin(origin(request.origin()));
        method.setCreatedBy(user);
        return methodResponse(methodRepository.save(method));
    }

    @Transactional(readOnly = true)
    public List<DataCollectionMethodResponse> dataCollectionMethods(UUID methodologyId, User user) {
        Methodology methodology = methodology(methodologyId);
        authorizationService.requireProjectViewer(methodology.getProject().getId(), user);
        return methodRepository.findAllByMethodologyIdOrderByDisplayOrderAsc(methodologyId).stream().map(this::methodResponse).toList();
    }

    @Transactional(readOnly = true)
    public DataCollectionMethodResponse dataCollectionMethod(UUID id, User user) {
        DataCollectionMethod method = dataMethod(id);
        authorizationService.requireProjectViewer(method.getMethodology().getProject().getId(), user);
        return methodResponse(method);
    }

    @Transactional
    public DataCollectionMethodResponse updateDataCollectionMethod(UUID id, User user, CreateDataCollectionMethodRequest request) {
        DataCollectionMethod method = dataMethod(id);
        authorizationService.requireProjectEditor(method.getMethodology().getProject().getId(), user);
        if (request.type() != null) method.setType(request.type());
        if (request.name() != null && !request.name().isBlank()) method.setName(request.name().trim());
        if (request.description() != null) method.setDescription(request.description());
        if (request.rationale() != null) method.setRationale(request.rationale());
        if (request.sourceType() != null) method.setSourceType(request.sourceType());
        if (request.displayOrder() != null) method.setDisplayOrder(request.displayOrder());
        return methodResponse(method);
    }

    public Object generateMethodology(UUID projectId, User user) {
        authorizationService.requireProjectEditor(projectId, user);
        throw new RagCapabilityUnavailableException("Methodology generation is not enabled.");
    }

    public Object generateDataCollection(UUID methodologyId, User user) {
        Methodology methodology = methodology(methodologyId);
        authorizationService.requireProjectEditor(methodology.getProject().getId(), user);
        throw new RagCapabilityUnavailableException("Data collection method generation is not enabled.");
    }

    private Methodology methodology(UUID id) { return methodologyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Methodology not found.")); }
    private StudyPopulation population(UUID id) { return populationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Study population not found.")); }
    private SamplingPlan samplingPlan(UUID id) { return samplingPlanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sampling plan not found.")); }
    private DataCollectionMethod dataMethod(UUID id) { return methodRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Data collection method not found.")); }
    private ContentOrigin origin(ContentOrigin origin) { return origin == null ? ContentOrigin.USER : origin; }
    private MethodologyResponse response(Methodology m) { return new MethodologyResponse(m.getId(), m.getProject().getId(), m.getResearchProblem() == null ? null : m.getResearchProblem().getId(), m.getTitle(), m.getApproach(), m.getDesignType(), m.getDesignDescription(), m.getStudySetting(), m.getStudyPeriod(), m.getRationale(), m.getStatus(), m.getOrigin(), m.getRevisionNumber(), m.getCreatedAt(), m.getUpdatedAt()); }
    private PopulationResponse populationResponse(StudyPopulation p) { return new PopulationResponse(p.getId(), p.getMethodology().getId(), p.getTargetPopulationDescription(), p.getTargetPopulationSize(), p.getAccessiblePopulationDescription(), p.getAccessiblePopulationSize(), p.getInclusionCriteria(), p.getExclusionCriteria(), p.getGeographicScope(), p.getDemographicCharacteristics(), p.getOrigin()); }
    private SamplingPlanResponse samplingResponse(SamplingPlan p) { return new SamplingPlanResponse(p.getId(), p.getMethodology().getId(), p.getPopulation() == null ? null : p.getPopulation().getId(), p.getApproach(), p.getTechnique(), p.getPlannedSampleSize(), p.getRationale(), p.getSamplingFrame(), p.getRecruitmentStrategy(), p.getOrigin()); }
    private SampleSizeCalculationResponse calculationResponse(SampleSizeCalculation c) { return new SampleSizeCalculationResponse(c.getId(), c.getMethod(), c.getPopulationSize(), c.getConfidenceLevel(), c.getMarginOfError(), c.getEstimatedProportion(), c.getDesignEffect(), c.getExpectedResponseRate(), c.getInitialSampleSize(), c.getAdjustedSampleSize(), c.getFormulaDescription(), c.getAssumptions()); }
    private DataCollectionMethodResponse methodResponse(DataCollectionMethod m) { return new DataCollectionMethodResponse(m.getId(), m.getMethodology().getId(), m.getType(), m.getName(), m.getDescription(), m.getRationale(), m.getSourceType(), m.getAdministrationMode(), m.getSetting(), m.getTiming(), m.isPrimaryMethod(), m.getDisplayOrder(), m.getOrigin()); }
}
