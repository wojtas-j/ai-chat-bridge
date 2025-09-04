package com.wojtasj.aichatbridge.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OpenAIService.
 */
@SpringBootTest
@ActiveProfiles("test")
class OpenAIServiceTest {

    private static final String WIREMOCK_URL = "/responses";
    private static final int WIREMOCK_PORT = 8089;

    private WireMockServer wireMockServer;

    @Autowired
    private OpenAIService openAIService;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().port(WIREMOCK_PORT));
        wireMockServer.start();
        wireMockServer.resetAll();
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    /**
     * Tests sending a message to OpenAI and receiving a response.
     */
    @Test
    void shouldSendMessageToOpenAI() {
        // Arrange
        wireMockServer.stubFor(post(urlEqualTo(WIREMOCK_URL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"output\":[{\"content\":[{\"text\":\"Hi, hello!\"}]}]}")));

        MessageEntity input = createMessageEntity();

        // Act
        Mono<MessageEntity> result = openAIService.sendMessageToOpenAI(input);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getContent()).isEqualTo("Hi, hello!");
                    assertThat(response.getCreatedAt()).isNotNull();
                })
                .expectComplete()
                .verify();
    }

    /**
     * Tests handling an error response from OpenAI.
     */
    @Test
    void shouldHandleOpenAIError() {
        // Arrange
        wireMockServer.stubFor(post(urlEqualTo(WIREMOCK_URL))
                .withRequestBody(containing("Hello"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Internal Server Error\"}")));

        MessageEntity input = createMessageEntity();

        // Act
        Mono<MessageEntity> result = openAIService.sendMessageToOpenAI(input);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof OpenAIServiceException &&
                        e.getMessage().contains("OpenAI API error: 500"))
                .verify();
    }

    /**
     * Tests handling invalid JSON response from OpenAI.
     */
    @Test
    void shouldHandleInvalidJsonResponse() {
        // Arrange
        wireMockServer.stubFor(post(urlEqualTo(WIREMOCK_URL))
                .withRequestBody(containing("Hello"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{")));

        MessageEntity input = createMessageEntity();

        // Act
        Mono<MessageEntity> result = openAIService.sendMessageToOpenAI(input);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof OpenAIServiceException
                        && e.getMessage().contains("Error parsing OpenAI response"))
                .verify();
    }

    private MessageEntity createMessageEntity() {
        MessageEntity entity = new MessageEntity();
        entity.setContent("Hello!");
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}