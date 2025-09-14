package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.entity.DiscordMessageEntity;
import com.wojtasj.aichatbridge.service.DiscordMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles HTTP requests related to Discord messages in the AI Chat Bridge application.
 * Provides admin-only endpoints for retrieving and deleting Discord messages.
 * @since 1.0
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/discord-messages")
@Slf4j
public class DiscordMessageController {

    private final DiscordMessageService discordMessageService;

    /**
     * Retrieves all Discord messages across all users (admin-only) with pagination and default sorting by {@code createdAt} in descending order.
     * @param pageable pagination information (page number, size)
     * @param userDetails the authenticated admin's details
     * @return a page of all Discord messages
     * @since 1.0
     */
    @Operation(summary = "Gets all Discord messages for admins with pagination", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All Discord messages fetched successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<Page<DiscordMessageEntity>> getAllAdminMessages(@PageableDefault(size = 20) Pageable pageable, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching all Discord messages for admin: {}", userDetails.getUsername());
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
        Page<DiscordMessageEntity> messages = discordMessageService.getAllMessages(sortedPageable);
        log.info("Successfully retrieved {} total Discord messages for admin: {}", messages.getTotalElements(), userDetails.getUsername());
        return ResponseEntity.ok(messages);
    }

    /**
     * Deletes a specific Discord message (admin-only).
     * @param id the ID of the message to delete
     * @param userDetails the authenticated admin's details
     * @return no content (204)
     * @since 1.0
     */
    @Operation(summary = "Deletes a Discord message", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Discord message deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Discord message not found", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Delete request for Discord message ID: {} by admin: {}", id, userDetails.getUsername());
        discordMessageService.deleteMessage(id);
        log.info("Discord message ID: {} deleted successfully by admin: {}", id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
