package com.researchassistant.ethics.dto;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.ethics.model.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class EthicsDtos {
    private EthicsDtos() {}
    public record CreateEthicsProtocolRequest(@NotBlank String title, String institutionName, String reviewBoardName,
            String protocolReference, String riskLevel, String riskDescription, String confidentialityPlan,
            String dataProtectionPlan, String retentionPlan, String withdrawalProcedure,
            String vulnerablePopulationConsiderations, ContentOrigin origin) {}
    public record RecordEthicsApprovalRequest(@NotBlank String approvalReference, LocalDate approvalDate,
            LocalDate expiryDate, String approvingBody, String conditions, String notes) {}
    public record CreateConsentFormRequest(@NotBlank String title, String languageCode) {}
    public record CreateConsentRevisionRequest(@NotBlank String introduction, @NotBlank String studyPurpose,
            @NotBlank String procedures, @NotBlank String risks, @NotBlank String benefits,
            @NotBlank String confidentiality, @NotBlank String voluntaryParticipation,
            @NotBlank String withdrawalRights, String contactInformation, String dataUsageStatement,
            String dataRetentionStatement, @NotBlank String consentStatement, ContentOrigin origin) {}
    public record RecordParticipantConsentRequest(@NotNull UUID consentRevisionId,
            @NotNull ParticipantConsent.Decision decision, @NotNull ParticipantConsent.Method method,
            OffsetDateTime consentedAt, String witnessCode) {}
    public record EthicsProtocolResponse(UUID id, UUID projectId, String title, EthicsProtocol.Status status,
            String protocolReference, OffsetDateTime createdAt) {
        public static EthicsProtocolResponse from(EthicsProtocol p) {
            return new EthicsProtocolResponse(p.getId(), p.getProject().getId(), p.getTitle(), p.getStatus(), p.getProtocolReference(), p.getCreatedAt());
        }
    }
    public record EthicsApprovalResponse(UUID id, UUID protocolId, String approvalReference, LocalDate approvalDate,
            LocalDate expiryDate, String approvingBody, OffsetDateTime createdAt) {
        public static EthicsApprovalResponse from(EthicsApproval a) {
            return new EthicsApprovalResponse(a.getId(), a.getProtocol().getId(), a.getApprovalReference(), a.getApprovalDate(), a.getExpiryDate(), a.getApprovingBody(), a.getCreatedAt());
        }
    }
    public record ConsentFormResponse(UUID id, UUID projectId, String title, ConsentForm.Status status, String languageCode) {
        public static ConsentFormResponse from(ConsentForm f) {
            return new ConsentFormResponse(f.getId(), f.getProject().getId(), f.getTitle(), f.getStatus(), f.getLanguageCode());
        }
    }
    public record ConsentRevisionResponse(UUID id, UUID consentFormId, int revisionNumber, String consentStatement) {
        public static ConsentRevisionResponse from(ConsentFormRevision r) {
            return new ConsentRevisionResponse(r.getId(), r.getConsentForm().getId(), r.getRevisionNumber(), r.getConsentStatement());
        }
    }
    public record ParticipantConsentResponse(UUID id, UUID participantId, UUID consentRevisionId,
            ParticipantConsent.Decision decision, ParticipantConsent.Method method, OffsetDateTime consentedAt,
            OffsetDateTime withdrawnAt) {
        public static ParticipantConsentResponse from(ParticipantConsent c) {
            return new ParticipantConsentResponse(c.getId(), c.getParticipant().getId(), c.getConsentRevision().getId(), c.getDecision(), c.getMethod(), c.getConsentedAt(), c.getWithdrawnAt());
        }
    }
    public record EthicsReadinessResponse(String status, List<String> blockingIssues, List<String> warnings) {}
}
