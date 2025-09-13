package com.wojtasj.aichatbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.dto.MessageDTO;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.exception.GlobalExceptionHandler;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.service.AuthenticationService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the functionality of {@link MessageController} in the AI Chat Bridge application.
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MessageControllerTest {

    private static final String MESSAGES_URL = "/api/messages";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_CONTENT = "Test message";
    private static final String HELLO_CONTENT = "Hello!";

    private MockMvc mockMvc;
    private UserEntity testUser;

    @Mock
    private MessageRepository repository;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private MessageController messageController;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    /**
     * Sets up the test environment with MockMvc, mocked dependencies, and authentication context.
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
                .build();

        UserDetails userDetails = new User(TEST_USERNAME, "password", Collections.emptyList());
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        mockMvc = MockMvcBuilders
                .standaloneSetup(messageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver()
                )
                .build();

        when(authenticationService.findByUsername(TEST_USERNAME)).thenReturn(testUser);
    }

    /**
     * Custom argument resolver for handling @AuthenticationPrincipal.
     */
    static class AuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(@NotNull MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      @NotNull NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null && auth.isAuthenticated()) ? auth.getPrincipal() : null;
        }
    }

    /**
     * Tests retrieving an empty page when no messages exist for the user.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyListWhenNoMessages() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        when(repository.findByUserId(testUser.getId(), pageable))
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
        verify(repository).findByUserId(testUser.getId(), pageable);
    }

    /**
     * Tests retrieving all messages for the authenticated user with pagination.
     * @since 1.0
     */
    @Test
    void shouldGetAllMessages() throws Exception {
        // Arrange
        MessageEntity message1 = createMessageEntity(1L, TEST_CONTENT);
        MessageEntity message2 = createMessageEntity(2L, HELLO_CONTENT);
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        Page<MessageEntity> page = new PageImpl<>(List.of(message1, message2), pageable, 2);
        when(repository.findByUserId(testUser.getId(), pageable)).thenReturn(page);

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
        verify(repository).findByUserId(testUser.getId(), pageable);
    }

    /**
     * Tests creating a new message for the authenticated user.
     * @since 1.0
     */
    @Test
    void shouldCreateMessage() throws Exception {
        // Arrange
        MessageDTO messageDTO = new MessageDTO(TEST_CONTENT);
        MessageEntity savedMessage = createMessageEntity(1L, TEST_CONTENT);
        when(repository.save(any(MessageEntity.class))).thenReturn(savedMessage);

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
        verify(repository).save(any(MessageEntity.class));
    }

    /**
     * Tests rejecting a request when the user is not found.
     * @since 1.0
     */
    @Test
    void shouldRejectWhenUserNotFound() throws Exception {
        // Arrange
        MessageDTO messageDTO = new MessageDTO(TEST_CONTENT);
        when(authenticationService.findByUsername(TEST_USERNAME))
                .thenThrow(new AuthenticationException("User not found"));

        // Act & Assert
        mockMvc.perform(post(MESSAGES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.title").value("Authentication Failed"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("User not found"));

        // Verify
        verify(authenticationService).findByUsername(TEST_USERNAME);
        verify(repository, never()).save(any());
    }

    /**
     * Creates a mock MessageEntity for testing purposes.
     * @param id the ID of the message
     * @param content the content of the message
     * @return a MessageEntity with the specified ID and content
     * @since 1.0
     */
    private MessageEntity createMessageEntity(Long id, String content) {
        return MessageEntity.builder()
                .id(id)
                .content(content)
                .createdAt(LocalDateTime.now())
                .user(testUser)
                .build();
    }
}
