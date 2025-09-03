package com.wojtasj.aichatbridge.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for OpenAI API integration.
 * <p>Properties include:</p>
 * <ul>
 *     <li>{@link #apiKey} - the API key used to authenticate with the OpenAI API</li>
 *     <li>{@link #baseUrl} - the base URL for API requests (e.g., <a href="https://api.openai.com">...</a>)</li>
 *     <li>{@link #model} - the default model to use for requests (e.g., "gpt-4")</li>
 *     <li>{@link #maxTokens} - the maximum number of tokens </li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "openai")
@Getter
@Setter
public class OpenAIProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
    private int maxTokens;
}
