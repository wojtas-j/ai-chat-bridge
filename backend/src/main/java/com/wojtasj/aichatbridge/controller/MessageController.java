package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.dto.MessageDTO;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.service.AuthenticationService;
import com.wojtasj.aichatbridge.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles HTTP requests related to messages in the AI Chat Bridge application, including retrieval, creation, and deletion.
 * @since 1.0
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/messages")
@Slf4j
public class MessageController {

    private final MessageService messageService;
    private final AuthenticationService authenticationService;

    /**
     * Retrieves all messages for the authenticated user with pagination and default sorting by {@code createdAt} in descending order.
     * Only returns messages owned by the authenticated user.
     * @param pageable pagination information (page number, size)
     * @param userDetails the authenticated user's details
     * @return a page of the user's messages
     * @since 1.0
     */
    @Operation(summary = "Gets all messages for the authenticated user with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages fetched successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to access", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<Page<MessageEntity>> getAllMessages(@PageableDefault(size = 20) Pageable pageable, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching user's own messages for: {}", userDetails.getUsername());
        UserEntity user = authenticationService.findByUsername(userDetails.getUsername());
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
        Page<MessageEntity> messages = messageService.getMessagesForUser(user.getId(), sortedPageable);
        log.info("Successfully retrieved {} messages for user: {}", messages.getTotalElements(), user.getUsername());
        return ResponseEntity.ok(messages);
    }

    /**
     * Retrieves all messages across all users (admin-only) with pagination and default sorting by {@code createdAt} in descending order.
     * @param pageable pagination information (page number, size)
     * @param userDetails the authenticated admin's details
     * @return a page of all messages
     * @since 1.0
     */
    @Operation(summary = "Gets all messages for admins with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All messages fetched successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<Page<MessageEntity>> getAllAdminMessages(@PageableDefault(size = 20) Pageable pageable, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching all messages for admin: {}", userDetails.getUsername());
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
        Page<MessageEntity> messages = messageService.getAllMessages(sortedPageable);
        log.info("Successfully retrieved {} total messages for admin: {}", messages.getTotalElements(), userDetails.getUsername());
        return ResponseEntity.ok(messages);
    }

    /**
     * Creates a new message and saves it to the database, associating it with the authenticated user.
     * @param messageDTO the message DTO containing the content to save
     * @param userDetails the authenticated user's details
     * @return the saved message entity
     * @since 1.0
     */
    @Operation(summary = "Creates a new message")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message created successfully", content = @Content(schema = @Schema(implementation = MessageEntity.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to access", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<MessageEntity> createMessage(@Valid @RequestBody MessageDTO messageDTO, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Creating new message for user: {} with content: {}", userDetails.getUsername(), messageDTO.content());
        UserEntity user = authenticationService.findByUsername(userDetails.getUsername());
        MessageEntity saved = messageService.createMessage(messageDTO.content(), user.getId());
        log.info("Message created successfully with ID: {} for user: {}", saved.getId(), user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Deletes a specific message. Users can delete only their own messages; admins can delete any.
     * @param id the ID of the message to delete
     * @param userDetails the authenticated user's details
     * @return no content (204)
     * @since 1.0
     */
    @Operation(summary = "Deletes a message")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Message deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to delete", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Message not found", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Delete request for message ID: {} by user: {}", id, userDetails.getUsername());
        UserEntity user = authenticationService.findByUsername(userDetails.getUsername());
        messageService.deleteMessage(id, user.getId());
        log.info("Message ID: {} deleted successfully by user: {}", id, user.getUsername());
        return ResponseEntity.noContent().build();
    }
}
