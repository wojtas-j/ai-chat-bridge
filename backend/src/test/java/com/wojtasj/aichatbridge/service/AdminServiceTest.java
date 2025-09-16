package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.dto.AdminGetUsersResponse;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.UserNotFoundException;
import com.wojtasj.aichatbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the functionality of {@link AdminServiceImpl} in the AI Chat Bridge application.
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AdminServiceTest {

    private static final Long TEST_USER_ID = 2L;
    private static final Long INVALID_USER_ID = Long.MAX_VALUE;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_MODEL = "gpt-4o-mini";
    private static final int TEST_MAX_TOKENS = 100;
    private static final LocalDateTime TEST_CREATED_AT = LocalDateTime.now();
    private static final Set<Role> TEST_ROLES = Set.of(Role.USER);

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .id(TEST_USER_ID)
                .username(TEST_USERNAME)
                .password("Password123!")
                .email(TEST_EMAIL)
                .maxTokens(TEST_MAX_TOKENS)
                .model(TEST_MODEL)
                .roles(TEST_ROLES)
                .createdAt(TEST_CREATED_AT)
                .apiKey("sk-api-key")
                .build();

        reset(userRepository, refreshTokenService);
    }

    @Nested
    class GetAllUsersTests {
        /**
         * Tests retrieving a paginated list of users successfully.
         * @since 1.0
         */
        @Test
        void shouldGetAllUsersSuccessfully() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            Page<UserEntity> usersPage = new PageImpl<>(List.of(testUser), pageable, 1);
            when(userRepository.findAll(any(Pageable.class))).thenReturn(usersPage);

            // Act
            Page<AdminGetUsersResponse> result = adminService.getAllUsers(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            AdminGetUsersResponse userResponse = result.getContent().getFirst();
            assertEquals(TEST_USER_ID, userResponse.id());
            assertEquals(TEST_USERNAME, userResponse.username());
            assertEquals(TEST_EMAIL, userResponse.email());
            assertEquals(TEST_MAX_TOKENS, userResponse.maxTokens());
            assertEquals(TEST_MODEL, userResponse.model());
            assertEquals(TEST_CREATED_AT, userResponse.createdAt());
            assertEquals(TEST_ROLES, userResponse.roles());
            verify(userRepository).findAll(any(Pageable.class));
        }

        /**
         * Tests retrieving an empty paginated list when no users exist.
         * @since 1.0
         */
        @Test
        void shouldReturnEmptyPageWhenNoUsersExist() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            Page<UserEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(userRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            // Act
            Page<AdminGetUsersResponse> result = adminService.getAllUsers(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
            verify(userRepository).findAll(any(Pageable.class));
        }

        /**
         * Tests retrieving users with different page size and ascending sort.
         * @since 1.0
         */
        @Test
        void shouldGetAllUsersWithDifferentPageSizeAndSort() {
            // Arrange
            Pageable inputPageable = PageRequest.of(1, 5, Sort.by("createdAt").ascending());
            Pageable servicePageable = PageRequest.of(inputPageable.getPageNumber(), inputPageable.getPageSize(), Sort.by("createdAt").descending());
            Page<UserEntity> usersPage = new PageImpl<>(List.of(testUser), servicePageable, 1L);
            when(userRepository.findAll(servicePageable)).thenReturn(usersPage);

            // Act
            Page<AdminGetUsersResponse> result = adminService.getAllUsers(inputPageable);

            // Assert
            assertNotNull(result);
            assertEquals(6, result.getTotalElements());
            assertEquals(5, result.getPageable().getPageSize());
            assertEquals(Sort.by("createdAt").descending(), result.getPageable().getSort());
            AdminGetUsersResponse userResponse = result.getContent().getFirst();
            assertEquals(TEST_USER_ID, userResponse.id());
            verify(userRepository).findAll(servicePageable);
        }
    }

    @Nested
    class DeleteUserTests {
        /**
         * Tests deleting a user successfully.
         * @since 1.0
         */
        @Test
        void shouldDeleteUserSuccessfully() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenService).deleteByUser(testUser);
            doNothing().when(userRepository).delete(testUser);

            // Act
            adminService.deleteUser(TEST_USER_ID);

            // Assert
            verify(userRepository).findById(TEST_USER_ID);
            verify(refreshTokenService).deleteByUser(testUser);
            verify(userRepository).delete(testUser);
        }

        /**
         * Tests throwing UserNotFoundException when user is not found.
         * @since 1.0
         */
        @Test
        void shouldThrowUserNotFoundExceptionWhenUserNotFound() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> adminService.deleteUser(TEST_USER_ID));
            assertEquals("User not found with ID: " + TEST_USER_ID, exception.getMessage());
            verify(userRepository).findById(TEST_USER_ID);
            verify(refreshTokenService, never()).deleteByUser(any());
            verify(userRepository, never()).delete(any());
        }

        /**
         * Tests throwing UserNotFoundException for extremely large user ID.
         * @since 1.0
         */
        @Test
        void shouldThrowUserNotFoundExceptionForLargeUserId() {
            // Arrange
            when(userRepository.findById(INVALID_USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> adminService.deleteUser(INVALID_USER_ID));
            assertEquals("User not found with ID: " + INVALID_USER_ID, exception.getMessage());
            verify(userRepository).findById(INVALID_USER_ID);
            verify(refreshTokenService, never()).deleteByUser(any());
            verify(userRepository, never()).delete(any());
        }

        /**
         * Tests handling exception from refreshTokenService.deleteByUser.
         * @since 1.0
         */
        @Test
        void shouldPropagateExceptionFromRefreshTokenService() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doThrow(new RuntimeException("Database error")).when(refreshTokenService).deleteByUser(testUser);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> adminService.deleteUser(TEST_USER_ID));
            assertEquals("Database error", exception.getMessage());
            verify(userRepository).findById(TEST_USER_ID);
            verify(refreshTokenService).deleteByUser(testUser);
            verify(userRepository, never()).delete(any());
        }
    }

    @Nested
    class DeleteRefreshTokensTests {
        /**
         * Tests deleting refresh tokens successfully.
         * @since 1.0
         */
        @Test
        void shouldDeleteRefreshTokensSuccessfully() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenService).deleteByUser(testUser);

            // Act
            adminService.deleteRefreshTokens(TEST_USER_ID);

            // Assert
            verify(userRepository).findById(TEST_USER_ID);
            verify(refreshTokenService).deleteByUser(testUser);
        }

        /**
         * Tests throwing UserNotFoundException when user is not found.
         * @since 1.0
         */
        @Test
        void shouldThrowUserNotFoundExceptionWhenUserNotFound() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> adminService.deleteRefreshTokens(TEST_USER_ID));
            assertEquals("User not found with ID: " + TEST_USER_ID, exception.getMessage());
            verify(userRepository).findById(TEST_USER_ID);
            verify(refreshTokenService, never()).deleteByUser(any());
        }

        /**
         * Tests throwing UserNotFoundException for extremely large user ID.
         * @since 1.0
         */
        @Test
        void shouldThrowUserNotFoundExceptionForLargeUserId() {
            // Arrange
            when(userRepository.findById(INVALID_USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> adminService.deleteRefreshTokens(INVALID_USER_ID));
            assertEquals("User not found with ID: " + INVALID_USER_ID, exception.getMessage());
            verify(userRepository).findById(INVALID_USER_ID);
            verify(refreshTokenService, never()).deleteByUser(any());
        }

        /**
         * Tests handling exception from refreshTokenService.deleteByUser.
         * @since 1.0
         */
        @Test
        void shouldPropagateExceptionFromRefreshTokenService() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doThrow(new RuntimeException("Database error")).when(refreshTokenService).deleteByUser(testUser);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> adminService.deleteRefreshTokens(TEST_USER_ID));
            assertEquals("Database error", exception.getMessage());
            verify(userRepository).findById(TEST_USER_ID);
            verify(refreshTokenService).deleteByUser(testUser);
        }
    }
}
