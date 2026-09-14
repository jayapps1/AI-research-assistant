package com.researchassistant.instruments.service;

import com.researchassistant.instruments.dto.InstrumentResponses.InstrumentValidationResult;
import com.researchassistant.instruments.entity.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ResearchInstrumentValidationService {
    private final EntityManager entityManager;

    public ResearchInstrumentValidationService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public InstrumentValidationResult validate(UUID instrumentId) {
        ResearchInstrument instrument = entityManager.find(ResearchInstrument.class, instrumentId);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> info = new ArrayList<>();
        if (instrument == null) {
            errors.add("Instrument not found.");
            return new InstrumentValidationResult(false, errors, warnings, info);
        }
        switch (instrument.getType()) {
            case QUESTIONNAIRE -> validateQuestionnaire(instrumentId, errors, warnings);
            case INTERVIEW_GUIDE -> validateCount("Interview guide must have questions.", count("select count(q) from InterviewQuestion q where q.section.guide.instrument.id = :id", instrumentId), errors);
            case FOCUS_GROUP_GUIDE -> validateCount("Focus group guide must have questions.", count("select count(q) from FocusGroupQuestion q where q.section.guide.instrument.id = :id", instrumentId), errors);
            case OBSERVATION_CHECKLIST -> validateCount("Observation checklist must have observable items.", count("select count(i) from ObservationItem i where i.section.checklist.instrument.id = :id", instrumentId), errors);
        }
        if (errors.isEmpty()) info.add("Instrument has the minimum structural content for review.");
        return new InstrumentValidationResult(errors.isEmpty(), errors, warnings, info);
    }

    private void validateQuestionnaire(UUID instrumentId, List<String> errors, List<String> warnings) {
        validateCount("Questionnaire must have at least one section.", count("select count(s) from QuestionnaireSection s where s.questionnaire.instrument.id = :id", instrumentId), errors);
        validateCount("Questionnaire must have at least one item.", count("select count(i) from QuestionnaireItem i where i.section.questionnaire.instrument.id = :id", instrumentId), errors);
        Long invalidChoice = count("select count(i) from QuestionnaireItem i where i.section.questionnaire.instrument.id = :id and i.type in ('SINGLE_CHOICE','MULTIPLE_CHOICE') and not exists (select o.id from QuestionnaireOption o where o.item = i)", instrumentId);
        if (invalidChoice > 0) errors.add("Choice questionnaire items must have options.");
        Long invalidLikert = count("select count(i) from QuestionnaireItem i where i.section.questionnaire.instrument.id = :id and i.type = 'LIKERT' and i.responseScale is null", instrumentId);
        if (invalidLikert > 0) errors.add("Likert questionnaire items must reference a response scale.");
        warnings.add("Semantic duplicate detection is deferred; exact duplicate checks only are currently planned.");
    }

    private void validateCount(String message, Long count, List<String> errors) {
        if (count == null || count == 0) errors.add(message);
    }

    private Long count(String jpql, UUID id) {
        return entityManager.createQuery(jpql, Long.class).setParameter("id", id).getSingleResult();
    }
}
