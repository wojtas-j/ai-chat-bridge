package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.dto.MessageDTO;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.service.AuthenticationService;
import com.wojtasj.aichatbridge.service.OpenAIServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles HTTP requests related to OpenAI interactions in the AI Chat Bridge application.
 * @since 1.0
 */
@RestController
@RequestMapping("/api/openai")
@RequiredArgsConstructor
@Slf4j
public class OpenAIController {

    private final MessageRepository messageRepository;
    private final OpenAIServiceImpl openAIService;
    private final AuthenticationService authenticationService;

    /**
     * Sends a message to OpenAI and saves the response in the database.
     * @param messageDTO the message DTO containing the content to send to OpenAI
     * @param userDetails the authenticated user's details
     * @return a ResponseEntity containing the AI-generated response as a MessageEntity
     * @throws OpenAIServiceException if an error occurs during OpenAI processing
     * @throws ResponseStatusException if an unexpected error occurs
     * @since 1.0
     */
    @Operation(summary = "Sends message to OpenAI", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message sent to OpenAI and received answer", content = @Content(schema = @Schema(implementation = MessageEntity.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to access", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<MessageEntity> sendToOpenAI(@Valid @RequestBody MessageDTO messageDTO,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Processing message with OpenAI for user: {}", userDetails.getUsername());
        try {
            UserEntity user = authenticationService.findByUsername(userDetails.getUsername());
            MessageEntity message = new MessageEntity();
            message.setContent(messageDTO.content());
            MessageEntity saved = messageRepository.save(message);
            log.info("Message saved before sending to OpenAI, ID: {}", saved.getId());
            MessageEntity response = openAIService.sendMessageToOpenAI(saved, false, user.getApiKey(), user.getMaxTokens());
            MessageEntity savedResponse = messageRepository.save(response);
            log.info("OpenAI response saved with ID: {}", savedResponse.getId());
            return ResponseEntity.ok(savedResponse);
        } catch (OpenAIServiceException e) {
            log.error("OpenAI error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to process OpenAI request: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process OpenAI request", e);
        }
    }
}
