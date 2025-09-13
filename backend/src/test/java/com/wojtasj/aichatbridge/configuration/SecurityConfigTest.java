package com.wojtasj.aichatbridge.configuration;

import com.wojtasj.aichatbridge.dto.TokenResponse;
import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.repository.RefreshTokenRepository;
import com.wojtasj.aichatbridge.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SecurityConfig with real Spring context and H2 database.
 * Tests public and authenticated endpoints, ensuring JWT-based authentication works correctly.
 * @since 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    private static final String AUTH_URL = "/api/auth";
    private static final String MESSAGES_URL = "/api/messages";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String TEST_REFRESH_TOKEN = "refresh-token";
    private static final String TEST_API_KEY = "test-api-key";
    private static final Integer MAX_TOKENS = 100;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Sets up the test environment by creating a test user and refresh token in H2 database.
     * @since 1.0
     */
    @BeforeEach
    @Transactional
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = UserEntity.builder()
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .roles(Set.of(Role.USER))
                .apiKey(TEST_API_KEY)
                .maxTokens(MAX_TOKENS)
                .build();
        userRepository.save(user);

        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setToken(TEST_REFRESH_TOKEN);
        token.setUser(user);
        refreshTokenRepository.save(token);
    }

    /**
     * Cleans up the database after each test.
     * @since 1.0
     */
    @AfterEach
    @Transactional
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Tests that public access to /api/auth/login is allowed without authentication.
     * @since 1.0
     */
    @Test
    void shouldAllowPublicAccessToLogin() throws Exception {
        // Arrange
        String loginJson = """
            {
                "username": "%s",
                "password": "%s"
            }
        """.formatted(TEST_USERNAME, TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(AUTH_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk());
    }

    /**
     * Tests that public access to /api/auth/register is allowed without authentication.
     * @since 1.0
     */
    @Test
    void shouldAllowPublicAccessToRegister() throws Exception {
        // Arrange
        String registerJson = """
            {
                "username": "newuser",
                "email": "newuser@example.com",
                "password": "%s",
                "apiKey": "%s",
                "maxTokens": "%s"
            }
        """.formatted(TEST_PASSWORD, TEST_API_KEY, MAX_TOKENS);

        // Act & Assert
        mockMvc.perform(post(AUTH_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());
    }

    /**
     * Tests that authenticated access to /api/auth/refresh is allowed with valid token.
     * @since 1.0
     */
    @Test
    void shouldAllowAuthenticatedAccessToRefresh() throws Exception {
        // Arrange
        String loginJson = """
            {
                "username": "%s",
                "password": "%s"
            }
        """.formatted(TEST_USERNAME, TEST_PASSWORD);

        String loginResponse = mockMvc.perform(post(AUTH_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TokenResponse tokenResponse = objectMapper.readValue(loginResponse, TokenResponse.class);
        String accessToken = "Bearer " + tokenResponse.accessToken();

        String refreshJson = """
            {
                "refreshToken": "%s"
            }
        """.formatted(TEST_REFRESH_TOKEN);

        // Act & Assert
        mockMvc.perform(post(AUTH_URL + "/refresh")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk());
    }

    /**
     * Tests that unauthenticated access to /api/auth/refresh is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockUnauthenticatedAccessToRefresh() throws Exception {
        // Arrange
        String refreshJson = """
            {
                "refreshToken": "invalid-token"
            }
        """;

        // Act & Assert
        mockMvc.perform(post(AUTH_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that authenticated access to /api/messages is allowed with valid token.
     * @since 1.0
     */
    @Test
    void shouldAllowAuthenticatedAccessToMessages() throws Exception {
        // Arrange
        String loginJson = """
            {
                "username": "%s",
                "password": "%s"
            }
        """.formatted(TEST_USERNAME, TEST_PASSWORD);

        String responseBody = mockMvc.perform(post(AUTH_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TokenResponse tokenResponse = objectMapper.readValue(responseBody, TokenResponse.class);
        String accessToken = "Bearer " + tokenResponse.accessToken();

        // Act & Assert
        mockMvc.perform(get(MESSAGES_URL)
                        .header("Authorization", accessToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    /**
     * Tests that unauthenticated access to /api/messages is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockUnauthenticatedAccessToMessages() throws Exception {
        // Act & Assert
        mockMvc.perform(get(MESSAGES_URL)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isForbidden());
    }
}
