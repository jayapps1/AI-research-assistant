package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.ResearchReportAuthor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResearchReportAuthorRepository extends JpaRepository<ResearchReportAuthor, UUID> {
    List<ResearchReportAuthor> findAllByReportIdOrderByAuthorOrderAsc(UUID reportId);
    boolean existsByReportIdAndAuthorOrder(UUID reportId, int authorOrder);
}
