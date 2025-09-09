package com.wojtasj.aichatbridge.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.wojtasj.aichatbridge.configuration.OpenAIProperties;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the functionality of {@link OpenAIServiceImpl} for interacting with the OpenAI API in the AI Chat Bridge application.
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
class OpenAIServiceTest {

    private static final String TEST_MESSAGE_CONTENT = "Hello!";
    private static final String EXPECTED_RESPONSE_CONTENT = "Hi, hello!";
    private static final String CONTENT_TYPE = "application/json";
    private static final String CONTENT_KEY = "content";
    private static final String TOO_MANY_REQUESTS_RESPONSE = "{\"error\":\"Too Many Requests\"}";
    private static final String INVALID_JSON_RESPONSE = "{";
    private static final String EMPTY_CHOICES_RESPONSE = "{\"choices\":[]}";
    private static final String INTERNAL_SERVER_ERROR_RESPONSE = "{\"error\":\"Internal Server Error\"}";
    private static final String MESSAGE_CONTENT_CANNOT_BE_NULL = "Message content cannot be null or empty";
    private static final int WIREMOCK_PORT = 8089;

    private WireMockServer wireMockServer;

    @Autowired
    private OpenAIServiceImpl openAIService;

    @Autowired
    private OpenAIProperties openAIProperties;

    /**
     * Sets up the WireMock server for mocking OpenAI API responses.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(WIREMOCK_PORT));
        wireMockServer.start();
        wireMockServer.resetAll();
    }

    /**
     * Stops the WireMock server after each test.
     * @since 1.0
     */
    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    /**
     * Tests sending a message to OpenAI and receiving a valid response.
     * @since 1.0
     */
    @Test
    void shouldSendMessageToOpenAI() {
        // Arrange
        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(String.format("{\"choices\":[{\"message\":{\"%s\":\"%s\"}}]}", CONTENT_KEY, EXPECTED_RESPONSE_CONTENT))));

        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT);

        // Act
        MessageEntity result = openAIService.sendMessageToOpenAI(input);

        // Assert
        assertThat(result.getContent()).isEqualTo(EXPECTED_RESPONSE_CONTENT);
        assertThat(result.getCreatedAt()).isNotNull();
    }

    /**
     * Tests handling a server error response from OpenAI.
     * @since 1.0
     */
    @Test
    void shouldHandleOpenAIError() {
        // Arrange
        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(INTERNAL_SERVER_ERROR_RESPONSE)));

        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input));
        assertThat(exception.getMessage()).contains("OpenAI API error: 500");
    }

    /**
     * Tests handling an invalid JSON response from OpenAI.
     * @since 1.0
     */
    @Test
    void shouldHandleInvalidJsonResponse() {
        // Arrange
        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(INVALID_JSON_RESPONSE)));

        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input));
        assertThat(exception.getMessage()).contains("Error parsing OpenAI response");
    }

    /**
     * Tests handling an empty choices response from OpenAI.
     * @since 1.0
     */
    @Test
    void shouldHandleEmptyChoicesResponse() {
        // Arrange
        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(EMPTY_CHOICES_RESPONSE)));

        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input));
        assertThat(exception.getMessage()).contains("No content in response");
    }

    /**
     * Tests retry logic when OpenAI returns a 429 Too Many Requests response.
     * @since 1.0
     */
    @Test
    void shouldRetryOnTooManyRequests() {
        // Arrange
        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(TOO_MANY_REQUESTS_RESPONSE))
                .willSetStateTo("First Retry"));

        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Retry")
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(TOO_MANY_REQUESTS_RESPONSE))
                .willSetStateTo("Second Retry"));

        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Retry")
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(String.format("{\"choices\":[{\"message\":{\"%s\":\"%s\"}}]}", CONTENT_KEY, EXPECTED_RESPONSE_CONTENT))));

        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT);

        // Act
        MessageEntity result = openAIService.sendMessageToOpenAI(input);

        // Assert
        assertThat(result.getContent()).isEqualTo(EXPECTED_RESPONSE_CONTENT);
        assertThat(result.getCreatedAt()).isNotNull();
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when the input message is null.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnNullInput() {
        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(null));
        assertThat(exception.getMessage()).contains(MESSAGE_CONTENT_CANNOT_BE_NULL);
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when the input message content is empty.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnEmptyInput() {
        // Arrange
        MessageEntity input = createMessageEntity("");

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input));
        assertThat(exception.getMessage()).contains(MESSAGE_CONTENT_CANNOT_BE_NULL);
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when the input message content is null.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnNullContent() {
        // Arrange
        MessageEntity input = new MessageEntity();
        input.setCreatedAt(LocalDateTime.now());

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input));
        assertThat(exception.getMessage()).contains(MESSAGE_CONTENT_CANNOT_BE_NULL);
    }

    /**
     * Creates a mock MessageEntity for testing purposes.
     * @param content the content of the message
     * @return a MessageEntity with the specified content
     * @since 1.0
     */
    private MessageEntity createMessageEntity(String content) {
        MessageEntity entity = new MessageEntity();
        entity.setContent(content);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
