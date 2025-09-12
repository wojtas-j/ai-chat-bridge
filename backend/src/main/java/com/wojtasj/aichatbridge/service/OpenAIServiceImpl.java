package com.wojtasj.aichatbridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wojtasj.aichatbridge.configuration.DiscordProperties;
import com.wojtasj.aichatbridge.configuration.OpenAIProperties;
import com.wojtasj.aichatbridge.entity.BaseMessage;
import com.wojtasj.aichatbridge.entity.DiscordMessageEntity;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * Implements interaction with the OpenAI API for processing messages in the AI Chat Bridge application.
 * @since 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAIServiceImpl implements OpenAIService {

    private final RestTemplate restTemplate;
    private final OpenAIProperties openAIProperties;
    private final DiscordProperties discordProperties;
    private final ObjectMapper objectMapper;

    /**
     * Sends a message to OpenAI and returns the response as a subclass of {@link BaseMessage}.
     * Uses configuration from {@link DiscordProperties} for Discord messages and {@link OpenAIProperties} for others.
     * @param message the message entity containing the prompt to send
     * @param isDiscordMessage indicates if the message originates from Discord
     * @param <T> the type of message, must extend {@link BaseMessage}
     * @return the OpenAI response as the same type as the input message
     * @throws OpenAIServiceException if the content or configuration is invalid or the API call fails
     * @since 1.0
     */
    @SuppressWarnings("unchecked")
    @Override
    @Retryable(retryFor = {HttpClientErrorException.TooManyRequests.class}, backoff = @Backoff(delay = 1000))
    public <T extends BaseMessage> T sendMessageToOpenAI(T message, boolean isDiscordMessage) {
        if (message == null || message.getContent() == null || message.getContent().trim().isEmpty()) {
            log.error("Invalid message content: null or empty");
            throw new OpenAIServiceException("Message content cannot be null or empty");
        }

        // Select configuration based on message source
        String apiKey = isDiscordMessage ? discordProperties.getApiKey() : openAIProperties.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Invalid OpenAI API key for {} message", isDiscordMessage ? "Discord" : "User");
            throw new OpenAIServiceException("OpenAI API key cannot be null or empty");
        }

        int maxTokens;
        try {
            maxTokens = Integer.parseInt(isDiscordMessage ? discordProperties.getMaxTokens() : String.valueOf(openAIProperties.getMaxTokens()));
        } catch (NumberFormatException e) {
            log.error("Invalid max-tokens value for {} message: {}", isDiscordMessage ? "Discord" : "User",
                    isDiscordMessage ? discordProperties.getMaxTokens() : openAIProperties.getMaxTokens(), e);
            throw new OpenAIServiceException("Invalid max-tokens configuration", e);
        }

        if (openAIProperties.getModel() == null || openAIProperties.getModel().trim().isEmpty()) {
            log.error("Invalid OpenAI model configuration");
            throw new OpenAIServiceException("OpenAI model cannot be null or empty");
        }

        if (openAIProperties.getBaseUrl() == null || openAIProperties.getBaseUrl().trim().isEmpty() ||
                openAIProperties.getChatCompletionsEndpoint() == null || openAIProperties.getChatCompletionsEndpoint().trim().isEmpty()) {
            log.error("Invalid OpenAI API URL configuration");
            throw new OpenAIServiceException("OpenAI base URL or chat completions endpoint cannot be null or empty");
        }

        log.info("Sending {} message to OpenAI: {}", isDiscordMessage ? "Discord" : "User", message.getContent());

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", openAIProperties.getModel());
        requestBody.put("max_tokens", maxTokens);

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", message.getContent());
        messages.add(userMessage);
        requestBody.set("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<String> entity;
        try {
            entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        } catch (Exception e) {
            log.error("Failed to serialize request body: {}", e.getMessage(), e);
            throw new OpenAIServiceException("Failed to serialize OpenAI request", e);
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    openAIProperties.getBaseUrl() + openAIProperties.getChatCompletionsEndpoint(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String responseText = parseResponse(response.getBody());

            if (message instanceof DiscordMessageEntity) {
                DiscordMessageEntity discordResponse = new DiscordMessageEntity();
                discordResponse.setContent(responseText);
                discordResponse.setDiscordNick("AI-Bot");
                discordResponse.setCreatedAt(LocalDateTime.now());
                return (T) discordResponse;
            } else if (message instanceof MessageEntity) {
                MessageEntity userResponse = new MessageEntity();
                userResponse.setContent(responseText);
                userResponse.setCreatedAt(LocalDateTime.now());
                return (T) userResponse;
            } else {
                log.error("Unsupported message type: {}", message.getClass().getSimpleName());
                throw new OpenAIServiceException("Unsupported message type: " + message.getClass().getSimpleName());
            }
        }
        catch (OpenAIServiceException e) {
            throw e;
        }
        catch (HttpClientErrorException e) {
            log.error("OpenAI API client error: {}", e.getStatusCode(), e);
            throw new OpenAIServiceException("OpenAI API error: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            log.error("OpenAI API server error: {}", e.getStatusCode(), e);
            throw new OpenAIServiceException("OpenAI API error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling OpenAI API: {}", e.getMessage(), e);
            throw new OpenAIServiceException("Unexpected error calling OpenAI API", e);
        }
    }

    /**
     * Parses the OpenAI API response to extract the generated text content.
     * @param response the JSON response from OpenAI
     * @return the extracted text content
     * @throws OpenAIServiceException if parsing fails or no content is found
     * @since 1.0
     */
    private String parseResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.error("Empty or null response from OpenAI");
            throw new OpenAIServiceException("Empty or null response from OpenAI");
        }

        try {
            ObjectNode json = (ObjectNode) objectMapper.readTree(response);
            if (!json.has("choices") || json.get("choices").isEmpty()) {
                log.error("No choices in OpenAI response: {}", response);
                throw new OpenAIServiceException("No content in response");
            }

            ObjectNode firstChoice = (ObjectNode) json.get("choices").get(0);
            if (!firstChoice.has("message") || !firstChoice.get("message").has("content")) {
                log.error("Invalid message structure in OpenAI response: {}", response);
                throw new OpenAIServiceException("Invalid message structure in response");
            }

            return firstChoice.get("message").get("content").asText();
        }
        catch (OpenAIServiceException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Error parsing OpenAI response: {}, cause: {}", response, e.getMessage(), e);
            throw new OpenAIServiceException("Error parsing OpenAI response: " + response, e);
        }
    }
}
