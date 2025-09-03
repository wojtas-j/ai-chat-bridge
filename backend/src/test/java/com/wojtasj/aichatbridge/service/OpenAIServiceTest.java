package com.wojtasj.aichatbridge.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.wojtasj.aichatbridge.configuration.OpenAIProperties;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OpenAIService.
 */
@SpringBootTest(
        classes = {OpenAIService.class, OpenAIProperties.class},
        properties = {
                "openai.api-key=test-key",
                "openai.base-url=http://localhost:8089",
                "openai.model=gpt-4o-mini",
                "openai.max-tokens=100",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=false"
        }
)
@ExtendWith(MockitoExtension.class)
public class OpenAIServiceTest {
    private WireMockServer wireMockServer;
    @Autowired
    private OpenAIService openAIService;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().port(8089));
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    /**
     * Tests sending a message to OpenAI and receiving a response.
     */
    @Test
    void shouldSendMessageToOpenAI() {
        wireMockServer.stubFor(post(urlEqualTo("/responses"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"output\":[{\"content\":[{\"text\":\"Hi, hello!\"}]}]}")));

        MessageEntity input = new MessageEntity();
        input.setContent("Hello!");
        input.setCreatedAt(LocalDateTime.now());

        MessageEntity result = openAIService.sendMessageToOpenAI(input);

        assertThat(result.getContent()).isEqualTo("Hi, hello!");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    /**
     * Tests handling an error response from OpenAI.
     */
    @Test
    void shouldHandleOpenAIError() {
        wireMockServer.stubFor(post(urlEqualTo("/responses"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Internal Server Error\"}")));

        MessageEntity input = new MessageEntity();
        input.setContent("Hello!");
        input.setCreatedAt(LocalDateTime.now());

        MessageEntity result = openAIService.sendMessageToOpenAI(input);

        assertThat(result.getContent()).startsWith("Error");
        assertThat(result.getCreatedAt()).isNotNull();
    }
}