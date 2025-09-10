package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;

/**
 * Interface for handling refresh token operations in the AI Chat Bridge application.
 * @since 1.0
 */
public interface RefreshTokenService {

    /**
     * Generates a new refresh token for the given user.
     * @param user the user for whom to generate the token
     * @return the generated RefreshTokenEntity
     * @since 1.0
     */
    RefreshTokenEntity generateRefreshToken(UserEntity user);

    /**
     * Validates a refresh token and returns the associated entity.
     * @param token the refresh token to validate
     * @return the RefreshTokenEntity
     * @throws AuthenticationException if the token is invalid or expired
     * @since 1.0
     */
    RefreshTokenEntity validateRefreshToken(String token);

    /**
     * Deletes all refresh tokens for a given user.
     * @param user the user whose tokens should be deleted
     * @since 1.0
     */
    void deleteByUser(UserEntity user);

    /**
     * Deletes all expired refresh tokens from the database.
     * @since 1.0
     */
    void deleteExpiredTokens();
}
