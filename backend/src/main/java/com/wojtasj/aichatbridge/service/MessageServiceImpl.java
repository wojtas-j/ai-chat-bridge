package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AccessDeniedException;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.exception.MessageNotFoundException;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link MessageService} interface.
 * Provides business logic for message operations with security checks.
 * @since 1.0
 */
@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    /**
     * Retrieves a paginated list of messages for a specific user, sorted by creation date in descending order.
     * @param userId   the ID of the user whose messages are to be retrieved
     * @param pageable pagination information (page number, size)
     * @return a {@link Page} of {@link MessageEntity} objects for the specified user
     * @throws AuthenticationException if the user with the specified ID is not found
     * @since 1.0
     */
    @Override
    public Page<MessageEntity> getMessagesForUser(Long userId, Pageable pageable) {
        log.info("Fetching messages for user ID: {}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found with ID: " + userId));
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
        Page<MessageEntity> messages = messageRepository.findByUserId(userId, sortedPageable);
        log.info("Retrieved {} messages for user ID: {}", messages.getTotalElements(), userId);
        return messages;
    }

    /**
     * Retrieves a paginated list of all messages across all users, sorted by creation date in descending order.
     * Intended for admin use only.
     * @param pageable pagination information (page number, size)
     * @return a {@link Page} of {@link MessageEntity} objects
     * @since 1.0
     */
    @Override
    public Page<MessageEntity> getAllMessages(Pageable pageable) {
        log.info("Fetching all messages (admin request)");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
        Page<MessageEntity> messages = messageRepository.findAll(sortedPageable);
        log.info("Retrieved {} total messages", messages.getTotalElements());
        return messages;
    }

    /**
     * Creates a new message and associates it with the specified user.
     * @param content the content of the message
     * @param userId  the ID of the user creating the message
     * @return the saved {@link MessageEntity}
     * @throws AuthenticationException if the user with the specified ID is not found
     * @since 1.0
     */
    @Override
    public MessageEntity createMessage(String content, Long userId) {
        log.info("Creating message for user ID: {} with content length: {}", userId, content.length());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found with ID: " + userId));
        MessageEntity message = MessageEntity.builder()
                .content(content)
                .user(user)
                .build();
        MessageEntity saved = messageRepository.save(message);
        log.info("Message created with ID: {} for user ID: {}", saved.getId(), userId);
        return saved;
    }

    /**
     * Deletes a specific message if the requesting user is the owner or an admin.
     * @param messageId     the ID of the message to delete
     * @param currentUserId the ID of the user requesting the deletion
     * @throws AuthenticationException if the user is not found
     * @throws MessageNotFoundException if the message do not exist
     * @throws AccessDeniedException if the requesting user is neither the message owner nor an admin
     * @since 1.0
     */
    @Override
    public void deleteMessage(Long messageId, Long currentUserId) {
        log.info("Attempting to delete message ID: {} by user ID: {}", messageId, currentUserId);
        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AuthenticationException("Current user not found with ID: " + currentUserId));
        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found with ID: " + messageId));

        if (!message.getUser().getId().equals(currentUserId) && !currentUser.getRoles().contains(Role.ADMIN)) {
            log.warn("Access denied: User ID {} cannot delete message ID {} (not owner)", currentUserId, messageId);
            throw new AccessDeniedException("You can only delete your own messages unless you are an admin.");
        }

        messageRepository.delete(message);
        log.info("Message ID: {} deleted successfully by user ID: {}", messageId, currentUserId);
    }
}
