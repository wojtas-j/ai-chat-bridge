package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.dto.AdminGetUsersResponse;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.UserNotFoundException;
import com.wojtasj.aichatbridge.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link AdminService} interface.
 * Provides business logic for admin-related operations with security checks.
 * @since 1.0
 */
@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    /**
     * Retrieves a paginated list of all users, excluding sensitive fields like password, API key, and ID.
     * @param pageable pagination information (page number, size)
     * @return a {@link Page} of {@link AdminGetUsersResponse} objects
     * @since 1.0
     */
    @Override
    public Page<AdminGetUsersResponse> getAllUsers(Pageable pageable) {
        log.info("Fetching all users for admin request");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
        Page<UserEntity> users = userRepository.findAll(sortedPageable);
        Page<AdminGetUsersResponse> userDTOs = users.map(user -> new AdminGetUsersResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getMaxTokens(),
                user.getModel(),
                user.getCreatedAt(),
                user.getRoles()
        ));
        log.info("Retrieved {} total users", userDTOs.getTotalElements());
        return userDTOs;
    }

    /**
     * Deletes a user and their associated refresh tokens.
     * @param id the ID of the user to delete
     * @throws UserNotFoundException if the user is not found
     * @since 1.0
     */
    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        refreshTokenService.deleteByUser(user);
        userRepository.delete(user);
        log.info("User deleted successfully with ID: {}", id);
    }

    /**
     * Deletes all refresh tokens for a specific user.
     * @param id the ID of the user whose tokens are to be deleted
     * @throws UserNotFoundException if the user is not found
     * @since 1.0
     */
    @Override
    public void deleteRefreshTokens(Long id) {
        log.info("Deleting refresh tokens for user with ID: {}", id);
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        refreshTokenService.deleteByUser(user);
        log.info("Refresh tokens deleted successfully for user with ID: {}", id);
    }
}
