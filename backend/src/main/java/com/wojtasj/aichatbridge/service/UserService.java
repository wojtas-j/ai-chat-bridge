package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.exception.UserAlreadyExistsException;

/**
 * Service interface for handling user-related operations in the AI Chat Bridge application.
 * @since 1.0
 */
public interface UserService {

    /**
     * Updates the password for a user after verifying the current password.
     * @param username the username of the user
     * @param currentPassword the current password for verification
     * @param newPassword the new password
     * @throws AuthenticationException if the user is not found or current password is incorrect
     * @since 1.0
     */
    void updatePassword(String username, String currentPassword, String newPassword);

    /**
     * Updates the email for a user.
     * @param username the username of the user
     * @param newEmail the new email address
     * @throws AuthenticationException if the user is not found
     * @throws UserAlreadyExistsException if the new email is already in use
     * @since 1.0
     */
    void updateEmail(String username, String newEmail);

    /**
     * Updates the OpenAI API key for a user.
     * @param username the username of the user
     * @param apiKey the new OpenAI API key provided by the user
     * @throws AuthenticationException if the user is not found
     * @since 1.0
     */
    void updateApiKey(String username, String apiKey);

    /**
     * Updates the maximum tokens for a user.
     * @param username the username of the user
     * @param maxTokens the new max tokens value
     * @throws AuthenticationException if the user is not found
     * @since 1.0
     */
    void updateMaxTokens(String username, Integer maxTokens);

    /**
     * Updates the model for a user.
     * @param username the username of the user
     * @param model the new model
     * @throws AuthenticationException if the user is not found
     * @since 1.0
     */
    void updateModel(String username, String model);

    /**
     * Deletes a user's account and associated refresh tokens.
     * @param username the username of the user
     * @throws AuthenticationException if the user is not found
     * @since 1.0
     */
    void deleteAccount(String username);

    /**
     * Finds a user by username.
     * @param username the username to search for
     * @return the UserEntity
     * @throws AuthenticationException if the user is not found
     * @since 1.0
     */
    UserEntity findByUsername(String username);
}

