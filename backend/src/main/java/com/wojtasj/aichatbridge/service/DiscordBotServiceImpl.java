package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.configuration.DiscordProperties;
import com.wojtasj.aichatbridge.entity.DiscordMessageEntity;
import com.wojtasj.aichatbridge.repository.DiscordMessageRepository;
import com.wojtasj.aichatbridge.exception.DiscordServiceException;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Implements Discord bot integration for processing messages in the AI Chat Bridge application.
 * @since 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "discord.bot-enabled", havingValue = "true", matchIfMissing = true)
public class DiscordBotServiceImpl implements DiscordBotService {

    private final OpenAIService openAIService;
    private final DiscordMessageRepository discordMessageRepository;
    private final DiscordProperties discordProperties;

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
     * Processes a Discord message event by handling messages with the configured prefix, saving them to the database,
     * sending to OpenAI, and responding in the Discord channel.
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

        String discordNick = message.getAuthor()
                .map(User::getUsername)
                .orElseThrow(() -> new DiscordServiceException("Failed to retrieve Discord username"));

        log.info("Received Discord bot message from {}: {}", discordNick, content);

        return message.getChannel()
                .onErrorMap(e -> new DiscordServiceException("Failed to get Discord channel", e))
                .flatMap(channel ->
                        // Save user message to the database
                        Mono.fromCallable(() -> {
                                    DiscordMessageEntity userMessage = new DiscordMessageEntity();
                                    userMessage.setContent(content);
                                    userMessage.setDiscordNick(discordNick);
                                    return discordMessageRepository.save(userMessage);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .onErrorMap(e -> new DiscordServiceException("Failed to save Discord user message", e))
                                .flatMap(savedUserMessage -> {
                                    log.info("Discord user message saved with ID: {} from nick: {}", savedUserMessage.getId(), savedUserMessage.getDiscordNick());
                                    // Send message to OpenAI and get response
                                    return Mono.fromCallable(() -> openAIService.sendMessageToOpenAI(savedUserMessage, true, null))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .onErrorMap(e -> new OpenAIServiceException("Failed to process message with OpenAI", e));
                                })
                                .flatMap(aiResponse -> {
                                    // Save OpenAI response to the database
                                    log.info("Received OpenAI response for Discord message: {}", aiResponse.getContent());
                                    return Mono.fromCallable(() -> discordMessageRepository.save(aiResponse))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .onErrorMap(e -> new DiscordServiceException("Failed to save AI response", e));
                                })
                                .flatMap(savedAiResponse -> {
                                    // Send response to Discord channel
                                    log.info("OpenAI response saved with ID: {} for nick: {}", savedAiResponse.getId(), savedAiResponse.getDiscordNick());
                                    return channel.createMessage(savedAiResponse.getContent())
                                            .onErrorMap(e -> new DiscordServiceException("Failed to send message to Discord", e));
                                }))
                .onErrorResume(e -> {
                    log.error("Error processing Discord message: {}", e.getMessage(), e);
                    return Mono.empty();
                });
    }
}
