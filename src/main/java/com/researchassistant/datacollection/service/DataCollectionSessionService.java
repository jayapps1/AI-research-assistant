package com.researchassistant.datacollection.service;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.datacollection.dto.DataCollectionDtos.*;
import com.researchassistant.datacollection.model.DataCollectionSession;
import com.researchassistant.datacollection.model.InstrumentResponse;
import com.researchassistant.datacollection.model.InstrumentResponseOption;
import com.researchassistant.datacollection.repository.DataCollectionSessionRepository;
import com.researchassistant.datacollection.repository.InstrumentResponseOptionRepository;
import com.researchassistant.datacollection.repository.InstrumentResponseRepository;
import com.researchassistant.ethics.service.ConsentPolicyService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.instruments.entity.ResearchInstrument;
import com.researchassistant.instruments.repository.ResearchInstrumentRepository;
import com.researchassistant.participant.model.Participant;
import com.researchassistant.participant.repository.ParticipantRepository;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.project.service.ProjectAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DataCollectionSessionService {
    private final DataCollectionSessionRepository sessionRepository;
    private final InstrumentResponseRepository responseRepository;
    private final InstrumentResponseOptionRepository optionRepository;
    private final ResearchInstrumentRepository instrumentRepository;
    private final ParticipantRepository participantRepository;
    private final ResearchProjectRepository projectRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ConsentPolicyService consentPolicyService;

    public DataCollectionSessionService(DataCollectionSessionRepository sessionRepository,
                                        InstrumentResponseRepository responseRepository,
                                        InstrumentResponseOptionRepository optionRepository,
                                        ResearchInstrumentRepository instrumentRepository,
                                        ParticipantRepository participantRepository,
                                        ResearchProjectRepository projectRepository,
                                        ProjectAuthorizationService authorizationService,
                                        ConsentPolicyService consentPolicyService) {
        this.sessionRepository = sessionRepository;
        this.responseRepository = responseRepository;
        this.optionRepository = optionRepository;
        this.instrumentRepository = instrumentRepository;
        this.participantRepository = participantRepository;
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
        this.consentPolicyService = consentPolicyService;
    }

    @Transactional
    public SessionResponse create(UUID projectId, User user, CreateSessionRequest request) {
        authorizationService.requireProjectEditor(projectId, user);
        ResearchProject project = projectRepository.findByIdForDocumentNumberAllocation(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
        ResearchInstrument instrument = instrumentRepository.findById(request.instrumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found."));
        if (!instrument.getProject().getId().equals(projectId)) throw new IllegalArgumentException("Instrument belongs to another project.");
        Participant participant = null;
        if (request.participantId() != null) {
            participant = participantRepository.findById(request.participantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Participant not found."));
            if (!participant.getProject().getId().equals(projectId)) throw new IllegalArgumentException("Participant belongs to another project.");
        }
        DataCollectionSession session = new DataCollectionSession();
        session.setProject(project);
        session.setInstrument(instrument);
        session.setInstrumentRevisionNumber(instrument.getRevisionNumber());
        session.setMethod(instrument.getDataCollectionMethod());
        session.setParticipant(participant);
        session.setType(request.type() == null ? inferType(instrument.getType().name()) : request.type());
        session.setScheduledAt(request.scheduledAt());
        session.setLocationOrSetting(request.locationOrSetting());
        session.setNotes(request.notes());
        session.setSessionCode("SES-%06d".formatted(project.getNextSessionNumber()));
        project.setNextSessionNumber(project.getNextSessionNumber() + 1);
        return SessionResponse.from(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public Page<SessionResponse> list(UUID projectId, User user, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, user);
        return sessionRepository.findAllByProjectId(projectId, pageable).map(SessionResponse::from);
    }

    @Transactional(readOnly = true)
    public SessionResponse get(UUID sessionId, User user) {
        DataCollectionSession session = load(sessionId);
        authorizationService.requireProjectViewer(session.getProject().getId(), user);
        return SessionResponse.from(session);
    }

    @Transactional
    public SessionResponse start(UUID sessionId, User user) {
        DataCollectionSession session = loadForEdit(sessionId, user);
        if (session.getStatus() != DataCollectionSession.Status.SCHEDULED) throw new IllegalStateException("Only scheduled sessions can be started.");
        if (session.getParticipant() != null) consentPolicyService.requireValidConsent(session.getParticipant());
        session.setStatus(DataCollectionSession.Status.IN_PROGRESS);
        session.setStartedAt(OffsetDateTime.now());
        session.setCollectedBy(user);
        return SessionResponse.from(session);
    }

    @Transactional
    public void recordResponse(UUID sessionId, User user, RecordResponseRequest request) {
        DataCollectionSession session = loadForEdit(sessionId, user);
        if (session.getStatus() == DataCollectionSession.Status.COMPLETED || session.getStatus() == DataCollectionSession.Status.INVALIDATED) {
            throw new IllegalStateException("Completed or invalidated session responses cannot be silently edited.");
        }
        InstrumentResponse response = new InstrumentResponse();
        response.setSession(session);
        response.setInstrumentItemType(request.itemType());
        response.setInstrumentItemId(request.itemId());
        response.setType(request.type());
        response.setTextValue(request.textValue());
        response.setNumericValue(request.numericValue());
        response.setBooleanValue(request.booleanValue());
        response.setDateValue(request.dateValue());
        response.setOptionValue(request.optionValue());
        response.setRawValue(request.rawValue());
        response.setRecordedBy(user);
        InstrumentResponse saved = responseRepository.save(response);
        if (request.options() != null) {
            int order = 1;
            for (ResponseOptionRequest option : request.options()) {
                InstrumentResponseOption entity = new InstrumentResponseOption();
                entity.setResponse(saved);
                entity.setOptionValue(option.value());
                entity.setOptionLabel(option.label());
                entity.setDisplayOrder(order++);
                optionRepository.save(entity);
            }
        }
    }

    @Transactional
    public SessionResponse complete(UUID sessionId, User user) {
        DataCollectionSession session = loadForEdit(sessionId, user);
        if (session.getStatus() != DataCollectionSession.Status.IN_PROGRESS && session.getStatus() != DataCollectionSession.Status.SCHEDULED) {
            throw new IllegalStateException("Session cannot be completed from current state.");
        }
        session.setStatus(DataCollectionSession.Status.COMPLETED);
        if (session.getStartedAt() == null) session.setStartedAt(OffsetDateTime.now());
        session.setCompletedAt(OffsetDateTime.now());
        return SessionResponse.from(session);
    }

    @Transactional
    public SessionResponse invalidate(UUID sessionId, User user) {
        DataCollectionSession session = loadForEdit(sessionId, user);
        session.setStatus(DataCollectionSession.Status.INVALIDATED);
        return SessionResponse.from(session);
    }

    private DataCollectionSession load(UUID id) {
        return sessionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Data collection session not found."));
    }

    private DataCollectionSession loadForEdit(UUID id, User user) {
        DataCollectionSession session = load(id);
        authorizationService.requireProjectEditor(session.getProject().getId(), user);
        return session;
    }

    private DataCollectionSession.Type inferType(String instrumentType) {
        if (instrumentType.equals("QUESTIONNAIRE")) return DataCollectionSession.Type.QUESTIONNAIRE;
        if (instrumentType.equals("INTERVIEW_GUIDE")) return DataCollectionSession.Type.INTERVIEW;
        if (instrumentType.equals("FOCUS_GROUP_GUIDE")) return DataCollectionSession.Type.FOCUS_GROUP;
        if (instrumentType.equals("OBSERVATION_CHECKLIST")) return DataCollectionSession.Type.OBSERVATION;
        return DataCollectionSession.Type.OTHER;
    }
}
