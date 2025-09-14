package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.DiscordMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing Discord messages in the AI Chat Bridge application.
 * @since 1.0
 */
public interface DiscordMessageService {

    /**
     * Retrieves a paginated list of all Discord messages, sorted by creation date in descending order.
     * Intended for admin use only.
     * @param pageable pagination information (page number, size)
     * @return a {@link Page} of {@link DiscordMessageEntity} objects
     * @since 1.0
     */
    Page<DiscordMessageEntity> getAllMessages(Pageable pageable);

    /**
     * Deletes a specific Discord message by its ID.
     * @param messageId the ID of the message to delete
     * @throws com.wojtasj.aichatbridge.exception.MessageNotFoundException if the message does not exist
     * @since 1.0
     */
    void deleteMessage(Long messageId);
}
