package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.DiscordMessageEntity;
import com.wojtasj.aichatbridge.exception.MessageNotFoundException;
import com.wojtasj.aichatbridge.repository.DiscordMessageRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link DiscordMessageService} interface.
 * Provides business logic for Discord message operations.
 * @since 1.0
 */
@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class DiscordMessageServiceImpl implements DiscordMessageService {

    private final DiscordMessageRepository discordMessageRepository;

    /**
     * Retrieves a paginated list of all Discord messages, sorted by creation date in descending order.
     * Intended for admin use only.
     * @param pageable pagination information (page number, size)
     * @return a {@link Page} of {@link DiscordMessageEntity} objects
     * @since 1.0
     */
    @Override
    public Page<DiscordMessageEntity> getAllMessages(Pageable pageable) {
        log.info("Fetching all Discord messages (admin request)");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
        Page<DiscordMessageEntity> messages = discordMessageRepository.findAll(sortedPageable);
        log.info("Retrieved {} total Discord messages", messages.getTotalElements());
        return messages;
    }

    /**
     * Deletes a specific Discord message by its ID.
     * @param messageId the ID of the message to delete
     * @throws MessageNotFoundException if the message does not exist
     * @since 1.0
     */
    @Override
    public void deleteMessage(Long messageId) {
        log.info("Attempting to delete Discord message ID: {}", messageId);
        DiscordMessageEntity message = discordMessageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Discord message not found with ID: " + messageId));
        discordMessageRepository.delete(message);
        log.info("Discord message ID: {} deleted successfully", messageId);
    }
}
