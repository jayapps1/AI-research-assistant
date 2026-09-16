package com.researchassistant.publicsite.repository;

import com.researchassistant.publicsite.entity.PublicPage;
import com.researchassistant.publicsite.entity.PublicPageStatus;
import com.researchassistant.publicsite.entity.PublicPageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicPageRepository extends JpaRepository<PublicPage, UUID> {

    Optional<PublicPage> findBySlugIgnoreCase(String slug);

    Optional<PublicPage> findBySlugIgnoreCaseAndStatus(String slug, PublicPageStatus status);

    Optional<PublicPage> findByType(PublicPageType type);

    List<PublicPage> findByStatusOrderByNavigationOrderAsc(PublicPageStatus status);

    List<PublicPage> findByStatusAndShowInNavigationTrueOrderByNavigationOrderAsc(PublicPageStatus status);

    List<PublicPage> findAllByOrderByNavigationOrderAsc();

    boolean existsBySlugIgnoreCase(String slug);

    @Query("SELECT p FROM PublicPage p LEFT JOIN FETCH p.sections WHERE LOWER(p.slug) = LOWER(:slug) AND p.status = :status")
    Optional<PublicPage> findBySlugWithSections(@Param("slug") String slug, @Param("status") PublicPageStatus status);
}
