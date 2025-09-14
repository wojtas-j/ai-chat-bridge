package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing messages in the AI Chat Bridge application.
 * Handles CRUD operations with role-based access control.
 *
 * @since 1.0
 */
public interface MessageService {

    /**
     * Retrieves all messages for a specific user with pagination.
     * @param userId the ID of the user whose messages to retrieve
     * @param pageable pagination information
     * @return a page of messages for the user
     * @throws com.wojtasj.aichatbridge.exception.AuthenticationException if user not found
     * @since 1.0
     */
    Page<MessageEntity> getMessagesForUser(Long userId, Pageable pageable);

    /**
     * Retrieves all messages across all users (admin-only).
     * @param pageable pagination information
     * @return a page of all messages
     * @since 1.0
     */
    Page<MessageEntity> getAllMessages(Pageable pageable);

    /**
     * Creates a new message for a specific user.
     * @param content the content of the message
     * @param userId the ID of the user creating the message
     * @return the saved message entity
     * @throws com.wojtasj.aichatbridge.exception.AuthenticationException if user not found
     * @since 1.0
     */
    MessageEntity createMessage(String content, Long userId);

    /**
     * Deletes a message if the current user is the owner (USER role) or has ADMIN role.
     * @param messageId the ID of the message to delete
     * @param currentUserId the ID of the authenticated user attempting deletion
     * @throws com.wojtasj.aichatbridge.exception.AccessDeniedException if not authorized
     * @throws com.wojtasj.aichatbridge.exception.AuthenticationException if entities not found
     * @since 1.0
     */
    void deleteMessage(Long messageId, Long currentUserId);
}
