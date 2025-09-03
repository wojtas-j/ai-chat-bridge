package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.service.OpenAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
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
    private final OpenAIService openAIService;

    public MessageController(MessageRepository repository, OpenAIService openAIService) {
        this.repository = repository;
        this.openAIService = openAIService;
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
    @Transactional
    public MessageEntity createMessage(@RequestBody MessageEntity message) {
        log.debug("Creating message: {}", message.getContent());
        return repository.save(message);
    }

    /**
     * Sends a message to OpenAI and saves the response in the database.
     *
     * @param message The message entity to send to OpenAI.
     * @return The AI-generated response as a MessageEntity.
     */
    @PostMapping("/openai")
    @Transactional
    public MessageEntity sendToOpenAI(@RequestBody MessageEntity message) {
        log.info("Processing message with OpenAI: {}", message.getContent());
        MessageEntity savedMessage = repository.save(message);
        MessageEntity response = openAIService.sendMessageToOpenAI(savedMessage);
        return repository.save(response);
    }
}
