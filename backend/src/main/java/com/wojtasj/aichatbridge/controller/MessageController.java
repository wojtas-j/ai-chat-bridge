package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.dto.MessageDTO;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.service.AuthenticationService;
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
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles HTTP requests related to messages in the AI Chat Bridge application, including retrieval and creation.
 * @since 1.0
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/messages")
@Slf4j
public class MessageController {

    private final MessageRepository repository;
    private final AuthenticationService authenticationService;

    /**
     * Retrieves all messages for the authenticated user with pagination and default sorting by {@code createdAt} in descending order.
     * @param pageable pagination information (page number, size, ignored sort parameters)
     * @param userDetails the authenticated user's details
     * @return a ResponseEntity containing a page of the user's messages
     * @throws ResponseStatusException if an error occurs during message retrieval
     * @since 1.0
     */
    @Operation(summary = "Gets all messages for the authenticated user with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages fetched successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to access"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<Page<MessageEntity>> getAllMessages(@PageableDefault(size = 20) Pageable pageable,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching messages for user: {}", userDetails.getUsername());
        try {
            UserEntity user = authenticationService.findByUsername(userDetails.getUsername());
            Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
            Page<MessageEntity> messages = repository.findByUserId(user.getId(), sortedPageable);
            log.info("Successfully retrieved {} messages for user: {}", messages.getTotalElements(), user.getUsername());
            return ResponseEntity.ok(messages);
        } catch (AuthenticationException ex) {
            log.error("Authentication failed for user {}: {}", userDetails.getUsername(), ex.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage(), ex);
        } catch (Exception e) {
            log.error("Failed to retrieve messages for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve messages", e);
        }
    }

    /**
     * Creates a new message and saves it to the database, associating it with the authenticated user.
     * @param messageDTO the message DTO containing the content to save
     * @param userDetails the authenticated user's details
     * @return a ResponseEntity containing the saved message entity
     * @throws ResponseStatusException if an error occurs during message creation
     * @since 1.0
     */
    @Operation(summary = "Creates a new message")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message created successfully", content = @Content(schema = @Schema(implementation = MessageEntity.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to access"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<MessageEntity> createMessage(@Valid @RequestBody MessageDTO messageDTO,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Creating new message for user: {} with content: {}", userDetails.getUsername(), messageDTO.content());
        try {
            UserEntity user = authenticationService.findByUsername(userDetails.getUsername());
            MessageEntity message = MessageEntity.builder()
                    .content(messageDTO.content())
                    .user(user)
                    .build();
            MessageEntity saved = repository.save(message);
            log.info("Message created successfully with ID: {} for user: {}", saved.getId(), user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (AuthenticationException ex) {
            log.error("Authentication failed for user {}: {}", userDetails.getUsername(), ex.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage(), ex);
        } catch (Exception e) {
            log.error("Failed to create message for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create message", e);
        }
    }
}
