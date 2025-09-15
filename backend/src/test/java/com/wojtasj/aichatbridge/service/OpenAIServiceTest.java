package com.wojtasj.aichatbridge.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.wojtasj.aichatbridge.configuration.OpenAIProperties;
import com.wojtasj.aichatbridge.entity.*;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link OpenAIServiceImpl} in the AI Chat Bridge application, focusing on interactions with the OpenAI API.
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
class OpenAIServiceTest {

    private static final String TEST_USER = "TEST_USER";
    private static final String TEST_MESSAGE_CONTENT = "Hello!";
    private static final String EXPECTED_RESPONSE_CONTENT = "Hi, hello!";
    private static final String TEST_API_KEY = "test-key";
    private static final String TEST_MODEL = "gpt-4o-mini";
    private static final String INVALID_MODEL = "invalid-model";
    private static final int TEST_MAX_TOKENS = 100;
    private static final String CONTENT_TYPE = "application/json";
    private static final String CONTENT_KEY = "content";
    private static final String TOO_MANY_REQUESTS_RESPONSE = "{\"error\":\"Too Many Requests\"}";
    private static final String INVALID_JSON_RESPONSE = "{";
    private static final String INVALID_RESPONSE_STRUCTURE = "{\"invalid\":[]}";
    private static final String EMPTY_CHOICES_RESPONSE = "{\"choices\":[]}";
    private static final String INTERNAL_SERVER_ERROR_RESPONSE = "{\"error\":\"Internal Server Error\"}";
    private static final String MESSAGE_CONTENT_CANNOT_BE_NULL = "Message content cannot be null or empty";
    private static final int WIREMOCK_PORT = 8089;

    private WireMockServer wireMockServer;

    @Autowired
    private OpenAIServiceImpl openAIService;

    @Autowired
    private OpenAIProperties openAIProperties;

    private UserEntity userEntity;

