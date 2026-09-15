package com.researchassistant.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkspaceSubscriptionHistoryRepository extends JpaRepository<WorkspaceSubscriptionHistory, UUID> {
}
