package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.exception.UserAlreadyExistsException;
import com.wojtasj.aichatbridge.exception.UserNotFoundException;
import com.wojtasj.aichatbridge.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link UserService} interface.
 * Provides business logic for user-related operations with security checks.
 * @since 1.0
 */
@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    /**
     * Updates the password for a user after verifying the current password.
     * Invalidates all refresh tokens after a successful update.
     * @param username the username of the user
     * @param currentPassword the current password for verification
     * @param newPassword the new password
     * @throws AuthenticationException if the user is not found or the current password is incorrect
     * @since 1.0
     */
    @Override
    public void updatePassword(String username, String currentPassword, String newPassword) {
        log.info("Updating password for user: {}", username);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("User not found with username: " + username));

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, currentPassword));
        } catch (BadCredentialsException e) {
            log.error("Incorrect current password for user: {}", username);
            throw new AuthenticationException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.deleteByUser(user);
        log.info("Password updated successfully for user: {}", username);
    }

    /**
     * Updates the email for a user after verifying that the new email is not already in use.
     * @param username the username of the user
     * @param newEmail the new email address
     * @throws UserNotFoundException if the user is not found
     * @throws UserAlreadyExistsException if the new email is already in use
     * @since 1.0
     */
    @Override
    public void updateEmail(String username, String newEmail) {
        log.info("Updating email for user: {}", username);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        if (userRepository.findByEmail(newEmail).isPresent()) {
            log.error("Email already in use: {}", newEmail);
            throw new UserAlreadyExistsException("Email already in use: " + newEmail);
        }

        user.setEmail(newEmail);
        userRepository.save(user);
        log.info("Email updated successfully for user: {}", username);
    }

    /**
     * Updates the OpenAI API key for a user.
     * The provided API key is saved to the database and encrypted using ApiKeyConverter.
     * @param username the username of the user
     * @param apiKey the new OpenAI API key provided by the user
     * @throws UserNotFoundException if the user is not found
     * @since 1.0
     */
    @Override
    public void updateApiKey(String username, String apiKey) {
        log.info("Updating OpenAI API key for user: {}", username);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        user.setApiKey(apiKey);
        userRepository.save(user);
        log.info("OpenAI API key updated successfully for user: {}", username);
    }

    /**
     * Updates the maximum tokens for a user.
     * @param username the username of the user
     * @param maxTokens the new max tokens value
     * @throws UserNotFoundException if the user is not found
     * @since 1.0
     */
    @Override
    public void updateMaxTokens(String username, Integer maxTokens) {
        log.info("Updating max tokens for user: {}", username);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        user.setMaxTokens(maxTokens);
        userRepository.save(user);
        log.info("Max tokens updated successfully for user: {}", username);
    }

    /**
     * Updates the model for a user.
     * @param username the username of the user
     * @param model the new model
     * @throws UserNotFoundException if the user is not found
     * @since 1.0
     */
    @Override
    public void updateModel(String username, String model) {
        log.info("Updating model for user: {}", username);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        user.setModel(model);
        userRepository.save(user);
        log.info("Model updated successfully for user: {}", username);
    }

    /**
     * Deletes a user's account and associated refresh tokens.
     * @param username the username of the user
     * @throws UserNotFoundException if the user is not found
     * @since 1.0
     */
    @Override
    public void deleteAccount(String username) {
        log.info("Deleting account for user: {}", username);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        refreshTokenService.deleteByUser(user);
        userRepository.delete(user);
        log.info("Account deleted successfully for user: {}", username);
    }

    /**
     * Finds a user by username.
     * @param username the username to search for
     * @return the UserEntity
     * @throws UserNotFoundException if the user is not found
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public UserEntity findByUsername(String username) {
        log.info("Finding user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }
}
