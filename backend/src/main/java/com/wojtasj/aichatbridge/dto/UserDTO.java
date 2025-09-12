package com.wojtasj.aichatbridge.dto;

import com.wojtasj.aichatbridge.entity.Role;

import java.util.Set;

/**
 * DTO for representing user details in the AI Chat Bridge application.
 * @param username the user's username
 * @param email the user's email
 * @param roles the set of roles assigned to the user
 * @param maxTokens the user's maximum tokens for OpenAI requests
 * @since 1.0
 */
public record UserDTO(String username, String email, Set<Role> roles, Integer maxTokens) {
}
