package com.researchassistant.reference.repository;

import com.researchassistant.reference.entity.ReferenceSourceLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReferenceSourceLinkRepository extends JpaRepository<ReferenceSourceLink, UUID> {
    List<ReferenceSourceLink> findAllByReferenceId(UUID referenceId);
}
