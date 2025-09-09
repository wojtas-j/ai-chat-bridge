package com.wojtasj.aichatbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wojtasj.aichatbridge.entity.MessageEntity;

/**
 * JPA repository for performing CRUD operations on {@link MessageEntity} in the AI Chat Bridge application.
 * @since 1.0
 * @see MessageEntity
 */
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
}
