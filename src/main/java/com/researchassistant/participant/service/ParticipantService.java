package com.researchassistant.participant.service;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.ethics.model.ParticipantConsent;
import com.researchassistant.ethics.repository.ParticipantConsentRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.methodology.entity.SamplingPlan;
import com.researchassistant.methodology.entity.StudyPopulation;
import com.researchassistant.methodology.repository.SamplingPlanRepository;
import com.researchassistant.methodology.repository.StudyPopulationRepository;
import com.researchassistant.participant.dto.ParticipantDtos.*;
import com.researchassistant.participant.model.Participant;
import com.researchassistant.participant.model.ParticipantEligibilityAssessment;
import com.researchassistant.participant.model.ParticipantIdentity;
import com.researchassistant.participant.repository.ParticipantEligibilityAssessmentRepository;
import com.researchassistant.participant.repository.ParticipantIdentityRepository;
import com.researchassistant.participant.repository.ParticipantRepository;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.project.service.ProjectAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ParticipantService {
    private final ParticipantRepository participantRepository;
    private final ParticipantIdentityRepository identityRepository;
    private final ParticipantEligibilityAssessmentRepository eligibilityRepository;
    private final ParticipantConsentRepository consentRepository;
    private final ResearchProjectRepository projectRepository;
    private final StudyPopulationRepository populationRepository;
    private final SamplingPlanRepository samplingPlanRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ResearchDataEncryptionService encryptionService;

    public ParticipantService(ParticipantRepository participantRepository,
                              ParticipantIdentityRepository identityRepository,
                              ParticipantEligibilityAssessmentRepository eligibilityRepository,
                              ParticipantConsentRepository consentRepository,
                              ResearchProjectRepository projectRepository,
                              StudyPopulationRepository populationRepository,
                              SamplingPlanRepository samplingPlanRepository,
                              ProjectAuthorizationService authorizationService,
                              ResearchDataEncryptionService encryptionService) {
        this.participantRepository = participantRepository;
        this.identityRepository = identityRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.consentRepository = consentRepository;
        this.projectRepository = projectRepository;
        this.populationRepository = populationRepository;
        this.samplingPlanRepository = samplingPlanRepository;
        this.authorizationService = authorizationService;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public ParticipantResponse create(UUID projectId, User user, CreateParticipantRequest request) {
        authorizationService.requireProjectEditor(projectId, user);
        ResearchProject project = projectRepository.findByIdForDocumentNumberAllocation(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
        Participant participant = new Participant();
        participant.setProject(project);
        participant.setParticipantCode("P-%06d".formatted(project.getNextParticipantNumber()));
        project.setNextParticipantNumber(project.getNextParticipantNumber() + 1);
        participant.setStatus(request.status() == null ? Participant.Status.ENROLLED : request.status());
        participant.setPopulation(resolvePopulation(request.populationId(), projectId));
        participant.setSamplingPlan(resolveSamplingPlan(request.samplingPlanId(), projectId));
        participant.setEnrolledBy(user);
        Participant saved = participantRepository.save(participant);
        if (request.identity() != null) {
            ParticipantIdentity identity = new ParticipantIdentity();
            identity.setParticipant(saved);
            identity.setEncryptedFullName(encryptionService.encryptNullable(request.identity().fullName()));
            identity.setEncryptedEmail(encryptionService.encryptNullable(request.identity().email()));
            identity.setEncryptedPhone(encryptionService.encryptNullable(request.identity().phone()));
            identity.setEncryptedExternalIdentifier(encryptionService.encryptNullable(request.identity().externalIdentifier()));
            identityRepository.save(identity);
        }
        return ParticipantResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ParticipantResponse> list(UUID projectId, User user, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, user);
        return participantRepository.findAllByProjectId(projectId, pageable).map(ParticipantResponse::from);
    }

    @Transactional(readOnly = true)
    public ParticipantResponse get(UUID participantId, User user) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found."));
        authorizationService.requireProjectViewer(participant.getProject().getId(), user);
        return ParticipantResponse.from(participant);
    }

    @Transactional
    public ParticipantResponse update(UUID participantId, User user, UpdateParticipantRequest request) {
        Participant participant = loadForEdit(participantId, user);
        if (request.status() != null) participant.setStatus(request.status());
        participant.setPopulation(resolvePopulation(request.populationId(), participant.getProject().getId()));
        participant.setSamplingPlan(resolveSamplingPlan(request.samplingPlanId(), participant.getProject().getId()));
        return ParticipantResponse.from(participant);
    }

    @Transactional
    public ParticipantResponse withdraw(UUID participantId, User user) {
        Participant participant = loadForEdit(participantId, user);
        participant.setStatus(Participant.Status.WITHDRAWN);
        participant.setWithdrawnAt(OffsetDateTime.now());
        return ParticipantResponse.from(participant);
    }

    @Transactional
    public void assessEligibility(UUID participantId, User user, EligibilityRequest request) {
        Participant participant = loadForEdit(participantId, user);
        ParticipantEligibilityAssessment assessment = new ParticipantEligibilityAssessment();
        assessment.setParticipant(participant);
        assessment.setEligible(request.eligible());
        assessment.setReasonCode(request.reasonCode());
        assessment.setNotes(request.notes());
        assessment.setAssessedBy(user);
        eligibilityRepository.save(assessment);
        participant.setStatus(request.eligible() ? Participant.Status.ELIGIBLE : Participant.Status.EXCLUDED);
    }

    @Transactional(readOnly = true)
    public List<ParticipantConsent> consents(UUID participantId, User user) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found."));
        authorizationService.requireProjectViewer(participant.getProject().getId(), user);
        return consentRepository.findAllByParticipantIdOrderByConsentedAtDesc(participantId);
    }

    private Participant loadForEdit(UUID participantId, User user) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found."));
        authorizationService.requireProjectEditor(participant.getProject().getId(), user);
        return participant;
    }

    private StudyPopulation resolvePopulation(UUID populationId, UUID projectId) {
        if (populationId == null) return null;
        StudyPopulation population = populationRepository.findById(populationId)
                .orElseThrow(() -> new ResourceNotFoundException("Population not found."));
        if (!population.getMethodology().getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Population belongs to another project.");
        }
        return population;
    }

    private SamplingPlan resolveSamplingPlan(UUID samplingPlanId, UUID projectId) {
        if (samplingPlanId == null) return null;
        SamplingPlan plan = samplingPlanRepository.findById(samplingPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Sampling plan not found."));
        if (!plan.getMethodology().getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Sampling plan belongs to another project.");
        }
        return plan;
    }
}
