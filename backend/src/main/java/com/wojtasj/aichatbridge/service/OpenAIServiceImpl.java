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
import jakarta.annotation.PostConstruct;
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
 * Service for interacting with the OpenAI API in the AI Chat Bridge application.
 * Handles message sending and API key validation.
 * @since 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAIServiceImpl implements OpenAIService {

    private static final String USER_ROLE = "user";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MODELS_ENDPOINT = "/models";

    private final RestTemplate restTemplate;
    private final OpenAIProperties openAIProperties;
    private final DiscordProperties discordProperties;
    private final ObjectMapper objectMapper;

    /**
     * Validates OpenAI configuration during application startup.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (isEmpty(openAIProperties.getModel())) {
            throw new IllegalStateException("OpenAI model cannot be null or empty");
        }
        if (isEmpty(openAIProperties.getBaseUrl()) || isEmpty(openAIProperties.getChatCompletionsEndpoint())) {
            throw new IllegalStateException("OpenAI base URL or chat completions endpoint cannot be null or empty");
        }
        if (isEmpty(discordProperties.getApiKey()) || isEmpty(discordProperties.getMaxTokens())) {
            throw new IllegalStateException("Discord API key or max tokens cannot be null or empty");
        }
    }

    /**
     * Sends a message to OpenAI and returns the response.
     * @param message the message to send
     * @param isDiscordMessage indicates if the message is from Discord
     * @param apiKey the OpenAI API key for non-Discord messages
     * @param maxTokens the maximum tokens for non-Discord messages
     * @return the response as a BaseMessage subclass
     * @throws OpenAIServiceException if the request or response processing fails
     */
    @Override
    @Retryable(retryFor = {HttpClientErrorException.TooManyRequests.class}, backoff = @Backoff(delay = 1000))
    public <T extends BaseMessage> T sendMessageToOpenAI(T message, boolean isDiscordMessage, String apiKey, Integer maxTokens) {
        validateMessage(message);
        ApiConfig config = resolveApiConfig(isDiscordMessage, apiKey, maxTokens);
        log.info("Sending {} message to OpenAI, content: {}", isDiscordMessage ? "Discord" : "User", message.getContent());

        HttpEntity<String> request = buildRequest(config.apiKey(), config.maxTokens(), message.getContent());
        ResponseEntity<String> response = executeRequest(request);

        String responseText = parseResponse(response.getBody());
        return createResponse(message, responseText);
    }

    /**
     * Validates the provided OpenAI API key.
     * @param apiKey the API key to validate
     * @throws OpenAIServiceException if the API key is invalid
     */
    @Override
    public void validateApiKey(String apiKey) {
        validateNotEmpty(apiKey, "API key cannot be null or empty during validation");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", BEARER_PREFIX + apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    openAIProperties.getBaseUrl() + MODELS_ENDPOINT,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("API key validation successful");
            } else {
                log.error("API key validation failed with status: {}", response.getStatusCode());
                throw new OpenAIServiceException("Invalid OpenAI API key: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("API key validation failed with status: {}", e.getStatusCode(), e);
            throw new OpenAIServiceException("Invalid OpenAI API key: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Unexpected error during API key validation: {}", e.getMessage(), e);
            throw new OpenAIServiceException("Failed to validate API key", e);
        }
    }

    private void validateMessage(BaseMessage message) {
        if (message == null || isEmpty(message.getContent())) {
            log.error("Invalid message content: null or empty");
            throw new OpenAIServiceException("Message content cannot be null or empty");
        }
    }

    private ApiConfig resolveApiConfig(boolean isDiscordMessage, String apiKey, Integer maxTokens) {
        if (isDiscordMessage) {
            try {
                return new ApiConfig(discordProperties.getApiKey(), Integer.parseInt(discordProperties.getMaxTokens()));
            } catch (NumberFormatException e) {
                log.error("Invalid max-tokens value in Discord properties: {}", discordProperties.getMaxTokens(), e);
                throw new OpenAIServiceException("Invalid max-tokens configuration for Discord");
            }
        } else {
            validateNotEmpty(apiKey, "API key required for non-Discord messages");
            if (maxTokens == null || maxTokens <= 0) {
                log.error("Max tokens is null or non-positive for non-Discord message: {}", maxTokens);
                throw new OpenAIServiceException("Max tokens must be positive for non-Discord messages");
            }
            validateApiKey(apiKey);
            return new ApiConfig(apiKey, maxTokens);
        }
    }

    private HttpEntity<String> buildRequest(String apiKey, int maxTokens, String content) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", openAIProperties.getModel());
        requestBody.put("max_tokens", maxTokens);

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", USER_ROLE);
        userMessage.put("content", content);
        messages.add(userMessage);
        requestBody.set("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", BEARER_PREFIX + apiKey);

        try {
            return new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        } catch (Exception e) {
            log.error("Failed to serialize request body: {}", e.getMessage(), e);
            throw new OpenAIServiceException("Failed to serialize OpenAI request", e);
        }
    }

    private ResponseEntity<String> executeRequest(HttpEntity<String> request) {
        try {
            return restTemplate.exchange(
                    openAIProperties.getBaseUrl() + openAIProperties.getChatCompletionsEndpoint(),
                    HttpMethod.POST,
                    request,
                    String.class
            );
        } catch (HttpClientErrorException e) {
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

    @SuppressWarnings("unchecked")
    private <T extends BaseMessage> T createResponse(T message, String responseText) {
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

    private String parseResponse(String response) {
        if (isEmpty(response)) {
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
        } catch (OpenAIServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing OpenAI response: {}, cause: {}", response, e.getMessage(), e);
            throw new OpenAIServiceException("Error parsing OpenAI response", e);
        }
    }

    private void validateNotEmpty(String value, String errorMessage) {
        if (isEmpty(value)) {
            log.error(errorMessage);
            throw new OpenAIServiceException(errorMessage);
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Internal record to hold API key and max tokens' configuration.
     */
    private record ApiConfig(String apiKey, int maxTokens) {
    }
}
