package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.configuration.DiscordProperties;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.repository.MessageRepository;
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
        if (message.getContent().startsWith(prefix)) {
            String content = message.getContent().substring(prefix.length()).trim();
            log.info("Received Discord bot message: {}", content);

            MessageEntity userMessage = new MessageEntity();
            userMessage.setContent(content);
            userMessage.setCreatedAt(LocalDateTime.now());

            // Save user message on a blocking scheduler
            return Mono.fromCallable(() -> messageRepository.save(userMessage))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(savedUserMessage -> {
                        // Send to OpenAI
                        MessageEntity aiResponse = openAIService.sendMessageToOpenAI(savedUserMessage);

                        // Save AI response on a blocking scheduler
                        return Mono.fromCallable(() -> messageRepository.save(aiResponse))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(savedAiResponse -> {
                                    // Reply on Discord
                                    return message.getChannel()
                                            .flatMap(channel -> channel.createMessage(savedAiResponse.getContent()));
                                });
                    });
        }
        return Mono.empty();
    }
}
