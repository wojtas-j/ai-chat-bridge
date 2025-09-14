package com.wojtasj.aichatbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.configuration.SecurityConfig;
import com.wojtasj.aichatbridge.dto.MessageDTO;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AccessDeniedException;
import com.wojtasj.aichatbridge.exception.GlobalExceptionHandler;
import com.wojtasj.aichatbridge.exception.MessageNotFoundException;
import com.wojtasj.aichatbridge.service.AuthenticationService;
import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
import com.wojtasj.aichatbridge.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the functionality of {@link MessageController} in the AI Chat Bridge application.
 * @since 1.0
 */
@WebMvcTest(MessageController.class)
@ContextConfiguration(classes = {MessageController.class, GlobalExceptionHandler.class, SecurityConfig.class})
@Import(SecurityConfig.class)
class MessageControllerTest {

    private static final String MESSAGES_URL = "/api/messages";
    private static final String ADMIN_MESSAGES_URL = "/api/messages/admin";
    private static final String TEST_USERNAME = "testuser";
    private static final String ADMIN_USERNAME = "adminuser";
    private static final String TEST_CONTENT = "Test message";
    private static final String HELLO_CONTENT = "Hello!";

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private MessageService messageService;

    @SuppressWarnings("unused")
    @MockitoBean
    private AuthenticationService authenticationService;

    @SuppressWarnings("unused")
    @MockitoBean
    private JwtTokenProviderImpl jwtTokenProvider;

    @SuppressWarnings("unused")
    @MockitoBean
    private UserDetailsService userDetailsService;


    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private UserEntity testUser;
    private UserEntity adminUser;

    /**
     * Sets up the test environment with mocked dependencies.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .id(1L)
                .username(TEST_USERNAME)
                .email("test@example.com")
                .password("password")
                .apiKey("test-api-key")
                .maxTokens(100)
                .roles(Set.of(Role.USER))
                .build();

        adminUser = UserEntity.builder()
                .id(2L)
                .username(ADMIN_USERNAME)
                .email("admin@example.com")
                .password("password")
                .apiKey("admin-api-key")
                .maxTokens(200)
                .roles(Set.of(Role.ADMIN))
                .build();

        lenient().when(authenticationService.findByUsername(TEST_USERNAME)).thenReturn(testUser);
        lenient().when(authenticationService.findByUsername(ADMIN_USERNAME)).thenReturn(adminUser);
    }

    /**
     * Tests retrieving an empty page when no messages exist for the user.
     * @since 1.0
     */
    @Test
    @WithMockUser(username = TEST_USERNAME, roles = "USER")
    void shouldReturnEmptyListWhenNoMessages() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        when(messageService.getMessagesForUser(testUser.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act & Assert
        mockMvc.perform(get(MESSAGES_URL)
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));

        // Verify
        verify(authenticationService).findByUsername(TEST_USERNAME);
        verify(messageService).getMessagesForUser(testUser.getId(), pageable);
    }

    /**
     * Tests retrieving all messages for the authenticated user with pagination.
     * @since 1.0
     */
    @Test
    @WithMockUser(username = TEST_USERNAME, roles = "USER")
    void shouldGetAllMessages() throws Exception {
        // Arrange
        MessageEntity message1 = createMessageEntity(1L, TEST_CONTENT, testUser);
        MessageEntity message2 = createMessageEntity(2L, HELLO_CONTENT, testUser);
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        Page<MessageEntity> page = new PageImpl<>(List.of(message1, message2), pageable, 2);
        when(messageService.getMessagesForUser(testUser.getId(), pageable)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(MESSAGES_URL)
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].content").value(TEST_CONTENT))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(2L))
                .andExpect(jsonPath("$.content[1].content").value(HELLO_CONTENT))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(2));

