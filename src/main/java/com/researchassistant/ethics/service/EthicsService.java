package com.researchassistant.ethics.service;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.ethics.dto.EthicsDtos.*;
import com.researchassistant.ethics.model.*;
import com.researchassistant.ethics.repository.ConsentFormRepository;
import com.researchassistant.ethics.repository.ConsentFormRevisionRepository;
import com.researchassistant.ethics.repository.EthicsApprovalRepository;
import com.researchassistant.ethics.repository.EthicsProtocolRepository;
import com.researchassistant.ethics.repository.ParticipantConsentRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.participant.model.Participant;
import com.researchassistant.participant.repository.ParticipantRepository;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EthicsService {
    private final EthicsProtocolRepository protocolRepository;
    private final EthicsApprovalRepository approvalRepository;
    private final ConsentFormRepository consentFormRepository;
    private final ConsentFormRevisionRepository revisionRepository;
    private final ParticipantConsentRepository participantConsentRepository;
    private final ParticipantRepository participantRepository;
    private final ProjectAuthorizationService authorizationService;

    public EthicsService(EthicsProtocolRepository protocolRepository, EthicsApprovalRepository approvalRepository,
                         ConsentFormRepository consentFormRepository, ConsentFormRevisionRepository revisionRepository,
                         ParticipantConsentRepository participantConsentRepository, ParticipantRepository participantRepository,
                         ProjectAuthorizationService authorizationService) {
        this.protocolRepository = protocolRepository;
        this.approvalRepository = approvalRepository;
        this.consentFormRepository = consentFormRepository;
        this.revisionRepository = revisionRepository;
        this.participantConsentRepository = participantConsentRepository;
        this.participantRepository = participantRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public EthicsProtocolResponse createProtocol(UUID projectId, User user, CreateEthicsProtocolRequest request) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        EthicsProtocol protocol = new EthicsProtocol();
        protocol.setProject(project);
        protocol.setTitle(request.title());
        protocol.setInstitutionName(request.institutionName());
        protocol.setReviewBoardName(request.reviewBoardName());
        protocol.setProtocolReference(request.protocolReference());
        protocol.setRiskLevel(request.riskLevel());
        protocol.setRiskDescription(request.riskDescription());
        protocol.setConfidentialityPlan(request.confidentialityPlan());
        protocol.setDataProtectionPlan(request.dataProtectionPlan());
        protocol.setRetentionPlan(request.retentionPlan());
        protocol.setWithdrawalProcedure(request.withdrawalProcedure());
        protocol.setVulnerablePopulationConsiderations(request.vulnerablePopulationConsiderations());
        protocol.setOrigin(request.origin() == null ? ContentOrigin.USER : request.origin());
        protocol.setCreatedBy(user);
        return EthicsProtocolResponse.from(protocolRepository.save(protocol));
    }

    @Transactional
    public EthicsApprovalResponse recordApproval(UUID protocolId, User user, RecordEthicsApprovalRequest request) {
        EthicsProtocol protocol = protocolRepository.findById(protocolId)
                .orElseThrow(() -> new ResourceNotFoundException("Ethics protocol not found."));
        authorizationService.requireProjectAdminAccess(protocol.getProject().getId(), user);
        EthicsApproval approval = new EthicsApproval();
        approval.setProtocol(protocol);
        approval.setApprovalReference(request.approvalReference());
        approval.setApprovalDate(request.approvalDate());
        approval.setExpiryDate(request.expiryDate());
        approval.setApprovingBody(request.approvingBody());
        approval.setConditions(request.conditions());
        approval.setNotes(request.notes());
        approval.setRecordedBy(user);
        return EthicsApprovalResponse.from(approvalRepository.save(approval));
    }

    @Transactional
    public ConsentFormResponse createConsentForm(UUID projectId, User user, CreateConsentFormRequest request) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        ConsentForm form = new ConsentForm();
        form.setProject(project);
        form.setTitle(request.title());
        form.setLanguageCode(request.languageCode() == null ? "en" : request.languageCode());
        form.setCreatedBy(user);
        return ConsentFormResponse.from(consentFormRepository.save(form));
    }

    @Transactional
    public ConsentRevisionResponse createRevision(UUID formId, User user, CreateConsentRevisionRequest request) {
        ConsentForm form = consentFormRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent form not found."));
        authorizationService.requireProjectEditor(form.getProject().getId(), user);
        ConsentFormRevision revision = new ConsentFormRevision();
        revision.setConsentForm(form);
        revision.setRevisionNumber((int) revisionRepository.countByConsentFormId(formId) + 1);
        revision.setIntroduction(request.introduction());
        revision.setStudyPurpose(request.studyPurpose());
        revision.setProcedures(request.procedures());
        revision.setRisks(request.risks());
        revision.setBenefits(request.benefits());
        revision.setConfidentiality(request.confidentiality());
        revision.setVoluntaryParticipation(request.voluntaryParticipation());
        revision.setWithdrawalRights(request.withdrawalRights());
        revision.setContactInformation(request.contactInformation());
        revision.setDataUsageStatement(request.dataUsageStatement());
        revision.setDataRetentionStatement(request.dataRetentionStatement());
        revision.setConsentStatement(request.consentStatement());
        revision.setOrigin(request.origin() == null ? ContentOrigin.USER : request.origin());
        revision.setCreatedBy(user);
        return ConsentRevisionResponse.from(revisionRepository.save(revision));
    }

    @Transactional
    public ConsentFormResponse activateConsentForm(UUID formId, User user) {
        ConsentForm form = consentFormRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent form not found."));
        authorizationService.requireProjectEditor(form.getProject().getId(), user);
        form.setStatus(ConsentForm.Status.ACTIVE);
        return ConsentFormResponse.from(form);
    }

    @Transactional
    public ParticipantConsentResponse recordConsent(UUID participantId, User user, RecordParticipantConsentRequest request) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found."));
        authorizationService.requireProjectEditor(participant.getProject().getId(), user);
        ConsentFormRevision revision = revisionRepository.findById(request.consentRevisionId())
                .orElseThrow(() -> new ResourceNotFoundException("Consent revision not found."));
        if (!revision.getConsentForm().getProject().getId().equals(participant.getProject().getId())) {
            throw new IllegalArgumentException("Consent revision belongs to another project.");
        }
        ParticipantConsent consent = new ParticipantConsent();
        consent.setProject(participant.getProject());
        consent.setParticipant(participant);
        consent.setConsentRevision(revision);
        consent.setDecision(request.decision());
        consent.setMethod(request.method());
        consent.setConsentedAt(request.consentedAt() == null ? OffsetDateTime.now() : request.consentedAt());
        consent.setWitnessCode(request.witnessCode());
        consent.setRecordedBy(user);
        return ParticipantConsentResponse.from(participantConsentRepository.save(consent));
    }

    @Transactional
    public ParticipantConsentResponse recordWithdrawal(UUID participantId, User user) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found."));
        authorizationService.requireProjectEditor(participant.getProject().getId(), user);
        ParticipantConsent latest = participantConsentRepository.findFirstByParticipantIdOrderByConsentedAtDesc(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Prior consent not found."));
        ParticipantConsent withdrawal = new ParticipantConsent();
        withdrawal.setProject(participant.getProject());
        withdrawal.setParticipant(participant);
        withdrawal.setConsentRevision(latest.getConsentRevision());
        withdrawal.setDecision(ParticipantConsent.Decision.WITHDRAWN);
        withdrawal.setMethod(latest.getMethod());
        withdrawal.setConsentedAt(OffsetDateTime.now());
        withdrawal.setWithdrawnAt(OffsetDateTime.now());
        withdrawal.setRecordedBy(user);
        return ParticipantConsentResponse.from(participantConsentRepository.save(withdrawal));
    }

    @Transactional(readOnly = true)
    public List<ParticipantConsentResponse> participantConsents(UUID participantId, User user) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found."));
        authorizationService.requireProjectViewer(participant.getProject().getId(), user);
        return participantConsentRepository.findAllByParticipantIdOrderByConsentedAtDesc(participantId)
                .stream().map(ParticipantConsentResponse::from).toList();
    }
}
