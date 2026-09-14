package com.researchassistant.operations.backup;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackupPolicyService {

    private final BackupPolicyRepository policyRepository;
    private final BackupProperties properties;

    public BackupPolicyService(
            BackupPolicyRepository policyRepository,
            BackupProperties properties
    ) {
        this.policyRepository = policyRepository;
        this.properties = properties;
    }

    @Transactional
    public BackupPolicy createDefaultPolicy(String name) {
        BackupPolicy policy = new BackupPolicy();
        policy.setName(name);
        policy.setStatus(BackupPolicyStatus.ACTIVE);
        policy.setFrequency(BackupFrequency.DAILY);
        policy.setRetentionDaily(properties.retention().daily());
        policy.setRetentionWeekly(properties.retention().weekly());
        policy.setRetentionMonthly(properties.retention().monthly());
        policy.setDatabaseBackupEnabled(true);
        policy.setObjectStorageBackupEnabled(true);
        policy.setVerificationRequired(properties.verification().required());
        policy.setTargetRpoMinutes(properties.target().rpoMinutes());
        policy.setTargetRtoMinutes(properties.target().rtoMinutes());
        validate(policy);
        return policyRepository.save(policy);
    }

    public void validate(BackupPolicy policy) {
        if (policy.getRetentionDaily() == null
                || policy.getRetentionDaily() < 0
                || policy.getRetentionWeekly() == null
                || policy.getRetentionWeekly() < 0
                || policy.getRetentionMonthly() == null
                || policy.getRetentionMonthly() < 0) {
            throw new IllegalArgumentException(
                    "Backup retention values must be zero or greater."
            );
        }
        if (policy.getTargetRpoMinutes() != null
                && policy.getTargetRpoMinutes() <= 0) {
            throw new IllegalArgumentException(
                    "Backup RPO target must be greater than zero."
            );
        }
        if (policy.getTargetRtoMinutes() != null
                && policy.getTargetRtoMinutes() <= 0) {
            throw new IllegalArgumentException(
                    "Backup RTO target must be greater than zero."
            );
        }
    }
}
