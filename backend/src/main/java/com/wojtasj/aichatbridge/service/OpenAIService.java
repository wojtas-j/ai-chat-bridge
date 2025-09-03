package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.MessageEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.json.JSONArray;
import org.json.JSONObject;

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

    public OpenAIService(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.model}") String model,
            @Value("${openai.max-tokens}") int maxTokens) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Sends a message to OpenAI and returns the response as a MessageEntity.
     *
     * @param message The message entity containing the prompt to send.
     * @return A new MessageEntity with the OpenAI response.
     */
    public MessageEntity sendMessageToOpenAI(MessageEntity message) {
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

        try {
            String response = webClient.post()
                    .uri("/responses")
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String aiResponse = parseResponse(response);
            log.debug("Received OpenAI response: {}", aiResponse);

            MessageEntity responseEntity = new MessageEntity();
            responseEntity.setContent(aiResponse);
            responseEntity.setCreatedAt(LocalDateTime.now());
            return responseEntity;
        } catch (Exception e) {
            log.error("Error calling OpenAI API: ", e);
            MessageEntity errorResponse = new MessageEntity();
            errorResponse.setContent("Error: " + e.getMessage());
            errorResponse.setCreatedAt(LocalDateTime.now());
            return errorResponse;
        }
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
            return "No content in response";
        } catch (Exception e) {
            log.error("Error parsing OpenAI response: ", e);
            return "Error parsing response";
        }
    }
}
