package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.configuration.JwtProperties;
import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service implementation for handling refresh token operations in the AI Chat Bridge application.
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /**
     * Generates a new refresh token for the given user.
     *
     * @param user the user for whom to generate the token
     * @return the generated RefreshTokenEntity
     * @since 1.0
     */
    @Override
    @Transactional
    public RefreshTokenEntity generateRefreshToken(UserEntity user) {
        log.info("Generating refresh token for user: {}", user.getUsername());
        long expirationDays = jwtProperties.getRefreshExpirationDays();
        if (expirationDays <= 0) {
            log.error("Invalid refresh token expiration time: {}", expirationDays);
            throw new IllegalArgumentException("Refresh token expiration time must be positive");
        }
        String token = UUID.randomUUID().toString();
        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .token(token)
                .user(user)
                .build();
        RefreshTokenEntity savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token generated successfully for user: {}", user.getUsername());
        return savedToken;
    }

    /**
     * Validates a refresh token and returns the associated entity.
     *
     * @param token the refresh token to validate
     * @return the RefreshTokenEntity
     * @throws AuthenticationException if the token is invalid or expired
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public RefreshTokenEntity validateRefreshToken(String token) {
        log.info("Validating refresh token");
        RefreshTokenEntity refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.error("Refresh token not found: {}", token);
                    return new AuthenticationException("Invalid refresh token");
                });

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.error("Refresh token expired for user: {}", refreshToken.getUser().getUsername());
            refreshTokenRepository.delete(refreshToken);
            throw new AuthenticationException("Refresh token expired");
        }

        log.info("Refresh token validated successfully for user: {}", refreshToken.getUser().getUsername());
        return refreshToken;
    }

    /**
     * Deletes all refresh tokens for a given user.
     *
     * @param user the user whose tokens should be deleted
     * @since 1.0
     */
    @Override
    @Transactional
    public void deleteByUser(UserEntity user) {
        log.info("Deleting refresh tokens for user: {}", user.getUsername());
        refreshTokenRepository.deleteByUser(user);
        log.info("Refresh tokens deleted successfully for user: {}", user.getUsername());
    }

    /**
     * Deletes all expired refresh tokens from the database.
     * Runs daily at midnight.
     * @since 1.0
     */
    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deleteExpiredTokens() {
        log.info("Deleting expired refresh tokens");
        refreshTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
