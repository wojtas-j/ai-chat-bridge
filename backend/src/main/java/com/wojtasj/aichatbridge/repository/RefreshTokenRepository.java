package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for managing RefreshTokenEntity in the AI Chat Bridge application.
 * @since 1.0
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    /**
     * Finds a refresh token by its token value.
     * @param token the refresh token value
     * @return an Optional containing the RefreshTokenEntity if found
     */
    Optional<RefreshTokenEntity> findByToken(String token);

    /**
     * Deletes all refresh tokens for a given user.
     * @param user the user whose tokens should be deleted
     */
    void deleteByUser(UserEntity user);

    /**
     * Deletes all refresh tokens with an expiry date before the given time.
     *
     * @param expiryDate the cutoff date for token expiration
     */
    void deleteByExpiryDateBefore(LocalDateTime expiryDate);
}
