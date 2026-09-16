package com.researchassistant.publicsite.repository;

import com.researchassistant.publicsite.entity.FaqCategory;
import com.researchassistant.publicsite.entity.FaqItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FaqItemRepository extends JpaRepository<FaqItem, UUID> {

    List<FaqItem> findByPublishedTrueOrderByDisplayOrderAsc();

    List<FaqItem> findByPublishedTrueAndFeaturedTrueOrderByDisplayOrderAsc();

    List<FaqItem> findByPublishedTrueAndCategoryOrderByDisplayOrderAsc(FaqCategory category);

    List<FaqItem> findAllByOrderByDisplayOrderAsc();
}
