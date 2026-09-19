package com.researchassistant.publicsite.repository;

import com.researchassistant.publicsite.entity.PublicStatistic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicStatisticRepository extends JpaRepository<PublicStatistic, UUID> {

    List<PublicStatistic> findAllByEnabledTrueOrderByDisplayOrderAsc();

    List<PublicStatistic> findAllByOrderByDisplayOrderAsc();

    Optional<PublicStatistic> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeIgnoreCase(String code);
}
