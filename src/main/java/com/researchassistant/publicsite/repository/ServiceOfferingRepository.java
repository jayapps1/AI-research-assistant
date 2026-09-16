package com.researchassistant.publicsite.repository;

import com.researchassistant.publicsite.entity.ServiceOffering;
import com.researchassistant.publicsite.entity.ServiceOfferingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    Optional<ServiceOffering> findByCodeIgnoreCase(String code);

    List<ServiceOffering> findByStatusOrderByDisplayOrderAsc(ServiceOfferingStatus status);

    List<ServiceOffering> findByStatusAndFeaturedTrueOrderByDisplayOrderAsc(ServiceOfferingStatus status);

    List<ServiceOffering> findAllByOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);
}
