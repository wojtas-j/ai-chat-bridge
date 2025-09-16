package com.wojtasj.aichatbridge.dto;

import com.wojtasj.aichatbridge.entity.Role;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for representing user details in the AI Chat Bridge application.
 * @param id the user's id
 * @param username the user's username
 * @param email the user's email
 * @param roles the set of roles assigned to the user
 * @param maxTokens the user's maximum tokens for OpenAI requests
 * @param model the user's openai model
 * @param createdAt the user's account created at time
 * @since 1.0
 */
public record AdminGetUsersResponse(Long id, String username, String email, Integer maxTokens, String model, LocalDateTime createdAt, Set<Role> roles) {
}
