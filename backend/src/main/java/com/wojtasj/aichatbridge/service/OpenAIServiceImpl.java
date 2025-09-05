package com.wojtasj.aichatbridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wojtasj.aichatbridge.configuration.OpenAIProperties;
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
 * Service for interacting with OpenAI API to process messages.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAIServiceImpl implements OpenAIService {

    private final RestTemplate restTemplate;
    private final OpenAIProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Sends a message to OpenAI and returns the response as a MessageEntity.
     *
     * @param message The message entity containing the prompt to send.
     * @return The OpenAI response as a MessageEntity.
     * @throws OpenAIServiceException if the content is invalid or API call fails.
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
            throw e; // Przepuszczamy wyjątek z parseResponse bez opakowywania
        } catch (Exception e) {
            log.error("Unexpected error calling OpenAI API: {}", e.getMessage(), e);
            throw new OpenAIServiceException("Unexpected error calling OpenAI API", e);
        }
    }

    /**
     * Parses the OpenAI API response to extract the generated text.
     *
     * @param response The JSON response from OpenAI.
     * @return The extracted text content.
     * @throws OpenAIServiceException if parsing fails or no content is found.
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