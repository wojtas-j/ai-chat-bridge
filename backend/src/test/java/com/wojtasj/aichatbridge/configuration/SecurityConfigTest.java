package com.wojtasj.aichatbridge.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.dto.TokenResponse;
import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
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

import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private static final String USERS_URL = "/api/users";
    private static final String ADMIN_URL = "/api/admin";
    private static final String MESSAGES_URL = "/api/messages";
    private static final String ADMIN_MESSAGES_URL = "/api/messages/admin";
    private static final String ACTUATOR_HEALTH_URL = "/actuator/health";
    private static final String TEST_USERNAME = "testuser";
    private static final String ADMIN_USERNAME = "adminuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String TEST_REFRESH_TOKEN = "refresh-token";
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_MODEL = "test-model";
    private static final Integer MAX_TOKENS = 100;
    private static final String ADMIN_DISCORD_MESSAGES_URL = "/api/discord-messages/admin";

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
                .model(TEST_MODEL)
                .build();
        userRepository.save(user);

        UserEntity admin = UserEntity.builder()
                .username(ADMIN_USERNAME)
                .email("admin@example.com")
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .roles(Set.of(Role.ADMIN))
                .apiKey(TEST_API_KEY)
                .maxTokens(MAX_TOKENS)
                .model(TEST_MODEL)
                .build();
        userRepository.save(admin);

        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .token(TEST_REFRESH_TOKEN)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
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
                "model": "%s",
                "maxTokens": "%s"
            }
        """.formatted(TEST_PASSWORD, TEST_API_KEY, TEST_MODEL, MAX_TOKENS);

        // Act & Assert
        mockMvc.perform(post(AUTH_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());
    }

    /**
     * Tests that public access to /api/auth/refresh is allowed with valid refresh token.
     * @since 1.0
     */
    @Test
    void shouldAllowPublicAccessToRefreshWithValidToken() throws Exception {
        // Arrange
        String refreshJson = """
            {
                "refreshToken": "%s"
            }
        """.formatted(TEST_REFRESH_TOKEN);

        // Act & Assert
        mockMvc.perform(post(AUTH_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    /**
     * Tests that access to /api/auth/refresh with invalid refresh token returns 401.
     * @since 1.0
     */
    @Test
    void shouldReturnUnauthorizedForInvalidRefreshToken() throws Exception {
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.status").value(401));
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /**
     * Tests that admin access to /api/messages/admin is allowed with valid token.
     * @since 1.0
     */
    @Test
    void shouldAllowAdminAccessToAdminMessages() throws Exception {
        // Arrange
        String loginJson = """
            {
                "username": "%s",
                "password": "%s"
            }
        """.formatted(ADMIN_USERNAME, TEST_PASSWORD);

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
        mockMvc.perform(get(ADMIN_MESSAGES_URL)
                        .header("Authorization", accessToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    /**
     * Tests that non-admin access to /api/messages/admin is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockNonAdminAccessToAdminMessages() throws Exception {
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
        mockMvc.perform(get(ADMIN_MESSAGES_URL)
                        .header("Authorization", accessToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                .andExpect(jsonPath("$.status").value(403));
    }

    /**
     * Tests that public access to /actuator/health is allowed without authentication.
     * @since 1.0
     */
    @Test
    void shouldAllowPublicAccessToActuatorHealth() throws Exception {
        // Act & Assert
        mockMvc.perform(get(ACTUATOR_HEALTH_URL))
                .andExpect(status().isOk());
    }

    /**
     * Tests that access with an invalid JWT token is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockInvalidJwtToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get(MESSAGES_URL)
                        .header("Authorization", "Bearer invalid-token")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /**
     * Tests that POST request to /api/messages works without CSRF token.
     * @since 1.0
     */
    @Test
    void shouldAllowPostWithoutCsrf() throws Exception {
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

        String messageJson = """
            {
                "content": "Test message"
            }
        """;

        // Act & Assert
        mockMvc.perform(post(MESSAGES_URL)
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson))
                .andExpect(status().isCreated());
    }

    /**
     * Tests that authenticated user can successfully log out.
     * @since 1.0
     */
    @Test
    void shouldAllowAuthenticatedUserToLogout() throws Exception {
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
        mockMvc.perform(post(AUTH_URL + "/logout")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assert refreshTokenRepository.findByToken(TEST_REFRESH_TOKEN).isEmpty();
    }

    /**
     * Tests that unauthenticated access to logout is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockUnauthenticatedAccessToLogout() throws Exception {
        // Act & Assert
        mockMvc.perform(post(AUTH_URL + "/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /**
     * Tests that admin access to /api/discord-messages/admin is allowed with valid token.
     * @since 1.0
     */
    @Test
    void shouldAllowAdminAccessToAdminDiscordMessages() throws Exception {
        // Arrange
        String loginJson = """
            {
                "username": "%s",
                "password": "%s"
            }
        """.formatted(ADMIN_USERNAME, TEST_PASSWORD);

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
        mockMvc.perform(get(ADMIN_DISCORD_MESSAGES_URL)
                        .header("Authorization", accessToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    /**
     * Tests that non-admin access to /api/discord-messages/admin is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockNonAdminAccessToAdminDiscordMessages() throws Exception {
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
        mockMvc.perform(get(ADMIN_DISCORD_MESSAGES_URL)
                        .header("Authorization", accessToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                .andExpect(jsonPath("$.status").value(403));
    }

    /**
     * Tests that unauthenticated access to /api/discord-messages/admin is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockUnauthenticatedAccessToAdminDiscordMessages() throws Exception {
        // Act & Assert
        mockMvc.perform(get(ADMIN_DISCORD_MESSAGES_URL)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /**
     * Tests that authenticated access to /api/users is allowed with valid token.
     * @since 1.0
     */
    @Test
    void shouldAllowAuthenticatedAccessToUsers() throws Exception {
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
        mockMvc.perform(patch(USERS_URL + "/model")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "model": "gpt-4"
                            }
                        """))
                .andExpect(status().isNoContent());
    }

    /**
     * Tests that unauthenticated access to /api/users is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockUnauthenticatedAccessToUsers() throws Exception {
        // Act & Assert
        mockMvc.perform(patch(USERS_URL + "/model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "model": "gpt-4"
                            }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /**
     * Tests that access to /api/users with an invalid JWT token is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockInvalidJwtTokenAccessToUsers() throws Exception {
        // Act & Assert
        mockMvc.perform(patch(USERS_URL + "/model")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "model": "gpt-4"
                            }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /**
     * Tests that admin access to /api/admin is allowed with valid token.
     * @since 1.0
     */
    @Test
    void shouldAllowAdminAccessToAdmin() throws Exception {
        // Arrange
        String loginJson = """
            {
                "username": "%s",
                "password": "%s"
            }
        """.formatted(ADMIN_USERNAME, TEST_PASSWORD);

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
        mockMvc.perform(delete(ADMIN_URL)
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "%s"
                            }
                        """.formatted(TEST_USERNAME)))
                .andExpect(status().isNoContent());
    }

    /**
     * Tests that non-admin access to /api/admin is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockNonAdminAccessToAdmin() throws Exception {
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
        mockMvc.perform(delete(ADMIN_URL)
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "%s"
                            }
                        """.formatted(TEST_USERNAME)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                .andExpect(jsonPath("$.status").value(403));
    }

    /**
     * Tests that unauthenticated access to /api/admin is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockUnauthenticatedAccessToAdmin() throws Exception {
        // Act & Assert
        mockMvc.perform(delete(ADMIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "%s"
                            }
                        """.formatted(TEST_USERNAME)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /**
     * Tests that access to /api/admin with an invalid JWT token is blocked.
     * @since 1.0
     */
    @Test
    void shouldBlockInvalidJwtTokenAccessToAdmin() throws Exception {
        // Act & Assert
        mockMvc.perform(delete(ADMIN_URL)
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "%s"
                            }
                        """.formatted(TEST_USERNAME)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.status").value(401));
    }
}
