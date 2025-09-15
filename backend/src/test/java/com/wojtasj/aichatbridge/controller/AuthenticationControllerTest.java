package com.wojtasj.aichatbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.configuration.JwtProperties;
import com.wojtasj.aichatbridge.configuration.SecurityConfig;
import com.wojtasj.aichatbridge.dto.LoginRequest;
import com.wojtasj.aichatbridge.dto.RefreshTokenRequest;
import com.wojtasj.aichatbridge.dto.RegisterRequest;
import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.exception.GlobalExceptionHandler;
import com.wojtasj.aichatbridge.exception.UserAlreadyExistsException;
import com.wojtasj.aichatbridge.service.AuthenticationService;
import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
import com.wojtasj.aichatbridge.service.RefreshTokenService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the functionality of {@link AuthenticationController} in the AI Chat Bridge application.
 * @since 1.0
 */
@WebMvcTest(AuthenticationController.class)
@ContextConfiguration(classes = {AuthenticationController.class, GlobalExceptionHandler.class, SecurityConfig.class})
@Import(SecurityConfig.class)
class AuthenticationControllerTest {

    private static final String AUTH_URL = "/api/auth";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String TEST_TOKEN = "jwt-token";
    private static final String TEST_REFRESH_TOKEN = "refresh-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String TEST_API_KEY = "testToken";
    private static final int MAX_TOKENS = 100;
    private static final String TEST_MODEL = "test-model";

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private AuthenticationService authenticationService;

    @SuppressWarnings("unused")
    @MockitoBean
    private AuthenticationManager authenticationManager;

    @SuppressWarnings("unused")
    @MockitoBean
    private JwtTokenProviderImpl jwtTokenProvider;

    @SuppressWarnings("unused")
    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @SuppressWarnings("unused")
    @MockitoBean
    private JwtProperties jwtProperties;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private UserEntity userEntity;
    private Authentication authentication;

