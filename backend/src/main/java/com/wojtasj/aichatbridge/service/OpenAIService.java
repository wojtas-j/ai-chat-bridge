package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.BaseMessage;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;

/**
 * Defines the service for interacting with the OpenAI API in the AI Chat Bridge application.
 * @since 1.0
 */
public interface OpenAIService {
    /**
     * Sends a message to OpenAI and returns the response as a subclass of {@link BaseMessage}.
     * @param message the message entity containing the prompt to send
     * @param isDiscordMessage indicates if the message originates from Discord
     * @param user the UserEntity with id, apikey and max tokens.
     * @param <T> the type of message, must extend {@link BaseMessage}
     * @return the OpenAI response as the same type as the input message
     * @throws OpenAIServiceException if the content, apiKey, maxTokens, or API call fails
     * @since 1.0
     */
    <T extends BaseMessage> T sendMessageToOpenAI(T message, boolean isDiscordMessage, UserEntity user) throws OpenAIServiceException;

    /**
     * Validates the provided OpenAI API key and model by making a test request to the OpenAI API.
     * @param apiKey the API key to validate
     * @param model the model to validate
     * @throws OpenAIServiceException if the API key is invalid or the validation request fails
     * @since 1.0
     */
    void validateApiKeyAndModel(String apiKey, String model) throws OpenAIServiceException;
}
