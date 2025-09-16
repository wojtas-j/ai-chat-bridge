package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.dto.*;
import com.wojtasj.aichatbridge.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles HTTP requests for user-related operations in the AI Chat Bridge application.
 * @since 1.0
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Updates the password for the authenticated user.
     * @param request the request containing the current and new password
     * @param userDetails the authenticated user's details
     * @return no content (204)
     * @since 1.0
     */
    @Operation(summary = "Updates the authenticated user's password")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing, invalid token or incorrect current password", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Updating password for user: {}", userDetails.getUsername());
        userService.updatePassword(userDetails.getUsername(), request.currentPassword(), request.newPassword());
        log.info("Password updated successfully for user: {}", userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates the email for the authenticated user.
     * @param request the request containing the new email
     * @param userDetails the authenticated user's details
     * @return no content (204)
     * @since 1.0
     */
    @Operation(summary = "Updates the authenticated user's email")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Email updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/email")
    public ResponseEntity<Void> updateEmail(@Valid @RequestBody UpdateEmailRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Updating email for user: {}", userDetails.getUsername());
        userService.updateEmail(userDetails.getUsername(), request.email());
        log.info("Email updated successfully for user: {}", userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates the OpenAI API key for the authenticated user.
     * @param request the request containing the new OpenAI API key
     * @param userDetails the authenticated user's details
     * @return no content (204)
     * @since 1.0
     */
    @Operation(summary = "Updates the authenticated user's OpenAI API key")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "OpenAI API key updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/api-key")
    public ResponseEntity<Void> updateApiKey(@Valid @RequestBody UpdateOpenAIApiKeyRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Updating OpenAI API key for user: {}", userDetails.getUsername());
        userService.updateApiKey(userDetails.getUsername(), request.apiKey());
        log.info("OpenAI API key updated successfully for user: {}", userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates the maximum tokens for the authenticated user.
     * @param request the request containing the new max tokens value
     * @param userDetails the authenticated user's details
     * @return no content (204)
     * @since 1.0
     */
    @Operation(summary = "Updates the authenticated user's max tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Max tokens updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/max-tokens")
    public ResponseEntity<Void> updateMaxTokens(@Valid @RequestBody UpdateMaxTokensRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Updating max tokens for user: {}", userDetails.getUsername());
        userService.updateMaxTokens(userDetails.getUsername(), request.maxTokens());
        log.info("Max tokens updated successfully for user: {}", userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates the model for the authenticated user.
     * @param request the request containing the new model
     * @param userDetails the authenticated user's details
     * @return no content (204)
     * @since 1.0
     */
    @Operation(summary = "Updates the authenticated user's model")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Model updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/model")
    public ResponseEntity<Void> updateModel(@Valid @RequestBody UpdateModelRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Updating model for user: {}", userDetails.getUsername());
        userService.updateModel(userDetails.getUsername(), request.model());
        log.info("Model updated successfully for user: {}", userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes the authenticated user's account and associated refresh tokens.
     * @param userDetails the authenticated user's details
     * @return no content (204)
     * @since 1.0
     */
    @Operation(summary = "Deletes the authenticated user's account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Deleting account for user: {}", userDetails.getUsername());
        userService.deleteAccount(userDetails.getUsername());
        log.info("Account deleted successfully for user: {}", userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
