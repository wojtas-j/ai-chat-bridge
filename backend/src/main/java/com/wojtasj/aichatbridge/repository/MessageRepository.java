package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing MessageEntity in the AI Chat Bridge application.
 * @since 1.0
 */
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    /**
     * Finds all messages for a specific user with pagination.
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return Page<MessageEntity> with the user's messages
     * @since 1.0
     */
    Page<MessageEntity> findByUserId(Long userId, Pageable pageable);
}
