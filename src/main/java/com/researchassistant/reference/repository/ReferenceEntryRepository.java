package com.researchassistant.reference.repository;

import com.researchassistant.reference.entity.ReferenceEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceEntryRepository extends JpaRepository<ReferenceEntry, UUID> {
    Optional<ReferenceEntry> findFirstByNormalizedDoiIgnoreCase(String normalizedDoi);
    Optional<ReferenceEntry> findFirstByUrlIgnoreCase(String url);
    List<ReferenceEntry> findAllByTitleContainingIgnoreCase(String title);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from ReferenceEntry entry where entry.id = :id")
    Optional<ReferenceEntry> findByIdForUpdate(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from ReferenceEntry entry where lower(entry.normalizedDoi) = lower(:normalizedDoi) order by entry.createdAt asc")
    List<ReferenceEntry> findAllByNormalizedDoiForUpdate(String normalizedDoi, Pageable pageable);
}
