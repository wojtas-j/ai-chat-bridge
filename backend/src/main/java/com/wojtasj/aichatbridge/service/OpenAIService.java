package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.MessageEntity;

/**
 * Defines the service for interacting with the OpenAI API in the AI Chat Bridge application.
 * @since 1.0
 */
public interface OpenAIService {
    /**
     * Sends a message to OpenAI and returns the response as a MessageEntity.
     * @param message the message entity containing the prompt to send
     * @return the OpenAI response as a MessageEntity
     * @since 1.0
     */
    MessageEntity sendMessageToOpenAI(MessageEntity message);
}
