package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.configuration.SecurityConfig;
import com.wojtasj.aichatbridge.entity.DiscordMessageEntity;
import com.wojtasj.aichatbridge.exception.GlobalExceptionHandler;
import com.wojtasj.aichatbridge.exception.MessageNotFoundException;
import com.wojtasj.aichatbridge.service.AuthenticationService;
import com.wojtasj.aichatbridge.service.DiscordMessageService;
import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
import com.wojtasj.aichatbridge.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the functionality of {@link DiscordMessageController} in the AI Chat Bridge application.
 * @since 1.0
 */
@WebMvcTest(DiscordMessageController.class)
@ContextConfiguration(classes = {DiscordMessageController.class, GlobalExceptionHandler.class, SecurityConfig.class})
@Import(SecurityConfig.class)
class DiscordMessageControllerTest {

    private static final String DISCORD_MESSAGES_URL = "/api/discord-messages";
    private static final String ADMIN_DISCORD_MESSAGES_URL = "/api/discord-messages/admin";
    private static final String ADMIN_USERNAME = "adminuser";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_CONTENT = "Test Discord message";
    private static final String TEST_DISCORD_NICK = "TestUser#1234";
    private static final String ANOTHER_CONTENT = "Another Discord message";
    private static final String ANOTHER_DISCORD_NICK = "AnotherUser#5678";

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private DiscordMessageService discordMessageService;

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

    private DiscordMessageEntity discordMessage;

    /**
     * Sets up the test environment with mocked dependencies and sample data.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        discordMessage = DiscordMessageEntity.builder()
                .id(1L)
                .content(TEST_CONTENT)
                .discordNick(TEST_DISCORD_NICK)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    class GetAllAdminMessagesTests {

        /**
         * Tests retrieving all Discord messages for admin with pagination.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = ADMIN_USERNAME, roles = {"ADMIN"})
        void shouldGetAllAdminMessages() throws Exception {
            // Arrange
            DiscordMessageEntity anotherMessage = DiscordMessageEntity.builder()
                    .id(2L)
                    .content(ANOTHER_CONTENT)
                    .discordNick(ANOTHER_DISCORD_NICK)
                    .createdAt(LocalDateTime.now())
                    .build();
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            Page<DiscordMessageEntity> page = new PageImpl<>(List.of(discordMessage, anotherMessage), pageable, 2);
            when(discordMessageService.getAllMessages(pageable)).thenReturn(page);

            // Act & Assert
            mockMvc.perform(get(ADMIN_DISCORD_MESSAGES_URL)
                            .param("page", "0")
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id").value(1L))
                    .andExpect(jsonPath("$.content[0].content").value(TEST_CONTENT))
                    .andExpect(jsonPath("$.content[0].discordNick").value(TEST_DISCORD_NICK))
                    .andExpect(jsonPath("$.content[0].createdAt").exists())
                    .andExpect(jsonPath("$.content[1].id").value(2L))
                    .andExpect(jsonPath("$.content[1].content").value(ANOTHER_CONTENT))
                    .andExpect(jsonPath("$.content[1].discordNick").value(ANOTHER_DISCORD_NICK))
                    .andExpect(jsonPath("$.content[1].createdAt").exists())
                    .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                    .andExpect(jsonPath("$.pageable.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(2));

            // Verify
            verify(discordMessageService).getAllMessages(pageable);
        }

        /**
         * Tests retrieving an empty page when no Discord messages exist for admin.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = ADMIN_USERNAME, roles = {"ADMIN"})
        void shouldReturnEmptyListWhenNoMessages() throws Exception {
            // Arrange
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            when(discordMessageService.getAllMessages(pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            // Act & Assert
            mockMvc.perform(get(ADMIN_DISCORD_MESSAGES_URL)
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
            verify(discordMessageService).getAllMessages(pageable);
        }

        /**
         * Tests rejecting non-admin user accessing admin Discord messages endpoint.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectNonAdminAccessToAdminEndpoint() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ADMIN_DISCORD_MESSAGES_URL)
                            .param("page", "0")
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                    .andExpect(jsonPath("$.title").value("Access Denied"))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));

            // Verify
            verify(discordMessageService, never()).getAllMessages(any());
        }

        /**
         * Tests rejecting unauthenticated access to get Discord messages.
         * @since 1.0
         */
        @Test
        void shouldRejectUnauthenticatedGetMessages() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ADMIN_DISCORD_MESSAGES_URL)
                            .param("page", "0")
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(discordMessageService, never()).getAllMessages(any());
        }
    }

    @Nested
    class DeleteMessageTests {

        /**
         * Tests deleting a Discord message as an admin.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = ADMIN_USERNAME, roles = {"ADMIN"})
        void shouldDeleteMessageAsAdmin() throws Exception {
            // Arrange
            doNothing().when(discordMessageService).deleteMessage(1L);

            // Act & Assert
            mockMvc.perform(delete(DISCORD_MESSAGES_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verify
            verify(discordMessageService).deleteMessage(1L);
        }

        /**
         * Tests rejecting deletion of a non-existent Discord message.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = ADMIN_USERNAME, roles = {"ADMIN"})
        void shouldRejectDeleteNonExistentMessage() throws Exception {
            // Arrange
            doThrow(new MessageNotFoundException("Discord message not found with ID: 999"))
                    .when(discordMessageService).deleteMessage(999L);

            // Act & Assert
            mockMvc.perform(delete(DISCORD_MESSAGES_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("/problems/message-not-found"))
                    .andExpect(jsonPath("$.title").value("Message Not Found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Discord message not found with ID: 999"));

            // Verify
            verify(discordMessageService).deleteMessage(999L);
        }

        /**
         * Tests rejecting non-admin user attempting to delete a Discord message.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectDeleteMessageByNonAdmin() throws Exception {
            // Act & Assert
            mockMvc.perform(delete(DISCORD_MESSAGES_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                    .andExpect(jsonPath("$.title").value("Access Denied"))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));

            // Verify
            verify(discordMessageService, never()).deleteMessage(anyLong());
        }

        /**
         * Tests rejecting unauthenticated access to delete a Discord message.
         * @since 1.0
         */
        @Test
        void shouldRejectUnauthenticatedDeleteMessage() throws Exception {
            // Act & Assert
            mockMvc.perform(delete(DISCORD_MESSAGES_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(discordMessageService, never()).deleteMessage(anyLong());
        }
    }
}
