package com.researchassistant.operations.backup;

public record BackupProviderCapabilities(
        String providerName,
        boolean createsBackups,
        boolean verifiesChecksum,
        boolean supportsRestore,
        boolean supportsPointInTimeRecovery,
        String notes
) {
}
