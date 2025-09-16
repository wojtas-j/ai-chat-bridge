package com.wojtasj.aichatbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.configuration.SecurityConfig;
import com.wojtasj.aichatbridge.dto.UpdateEmailRequest;
import com.wojtasj.aichatbridge.dto.UpdateMaxTokensRequest;
import com.wojtasj.aichatbridge.dto.UpdateModelRequest;
import com.wojtasj.aichatbridge.dto.UpdateOpenAIApiKeyRequest;
import com.wojtasj.aichatbridge.dto.UpdatePasswordRequest;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.exception.GlobalExceptionHandler;
import com.wojtasj.aichatbridge.exception.UserAlreadyExistsException;
import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
import com.wojtasj.aichatbridge.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the functionality of {@link UserController} in the AI Chat Bridge application.
 * @since 1.0
 */
@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {UserController.class, GlobalExceptionHandler.class, SecurityConfig.class})
@Import(SecurityConfig.class)
class UserControllerTest {

    private static final String USERS_URL = "/api/users";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String TEST_NEW_PASSWORD = "NewPassword123!";
    private static final String WRONG_PASSWORD = "wrongpassword";
    private static final String TEST_MODEL = "gpt-4o-mini";
    private static final String TEST_API_KEY = "sk-test-api-key";

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private UserService userService;

    @SuppressWarnings("unused")
    @MockitoBean
    private JwtTokenProviderImpl jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @Nested
    class UpdatePasswordTests {
        /**
         * Tests updating the user's password successfully.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldUpdatePasswordSuccessfully() throws Exception {
            // Arrange
            UpdatePasswordRequest request = new UpdatePasswordRequest(TEST_PASSWORD, TEST_NEW_PASSWORD);
            doNothing().when(userService).updatePassword(TEST_USERNAME, TEST_PASSWORD, TEST_NEW_PASSWORD);

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify
            verify(userService).updatePassword(TEST_USERNAME, TEST_PASSWORD, TEST_NEW_PASSWORD);
        }

        /**
         * Tests rejecting password update with incorrect current password.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectUpdatePasswordWithIncorrectCurrentPassword() throws Exception {
            // Arrange
            UpdatePasswordRequest request = new UpdatePasswordRequest(WRONG_PASSWORD, TEST_NEW_PASSWORD);
            doThrow(new AuthenticationException("Current password is incorrect"))
                    .when(userService).updatePassword(TEST_USERNAME, WRONG_PASSWORD, TEST_NEW_PASSWORD);

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value("Current password is incorrect"));

            // Verify
            verify(userService).updatePassword(TEST_USERNAME, WRONG_PASSWORD, TEST_NEW_PASSWORD);
        }

        /**
         * Tests rejecting password update with invalid new password.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectUpdatePasswordWithInvalidNewPassword() throws Exception {
            // Arrange
            UpdatePasswordRequest request = new UpdatePasswordRequest(TEST_PASSWORD, "short");

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("newPassword Password must be at least 8 characters")));

            // Verify
            verify(userService, never()).updatePassword(any(), any(), any());
        }

        /**
         * Tests rejecting password update when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectUpdatePasswordWhenNotAuthenticated() throws Exception {
            // Arrange
            UpdatePasswordRequest request = new UpdatePasswordRequest(TEST_PASSWORD, TEST_NEW_PASSWORD);

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(userService, never()).updatePassword(any(), any(), any());
        }
    }

    @Nested
    class UpdateEmailTests {
        /**
         * Tests updating the user's email successfully.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldUpdateEmailSuccessfully() throws Exception {
            // Arrange
            UpdateEmailRequest request = new UpdateEmailRequest("newemail@example.com");
            doNothing().when(userService).updateEmail(TEST_USERNAME, "newemail@example.com");

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify
            verify(userService).updateEmail(TEST_USERNAME, "newemail@example.com");
        }

        /**
         * Tests rejecting email update with email already in use.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectUpdateEmailWithEmailAlreadyInUse() throws Exception {
            // Arrange
            UpdateEmailRequest request = new UpdateEmailRequest("existing@example.com");
            doThrow(new UserAlreadyExistsException("Email already in use: existing@example.com"))
                    .when(userService).updateEmail(TEST_USERNAME, "existing@example.com");

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("/problems/registration-failed"))
                    .andExpect(jsonPath("$.title").value("Registration Failed"))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value("Email already in use: existing@example.com"));

            // Verify
            verify(userService).updateEmail(TEST_USERNAME, "existing@example.com");
        }

        /**
         * Tests rejecting email update with invalid email.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectUpdateEmailWithInvalidEmail() throws Exception {
            // Arrange
            UpdateEmailRequest request = new UpdateEmailRequest("invalid-email");

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("email Email must be a valid email address")));

            // Verify
            verify(userService, never()).updateEmail(any(), any());
        }

        /**
         * Tests rejecting email update when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectUpdateEmailWhenNotAuthenticated() throws Exception {
            // Arrange
            UpdateEmailRequest request = new UpdateEmailRequest("newemail@example.com");

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(userService, never()).updateEmail(any(), any());
        }
    }

    @Nested
    class UpdateApiKeyTests {
        /**
         * Tests updating the user's OpenAI API key successfully.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldUpdateApiKeySuccessfully() throws Exception {
            // Arrange
            UpdateOpenAIApiKeyRequest request = new UpdateOpenAIApiKeyRequest(TEST_API_KEY);
            doNothing().when(userService).updateApiKey(TEST_USERNAME, TEST_API_KEY);

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/api-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify
            verify(userService).updateApiKey(TEST_USERNAME, TEST_API_KEY);
        }

        /**
         * Tests updating the user's OpenAI API key successfully.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectUpdateApiKeyWithInvalidApiKey() throws Exception {
            // Arrange
            String invalidApiKey = "invalid-api-key";
            UpdateOpenAIApiKeyRequest request = new UpdateOpenAIApiKeyRequest(invalidApiKey);
            doNothing().when(userService).updateApiKey(TEST_USERNAME, invalidApiKey);

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/api-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("apiKey API key must start with 'sk-' and contain only letters, numbers, underscores, or hyphens")));

            // Verify
            verify(userService, never()).updateApiKey(any(), any());
        }

        /**
         * Tests rejecting API key update with empty API key.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectUpdateApiKeyWithEmptyApiKey() throws Exception {
            // Arrange
            UpdateOpenAIApiKeyRequest request = new UpdateOpenAIApiKeyRequest("");

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/api-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("apiKey API key cannot be blank")));

            // Verify
            verify(userService, never()).updateApiKey(any(), any());
        }

        /**
         * Tests rejecting API key update when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectUpdateApiKeyWhenNotAuthenticated() throws Exception {
            // Arrange
            UpdateOpenAIApiKeyRequest request = new UpdateOpenAIApiKeyRequest(TEST_API_KEY);

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/api-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(userService, never()).updateApiKey(any(), any());
        }
    }

    @Nested
    class UpdateMaxTokensTests {
        /**
         * Tests updating the user's max tokens successfully.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldUpdateMaxTokensSuccessfully() throws Exception {
            // Arrange
            UpdateMaxTokensRequest request = new UpdateMaxTokensRequest(200);
            doNothing().when(userService).updateMaxTokens(TEST_USERNAME, 200);

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/max-tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify
            verify(userService).updateMaxTokens(TEST_USERNAME, 200);
        }

        /**
         * Tests rejecting max tokens update with invalid value.
         * @since 1.0
         */
        @SuppressWarnings("ConstantConditions")
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectUpdateMaxTokensWithInvalidValue() throws Exception {
            // Arrange
            UpdateMaxTokensRequest request = new UpdateMaxTokensRequest(0);

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/max-tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("maxTokens Max tokens must be at least 1")));

