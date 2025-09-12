package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.dto.RegisterRequest;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Service implementation for handling user registration and user details loading in the AI Chat Bridge application.
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService, UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Registers a new user with the provided details.
     * @param request the registration request containing username, email, password, apiKey, and maxTokens
     * @return the registered UserEntity
     * @throws AuthenticationException if the username or email is already taken
     * @since 1.0
     */
    @Override
    @Transactional
    public UserEntity register(RegisterRequest request) {
        log.info("Registering new user: {}", request.username());
        if (userRepository.findByUsername(request.username()).isPresent()) {
            log.error("Username already taken: {}", request.username());
            throw new AuthenticationException("Username already taken");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.error("Email already taken: {}", request.email());
            throw new AuthenticationException("Email already taken");
        }

        UserEntity user = UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .apiKey(request.apiKey())
                .maxTokens(request.maxTokens())
                .roles(Set.of(Role.USER))
                .build();
        UserEntity savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());
        return savedUser;
    }

    /**
     * Finds a user by their username.
     * @param username the username of the user to find
     * @return the UserEntity corresponding to the given username
     * @throws AuthenticationException if the user is not found
     * @since 1.0
     */
    @Override
    public UserEntity findByUsername(String username) {
        log.info("Finding user by username: {}", username);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new AuthenticationException("User not found: " + username);
                });
        log.info("User found: {}", username);
        return user;
    }

    /**
     * Loads user details by username or email for authentication.
     * @param username the username or email identifying the user
     * @return the UserDetails object for the user
     * @throws UsernameNotFoundException if the user is not found
     * @since 1.0
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Loading user details for username or email: {}", username);
        UserDetails user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> {
                    log.error("User not found with username or email: {}", username);
                    return new UsernameNotFoundException("User not found with username or email: " + username);
                });
        log.info("User details loaded for: {}", username);
        return user;
    }
}
