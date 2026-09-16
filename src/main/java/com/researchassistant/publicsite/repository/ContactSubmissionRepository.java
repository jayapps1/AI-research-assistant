package com.researchassistant.publicsite.repository;

import com.researchassistant.publicsite.entity.ContactSubmission;
import com.researchassistant.publicsite.entity.ContactSubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ContactSubmissionRepository extends JpaRepository<ContactSubmission, UUID> {

    Optional<ContactSubmission> findByReferenceCode(String referenceCode);

    @Query(value = "SELECT nextval('contact_submission_ref_seq')", nativeQuery = true)
    Long getNextReferenceSequenceValue();

    Page<ContactSubmission> findByStatus(ContactSubmissionStatus status, Pageable pageable);

    @Query("SELECT c FROM ContactSubmission c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.referenceCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.subject) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ContactSubmission> searchSubmissions(@Param("status") ContactSubmissionStatus status,
                                             @Param("search") String search,
                                             Pageable pageable);
}
