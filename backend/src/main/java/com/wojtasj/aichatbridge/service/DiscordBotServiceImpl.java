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
 * Implements Discord bot integration for processing messages in the AI Chat Bridge application.
 * @since 1.0
 * @see DiscordBotService
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "discord.bot-enabled", havingValue = "true", matchIfMissing = true)
public class DiscordBotServiceImpl implements DiscordBotService {

    private final OpenAIServiceImpl openAIService;
    private final MessageRepository messageRepository;
    private final DiscordProperties discordProperties;


    /**
     * Constructs a new DiscordBotServiceImpl with the required dependencies.
     * @param openAIService the service for interacting with OpenAI
     * @param messageRepository the repository for managing messages
     * @param discordProperties the configuration properties for the Discord bot
     * @since 1.0
     */
    public DiscordBotServiceImpl(OpenAIServiceImpl openAIService, MessageRepository messageRepository, DiscordProperties discordProperties) {
        this.openAIService = openAIService;
        this.messageRepository = messageRepository;
        this.discordProperties = discordProperties;
    }

    /**
     * Initializes the Discord bot and sets up message event handling using the configured bot token.
     * @since 1.0
     */
    @PostConstruct
    public void init() {
        DiscordClient.create(discordProperties.getBotToken())
                .withGateway(gateway -> gateway.on(MessageCreateEvent.class, this::handleMessageCreateEvent))
                .subscribe();
    }

    /**
     * Processes a Discord message event by handling messages with the configured prefix, saving them to the database, sending to OpenAI, and responding in the Discord channel.
     * @param event the MessageCreateEvent to process
     * @return a Mono representing the result of message processing
     * @since 1.0
     */
    @Override
    public Mono<Message> handleMessageCreateEvent(MessageCreateEvent event) {
        Message message = event.getMessage();
        String prefix = discordProperties.getBotPrefix();
        if (!message.getContent().startsWith(prefix)) {
            return Mono.empty();
        }

        String content = message.getContent().substring(prefix.length()).trim();
        if (content.isEmpty()) {
            log.warn("Empty message content after prefix removal, ignoring: {}", message.getContent());
            return Mono.empty();
        }

        log.info("Received Discord bot message: {}", content);

        // Get channel first
        return message.getChannel()
                .onErrorMap(e -> new DiscordServiceException("Failed to get Discord channel", e))
                .flatMap(channel -> {
                    // Save user message to the database
                    return Mono.fromCallable(() -> {
                                MessageEntity userMessage = new MessageEntity();
                                userMessage.setContent(content);
                                userMessage.setCreatedAt(LocalDateTime.now());
                                return messageRepository.save(userMessage);
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .onErrorMap(e -> new DiscordServiceException("Failed to save user message", e))
                            .flatMap(savedUserMessage -> {
                                log.info("User message saved with ID: {}", savedUserMessage.getId());
                                // Send message to OpenAI and get response
                                return Mono.fromCallable(() -> openAIService.sendMessageToOpenAI(savedUserMessage))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .onErrorMap(e -> new OpenAIServiceException("Failed to process message with OpenAI", e));
                            })
                            .flatMap(aiResponse -> {
                                log.info("Received OpenAI response: {}", aiResponse.getContent());
                                // Save OpenAI response to the database
                                return Mono.fromCallable(() -> messageRepository.save(aiResponse))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .onErrorMap(e -> new DiscordServiceException("Failed to save AI response", e));
                            })
                            .flatMap(savedAiResponse -> {
                                log.info("OpenAI response saved with ID: {}", savedAiResponse.getId());
                                // Send response to Discord channel
                                return channel.createMessage(savedAiResponse.getContent())
                                        .onErrorMap(e -> new DiscordServiceException("Failed to send message to Discord", e));
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error processing Discord message: {}", e.getMessage(), e);
                    return Mono.empty();
                });
    }
}
