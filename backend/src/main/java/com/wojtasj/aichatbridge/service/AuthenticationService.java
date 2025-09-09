package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.dto.RegisterRequest;
import com.wojtasj.aichatbridge.entity.UserEntity;

/**
 * Interface for handling user authentication and registration in the AI Chat Bridge application.
 * @since 1.0
 */
public interface AuthenticationService {

    /**
     * Registers a new user with the provided details.
     * @param request the registration request containing username, email, and password
     * @return the registered UserEntity
     * @throws com.wojtasj.aichatbridge.exception.AuthenticationException if the username or email is already taken
     * @since 1.0
     */
    UserEntity register(RegisterRequest request);

    /**
     * Finds a user by their username.
     *
     * @param username the username of the user to find
     * @return the UserEntity corresponding to the given username
     * @throws com.wojtasj.aichatbridge.exception.AuthenticationException if the user is not found
     * @since 1.0
     */
    UserEntity findByUsername(String username);
}
