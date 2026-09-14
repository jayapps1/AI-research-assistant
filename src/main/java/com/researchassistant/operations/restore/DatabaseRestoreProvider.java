package com.researchassistant.operations.restore;

import com.researchassistant.operations.backup.BackupArtifactDescriptor;
import com.researchassistant.operations.backup.BackupVerificationOutcome;

public interface DatabaseRestoreProvider {

    BackupVerificationOutcome verifyRestoreInput(
            BackupArtifactDescriptor artifact,
            RestoreTargetEnvironment targetEnvironment
    );
}
