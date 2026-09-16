package com.researchassistant.publicsite.repository;

import com.researchassistant.publicsite.entity.PublicSiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PublicSiteSettingsRepository extends JpaRepository<PublicSiteSettings, UUID> {
    Optional<PublicSiteSettings> findTopByOrderByCreatedAtAsc();
}