    /**
     * Sets up the WireMock server for mocking OpenAI API responses and initializes test user.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(WIREMOCK_PORT));
        wireMockServer.start();
        wireMockServer.resetAll();

        userEntity = UserEntity.builder()
                .username(TEST_USER)
                .email("test@example.com")
                .password("password123P!")
                .roles(Set.of(Role.USER))
                .apiKey(TEST_API_KEY)
                .model(TEST_MODEL)
                .maxTokens(TEST_MAX_TOKENS)
                .build();
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
     * Sets up a WireMock stub for the /models endpoint to simulate successful API key and model validation.
     *
     * @since 1.0
     */
    private void stubForModelsEndpoint() {
        wireMockServer.stubFor(get(urlEqualTo("/models"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(String.format("{\"data\":[{\"id\":\"%s\"}]}", OpenAIServiceTest.TEST_MODEL))));
    }

    /**
     * Tests sending a message to OpenAI and receiving a valid response for MessageEntity with associated user.
     * @since 1.0
     */
    @Test
    void shouldSendMessageToOpenAIWithUser() {
        // Arrange
        stubForModelsEndpoint();
        wireMockServer.stubFor(post(urlPathEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(String.format("{\"choices\":[{\"message\":{\"%s\":\"%s\"}}]}", CONTENT_KEY, EXPECTED_RESPONSE_CONTENT))));

        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT, userEntity);

        // Act
        MessageEntity result = openAIService.sendMessageToOpenAI(input, false, userEntity);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(EXPECTED_RESPONSE_CONTENT);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUser()).isEqualTo(userEntity);
    }

    /**
     * Tests sending a Discord message to OpenAI and receiving a valid response for DiscordMessageEntity.
     * @since 1.0
     */
    @Test
    void shouldSendDiscordMessageToOpenAI() {
        // Arrange
        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(String.format("{\"choices\":[{\"message\":{\"%s\":\"%s\"}}]}", CONTENT_KEY, EXPECTED_RESPONSE_CONTENT))));

        DiscordMessageEntity input = createDiscordMessageEntity();

        // Act
        DiscordMessageEntity result = openAIService.sendMessageToOpenAI(input, true, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(EXPECTED_RESPONSE_CONTENT);
        assertThat(result.getDiscordNick()).isEqualTo("AI-Bot");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    /**
     * Tests handling a server error response from OpenAI.
     * @since 1.0
     */
    @Test
    void shouldHandleOpenAIError() {
        // Arrange
        stubForModelsEndpoint();
        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(INTERNAL_SERVER_ERROR_RESPONSE)));

        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT, userEntity);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input, false, userEntity));
        assertThat(exception.getMessage()).contains("OpenAI API error: 500");
    }

    /**
     * Tests handling an invalid JSON response from OpenAI.
     * @since 1.0
     */
    @Test
    void shouldHandleInvalidJsonResponse() {
        // Arrange
        stubForModelsEndpoint();
        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(INVALID_JSON_RESPONSE)));

        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT, userEntity);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input, false, userEntity));
        assertThat(exception.getMessage()).contains("Error parsing OpenAI response");
    }

    /**
     * Tests handling an empty choices response from OpenAI.
     * @since 1.0
     */
    @Test
    void shouldHandleEmptyChoicesResponse() {
        // Arrange
        stubForModelsEndpoint();
        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(EMPTY_CHOICES_RESPONSE)));

        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT, userEntity);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input, false, userEntity));
        assertThat(exception.getMessage()).contains("No content in response");
    }

    /**
     * Tests retry logic when OpenAI returns a 429 Too Many Requests response.
     * @since 1.0
     */
    @Test
    void shouldRetryOnTooManyRequests() {
        // Arrange
        stubForModelsEndpoint();
        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(TOO_MANY_REQUESTS_RESPONSE))
                .willSetStateTo("First Retry"));

        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Retry")
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(TOO_MANY_REQUESTS_RESPONSE))
                .willSetStateTo("Second Retry"));

        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Retry")
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(String.format("{\"choices\":[{\"message\":{\"%s\":\"%s\"}}]}", CONTENT_KEY, EXPECTED_RESPONSE_CONTENT))));

        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT, userEntity);

        // Act
        MessageEntity result = openAIService.sendMessageToOpenAI(input, false, userEntity);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(EXPECTED_RESPONSE_CONTENT);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUser()).isEqualTo(userEntity);
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when the input message is null.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnNullInput() {
        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(null, false, userEntity));
        assertThat(exception.getMessage()).contains(MESSAGE_CONTENT_CANNOT_BE_NULL);
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when the input message content is empty.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnEmptyInput() {
        // Arrange
        MessageEntity input = createMessageEntity("", userEntity);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input, false, userEntity));
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
        input.setUser(userEntity);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input, false, userEntity));
        assertThat(exception.getMessage()).contains(MESSAGE_CONTENT_CANNOT_BE_NULL);
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when the user is null for non-Discord messages.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnNullUserForNonDiscordMessage() {
        // Arrange
        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT, null);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input, false, null));
        assertThat(exception.getMessage()).contains("User is required for non-Discord messages");
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when the API key is null for non-Discord messages.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnNullApiKeyForNonDiscordMessage() {
        // Arrange
        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT, userEntity);
        userEntity.setApiKey(null);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input, false, userEntity));
        assertThat(exception.getMessage()).contains("API key required for non-Discord messages");
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when max tokens is null for non-Discord messages.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnNullMaxTokensForNonDiscordMessage() {
        // Arrange
        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT, userEntity);
        userEntity.setMaxTokens(null);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input, false, userEntity));
        assertThat(exception.getMessage()).contains("Max tokens must be positive for non-Discord messages");
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when max tokens is non-positive for non-Discord messages.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnNonPositiveMaxTokensForNonDiscordMessage() {
        // Arrange
        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT, userEntity);
        userEntity.setMaxTokens(0);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input, false, userEntity));
        assertThat(exception.getMessage()).contains("Max tokens must be positive for non-Discord messages");
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when the model is null for non-Discord messages.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnNullModelForNonDiscordMessage() {
        // Arrange
        MessageEntity input = createMessageEntity(TEST_MESSAGE_CONTENT, userEntity);
        userEntity.setModel(null);

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(input, false, userEntity));
        assertThat(exception.getMessage()).contains("Model required for non-Discord messages");
    }

    /**
     * Tests successful validation of an API key and model.
     * @since 1.0
     */
    @Test
    void shouldValidateApiKeyAndModelSuccessfully() {
        // Arrange
        wireMockServer.stubFor(get(urlEqualTo("/models"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(String.format("{\"data\":[{\"id\":\"%s\"}]}", TEST_MODEL))));

        // Act
        openAIService.validateApiKeyAndModel(TEST_API_KEY, TEST_MODEL);

        // Assert
        assertThat(wireMockServer.getAllServeEvents()).hasSize(1);
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when validating an invalid API key.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnInvalidApiKey() {
        // Arrange
        wireMockServer.stubFor(get(urlEqualTo("/models"))
                .withHeader("Authorization", equalTo("Bearer invalid-key"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody("{\"error\":\"Unauthorized\"}")));

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.validateApiKeyAndModel("invalid-key", TEST_MODEL));
        assertThat(exception.getMessage()).contains("Invalid OpenAI API key: 401");
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when validating a null API key.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnNullApiKeyValidation() {
        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.validateApiKeyAndModel(null, TEST_MODEL));
        assertThat(exception.getMessage()).contains("API key cannot be null or empty during validation");
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when validating a null model.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnNullModelValidation() {
        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.validateApiKeyAndModel(TEST_API_KEY, null));
        assertThat(exception.getMessage()).contains("Model cannot be null or empty during validation");
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when validating an invalid model.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnInvalidModel() {
        // Arrange
        wireMockServer.stubFor(get(urlEqualTo("/models"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody("{\"data\":[{\"id\":\"gpt-4o-mini\"},{\"id\":\"gpt-4\"}]}")));

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.validateApiKeyAndModel(TEST_API_KEY, INVALID_MODEL));
        assertThat(exception.getMessage()).contains("Invalid OpenAI model: " + INVALID_MODEL);
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when validating with too many requests.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnTooManyRequestsValidation() {
        // Arrange
        wireMockServer.stubFor(get(urlEqualTo("/models"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(TOO_MANY_REQUESTS_RESPONSE)));

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.validateApiKeyAndModel(TEST_API_KEY, TEST_MODEL));
        assertThat(exception.getMessage()).contains("Too many requests to OpenAI API");
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when validating with invalid response structure.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnInvalidResponseStructure() {
        // Arrange
        wireMockServer.stubFor(get(urlEqualTo("/models"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(INVALID_RESPONSE_STRUCTURE)));

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.validateApiKeyAndModel(TEST_API_KEY, TEST_MODEL));
        assertThat(exception.getMessage()).contains("Invalid response structure from OpenAI models endpoint");
    }

    /**
     * Tests throwing {@link OpenAIServiceException} when validating with server error.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionOnServerErrorValidation() {
        // Arrange
        wireMockServer.stubFor(get(urlEqualTo("/models"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(INTERNAL_SERVER_ERROR_RESPONSE)));

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.validateApiKeyAndModel(TEST_API_KEY, TEST_MODEL));
        assertThat(exception.getMessage()).contains("OpenAI API server error: 500");
    }

    /**
     * Tests throwing {@link OpenAIServiceException} for unsupported message type.
     * @since 1.0
     */
    @Test
    void shouldThrowExceptionForUnsupportedMessageType() {
        // Arrange
        stubForModelsEndpoint();
        wireMockServer.stubFor(post(urlEqualTo(openAIProperties.getChatCompletionsEndpoint()))
                .withHeader("Authorization", equalTo("Bearer " + TEST_API_KEY))
                .withRequestBody(containing(String.format("\"%s\":\"%s\"", CONTENT_KEY, TEST_MESSAGE_CONTENT)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(String.format("{\"choices\":[{\"message\":{\"%s\":\"%s\"}}]}", CONTENT_KEY, EXPECTED_RESPONSE_CONTENT))));

        BaseMessage unsupportedMessage = new BaseMessage() {
            @Override
            public String getContent() {
                return TEST_MESSAGE_CONTENT;
            }

            @Override
            public void setContent(String content) {
            }
        };

        // Act & Assert
        OpenAIServiceException exception = assertThrows(OpenAIServiceException.class,
                () -> openAIService.sendMessageToOpenAI(unsupportedMessage, false, userEntity));
        assertThat(exception.getMessage()).contains("Unsupported message type");
    }

    /**
     * Creates a mock MessageEntity for testing purposes.
     * @param content the content of the message
     * @param user the user associated with the message
     * @return a MessageEntity with the specified content and user
     * @since 1.0
     */
    private MessageEntity createMessageEntity(String content, UserEntity user) {
        MessageEntity entity = new MessageEntity();
        entity.setContent(content);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUser(user);
        return entity;
    }

    /**
     * Creates a mock DiscordMessageEntity for testing purposes.
     * @return a DiscordMessageEntity with predefined content and discordNick
     * @since 1.0
     */
    private DiscordMessageEntity createDiscordMessageEntity() {
        DiscordMessageEntity entity = new DiscordMessageEntity();
        entity.setContent(TEST_MESSAGE_CONTENT);
        entity.setDiscordNick(TEST_USER);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
