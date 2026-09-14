package com.researchassistant.integrity.repository;

import com.researchassistant.integrity.entity.AcademicWritingReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AcademicWritingReviewRepository extends JpaRepository<AcademicWritingReview, UUID> {}
