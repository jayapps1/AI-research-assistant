package com.researchassistant.ethics.service;

import com.researchassistant.ethics.model.ParticipantConsent;
import com.researchassistant.ethics.repository.ParticipantConsentRepository;
import com.researchassistant.participant.model.Participant;
import org.springframework.stereotype.Service;

@Service
public class ConsentPolicyService {
    private final ParticipantConsentRepository consentRepository;

    public ConsentPolicyService(ParticipantConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
    }

    public void requireValidConsent(Participant participant) {
        ParticipantConsent latest = consentRepository.findFirstByParticipantIdOrderByConsentedAtDesc(participant.getId())
                .orElseThrow(() -> new IllegalStateException("Participant consent is required before data collection."));
        if (participant.getStatus() == Participant.Status.WITHDRAWN
                || latest.getDecision() != ParticipantConsent.Decision.CONSENTED
                || latest.getWithdrawnAt() != null) {
            throw new IllegalStateException("Participant does not have active consent.");
        }
    }
}
