package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.configuration.OpenAIProperties;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.json.JSONArray;
import org.json.JSONObject;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Service for interacting with OpenAI API to process messages.
 */
@Service
@Slf4j
public class OpenAIService {

    private final WebClient webClient;
    private final String model;
    private final int maxTokens;

    public OpenAIService(OpenAIProperties properties) {
        this.model = properties.getModel();
        this.maxTokens = properties.getMaxTokens();
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
        log.info("XD: Initialized OpenAIService with baseUrl: {}, model: {}, maxTokens: {}",
                properties.getBaseUrl(), model, maxTokens);
    }

    /**
     * Sends a message to OpenAI and returns the response as a MessageEntity.
     *
     * @param message The message entity containing the prompt to send.
     * @return A Mono containing the OpenAI response as a MessageEntity.
     */
    public Mono<MessageEntity> sendMessageToOpenAI(MessageEntity message) {
        log.info("Sending message to OpenAI: {}", message.getContent());

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("max_output_tokens", maxTokens);

        JSONArray inputArray = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", message.getContent());
        inputArray.put(userMessage);
        requestBody.put("input", inputArray);

        return webClient.post()
                .uri("/responses")
                .bodyValue(requestBody.toString())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new OpenAIServiceException("OpenAI API error: " + response.statusCode())))
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .map(responseText -> {
                    MessageEntity responseEntity = new MessageEntity();
                    responseEntity.setContent(responseText);
                    responseEntity.setCreatedAt(LocalDateTime.now());
                    return responseEntity;
                })
                .onErrorMap(e -> e instanceof OpenAIServiceException ? e :
                        new OpenAIServiceException("Failed to call OpenAI API", e));
    }

    /**
     * Parses the OpenAI API response to extract the generated text.
     *
     * @param response The JSON response from OpenAI.
     * @return The extracted text content or an error message.
     */
    private String parseResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            JSONArray outputArray = json.getJSONArray("output");
            if (!outputArray.isEmpty()) {
                JSONObject firstOutput = outputArray.getJSONObject(0);
                JSONArray contentArray = firstOutput.getJSONArray("content");
                if (!contentArray.isEmpty()) {
                    JSONObject firstContent = contentArray.getJSONObject(0);
                    return firstContent.getString("text");
                }
            }
            throw new OpenAIServiceException("No content in response");
        } catch (Exception e) {
            throw new OpenAIServiceException("Error parsing OpenAI response", e);
        }
    }
}