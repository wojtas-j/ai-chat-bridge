package com.wojtasj.aichatbridge.dto;

import com.wojtasj.aichatbridge.entity.Role;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for representing user details in the AI Chat Bridge application.
 * @param id the unique identifier of the user
 * @param username the user's username
 * @param email the user's email
 * @param roles the set of roles assigned to the user
 * @param createdAt the timestamp when the user was created
 * @since 1.0
 */
public record UserDTO(Long id, String username, String email, Set<Role> roles, LocalDateTime createdAt) {
}
