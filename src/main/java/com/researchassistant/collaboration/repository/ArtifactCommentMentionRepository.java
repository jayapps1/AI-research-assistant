package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.ArtifactCommentMention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArtifactCommentMentionRepository extends JpaRepository<ArtifactCommentMention, UUID> {
    List<ArtifactCommentMention> findAllByCommentId(UUID commentId);
}
