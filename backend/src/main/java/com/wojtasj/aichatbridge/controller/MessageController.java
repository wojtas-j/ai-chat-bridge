package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.dto.MessageDTO;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.service.OpenAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Controller for handling message-related requests.
 */
@RestController
@RequestMapping("/api/messages")
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
     * @return A list of all messages.
     */
    @GetMapping
    public List<MessageEntity> getAllMessages() {
        return repository.findAll();
    }

    /**
     * Creates a new message and saves it to the database.
     *
     * @param messageDTO The message DTO containing the content to save.
     * @return The saved message entity.
     */
    @PostMapping
    public MessageEntity createMessage(@RequestBody MessageDTO messageDTO) {
        MessageEntity message = new MessageEntity();
        message.setContent(messageDTO.content());
        return repository.save(message);
    }

    /**
     * Sends a message to OpenAI and saves the response in the database.
     *
     * @param messageDTO The message DTO containing the content to send to OpenAI.
     * @return A Mono containing the AI-generated response as a MessageEntity.
     */
    @PostMapping("/openai")
    public Mono<MessageEntity> sendToOpenAI(@RequestBody MessageDTO messageDTO) {
        log.info("Processing message with OpenAI: {}", messageDTO.content());
        MessageEntity message = new MessageEntity();
        message.setContent(messageDTO.content());
        return Mono.just(message)
                .flatMap(m -> Mono.fromCallable(() -> repository.save(m))
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(openAIService::sendMessageToOpenAI)
                .flatMap(response -> Mono.fromCallable(() -> repository.save(response))
                        .subscribeOn(Schedulers.boundedElastic()))
                .onErrorMap(e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process OpenAI request", e));
    }
}