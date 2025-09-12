package com.wojtasj.aichatbridge.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.wojtasj.aichatbridge.entity.DiscordMessageEntity;

/**
 * Repository for managing DiscordMessageEntity in the AI Chat Bridge application.
 * @since 1.0
 */
@Repository
public interface DiscordMessageRepository extends JpaRepository<DiscordMessageEntity, Long> {
}
