package com.researchassistant.reference.repository;

import com.researchassistant.reference.entity.ReferenceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceEntryRepository extends JpaRepository<ReferenceEntry, UUID> {
    Optional<ReferenceEntry> findFirstByNormalizedDoiIgnoreCase(String normalizedDoi);
    List<ReferenceEntry> findAllByTitleContainingIgnoreCase(String title);
}
