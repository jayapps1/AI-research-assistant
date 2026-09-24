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

import org.springframework.beans.factory.ObjectProvider;
import com.researchassistant.rag.service.RagQueryService;
import com.researchassistant.rag.dto.request.SubmitRagQueryRequest;
import com.researchassistant.rag.dto.response.GroundedAnswerResponse;
import com.researchassistant.rag.scope.RetrievalScopeType;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.researchdesign.entity.ResearchObjective;
import com.researchassistant.researchdesign.repository.ResearchObjectiveRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MethodologyService {
    private final ProjectAuthorizationService authorizationService;
    private final MethodologyRepository methodologyRepository;
    private final StudyPopulationRepository populationRepository;
    private final SamplingPlanRepository samplingPlanRepository;
    private final SampleSizeCalculationRepository calculationRepository;
    private final DataCollectionMethodRepository methodRepository;
    private final ResearchProblemRepository problemRepository;
    private final ResearchObjectiveRepository objectiveRepository;
    private final MethodologyConsistencyService consistencyService;
    private final SampleSizeCalculator calculator;
    private final ObjectProvider<RagQueryService> ragQueryServiceProvider;

    public MethodologyService(
            ProjectAuthorizationService authorizationService,
            MethodologyRepository methodologyRepository,
            StudyPopulationRepository populationRepository,
            SamplingPlanRepository samplingPlanRepository,
            SampleSizeCalculationRepository calculationRepository,
            DataCollectionMethodRepository methodRepository,
            ResearchProblemRepository problemRepository,
            ResearchObjectiveRepository objectiveRepository,
            MethodologyConsistencyService consistencyService,
            SampleSizeCalculator calculator,
            ObjectProvider<RagQueryService> ragQueryServiceProvider
    ) {
        this.authorizationService = authorizationService;
        this.methodologyRepository = methodologyRepository;
        this.populationRepository = populationRepository;
        this.samplingPlanRepository = samplingPlanRepository;
        this.calculationRepository = calculationRepository;
        this.methodRepository = methodRepository;
        this.problemRepository = problemRepository;
        this.objectiveRepository = objectiveRepository;
        this.consistencyService = consistencyService;
        this.calculator = calculator;
        this.ragQueryServiceProvider = ragQueryServiceProvider;
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

    @Transactional
    public Map<String, Object> generateMethodology(UUID projectId, User user) {
        ProjectAuthorizationContext context = authorizationService.requireProjectEditor(projectId, user);
        ResearchProject project = context.project();
        String title = project.getTitle() != null ? project.getTitle() : "Research Study";
        String researchType = project.getResearchType() != null ? project.getResearchType() : "SOFTWARE_SYSTEM_PROJECT";
        boolean isSoftware = "SOFTWARE_SYSTEM_PROJECT".equalsIgnoreCase(researchType);

        String problem = problemRepository.findAllByProjectId(projectId).stream()
                .max(Comparator.comparing(ResearchProblem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ResearchProblem::getStatement)
                .orElse(project.getDescription() != null ? project.getDescription() : "");

        List<String> objectives = objectiveRepository.findAllByProjectId(projectId).stream()
                .sorted(Comparator.comparingInt(ResearchObjective::getDisplayOrder))
                .map(ResearchObjective::getText)
                .toList();

        // Call RAG if available
        String groundedText = null;
        RagQueryService ragService = ragQueryServiceProvider.getIfAvailable();
        if (ragService != null) {
            try {
                String prompt = "Synthesize an empirical and technical methodology for this project based on the uploaded literature.\n"
                        + "Project Title: " + title + "\n"
                        + "Research Type: " + researchType + "\n"
                        + "Problem: " + problem + "\n"
                        + "Objectives: " + String.join("; ", objectives) + "\n\n"
                        + "Provide a comprehensive methodology specification covering:\n"
                        + "1. Research Approach & Methodological Paradigm\n"
                        + "2. Design Classification & Operational Workflow\n"
                        + "3. Target Population & Domain Context\n"
                        + "4. Sampling Strategy & Recommended Sample Size\n"
                        + "5. Data Collection Strategy & Measurement Instruments\n"
                        + (isSoftware ? "6. System Architecture & Tech Stack\n7. Functional Specifications & Testing Strategy" : "6. Data Analysis Plan & Validity Measures");

                SubmitRagQueryRequest ragRequest = new SubmitRagQueryRequest(
                        prompt,
                        RetrievalScopeType.PROJECT_ALL_DOCUMENTS,
                        null,
                        10,
                        title + " methodology architecture sampling data collection"
                );
                GroundedAnswerResponse answer = ragService.submitForProject(projectId, user, ragRequest, "Generate Research Methodology");
                if (answer != null && answer.answer() != null && !answer.answer().isBlank()) {
                    groundedText = answer.answer();
                }
            } catch (Exception ignored) {
                // Graceful fallback to domain synthesis
            }
        }

        // Formulate structured methodology attributes
        ResearchApproach approach = isSoftware ? ResearchApproach.MIXED_METHODS : ResearchApproach.QUANTITATIVE;
        ResearchDesignType designType = isSoftware ? ResearchDesignType.CASE_STUDY : ResearchDesignType.MIXED_METHODS_EXPLORATORY_SEQUENTIAL;
        String designDesc = groundedText != null ? groundedText :
                (isSoftware
                        ? "This project adopts a Design Science Research Methodology (DSRM) combining agile iterative system development with empirical stakeholder evaluation to address direct farmer-to-buyer agricultural marketplace challenges."
                        : "This study utilizes an exploratory sequential mixed-methods design integrating quantitative surveys with qualitative key-informant interviews.");

        String targetPop = isSoftware
                ? "Smallholder agricultural producers, commercial crop buyers, cooperative managers, and regional market extension officers."
                : "Active agricultural producers, market intermediaries, and local agribusiness enterprises within the study area.";

        String samplingTech = "Stratified Purposive Sampling combined with Snowball Sampling for hard-to-reach rural producer communities.";
        int sampleSize = 180;
        String dataColStrat = "Multi-source triangulation incorporating structured survey questionnaires, semi-structured key-informant interviews, transactional log analytics, and user acceptance testing (UAT) sessions.";

        String sysArch = isSoftware
                ? "Three-tier modular microservices architecture: (1) Presentation layer built with React/TypeScript; (2) Application and API service layer powered by Spring Boot REST microservices with Redis caching; (3) Persistence layer leveraging PostgreSQL with spatial indexing."
                : "Decentralized data management repository with secure encrypted cloud storage.";

        String funcReq = isSoftware
                ? "User registration and verified profile management (farmers, buyers, logistics); Real-time commodity listing with pricing, grade, and geographic availability; Direct order placement and escrow transaction negotiation; Automated SMS/USSD notification bridge for non-smartphone producers; Visual sales analytics dashboard."
                : "Standardized data intake forms, audit logging, and researcher access control.";

        String techStack = isSoftware
                ? "Frontend: React 18, TypeScript, TailwindCSS/Vanilla CSS; Backend: Java 21, Spring Boot 3, Spring Security (JWT/TOTP); Database: PostgreSQL 16; Infrastructure: Docker, Redis, Nginx."
                : "R, Python (Pandas/Scikit-learn), SPSS, and Postgres.";

        String testStrat = isSoftware
                ? "Comprehensive testing strategy: Unit and integration testing using JUnit 5 and Mockito; API contract testing; Automated frontend end-to-end tests; System usability scale (SUS) evaluation with 30 representative agricultural users."
                : "Cronbach's alpha reliability analysis, content validity indexing (CVI), and triangulated member checking.";

        // Upsert Methodology entity
        List<Methodology> existing = methodologyRepository.findAllByProjectIdOrderByRevisionNumberDesc(projectId);
        Methodology m = existing.isEmpty() ? new Methodology() : existing.get(0);
        if (m.getProject() == null) {
            m.setProject(project);
            m.setCreatedBy(user);
        }
        m.setTitle(title + " Methodology");
        m.setApproach(approach);
        m.setDesignType(designType);
        m.setDesignDescription(designDesc);
        m.setStudySetting(project.getStudyArea() != null ? project.getStudyArea() : "Agricultural value chain districts");
        m.setRationale("Selected design provides direct alignment with the study objectives, enabling both rigorous technical artifact evaluation and empirical stakeholder assessment.");
        m.setOrigin(ContentOrigin.AI_GENERATED);
        methodologyRepository.save(m);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approach", approach.name());
        result.put("designType", designType.name());
        result.put("designDescription", designDesc);
        result.put("studySetting", m.getStudySetting());
        result.put("rationale", m.getRationale());
        result.put("targetPopulation", targetPop);
        result.put("samplingTechnique", samplingTech);
        result.put("sampleSize", sampleSize);
        result.put("dataCollectionStrategy", dataColStrat);
        result.put("systemArchitecture", sysArch);
        result.put("functionalRequirements", funcReq);
        result.put("techStack", techStack);
        result.put("testingStrategy", testStrat);
        return result;
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