        // Verify
        verify(authenticationService).findByUsername(TEST_USERNAME);
        verify(messageService).getMessagesForUser(testUser.getId(), pageable);
    }

    /**
     * Tests retrieving all messages for admin with pagination.
     * @since 1.0
     */
    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = {"ADMIN"})
    void shouldGetAllAdminMessages() throws Exception {
        // Arrange
        MessageEntity message1 = createMessageEntity(1L, TEST_CONTENT, testUser);
        MessageEntity message2 = createMessageEntity(2L, HELLO_CONTENT, adminUser);
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        Page<MessageEntity> page = new PageImpl<>(List.of(message1, message2), pageable, 2);
        when(messageService.getAllMessages(pageable)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(ADMIN_MESSAGES_URL)
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].content").value(TEST_CONTENT))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(2L))
                .andExpect(jsonPath("$.content[1].content").value(HELLO_CONTENT))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(2));

        // Verify
        verify(messageService).getAllMessages(pageable);
    }

    /**
     * Tests rejecting non-admin user accessing admin endpoint.
     * @since 1.0
     */
    @Test
    @WithMockUser(username = TEST_USERNAME, roles = "USER")
    void shouldRejectNonAdminAccessToAdminEndpoint() throws Exception {
        // Act & Assert
        mockMvc.perform(get(ADMIN_MESSAGES_URL)
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                .andExpect(jsonPath("$.title").value("Access Denied"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));

        // Verify
        verify(authenticationService, never()).findByUsername(anyString());
        verify(messageService, never()).getAllMessages(any());
    }

    /**
     * Tests creating a new message for the authenticated user.
     * @since 1.0
     */
    @Test
    @WithMockUser(username = TEST_USERNAME, roles = "USER")
    void shouldCreateMessage() throws Exception {
        // Arrange
        MessageDTO messageDTO = new MessageDTO(TEST_CONTENT);
        MessageEntity savedMessage = createMessageEntity(1L, TEST_CONTENT, testUser);
        when(messageService.createMessage(TEST_CONTENT, testUser.getId())).thenReturn(savedMessage);

        // Act & Assert
        mockMvc.perform(post(MESSAGES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value(TEST_CONTENT))
                .andExpect(jsonPath("$.createdAt").exists());

        // Verify
        verify(authenticationService).findByUsername(TEST_USERNAME);
        verify(messageService).createMessage(TEST_CONTENT, testUser.getId());
    }

    /**
     * Tests rejecting message creation with invalid input (empty content).
     * @since 1.0
     */
    @Test
    @WithMockUser(username = TEST_USERNAME, roles = "USER")
    void shouldRejectCreateMessageWithEmptyContent() throws Exception {
        // Arrange
        MessageDTO messageDTO = new MessageDTO("");

        // Act & Assert
        mockMvc.perform(post(MESSAGES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists());

        // Verify
        verify(authenticationService, never()).findByUsername(anyString());
        verify(messageService, never()).createMessage(anyString(), anyLong());
    }

    /**
     * Tests deleting a message as the owner.
     * @since 1.0
     */
    @Test
    @WithMockUser(username = TEST_USERNAME, roles = "USER")
    void shouldDeleteMessageAsOwner() throws Exception {
        // Arrange
        doNothing().when(messageService).deleteMessage(1L, testUser.getId());

        // Act & Assert
        mockMvc.perform(delete(MESSAGES_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify
        verify(authenticationService).findByUsername(TEST_USERNAME);
        verify(messageService).deleteMessage(1L, testUser.getId());
    }

    /**
     * Tests deleting a message as an admin (for any message).
     * @since 1.0
     */
    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = "ADMIN")
    void shouldDeleteMessageAsAdmin() throws Exception {
        // Arrange
        doNothing().when(messageService).deleteMessage(1L, adminUser.getId());

        // Act & Assert
        mockMvc.perform(delete(MESSAGES_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify
        verify(authenticationService).findByUsername(ADMIN_USERNAME);
        verify(messageService).deleteMessage(1L, adminUser.getId());
    }

    /**
     * Tests rejecting deletion of another user's message by non-admin.
     * @since 1.0
     */
    @Test
    @WithMockUser(username = TEST_USERNAME, roles = "USER")
    void shouldRejectDeleteMessageByNonOwnerNonAdmin() throws Exception {
        // Arrange
        doThrow(new AccessDeniedException("You can only delete your own messages unless you are an admin."))
                .when(messageService).deleteMessage(1L, testUser.getId());

        // Act & Assert
        mockMvc.perform(delete(MESSAGES_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                .andExpect(jsonPath("$.title").value("Access Denied"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("You can only delete your own messages unless you are an admin."));

        // Verify
        verify(authenticationService).findByUsername(TEST_USERNAME);
        verify(messageService).deleteMessage(1L, testUser.getId());
    }

    /**
     * Tests rejecting deletion of non-existent message.
     * @since 1.0
     */
    @Test
    @WithMockUser(username = TEST_USERNAME, roles = "USER")
    void shouldRejectDeleteNonExistentMessage() throws Exception {
        // Arrange
        doThrow(new MessageNotFoundException("Message not found with ID: 999"))
                .when(messageService).deleteMessage(999L, testUser.getId());

        // Act & Assert
        mockMvc.perform(delete(MESSAGES_URL + "/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problems/message-not-found"))
                .andExpect(jsonPath("$.title").value("Message Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Message not found with ID: 999"));

        // Verify
        verify(authenticationService).findByUsername(TEST_USERNAME);
        verify(messageService).deleteMessage(999L, testUser.getId());
    }

    /**
     * Tests rejecting unauthenticated access to get messages.
     * @since 1.0
     */
    @Test
    void shouldRejectUnauthenticatedGetMessages() throws Exception {
        // Act & Assert
        mockMvc.perform(get(MESSAGES_URL)
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.title").value("Authentication Failed"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").exists());

        // Verify
        verify(authenticationService, never()).findByUsername(anyString());
        verify(messageService, never()).getMessagesForUser(anyLong(), any());
    }

    /**
     * Tests rejecting unauthenticated access to create message.
     * @since 1.0
     */
    @Test
    void shouldRejectUnauthenticatedCreateMessage() throws Exception {
        // Arrange
        MessageDTO messageDTO = new MessageDTO(TEST_CONTENT);

        // Act & Assert
        mockMvc.perform(post(MESSAGES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.title").value("Authentication Failed"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").exists());

        // Verify
        verify(authenticationService, never()).findByUsername(anyString());
        verify(messageService, never()).createMessage(anyString(), anyLong());
    }

    /**
     * Tests rejecting unauthenticated access to delete message.
     * @since 1.0
     */
    @Test
    void shouldRejectUnauthenticatedDeleteMessage() throws Exception {
        // Act & Assert
        mockMvc.perform(delete(MESSAGES_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.title").value("Authentication Failed"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").exists());

        // Verify
        verify(authenticationService, never()).findByUsername(anyString());
        verify(messageService, never()).deleteMessage(anyLong(), anyLong());
    }

    /**
     * Creates a mock MessageEntity for testing purposes.
     * @param id the ID of the message
     * @param content the content of the message
     * @param user the user who owns the message
     * @return a MessageEntity with the specified ID and content
     * @since 1.0
     */
    private MessageEntity createMessageEntity(Long id, String content, UserEntity user) {
        return MessageEntity.builder()
                .id(id)
                .content(content)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();
    }
}
