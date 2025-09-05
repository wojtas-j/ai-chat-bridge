package com.wojtasj.aichatbridge.service;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public interface DiscordBotService {
    Mono<Message> handleMessageCreateEvent(MessageCreateEvent event);
}
