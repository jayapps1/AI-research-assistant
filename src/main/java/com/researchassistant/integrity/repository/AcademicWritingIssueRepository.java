package com.researchassistant.integrity.repository;

import com.researchassistant.integrity.entity.AcademicWritingIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AcademicWritingIssueRepository extends JpaRepository<AcademicWritingIssue, UUID> {
    List<AcademicWritingIssue> findAllByReviewId(UUID reviewId);
}
