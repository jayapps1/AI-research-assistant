package com.researchassistant.common.storage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StorageObjectRepository extends JpaRepository<StorageObjectEntity, UUID> {

    Optional<StorageObjectEntity> findByProviderAndStorageKey(StorageProvider provider, String storageKey);

    long countByStatus(StorageObjectStatus status);
}
