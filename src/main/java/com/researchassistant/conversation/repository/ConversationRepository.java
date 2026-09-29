package com.researchassistant.conversation.repository;

import com.researchassistant.conversation.entity.Conversation;
import com.researchassistant.conversation.entity.ConversationMessageRole;
import com.researchassistant.conversation.entity.ConversationStatus;
import com.researchassistant.conversation.entity.ConversationType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT c
            FROM Conversation c
            WHERE c.user.id = :userId
              AND (:status IS NULL OR c.status = :status)
              AND (:type IS NULL OR c.type = :type)
              AND (
                    :queryText IS NULL
                    OR :queryText = ''
                    OR LOWER(c.title) LIKE LOWER(CONCAT('%', :queryText, '%'))
                    OR EXISTS (
                        SELECT 1
                        FROM ConversationMessage m
                        WHERE m.conversation = c
                          AND m.role <> :excludedRole
                          AND LOWER(m.content) LIKE LOWER(CONCAT('%', :queryText, '%'))
                    )
              )
            ORDER BY COALESCE(c.lastMessageAt, c.updatedAt, c.createdAt) DESC
            """)
    Page<Conversation> searchForUser(
            @Param("userId") UUID userId,
            @Param("status") ConversationStatus status,
            @Param("type") ConversationType type,
            @Param("queryText") String queryText,
            @Param("excludedRole") ConversationMessageRole excludedRole,
            Pageable pageable
    );

    @Query("""
            SELECT c
            FROM Conversation c
            WHERE c.user.id = :userId
              AND c.project.id = :projectId
              AND (:status IS NULL OR c.status = :status)
              AND (
                    :queryText IS NULL
                    OR :queryText = ''
                    OR LOWER(c.title) LIKE LOWER(CONCAT('%', :queryText, '%'))
                    OR EXISTS (
                        SELECT 1
                        FROM ConversationMessage m
                        WHERE m.conversation = c
                          AND m.role <> :excludedRole
                          AND LOWER(m.content) LIKE LOWER(CONCAT('%', :queryText, '%'))
                    )
              )
            ORDER BY COALESCE(c.lastMessageAt, c.updatedAt, c.createdAt) DESC
            """)
    Page<Conversation> searchForUserAndProject(
            @Param("userId") UUID userId,
            @Param("projectId") UUID projectId,
            @Param("status") ConversationStatus status,
            @Param("queryText") String queryText,
            @Param("excludedRole") ConversationMessageRole excludedRole,
            Pageable pageable
    );
}
