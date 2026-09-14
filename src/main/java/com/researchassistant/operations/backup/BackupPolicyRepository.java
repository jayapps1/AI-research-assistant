package com.researchassistant.operations.backup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BackupPolicyRepository extends JpaRepository<BackupPolicy, UUID> {

    List<BackupPolicy> findAllByStatus(BackupPolicyStatus status);

    Optional<BackupPolicy> findFirstByStatusOrderByCreatedAtAsc(
            BackupPolicyStatus status
    );
}
