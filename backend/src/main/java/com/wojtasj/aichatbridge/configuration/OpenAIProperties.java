package com.wojtasj.aichatbridge.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Defines configuration properties for OpenAI API integration in the AI Chat Bridge application.
 * <p>Properties include:</p>
 * <ul>
 *     <li>{@link #baseUrl} - the base URL for API requests (e.g., https://api.openai.com)</li>
 *     <li>{@link #chatCompletionsEndpoint} - the endpoint for chat completions (default: "/chat/completions")</li>
 * </ul>
 * @since 1.0
 * @see <a href="https://api.openai.com">OpenAI API</a>
 */
@Component
@ConfigurationProperties(prefix = "openai")
@Getter
@Setter
public class OpenAIProperties {
    private String baseUrl;
    private String chatCompletionsEndpoint = "/chat/completions";
}
