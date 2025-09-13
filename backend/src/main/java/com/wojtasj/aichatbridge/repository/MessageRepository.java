package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing MessageEntity in the AI Chat Bridge application.
 * @since 1.0
 */
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
}
