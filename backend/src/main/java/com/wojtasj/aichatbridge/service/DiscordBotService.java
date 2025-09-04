package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.configuration.DiscordProperties;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.exception.DiscordServiceException;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

/**
 * Service for integrating with Discord bot to handle messages.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "discord.bot-enabled", havingValue = "true", matchIfMissing = true)
public class DiscordBotService {

    private final OpenAIService openAIService;
    private final MessageRepository messageRepository;
    private final DiscordProperties discordProperties;

    public DiscordBotService(OpenAIService openAIService, MessageRepository messageRepository, DiscordProperties discordProperties) {
        this.openAIService = openAIService;
        this.messageRepository = messageRepository;
        this.discordProperties = discordProperties;
    }

    /**
     * Initializes the Discord bot and sets up message event handling.
     */
    @PostConstruct
    public void init() {
        DiscordClient.create(discordProperties.getBotToken())
                .withGateway(gateway -> gateway.on(MessageCreateEvent.class, this::handleMessageCreateEvent))
                .subscribe();
    }

    /**
     * Handles a MessageCreateEvent by processing messages with the configured prefix.
     *
     * @param event the MessageCreateEvent to process
     * @return a Mono representing the result of message processing
     */
    public Mono<Message> handleMessageCreateEvent(MessageCreateEvent event) {
        Message message = event.getMessage();
        String prefix = discordProperties.getBotPrefix();
        if (!message.getContent().startsWith(prefix)) {
            return Mono.empty();
        }

        String content = message.getContent().substring(prefix.length()).trim();
        log.info("Received Discord bot message: {}", content);

        MessageEntity userMessage = new MessageEntity();
        userMessage.setContent(content);
        userMessage.setCreatedAt(LocalDateTime.now());

        return message.getChannel()
                .onErrorMap(e -> new DiscordServiceException("Failed to access Discord channel", e))
                .flatMap(channel -> Mono.fromCallable(() -> messageRepository.save(userMessage))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorMap(e -> new DiscordServiceException("Failed to save user message", e)))
                .flatMap(savedUserMessage ->
                        openAIService.sendMessageToOpenAI(savedUserMessage)
                                .onErrorMap(e -> new OpenAIServiceException("Failed to process message with OpenAI", e)))
                .flatMap(aiResponse ->
                        Mono.fromCallable(() -> messageRepository.save(aiResponse))
                                .subscribeOn(Schedulers.boundedElastic())
                                .onErrorMap(e -> new DiscordServiceException("Failed to save AI response", e)))
                .flatMap(savedAiResponse ->
                        message.getChannel()
                                .flatMap(channel -> channel.createMessage(savedAiResponse.getContent()))
                                .onErrorMap(e -> new DiscordServiceException("Failed to send message to Discord", e)))
                .onErrorResume(e -> {
                    log.error("Error processing Discord message: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}