    /**
     * Sets up the test environment with mocked dependencies.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        userEntity = UserEntity.builder()
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password("encodedPassword")
                .roles(Set.of(Role.USER))
                .apiKey(TEST_API_KEY)
                .maxTokens(MAX_TOKENS)
                .build();

        User userDetails = new User(TEST_USERNAME, "encodedPassword", Collections.emptyList());
        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        lenient().when(authenticationService.findByUsername(TEST_USERNAME)).thenReturn(userEntity);
    }

    @Nested
    class GetCurrentUserTests {
        /**
         * Tests retrieving current user information successfully.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldGetCurrentUserSuccessfully() throws Exception {
            // Arrange
            when(authenticationService.findByUsername(TEST_USERNAME)).thenReturn(userEntity);

            // Act & Assert
            mockMvc.perform(get(AUTH_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.maxTokens").value(MAX_TOKENS))
                    .andExpect(jsonPath("$.roles", hasSize(1)))
                    .andExpect(jsonPath("$.roles[0]").value("USER"));

            // Verify
            verify(authenticationService).findByUsername(TEST_USERNAME);
        }

        /**
         * Tests rejecting get current user when user is not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectGetCurrentUserWhenNotAuthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(get(AUTH_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                    .andExpect(jsonPath("$.title").value("Access Denied"))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(authenticationService, never()).findByUsername(any());
        }

        /**
         * Tests rejecting get current user when user is not found.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectGetCurrentUserWhenUserNotFound() throws Exception {
            // Arrange
            when(authenticationService.findByUsername(TEST_USERNAME))
                    .thenThrow(new AuthenticationException("User not found: " + TEST_USERNAME));

            // Act & Assert
            mockMvc.perform(get(AUTH_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value("User not found: " + TEST_USERNAME));

            // Verify
            verify(authenticationService).findByUsername(TEST_USERNAME);
        }
    }

    @Nested
    class RegistrationTests {
        /**
         * Tests registering a new user successfully.
         * @since 1.0
         */
        @Test
        void shouldRegisterUserSuccessfully() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, TEST_API_KEY, MAX_TOKENS, TEST_MODEL);
            when(authenticationService.register(request)).thenReturn(userEntity);

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.maxTokens").value(MAX_TOKENS))
                    .andExpect(jsonPath("$.roles", hasSize(1)))
                    .andExpect(jsonPath("$.roles[0]").value("USER"));

            // Verify
            verify(authenticationService).register(request);
        }

        /**
         * Tests rejecting registration with empty username.
         * @since 1.0
         */
        @Test
        void shouldRejectRegistrationWithEmptyUsername() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest("", TEST_EMAIL, TEST_PASSWORD, TEST_API_KEY, MAX_TOKENS, TEST_MODEL);

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("username Username cannot be blank")));

            // Verify
            verify(authenticationService, never()).register(any());
        }

        /**
         * Tests rejecting registration with invalid email.
         * @since 1.0
         */
        @Test
        void shouldRejectRegistrationWithInvalidEmail() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest(TEST_USERNAME, "invalid-email", TEST_PASSWORD, TEST_API_KEY, MAX_TOKENS, TEST_MODEL);

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("email Email must be a valid email address")));

            // Verify
            verify(authenticationService, never()).register(any());
        }

        /**
         * Tests rejecting registration with invalid password.
         * @since 1.0
         */
        @Test
        void shouldRejectRegistrationWithInvalidPassword() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, "invalid", TEST_API_KEY, MAX_TOKENS, TEST_MODEL);

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("password Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")));

            // Verify
            verify(authenticationService, never()).register(any());
        }

        /**
         * Tests rejecting registration when username is taken.
         * @since 1.0
         */
        @Test
        void shouldRejectRegistrationWhenUsernameTaken() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, TEST_API_KEY, MAX_TOKENS, TEST_MODEL);
            when(authenticationService.register(request))
                    .thenThrow(new UserAlreadyExistsException("Username already taken"));

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("/problems/registration-failed"))
                    .andExpect(jsonPath("$.title").value("Registration Failed"))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value("Username already taken"));

            // Verify
            verify(authenticationService).register(request);
        }

        /**
         * Tests rejecting registration with empty API key.
         * @since 1.0
         */
        @Test
        void shouldRejectRegistrationWithEmptyApiKey() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, "", MAX_TOKENS, TEST_MODEL);

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("apiKey API key cannot be blank")));

            // Verify
            verify(authenticationService, never()).register(any());
        }

        /**
         * Tests rejecting registration with negative maxTokens.
         * @since 1.0
         */
        @Test
        void shouldRejectRegistrationWithNegativeMaxTokens() throws Exception {
            // Arrange
            @SuppressWarnings("DataFlowIssue")
            RegisterRequest request = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, TEST_API_KEY, -1, TEST_MODEL);

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("maxTokens Max tokens must be at least 1")));

            // Verify
            verify(authenticationService, never()).register(any());
        }

        /**
         * Tests rejecting registration with null maxTokens.
         * @since 1.0
         */
        @Test
        void shouldRejectRegistrationWithNullMaxTokens() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, TEST_API_KEY, null, TEST_MODEL);

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("maxTokens Max tokens cannot be null")));

            // Verify
            verify(authenticationService, never()).register(any());
        }
    }

    @Nested
    class LoginTests {
        /**
         * Tests successful user login and token generation.
         * @since 1.0
         */
        @Test
        void shouldLoginUserSuccessfully() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtTokenProvider.generateToken(authentication)).thenReturn(TEST_TOKEN);
            when(authenticationService.findByUsername(TEST_USERNAME)).thenReturn(userEntity);
            when(refreshTokenService.generateRefreshToken(userEntity))
                    .thenReturn(createRefreshTokenEntity(TEST_REFRESH_TOKEN, userEntity));

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(TEST_TOKEN))
                    .andExpect(jsonPath("$.refreshToken").value(TEST_REFRESH_TOKEN));

            // Verify
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtTokenProvider).generateToken(authentication);
            verify(authenticationService).findByUsername(TEST_USERNAME);
            verify(refreshTokenService).generateRefreshToken(userEntity);
        }

        /**
         * Tests rejecting login with invalid credentials.
         * @since 1.0
         */
        @Test
        void shouldRejectLoginWithInvalidCredentials() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest(TEST_USERNAME, "wrongpassword");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value("Invalid username or password"));

            // Verify
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtTokenProvider, never()).generateToken(any());
            verify(authenticationService, never()).findByUsername(any());
            verify(refreshTokenService, never()).generateRefreshToken(any());
        }

        /**
         * Tests rejecting login with empty username.
         * @since 1.0
         */
        @Test
        void shouldRejectLoginWithEmptyUsername() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("", TEST_PASSWORD);

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("username Username or email cannot be blank")));

            // Verify
            verify(authenticationManager, never()).authenticate(any());
            verify(jwtTokenProvider, never()).generateToken(any());
            verify(authenticationService, never()).findByUsername(any());
            verify(refreshTokenService, never()).generateRefreshToken(any());
        }
    }

    @Nested
    class RefreshTokenTests {
        /**
         * Tests successful token refresh and rotation.
         * @since 1.0
         */
        @Test
        void shouldRefreshTokenSuccessfully() throws Exception {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest(TEST_REFRESH_TOKEN);
            RefreshTokenEntity refreshToken = createRefreshTokenEntity(TEST_REFRESH_TOKEN, userEntity);
            when(refreshTokenService.validateRefreshToken(TEST_REFRESH_TOKEN)).thenReturn(refreshToken);
            when(refreshTokenService.generateRefreshToken(userEntity))
                    .thenReturn(createRefreshTokenEntity("new-refresh-token", userEntity));
            when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("new-access-token");

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

            // Verify
            verify(refreshTokenService).validateRefreshToken(TEST_REFRESH_TOKEN);
            verify(refreshTokenService).deleteByUser(userEntity);
            verify(refreshTokenService).generateRefreshToken(userEntity);
            verify(jwtTokenProvider).generateToken(any(Authentication.class));
        }

        /**
         * Tests rejecting token refresh with invalid refresh token.
         * @since 1.0
         */
        @Test
        void shouldRejectRefreshWithInvalidToken() throws Exception {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest(INVALID_TOKEN);
            when(refreshTokenService.validateRefreshToken(INVALID_TOKEN))
                    .thenThrow(new AuthenticationException("Invalid refresh token"));

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value("Invalid refresh token"));

            // Verify
            verify(refreshTokenService).validateRefreshToken(INVALID_TOKEN);
            verify(refreshTokenService, never()).deleteByUser(any());
            verify(refreshTokenService, never()).generateRefreshToken(any());
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        /**
         * Tests rejecting token refresh with empty refresh token.
         * @since 1.0
         */
        @Test
        void shouldRejectRefreshWithEmptyToken() throws Exception {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest("");

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("refreshToken Refresh token cannot be blank")));

            // Verify
            verify(refreshTokenService, never()).validateRefreshToken(any());
        }

        /**
         * Tests rate limiting for token refresh endpoint.
         * @since 1.0
         */
        @Test
        void shouldRejectRefreshWhenRateLimitExceeded() throws Exception {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest(TEST_REFRESH_TOKEN);
            RateLimiter rateLimiter = mock(RateLimiter.class);
            RateLimiterConfig rateLimiterConfig = RateLimiterConfig.ofDefaults();
            when(rateLimiter.getName()).thenReturn("refreshToken");
            when(rateLimiter.getRateLimiterConfig()).thenReturn(rateLimiterConfig);
            RequestNotPermitted exception = RequestNotPermitted.createRequestNotPermitted(rateLimiter);
            when(refreshTokenService.validateRefreshToken(TEST_REFRESH_TOKEN)).thenThrow(exception);

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.type").value("/problems/rate-limit-exceeded"))
                    .andExpect(jsonPath("$.title").value("Rate Limit Exceeded"))
                    .andExpect(jsonPath("$.status").value(429))
                    .andExpect(jsonPath("$.detail").value("Too many requests - rate limit exceeded for RateLimiter 'refreshToken' does not permit further calls"))
                    .andExpect(jsonPath("$.instance").value(AUTH_URL + "/refresh"));

            // Verify
            verify(refreshTokenService).validateRefreshToken(TEST_REFRESH_TOKEN);
            verify(refreshTokenService, never()).deleteByUser(any());
            verify(refreshTokenService, never()).generateRefreshToken(any());
            verify(jwtTokenProvider, never()).generateToken(any());
        }
    }

    @Nested
    class LogoutTests {
        /**
         * Tests successful user logout.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldLogoutUserSuccessfully() throws Exception {
            // Arrange
            when(authenticationService.findByUsername(TEST_USERNAME)).thenReturn(userEntity);
            doNothing().when(refreshTokenService).deleteByUser(userEntity);

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // Verify
            verify(authenticationService).findByUsername(TEST_USERNAME);
            verify(refreshTokenService).deleteByUser(userEntity);
        }

        /**
         * Tests rejecting logout when user is not found.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectLogoutWhenUserNotFound() throws Exception {
            // Arrange
            when(authenticationService.findByUsername(TEST_USERNAME))
                    .thenThrow(new AuthenticationException("User not found: " + TEST_USERNAME));

            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value("User not found: " + TEST_USERNAME));

            // Verify
            verify(authenticationService).findByUsername(TEST_USERNAME);
            verify(refreshTokenService, never()).deleteByUser(any());
        }

        /**
         * Tests rejecting logout when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectLogoutWhenNotAuthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(post(AUTH_URL + "/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(authenticationService, never()).findByUsername(any());
            verify(refreshTokenService, never()).deleteByUser(any());
        }
    }

    /**
     * Creates a mock RefreshTokenEntity for testing purposes.
     * @param token the refresh token
     * @param user the UserEntity object
     * @return a RefreshTokenEntity with the specified token and user
     * @since 1.0
     */
    private RefreshTokenEntity createRefreshTokenEntity(String token, UserEntity user) {
        return RefreshTokenEntity.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
    }
}
