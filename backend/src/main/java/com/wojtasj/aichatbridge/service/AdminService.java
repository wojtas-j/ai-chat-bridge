package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.dto.AdminGetUsersResponse;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for admin-related operations in the AI Chat Bridge application.
 * @since 1.0
 */
public interface AdminService {

    /**
     * Retrieves a paginated list of all users, excluding sensitive fields like password, API key, and ID.
     * @param pageable pagination information (page number, size)
     * @return a {@link Page} of {@link AdminGetUsersResponse} objects
     * @since 1.0
     */
    Page<AdminGetUsersResponse> getAllUsers(Pageable pageable);

    /**
     * Deletes a user and their associated refresh tokens.
     * @param id the ID of the user to delete
     * @throws AuthenticationException if the user is not found
     * @since 1.0
     */
    void deleteUser(Long id);

    /**
     * Deletes all refresh tokens for a specific user.
     * @param id the ID of the user whose tokens are to be deleted
     * @throws AuthenticationException if the user is not found
     * @since 1.0
     */
    void deleteRefreshTokens(Long id);
}
