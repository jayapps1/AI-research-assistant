package com.researchassistant.publicsite.service;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.audit.AuditEventType;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.notification.NotificationProperties;
import com.researchassistant.publicsite.dto.*;
import com.researchassistant.publicsite.entity.*;
import com.researchassistant.publicsite.repository.ContactResponseRepository;
import com.researchassistant.publicsite.repository.ContactSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
public class ContactSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(ContactSubmissionService.class);
    private static final AtomicLong FALLBACK_SEQUENCE = new AtomicLong(1000);

    private final ContactSubmissionRepository submissionRepository;
    private final ContactResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final AuditEventService auditEventService;
    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;

    public ContactSubmissionService(ContactSubmissionRepository submissionRepository,
                                  ContactResponseRepository responseRepository,
                                  UserRepository userRepository,
                                  AuditEventService auditEventService,
                                  ObjectProvider<JavaMailSender> mailSenderProvider,
                                  ObjectProvider<NotificationProperties> propertiesProvider) {
        this.submissionRepository = submissionRepository;
        this.responseRepository = responseRepository;
        this.userRepository = userRepository;
        this.auditEventService = auditEventService;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.notificationProperties = propertiesProvider.getIfAvailable();
    }

    // =========================================================================
    // PUBLIC SUBMISSION
    // =========================================================================

    public ContactSubmissionResultResponse submitContactForm(ContactSubmissionRequest request,
                                                            String rawIpAddress,
                                                            String userAgent) {
        // Spam honeypot detection: If bot fills the hidden honeypot field, silently categorize as SPAM
        boolean isSpam = request.honeypot() != null && !request.honeypot().trim().isEmpty();

        String referenceCode = generateReferenceCode();
        String ipHash = hashIp(rawIpAddress);
        String safeUserAgent = userAgent != null && userAgent.length() > 255
                ? userAgent.substring(0, 255)
                : userAgent;

        ContactSubmission submission = new ContactSubmission();
        submission.setReferenceCode(referenceCode);
        submission.setName(request.name().trim());
        submission.setEmail(request.email().trim().toLowerCase());
        submission.setPhone(request.phone() != null ? request.phone().trim() : null);
        submission.setSubject(request.subject().trim());
        submission.setMessage(request.message().trim());
        submission.setStatus(isSpam ? ContactSubmissionStatus.SPAM : ContactSubmissionStatus.NEW);
        submission.setSource(ContactSubmissionSource.PUBLIC_WEBSITE);
        submission.setIpHash(ipHash);
        submission.setUserAgentSummary(safeUserAgent);
        submission.setSubmittedAt(OffsetDateTime.now());

        ContactSubmission saved = submissionRepository.save(submission);

        // Operational audit log without full message body
        auditEventService.record(null, "VISITOR", null, null,
                AuditEventType.CONTACT_SUBMISSION_RECEIVED, "ContactSubmission", saved.getId(),
                "{\"referenceCode\":\"" + referenceCode + "\",\"isSpam\":" + isSpam + "}");

        // If email delivery is configured and not spam, send acknowledgment
        if (!isSpam) {
            sendAcknowledgmentEmailSafe(saved);
        }

        return new ContactSubmissionResultResponse(referenceCode, "Message received.");
    }

    // =========================================================================
    // ADMIN OPERATIONS
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<AdminContactSubmissionResponse> searchSubmissions(ContactSubmissionStatus status,
                                                                 String search,
                                                                 Pageable pageable) {
        return submissionRepository.searchSubmissions(status, search, pageable)
                .map(AdminContactSubmissionResponse::fromEntity);
    }

    public AdminContactSubmissionResponse getSubmissionDetail(UUID id) {
        ContactSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact submission not found: " + id));

        if (submission.getStatus() == ContactSubmissionStatus.NEW) {
            submission.setStatus(ContactSubmissionStatus.READ);
            submission.setFirstReadAt(OffsetDateTime.now());
            submission = submissionRepository.save(submission);
        }

        return AdminContactSubmissionResponse.fromEntity(submission);
    }

    public AdminContactSubmissionResponse updateStatus(UUID id, ContactSubmissionStatus newStatus, User adminUser) {
        ContactSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact submission not found: " + id));

        submission.setStatus(newStatus);
        if (newStatus == ContactSubmissionStatus.CLOSED && submission.getClosedAt() == null) {
            submission.setClosedAt(OffsetDateTime.now());
        }

        ContactSubmission saved = submissionRepository.save(submission);

        if (newStatus == ContactSubmissionStatus.CLOSED) {
            auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                    AuditEventType.CONTACT_SUBMISSION_CLOSED, "ContactSubmission", saved.getId(),
                    "{\"referenceCode\":\"" + saved.getReferenceCode() + "\"}");
        }

        return AdminContactSubmissionResponse.fromEntity(saved);
    }

    public AdminContactSubmissionResponse assignSubmission(UUID id, UUID assignedToUserId, User adminUser) {
        ContactSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact submission not found: " + id));

        User assignee = null;
        if (assignedToUserId != null) {
            assignee = userRepository.findById(assignedToUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found for assignment: " + assignedToUserId));
        }

        submission.setAssignedTo(assignee);
        ContactSubmission saved = submissionRepository.save(submission);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.CONTACT_SUBMISSION_ASSIGNED, "ContactSubmission", saved.getId(),
                "{\"assignedTo\":\"" + (assignee != null ? assignee.getId() : "UNASSIGNED") + "\"}");

        return AdminContactSubmissionResponse.fromEntity(saved);
    }

    public AdminContactResponseDto respondToSubmission(UUID id, User adminUser, CreateContactReplyRequest request) {
        ContactSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact submission not found: " + id));

        ContactResponse response = new ContactResponse();
        response.setSubmission(submission);
        response.setRespondedBy(adminUser);
        response.setResponseMessage(request.message().trim());
        response.setChannel(request.channel());
        response.setStatus(ContactResponseStatus.DRAFT);

        if (request.channel() == ContactResponseChannel.INTERNAL_NOTE) {
            // Strictly internal note: never sent to the user
            response.setStatus(ContactResponseStatus.SENT);
            response.setSentAt(OffsetDateTime.now());
            if (submission.getStatus() == ContactSubmissionStatus.NEW || submission.getStatus() == ContactSubmissionStatus.READ) {
                submission.setStatus(ContactSubmissionStatus.IN_PROGRESS);
                submissionRepository.save(submission);
            }
        } else if (request.channel() == ContactResponseChannel.EMAIL) {
            // Outbound email to visitor
            boolean emailSent = sendStaffReplyEmail(submission, request.message().trim());
            if (emailSent) {
                response.setStatus(ContactResponseStatus.SENT);
                response.setSentAt(OffsetDateTime.now());
                submission.setStatus(ContactSubmissionStatus.RESPONDED);
                submissionRepository.save(submission);
            } else {
                response.setStatus(ContactResponseStatus.FAILED);
            }
        }

        ContactResponse saved = responseRepository.save(response);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.CONTACT_RESPONSE_SENT, "ContactResponse", saved.getId(),
                "{\"referenceCode\":\"" + submission.getReferenceCode() + "\",\"channel\":\"" + request.channel() + "\",\"status\":\"" + response.getStatus() + "\"}");

        return AdminContactResponseDto.fromEntity(saved);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private String generateReferenceCode() {
        int year = OffsetDateTime.now().getYear();
        long seqValue;
        try {
            Long nextVal = submissionRepository.getNextReferenceSequenceValue();
            seqValue = nextVal != null ? nextVal : FALLBACK_SEQUENCE.incrementAndGet();
        } catch (Exception e) {
            log.warn("Sequence nextval failed; using fallback counter: {}", e.getMessage());
            seqValue = FALLBACK_SEQUENCE.incrementAndGet();
        }
        return String.format("CNT-%d-%06d", year, seqValue);
    }

    private String hashIp(String rawIp) {
        if (rawIp == null || rawIp.isBlank()) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawIp.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendAcknowledgmentEmailSafe(ContactSubmission submission) {
        if (mailSender == null || notificationProperties == null ||
                notificationProperties.email() == null || !notificationProperties.email().enabled()) {
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(submission.getEmail());
            msg.setFrom(notificationProperties.email().fromAddress());
            msg.setSubject("We have received your message [" + submission.getReferenceCode() + "]");
            msg.setText("Hello " + submission.getName() + ",\n\n" +
                    "Thank you for contacting the AI Research Assistant team. We have received your inquiry.\n\n" +
                    "Your reference number is: " + submission.getReferenceCode() + "\n\n" +
                    "Our academic research support team will review your message and get back to you shortly.\n\n" +
                    "Regards,\nAI Research Assistant Support Team");
            mailSender.send(msg);
            log.info("Sent contact acknowledgment to {} for reference {}", submission.getEmail(), submission.getReferenceCode());
        } catch (Exception e) {
            log.warn("Failed to send contact acknowledgment email to {}: {}", submission.getEmail(), e.getMessage());
        }
    }

    private boolean sendStaffReplyEmail(ContactSubmission submission, String replyText) {
        if (mailSender == null || notificationProperties == null ||
                notificationProperties.email() == null || !notificationProperties.email().enabled()) {
            log.warn("Email sender is not configured or disabled; reply cannot be dispatched via EMAIL.");
            return false;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(submission.getEmail());
            msg.setFrom(notificationProperties.email().fromAddress());
            msg.setSubject("Re: " + submission.getSubject() + " [" + submission.getReferenceCode() + "]");
            msg.setText("Hello " + submission.getName() + ",\n\n" +
                    replyText + "\n\n" +
                    "---\n" +
                    "AI Research Assistant Support\n" +
                    "Reference: " + submission.getReferenceCode());
            mailSender.send(msg);
            log.info("Sent staff reply to {} for reference {}", submission.getEmail(), submission.getReferenceCode());
            return true;
        } catch (Exception e) {
            log.error("Failed to send staff reply email to {}: {}", submission.getEmail(), e.getMessage());
            return false;
        }
    }
}
