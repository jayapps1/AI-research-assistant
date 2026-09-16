package com.researchassistant.publicsite.repository;

import com.researchassistant.publicsite.entity.PublicPageSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicPageSectionRepository extends JpaRepository<PublicPageSection, UUID> {

    List<PublicPageSection> findByPageIdOrderByDisplayOrderAsc(UUID pageId);

    List<PublicPageSection> findByPageIdAndEnabledTrueOrderByDisplayOrderAsc(UUID pageId);

    Optional<PublicPageSection> findByPageIdAndSectionKey(UUID pageId, String sectionKey);

    void deleteByPageId(UUID pageId);
}
