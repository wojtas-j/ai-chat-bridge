package com.wojtasj.aichatbridge.service;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

/**
 * Defines the service for handling Discord bot message events in the AI Chat Bridge application.
 * @since 1.0
 */
public interface DiscordBotService {
    /**
     * Processes a Discord message creation event and returns the bot's response.
     * @param event the MessageCreateEvent to process
     * @return a Mono representing the result of message processing
     * @since 1.0
     */
    Mono<Message> handleMessageCreateEvent(MessageCreateEvent event);
}
