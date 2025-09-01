package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing messages in the AI Chat Bridge application.
 */
@RestController
@RequestMapping("api/messages")
@Slf4j
public class MessageController {
    private final MessageRepository repository;

    public MessageController(MessageRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieves all messages from the database.
     *
     * @return List of all messages.
     */
    @GetMapping
    public List<MessageEntity> getAllMessages() {
        log.info("Fetching all messages");
        return repository.findAll();
    }

    /**
     * Creates a new message in the database.
     *
     * @param message The message entity to create.
     * @return The created message entity.
     */
    @PostMapping
    public MessageEntity createMessage(@RequestBody MessageEntity message) {
        log.debug("Creating message: {}", message.getContent());
        return repository.save(message);
    }
}
