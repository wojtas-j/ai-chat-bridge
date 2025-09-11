package com.wojtasj.aichatbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.dto.LoginRequest;
import com.wojtasj.aichatbridge.dto.RefreshTokenRequest;
import com.wojtasj.aichatbridge.dto.RegisterRequest;
import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.exception.UserAlreadyExistsException;
import com.wojtasj.aichatbridge.exception.GlobalExceptionHandler;
import com.wojtasj.aichatbridge.service.AuthenticationService;
import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
import com.wojtasj.aichatbridge.service.RefreshTokenService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationControllerTest {

    private static final String AUTH_URL = "/api/auth";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String TEST_TOKEN = "jwt-token";
    private static final String TEST_REFRESH_TOKEN = "refresh-token";
    private static final String INVALID_TOKEN = "invalid-token";

    private MockMvc mockMvc;
    private AuthenticationService authenticationService;
    private AuthenticationManager authenticationManager;
    private JwtTokenProviderImpl jwtTokenProvider;
    private RefreshTokenService refreshTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private UserEntity userEntity;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtTokenProvider = mock(JwtTokenProviderImpl.class);
        refreshTokenService = mock(RefreshTokenService.class);

        AuthenticationController authenticationController = new AuthenticationController(
                authenticationService,
                authenticationManager,
                jwtTokenProvider,
                refreshTokenService
        );

        userEntity = UserEntity.builder()
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password("encodedPassword")
                .roles(Set.of(Role.USER))
                .build();

        UserDetails userDetails = new User(TEST_USERNAME, "encodedPassword", Collections.emptyList());
        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        HandlerMethodArgumentResolver authenticationPrincipalResolver = new AuthenticationPrincipalArgumentResolver();

        mockMvc = MockMvcBuilders
                .standaloneSetup(authenticationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(authenticationPrincipalResolver)
                .build();

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

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
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }
            return auth.getPrincipal();
        }
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
            when(authenticationService.findByUsername(TEST_USERNAME)).thenReturn(userEntity);

            mockMvc.perform(get(AUTH_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.roles", hasSize(1)))
                    .andExpect(jsonPath("$.roles[0]").value("USER"));

            verify(authenticationService).findByUsername(TEST_USERNAME);
        }

        /**
         * Tests rejecting get current user when user is not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectGetCurrentUserWhenNotAuthenticated() throws Exception {
            SecurityContextHolder.clearContext();

            mockMvc.perform(get(AUTH_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                    .andExpect(jsonPath("$.title").value("Access Denied"))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));
            verify(authenticationService, never()).findByUsername(any());
        }

        /**
         * Tests rejecting get current user when user is not found.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectGetCurrentUserWhenUserNotFound() throws Exception {
            when(authenticationService.findByUsername(TEST_USERNAME)).thenThrow(new AuthenticationException("User not found: " + TEST_USERNAME));

            mockMvc.perform(get(AUTH_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(containsString("User not found: " + TEST_USERNAME)));
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
            RegisterRequest request = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);
            when(authenticationService.register(request)).thenReturn(userEntity);

            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.roles", hasSize(1)))
                    .andExpect(jsonPath("$.roles[0]").value("USER"));
            verify(authenticationService).register(request);
        }

        /**
         * Tests rejecting registration with invalid request data.
         * @since 1.0
         */
        @Test
        void shouldRejectRegistrationWithInvalidData() throws Exception {
            RegisterRequest request = new RegisterRequest("", TEST_EMAIL, TEST_PASSWORD);

            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("username Username cannot be blank")));
            verify(authenticationService, never()).register(any());
        }

        /**
         * Tests rejecting registration when username is taken.
         * @since 1.0
         */
        @Test
        void shouldRejectRegistrationWhenUsernameTaken() throws Exception {
            RegisterRequest request = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);
            when(authenticationService.register(request))
                    .thenThrow(new UserAlreadyExistsException("Username already taken"));

            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("/problems/registration-failed"))
                    .andExpect(jsonPath("$.title").value("Registration Failed"))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value("Username already taken"));
            verify(authenticationService).register(request);
        }

        /**
         * Tests rejecting registration with invalid password.
         * @since 1.0
         */
        @Test
        void shouldRejectRegistrationWithInvalidPassword() throws Exception {
            RegisterRequest request = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, "invalid");

            mockMvc.perform(post(AUTH_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("password Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")));
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
            LoginRequest request = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtTokenProvider.generateToken(authentication)).thenReturn(TEST_TOKEN);
            when(authenticationService.findByUsername(TEST_USERNAME)).thenReturn(userEntity);
            when(refreshTokenService.generateRefreshToken(userEntity))
                    .thenReturn(createRefreshTokenEntity(TEST_REFRESH_TOKEN, userEntity));

            mockMvc.perform(post(AUTH_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(TEST_TOKEN))
                    .andExpect(jsonPath("$.refreshToken").value(TEST_REFRESH_TOKEN));
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
            LoginRequest request = new LoginRequest(TEST_USERNAME, "wrongpassword");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            mockMvc.perform(post(AUTH_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value("Invalid username or password"));
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtTokenProvider, never()).generateToken(any());
            verify(authenticationService, never()).findByUsername(any());
            verify(refreshTokenService, never()).generateRefreshToken(any());
        }

        /**
         * Tests rejecting login with invalid request data.
         * @since 1.0
         */
        @Test
        void shouldRejectLoginWithInvalidData() throws Exception {
            LoginRequest request = new LoginRequest("", TEST_PASSWORD);

            mockMvc.perform(post(AUTH_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("username Username or email cannot be blank")));
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
        @WithMockUser(username = TEST_USERNAME)
        void shouldRefreshTokenSuccessfully() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest(TEST_REFRESH_TOKEN);
            RefreshTokenEntity refreshToken = createRefreshTokenEntity(TEST_REFRESH_TOKEN, userEntity);
            when(refreshTokenService.validateRefreshToken(TEST_REFRESH_TOKEN)).thenReturn(refreshToken);
            when(refreshTokenService.generateRefreshToken(userEntity))
                    .thenReturn(createRefreshTokenEntity("new-refresh-token", userEntity));
            when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("new-access-token");

            mockMvc.perform(post(AUTH_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
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
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectRefreshWithInvalidToken() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest(INVALID_TOKEN);
            when(refreshTokenService.validateRefreshToken(INVALID_TOKEN)).thenReturn(null);

            mockMvc.perform(post(AUTH_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value("Invalid refresh token"));
            verify(refreshTokenService).validateRefreshToken(INVALID_TOKEN);
            verify(refreshTokenService, never()).deleteByUser(any());
            verify(refreshTokenService, never()).generateRefreshToken(any());
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        /**
         * Tests rejecting token refresh when user does not match.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectRefreshWhenUserDoesNotMatch() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest(TEST_REFRESH_TOKEN);
            UserEntity otherUser = UserEntity.builder()
                    .username("otheruser")
                    .email("other@example.com")
                    .password("encodedPassword")
                    .roles(Set.of(Role.USER))
                    .build();
            RefreshTokenEntity refreshToken = createRefreshTokenEntity(TEST_REFRESH_TOKEN, otherUser);
            when(refreshTokenService.validateRefreshToken(TEST_REFRESH_TOKEN)).thenReturn(refreshToken);

            mockMvc.perform(post(AUTH_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value("Refresh token does not match authenticated user"));
            verify(refreshTokenService).validateRefreshToken(TEST_REFRESH_TOKEN);
            verify(refreshTokenService, never()).deleteByUser(any());
            verify(refreshTokenService, never()).generateRefreshToken(any());
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        /**
         * Tests rejecting token refresh when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectRefreshWhenNotAuthenticated() throws Exception {
            SecurityContextHolder.clearContext();
            RefreshTokenRequest request = new RefreshTokenRequest(TEST_REFRESH_TOKEN);

            mockMvc.perform(post(AUTH_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                    .andExpect(jsonPath("$.title").value("Access Denied"))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));
            verify(refreshTokenService, never()).validateRefreshToken(any());
        }

        /**
         * Tests rejecting token refresh with invalid request data.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectRefreshWithInvalidData() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("");

            mockMvc.perform(post(AUTH_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("refreshToken Refresh token cannot be blank")));
            verify(refreshTokenService, never()).validateRefreshToken(any());
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
            when(authenticationService.findByUsername(TEST_USERNAME)).thenReturn(userEntity);
            doNothing().when(refreshTokenService).deleteByUser(userEntity);

            mockMvc.perform(post(AUTH_URL + "/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
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
            when(authenticationService.findByUsername(TEST_USERNAME)).thenThrow(new AuthenticationException("User not found: " + TEST_USERNAME));

            mockMvc.perform(post(AUTH_URL + "/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(containsString("User not found: " + TEST_USERNAME)));
            verify(authenticationService).findByUsername(TEST_USERNAME);
            verify(refreshTokenService, never()).deleteByUser(any());
        }

        /**
         * Tests rejecting logout when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectLogoutWhenNotAuthenticated() throws Exception {
            SecurityContextHolder.clearContext();

            mockMvc.perform(post(AUTH_URL + "/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                    .andExpect(jsonPath("$.title").value("Access Denied"))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));
            verify(authenticationService, never()).findByUsername(any());
            verify(refreshTokenService, never()).deleteByUser(any());
        }
    }

    private RefreshTokenEntity createRefreshTokenEntity(String token, UserEntity user) {
        return RefreshTokenEntity.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
    }
}
