package com.wojtasj.aichatbridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wojtasj.aichatbridge.configuration.OpenAIProperties;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
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
 * @see com.wojtasj.aichatbridge.service.OpenAIService
 */
@Service
@Slf4j
public class OpenAIServiceImpl implements OpenAIService {

    private final RestTemplate restTemplate;
    private final OpenAIProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new OpenAIServiceImpl with the required dependencies.
     * @param restTemplate the RestTemplate for making HTTP requests
     * @param properties the configuration properties for OpenAI
     * @param objectMapper the ObjectMapper for JSON serialization and deserialization
     * @since 1.0
     */
    public OpenAIServiceImpl(RestTemplate restTemplate, OpenAIProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a message to OpenAI and returns the response as a MessageEntity, with retry logic for handling rate limits.
     * @param message the message entity containing the prompt to send
     * @return the OpenAI response as a MessageEntity
     * @throws OpenAIServiceException if the content is invalid or the API call fails
     * @since 1.0
     */
    @Override
    @Retryable(
            retryFor = {HttpClientErrorException.TooManyRequests.class},
            backoff = @Backoff(delay = 1000)
    )
    public MessageEntity sendMessageToOpenAI(MessageEntity message) {
        if (message == null || message.getContent() == null || message.getContent().trim().isEmpty()) {
            log.error("Invalid message content: null or empty");
            throw new OpenAIServiceException("Message content cannot be null or empty");
        }

        log.info("Sending message to OpenAI: {}", message.getContent());

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", properties.getModel());
        requestBody.put("max_tokens", properties.getMaxTokens());

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", message.getContent());
        messages.add(userMessage);
        requestBody.set("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + properties.getApiKey());

        HttpEntity<String> entity;
        try {
            entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        } catch (Exception e) {
            log.error("Failed to serialize request body: {}", e.getMessage(), e);
            throw new OpenAIServiceException("Failed to serialize OpenAI request", e);
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getBaseUrl() + properties.getChatCompletionsEndpoint(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String responseText = parseResponse(response.getBody());
            MessageEntity responseEntity = new MessageEntity();
            responseEntity.setContent(responseText);
            responseEntity.setCreatedAt(LocalDateTime.now());
            return responseEntity;
        } catch (HttpClientErrorException e) {
            log.error("OpenAI API client error: {}", e.getStatusCode(), e);
            throw new OpenAIServiceException("OpenAI API error: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            log.error("OpenAI API server error: {}", e.getStatusCode(), e);
            throw new OpenAIServiceException("OpenAI API error: " + e.getStatusCode(), e);
        } catch (OpenAIServiceException e) {
            throw e;
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
        } catch (OpenAIServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing OpenAI response: {}, cause: {}", response, e.getMessage(), e);
            throw new OpenAIServiceException("Error parsing OpenAI response: " + response, e);
        }
    }
}
