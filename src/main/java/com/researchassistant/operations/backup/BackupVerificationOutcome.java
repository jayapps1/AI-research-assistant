package com.researchassistant.operations.backup;

public record BackupVerificationOutcome(
        boolean valid,
        String code,
        String message
) {

    public static BackupVerificationOutcome valid(String message) {
        return new BackupVerificationOutcome(true, "VALID", message);
    }

    public static BackupVerificationOutcome invalid(
            String code,
            String message
    ) {
        return new BackupVerificationOutcome(false, code, message);
    }
}
