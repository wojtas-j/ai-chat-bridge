package com.wojtasj.aichatbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.configuration.SecurityConfig;
import com.wojtasj.aichatbridge.dto.AdminGetUsersResponse;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.exception.GlobalExceptionHandler;
import com.wojtasj.aichatbridge.exception.UserNotFoundException;
import com.wojtasj.aichatbridge.service.AdminService;
import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the functionality of {@link AdminController} in the AI Chat Bridge application.
 * @since 1.0
 */
@WebMvcTest(AdminController.class)
@ContextConfiguration(classes = {AdminController.class, GlobalExceptionHandler.class, SecurityConfig.class})
@Import(SecurityConfig.class)
class AdminControllerTest {

    private static final String ADMIN_URL = "/api/admin";
    private static final Long TEST_USER_ID = 2L;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_MODEL = "gpt-4o-mini";
    private static final int TEST_MAX_TOKENS = 100;

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private AdminService adminService;

    @SuppressWarnings("unused")
    @MockitoBean
    private JwtTokenProviderImpl jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @Nested
    class GetAllUsersTests {
        /**
         * Tests retrieving a paginated list of users successfully as an admin.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME, roles = {"ADMIN"})
        void shouldGetAllUsersSuccessfully() throws Exception {
            // Arrange
            AdminGetUsersResponse userResponse = new AdminGetUsersResponse(
                    TEST_USER_ID, TEST_USERNAME, TEST_EMAIL, TEST_MAX_TOKENS, TEST_MODEL, LocalDateTime.now(), Set.of(Role.USER)
            );
            Page<AdminGetUsersResponse> usersPage = new PageImpl<>(List.of(userResponse), PageRequest.of(0, 20, Sort.by("createdAt").descending()), 1);
            when(adminService.getAllUsers(any())).thenReturn(usersPage);

            // Act & Assert
            mockMvc.perform(get(ADMIN_URL + "/users")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.content[0].email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.content[0].roles[0]").value("USER"))
                    .andExpect(jsonPath("$.content[0].maxTokens").value(TEST_MAX_TOKENS))
                    .andExpect(jsonPath("$.content[0].model").value(TEST_MODEL))
                    .andExpect(jsonPath("$.totalElements").value(1));

            // Verify
            verify(adminService).getAllUsers(any());
        }

        /**
         * Tests rejecting get all users when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectGetAllUsersWhenNotAuthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ADMIN_URL + "/users")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(adminService, never()).getAllUsers(any());
        }

        /**
         * Tests rejecting get all users when user lacks ADMIN role.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME, roles = {"USER"})
        void shouldRejectGetAllUsersWhenNotAdmin() throws Exception {
            // Act & Assert
            mockMvc.perform(get(ADMIN_URL + "/users")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                    .andExpect(jsonPath("$.title").value("Access Denied"))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));

            // Verify
            verify(adminService, never()).getAllUsers(any());
        }
    }

    @Nested
    class DeleteUserTests {
        /**
         * Tests deleting a user successfully as an admin.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME, roles = {"ADMIN"})
        void shouldDeleteUserSuccessfully() throws Exception {
            // Arrange
            doNothing().when(adminService).deleteUser(TEST_USER_ID);

            // Act & Assert
            mockMvc.perform(delete(ADMIN_URL + "/users/{id}", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verify
            verify(adminService).deleteUser(TEST_USER_ID);
        }

        /**
         * Tests rejecting user deletion when user is not found.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME, roles = {"ADMIN"})
        void shouldRejectDeleteUserWhenUserNotFound() throws Exception {
            // Arrange
            doThrow(new UserNotFoundException("User not found with ID: " + TEST_USER_ID))
                    .when(adminService).deleteUser(TEST_USER_ID);

            // Act & Assert
            mockMvc.perform(delete(ADMIN_URL + "/users/{id}", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("/problems/user-not-found"))
                    .andExpect(jsonPath("$.title").value("User Not Found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("User not found with ID: " + TEST_USER_ID));

            // Verify
            verify(adminService).deleteUser(TEST_USER_ID);
        }

        /**
         * Tests rejecting user deletion when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectDeleteUserWhenNotAuthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(delete(ADMIN_URL + "/users/{id}", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(adminService, never()).deleteUser(any());
        }

        /**
         * Tests rejecting user deletion when user lacks ADMIN role.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME, roles = {"USER"})
        void shouldRejectDeleteUserWhenNotAdmin() throws Exception {
            // Act & Assert
            mockMvc.perform(delete(ADMIN_URL + "/users/{id}", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                    .andExpect(jsonPath("$.title").value("Access Denied"))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));

            // Verify
            verify(adminService, never()).deleteUser(any());
        }
    }

    @Nested
    class DeleteRefreshTokensTests {
        /**
         * Tests deleting a user's refresh tokens successfully as an admin.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME, roles = {"ADMIN"})
        void shouldDeleteRefreshTokensSuccessfully() throws Exception {
            // Arrange
            doNothing().when(adminService).deleteRefreshTokens(TEST_USER_ID);

            // Act & Assert
            mockMvc.perform(delete(ADMIN_URL + "/refresh-tokens/{id}", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verify
            verify(adminService).deleteRefreshTokens(TEST_USER_ID);
        }

        /**
         * Tests rejecting refresh tokens deletion when user is not found.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME, roles = {"ADMIN"})
        void shouldRejectDeleteRefreshTokensWhenUserNotFound() throws Exception {
            // Arrange
            doThrow(new UserNotFoundException("User not found with ID: " + TEST_USER_ID))
                    .when(adminService).deleteRefreshTokens(TEST_USER_ID);

            // Act & Assert
            mockMvc.perform(delete(ADMIN_URL + "/refresh-tokens/{id}", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("/problems/user-not-found"))
                    .andExpect(jsonPath("$.title").value("User Not Found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("User not found with ID: " + TEST_USER_ID));

            // Verify
            verify(adminService).deleteRefreshTokens(TEST_USER_ID);
        }

        /**
         * Tests rejecting refresh tokens deletion when not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectDeleteRefreshTokensWhenNotAuthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(delete(ADMIN_URL + "/refresh-tokens/{id}", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(adminService, never()).deleteRefreshTokens(any());
        }

        /**
         * Tests rejecting refresh tokens deletion when user lacks ADMIN role.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME, roles = {"USER"})
        void shouldRejectDeleteRefreshTokensWhenNotAdmin() throws Exception {
            // Act & Assert
            mockMvc.perform(delete(ADMIN_URL + "/refresh-tokens/{id}", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                    .andExpect(jsonPath("$.title").value("Access Denied"))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));

            // Verify
            verify(adminService, never()).deleteRefreshTokens(any());
        }
    }
}
