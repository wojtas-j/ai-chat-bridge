package com.wojtasj.aichatbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wojtasj.aichatbridge.entity.MessageEntity;

/**
 * Repository for performing CRUD operations on {@link MessageEntity}.
 */
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
}