            // Verify
            verify(userService, never()).updateMaxTokens(any(), any());
        }

        /**
         * Tests rejecting max tokens update when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectUpdateMaxTokensWhenNotAuthenticated() throws Exception {
            // Arrange
            UpdateMaxTokensRequest request = new UpdateMaxTokensRequest(200);

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/max-tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(userService, never()).updateMaxTokens(any(), any());
        }
    }

    @Nested
    class UpdateModelTests {
        /**
         * Tests updating the user's model successfully.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldUpdateModelSuccessfully() throws Exception {
            // Arrange
            UpdateModelRequest request = new UpdateModelRequest(TEST_MODEL);
            doNothing().when(userService).updateModel(TEST_USERNAME, TEST_MODEL);

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/model")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify
            verify(userService).updateModel(TEST_USERNAME, TEST_MODEL);
        }

        /**
         * Tests rejecting model update with empty model.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectUpdateModelWithEmptyModel() throws Exception {
            // Arrange
            UpdateModelRequest request = new UpdateModelRequest("");

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/model")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("model Model cannot be blank")));

            // Verify
            verify(userService, never()).updateModel(any(), any());
        }

        /**
         * Tests rejecting model update when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectUpdateModelWhenNotAuthenticated() throws Exception {
            // Arrange
            UpdateModelRequest request = new UpdateModelRequest(TEST_MODEL);

            // Act & Assert
            mockMvc.perform(patch(USERS_URL + "/model")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(userService, never()).updateModel(any(), any());
        }
    }

    @Nested
    class DeleteAccountTests {
        /**
         * Tests deleting the user's account successfully.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldDeleteAccountSuccessfully() throws Exception {
            // Arrange
            doNothing().when(userService).deleteAccount(TEST_USERNAME);

            // Act & Assert
            mockMvc.perform(delete(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verify
            verify(userService).deleteAccount(TEST_USERNAME);
        }

        /**
         * Tests rejecting account deletion when user is not found.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectDeleteAccountWhenUserNotFound() throws Exception {
            // Arrange
            doThrow(new AuthenticationException("User not found: " + TEST_USERNAME))
                    .when(userService).deleteAccount(TEST_USERNAME);

            // Act & Assert
            mockMvc.perform(delete(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value("User not found: " + TEST_USERNAME));

            // Verify
            verify(userService).deleteAccount(TEST_USERNAME);
        }

        /**
         * Tests rejecting account deletion when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectDeleteAccountWhenNotAuthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(delete(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(userService, never()).deleteAccount(any());
        }
    }
}
