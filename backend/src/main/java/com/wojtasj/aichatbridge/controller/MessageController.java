package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.dto.MessageDTO;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.service.OpenAIServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller for handling message-related requests.
 */
@RestController
@RequestMapping("/api/messages")
@Slf4j
public class MessageController {

    private final MessageRepository repository;
    private final OpenAIServiceImpl openAIServiceImpl;

    public MessageController(MessageRepository repository, OpenAIServiceImpl openAIServiceImpl) {
        this.repository = repository;
        this.openAIServiceImpl = openAIServiceImpl;
    }

    /**
     * Retrieves all messages from the database with pagination and default sorting by createdAt (descending).
     *
     * @param pageable Pagination information (page number, size, ignored sort parameters).
     * @return A ResponseEntity containing a page of messages.
     */
    @Operation(summary = "Gets all messages with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages fetched successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to access"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping
    public ResponseEntity<Page<MessageEntity>> getAllMessages(@PageableDefault(size = 20) Pageable pageable) {
        log.info("Getting all messages with pagination");
        try {
            Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
            Page<MessageEntity> messages = repository.findAll(sortedPageable);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Failed to retrieve messages: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve messages", e);
        }
    }

    /**
     * Creates a new message and saves it to the database.
     *
     * @param messageDTO The message DTO containing the content to save.
     * @return A ResponseEntity containing the saved message entity.
     */
    @Operation(summary = "Creates a new message")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message created successfully", content = @Content(schema = @Schema(implementation = MessageEntity.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to access"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping
    public ResponseEntity<MessageEntity> createMessage(@Valid @RequestBody MessageDTO messageDTO) {
        log.info("Creating the new message: {}", messageDTO.content());
        try {
            MessageEntity message = new MessageEntity();
            message.setContent(messageDTO.content());
            MessageEntity saved = repository.save(message);
            log.info("Message created successfully with ID: {}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Failed to create message: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create message", e);
        }
    }

    /**
     * Sends a message to OpenAI and saves the response in the database.
     *
     * @param messageDTO The message DTO containing the content to send to OpenAI.
     * @return A ResponseEntity containing the AI-generated response as a MessageEntity.
     */
    @Operation(summary = "Sends message to OpenAI")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message sent to OpenAI and received answer", content = @Content(schema = @Schema(implementation = MessageEntity.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to access"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/openai")
    public ResponseEntity<MessageEntity> sendToOpenAI(@Valid @RequestBody MessageDTO messageDTO) {
        log.info("Processing message with OpenAI: {}", messageDTO.content());
        try {
            MessageEntity message = new MessageEntity();
            message.setContent(messageDTO.content());
            MessageEntity saved = repository.save(message);
            log.info("Message saved before sending to OpenAI, ID: {}", saved.getId());
            MessageEntity response = openAIServiceImpl.sendMessageToOpenAI(saved);
            MessageEntity savedResponse = repository.save(response);
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