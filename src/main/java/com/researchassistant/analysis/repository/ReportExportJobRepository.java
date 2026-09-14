package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ReportExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReportExportJobRepository extends JpaRepository<ReportExportJob, UUID> {}
