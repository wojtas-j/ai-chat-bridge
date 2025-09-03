package com.wojtasj.aichatbridge.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Discord bot integration.
 * <p>Properties include:</p>
 * <ul>
 *     <li>{@link #botToken} - the token used to authenticate and connect with the Discord bot</li>
 *     <li>{@link #botPrefix} - the prefix that messages must start with to be recognized by the bot (e.g., "!ai Hello")</li>
 *     <li>{@link #botEnabled} - whether the Discord bot is enabled and should respond to messages</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "discord")
@Getter
@Setter
public class DiscordProperties {
    private String botToken;
    private String botPrefix;
    private boolean botEnabled;
}
