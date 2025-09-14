package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AccessDeniedException;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.exception.MessageNotFoundException;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MessageServiceImpl} in the AI Chat Bridge application.
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MessageServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long MESSAGE_ID = 1L;
    private static final String TEST_CONTENT = "Test message";
    private static final String HELLO_CONTENT = "Hello!";
    private static final String NO_USER_FOUND_WITH_ID = "User not found with ID: ";

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private UserEntity testUser;
    private UserEntity adminUser;
    private MessageEntity message;

    /**
     * Sets up the test environment with mock UserEntity and MessageEntity.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .id(USER_ID)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .apiKey("test-api-key")
                .maxTokens(100)
                .roles(Set.of(Role.USER))
                .build();

        adminUser = UserEntity.builder()
                .id(ADMIN_ID)
                .username("adminuser")
                .email("admin@example.com")
                .password("password")
                .apiKey("admin-api-key")
                .maxTokens(200)
                .roles(Set.of(Role.ADMIN))
                .build();

        message = MessageEntity.builder()
                .id(MESSAGE_ID)
                .content(TEST_CONTENT)
                .createdAt(LocalDateTime.now())
                .user(testUser)
                .build();
    }

    /**
     * Tests retrieving messages for a user successfully.
     * @since 1.0
     */
    @Test
    void shouldGetMessagesForUserSuccessfully() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        Page<MessageEntity> page = new PageImpl<>(List.of(message), pageable, 1);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(messageRepository.findByUserId(USER_ID, pageable)).thenReturn(page);

        // Act
        Page<MessageEntity> result = messageService.getMessagesForUser(USER_ID, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(MESSAGE_ID);
        assertThat(result.getContent().getFirst().getContent()).isEqualTo(TEST_CONTENT);
        assertThat(result.getContent().getFirst().getUser().getId()).isEqualTo(USER_ID);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userRepository).findById(USER_ID);
        verify(messageRepository).findByUserId(USER_ID, pageable);
    }

    /**
     * Tests throwing AuthenticationException when user is not found for getMessagesForUser.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForNonExistentUserInGetMessages() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> messageService.getMessagesForUser(USER_ID, pageable));
        assertThat(exception.getMessage()).isEqualTo(NO_USER_FOUND_WITH_ID + USER_ID);
        verify(userRepository).findById(USER_ID);
        verify(messageRepository, never()).findByUserId(anyLong(), any());
    }

    /**
     * Tests retrieving all messages for admin successfully.
     * @since 1.0
     */
    @Test
    void shouldGetAllMessagesSuccessfully() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        MessageEntity message2 = MessageEntity.builder()
                .id(2L)
                .content(HELLO_CONTENT)
                .createdAt(LocalDateTime.now())
                .user(adminUser)
                .build();
        Page<MessageEntity> page = new PageImpl<>(List.of(message, message2), pageable, 2);
        when(messageRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<MessageEntity> result = messageService.getAllMessages(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(MESSAGE_ID);
        assertThat(result.getContent().get(0).getContent()).isEqualTo(TEST_CONTENT);
        assertThat(result.getContent().get(1).getId()).isEqualTo(2L);
        assertThat(result.getContent().get(1).getContent()).isEqualTo(HELLO_CONTENT);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(messageRepository).findAll(pageable);
    }

    /**
     * Tests creating a message successfully.
     * @since 1.0
     */
    @Test
    void shouldCreateMessageSuccessfully() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(MessageEntity.class))).thenReturn(message);

        // Act
        MessageEntity result = messageService.createMessage(TEST_CONTENT, USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(MESSAGE_ID);
        assertThat(result.getContent()).isEqualTo(TEST_CONTENT);
        assertThat(result.getUser().getId()).isEqualTo(USER_ID);
        assertThat(result.getCreatedAt()).isNotNull();
        verify(userRepository).findById(USER_ID);
        verify(messageRepository).save(any(MessageEntity.class));
    }

    /**
     * Tests throwing AuthenticationException when user is not found for createMessage.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForNonExistentUserInCreateMessage() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> messageService.createMessage(TEST_CONTENT, USER_ID));
        assertThat(exception.getMessage()).isEqualTo(NO_USER_FOUND_WITH_ID + USER_ID);
        verify(userRepository).findById(USER_ID);
        verify(messageRepository, never()).save(any());
    }

    /**
     * Tests deleting a message as the owner successfully.
     * @since 1.0
     */
    @Test
    void shouldDeleteMessageAsOwnerSuccessfully() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
        doNothing().when(messageRepository).delete(message);

        // Act
        messageService.deleteMessage(MESSAGE_ID, USER_ID);

        // Assert
        verify(userRepository).findById(USER_ID);
        verify(messageRepository).findById(MESSAGE_ID);
        verify(messageRepository).delete(message);
    }

    /**
     * Tests deleting a message as an admin successfully.
     * @since 1.0
     */
    @Test
    void shouldDeleteMessageAsAdminSuccessfully() {
        // Arrange
        MessageEntity otherUserMessage = MessageEntity.builder()
                .id(MESSAGE_ID)
                .content(TEST_CONTENT)
                .createdAt(LocalDateTime.now())
                .user(testUser)
                .build();
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminUser));
        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(otherUserMessage));
        doNothing().when(messageRepository).delete(otherUserMessage);

        // Act
        messageService.deleteMessage(MESSAGE_ID, ADMIN_ID);

        // Assert
        verify(userRepository).findById(ADMIN_ID);
        verify(messageRepository).findById(MESSAGE_ID);
        verify(messageRepository).delete(otherUserMessage);
    }

    /**
     * Tests throwing MessageNotFoundException when message is not found for deletion.
     * @since 1.0
     */
    @Test
    void shouldThrowMessageNotFoundExceptionForNonExistentMessage() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

        // Act & Assert
        MessageNotFoundException exception = assertThrows(MessageNotFoundException.class,
                () -> messageService.deleteMessage(MESSAGE_ID, USER_ID));
        assertThat(exception.getMessage()).isEqualTo("Message not found with ID: " + MESSAGE_ID);
        verify(userRepository).findById(USER_ID);
        verify(messageRepository).findById(MESSAGE_ID);
        verify(messageRepository, never()).delete(any());
    }

    /**
     * Tests throwing AuthenticationException when user is not found for deletion.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForNonExistentUserInDeleteMessage() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> messageService.deleteMessage(MESSAGE_ID, USER_ID));
        assertThat(exception.getMessage()).isEqualTo("Current user not found with ID: " + USER_ID);
        verify(userRepository).findById(USER_ID);
        verify(messageRepository, never()).findById(anyLong());
        verify(messageRepository, never()).delete(any());
    }

    /**
     * Tests throwing AccessDeniedException when non-admin tries to delete another user's message.
     * @since 1.0
     */
    @Test
    void shouldThrowAccessDeniedExceptionForNonOwnerNonAdminDelete() {
        // Arrange
        UserEntity otherUser = UserEntity.builder()
                .id(3L)
                .username("otheruser")
                .email("other@example.com")
                .password("password")
                .apiKey("other-api-key")
                .maxTokens(100)
                .roles(Set.of(Role.USER))
                .build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherUser));
        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> messageService.deleteMessage(MESSAGE_ID, 3L));
        assertThat(exception.getMessage()).isEqualTo("You can only delete your own messages unless you are an admin.");
        verify(userRepository).findById(3L);
        verify(messageRepository).findById(MESSAGE_ID);
        verify(messageRepository, never()).delete(any());
    }
}
