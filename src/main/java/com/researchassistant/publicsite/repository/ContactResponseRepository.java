package com.researchassistant.publicsite.repository;

import com.researchassistant.publicsite.entity.ContactResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactResponseRepository extends JpaRepository<ContactResponse, UUID> {
    List<ContactResponse> findBySubmissionIdOrderByCreatedAtAsc(UUID submissionId);
}
