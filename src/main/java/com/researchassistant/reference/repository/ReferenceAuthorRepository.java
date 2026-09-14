package com.researchassistant.reference.repository;

import com.researchassistant.reference.entity.AuthorRole;
import com.researchassistant.reference.entity.ReferenceAuthor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReferenceAuthorRepository extends JpaRepository<ReferenceAuthor, UUID> {
    List<ReferenceAuthor> findAllByReferenceIdOrderByDisplayOrderAsc(UUID referenceId);
    List<ReferenceAuthor> findAllByReferenceIdAndRoleOrderByDisplayOrderAsc(UUID referenceId, AuthorRole role);
}
