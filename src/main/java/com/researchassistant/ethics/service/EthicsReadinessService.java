package com.researchassistant.ethics.service;

import com.researchassistant.ethics.dto.EthicsDtos.EthicsReadinessResponse;
import com.researchassistant.ethics.model.ConsentForm;
import com.researchassistant.ethics.model.EthicsApproval;
import com.researchassistant.ethics.model.EthicsProtocol;
import com.researchassistant.ethics.repository.ConsentFormRepository;
import com.researchassistant.ethics.repository.EthicsApprovalRepository;
import com.researchassistant.ethics.repository.EthicsProtocolRepository;
import com.researchassistant.instruments.entity.ResearchInstrumentStatus;
import com.researchassistant.instruments.repository.ResearchInstrumentRepository;
import com.researchassistant.methodology.entity.MethodologyStatus;
import com.researchassistant.methodology.repository.MethodologyRepository;
import com.researchassistant.methodology.repository.SamplingPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EthicsReadinessService {
    private final EthicsProtocolRepository protocolRepository;
    private final EthicsApprovalRepository approvalRepository;
    private final ConsentFormRepository consentFormRepository;
    private final ResearchInstrumentRepository instrumentRepository;
    private final MethodologyRepository methodologyRepository;
    private final SamplingPlanRepository samplingPlanRepository;

    public EthicsReadinessService(EthicsProtocolRepository protocolRepository, EthicsApprovalRepository approvalRepository,
                                  ConsentFormRepository consentFormRepository, ResearchInstrumentRepository instrumentRepository,
                                  MethodologyRepository methodologyRepository, SamplingPlanRepository samplingPlanRepository) {
        this.protocolRepository = protocolRepository;
        this.approvalRepository = approvalRepository;
        this.consentFormRepository = consentFormRepository;
        this.instrumentRepository = instrumentRepository;
        this.methodologyRepository = methodologyRepository;
        this.samplingPlanRepository = samplingPlanRepository;
    }

    public EthicsReadinessResponse evaluate(UUID projectId) {
        var blocking = new ArrayList<String>();
        var warnings = new ArrayList<String>();
        var protocol = protocolRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId);
        if (protocol.isEmpty()) {
            blocking.add("No ethics protocol is recorded for this project.");
        } else {
            EthicsProtocol p = protocol.get();
            if (p.getStatus() != EthicsProtocol.Status.APPROVED && p.getStatus() != EthicsProtocol.Status.CONDITIONALLY_APPROVED) {
                blocking.add("Latest ethics protocol is not approved or conditionally approved.");
            }
            var approval = approvalRepository.findFirstByProtocolIdOrderByCreatedAtDesc(p.getId());
            if (approval.isEmpty()) {
                blocking.add("No manual ethics approval record exists.");
            } else if (isExpired(approval.get())) {
                blocking.add("Ethics approval has expired.");
            }
            if (p.getConfidentialityPlan() == null || p.getConfidentialityPlan().isBlank()) warnings.add("Confidentiality plan is not documented.");
            if (p.getDataProtectionPlan() == null || p.getDataProtectionPlan().isBlank()) warnings.add("Data protection plan is not documented.");
            if (p.getWithdrawalProcedure() == null || p.getWithdrawalProcedure().isBlank()) warnings.add("Withdrawal procedure is not documented.");
        }
        if (!consentFormRepository.existsByProjectIdAndStatus(projectId, ConsentForm.Status.ACTIVE)) {
            blocking.add("No active consent form exists.");
        }
        if (methodologyRepository.findByProjectIdAndStatus(projectId, MethodologyStatus.ACTIVE).isEmpty()) {
            blocking.add("No active methodology exists.");
        }
        if (samplingPlanRepository.findAll().stream().noneMatch(p -> p.getMethodology().getProject().getId().equals(projectId))) {
            blocking.add("No sampling plan exists.");
        }
        boolean activeInstrument = instrumentRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .anyMatch(i -> i.getStatus() == ResearchInstrumentStatus.ACTIVE);
        if (!activeInstrument) blocking.add("No active research instrument exists.");
        String status = blocking.isEmpty() ? (warnings.isEmpty() ? "READY" : "WARNINGS") : "NOT_READY";
        return new EthicsReadinessResponse(status, blocking, warnings);
    }

    private boolean isExpired(EthicsApproval approval) {
        return approval.getExpiryDate() != null && approval.getExpiryDate().isBefore(LocalDate.now());
    }
}
