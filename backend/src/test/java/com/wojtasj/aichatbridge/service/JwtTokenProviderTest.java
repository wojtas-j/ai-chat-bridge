package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.configuration.JwtProperties;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtTokenProviderImpl} in the AI Chat Bridge application.
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtTokenProviderTest {

    private static final String TEST_USER = "testuser";
    private static final String INVALID_OR_EXPIRED_TOKEN = "Invalid or expired JWT token";
    private static final String JWT_TOKEN_EMPTY_OR_NULL = "JWT token is empty or null";
    private static final String INVALID_TOKEN = "invalid-token";
    
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtTokenProviderImpl jwtTokenProvider;

    private Authentication authentication;
    private String secretKey;
    private SecretKey signingKey;
    private long expirationMs;

    /**
     * Sets up the test environment with mock UserEntity, Authentication, and JWT properties.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        secretKey = "dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9yc2lnbmluZ2p3dHRva2Vucw==";
        signingKey = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secretKey));
        expirationMs = 3_600_000L;
        UserEntity userEntity = UserEntity.builder()
                .username(TEST_USER)
                .email("testuser@example.com")
                .roles(Set.of(Role.USER))
                .build();
        authentication = new UsernamePasswordAuthenticationToken(
                userEntity, null, userEntity.getAuthorities());

        lenient().when(jwtProperties.getSecret()).thenReturn(secretKey);
        lenient().when(jwtProperties.getExpirationMs()).thenReturn(expirationMs);
    }

    /**
     * Tests successful generation of a JWT token.
     * @since 1.0
     */
    @Test
    void shouldGenerateTokenSuccessfully() {
        // Arrange & Act
        String token = jwtTokenProvider.generateToken(authentication);
        var claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        @SuppressWarnings("unchecked")
        List<Map<String, String>> roles = (List<Map<String, String>>) claims.get("roles");

        // Assert
        assertThat(token).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(TEST_USER);
        assertThat(claims.get("roles")).isInstanceOf(List.class);
        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().get("authority")).isEqualTo("ROLE_USER");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getIssuedAt().getTime()).isCloseTo(System.currentTimeMillis(), within(1000L));
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().getTime()).isGreaterThan(System.currentTimeMillis());
        assertThat(claims.getExpiration().getTime()).isLessThanOrEqualTo(System.currentTimeMillis() + expirationMs);
        verify(jwtProperties).getSecret();
        verify(jwtProperties).getExpirationMs();
    }

    /**
     * Tests throwing AuthenticationException for invalid principal type.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForInvalidPrincipal() {
        // Arrange
        Authentication invalidAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                TEST_USER, null, Set.of(new SimpleGrantedAuthority("ROLE_USER")));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> jwtTokenProvider.generateToken(invalidAuth));
        assertThat(exception.getMessage()).contains("Invalid authentication principal");
        verify(jwtProperties, never()).getSecret();
        verify(jwtProperties, never()).getExpirationMs();
    }

    /**
     * Tests throwing AuthenticationException for invalid secret key.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForInvalidSecretKey() {
        // Arrange
        when(jwtProperties.getSecret()).thenReturn("invalid-base64-key");

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> jwtTokenProvider.generateToken(authentication));
        assertThat(exception.getMessage()).contains("Failed to generate JWT token");
        verify(jwtProperties).getSecret();
        verify(jwtProperties).getExpirationMs();
    }

    /**
     * Tests successful extraction of username from a valid JWT token.
     * @since 1.0
     */
    @Test
    void shouldGetUsernameFromTokenSuccessfully() {
        // Arrange
        String token = Jwts.builder()
                .subject(TEST_USER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();

        // Act
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Assert
        assertThat(username).isEqualTo(TEST_USER);
        verify(jwtProperties).getSecret();
    }

    /**
     * Tests throwing AuthenticationException for an invalid JWT token.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForInvalidTokenInGetUsername() {
        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> jwtTokenProvider.getUsernameFromToken(INVALID_TOKEN));
        assertThat(exception.getMessage()).contains(INVALID_OR_EXPIRED_TOKEN);
        verify(jwtProperties).getSecret();
    }

    /**
     * Tests throwing AuthenticationException for an expired JWT token in getUsername.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForExpiredTokenInGetUsername() {
        // Arrange
        String expiredToken = Jwts.builder()
                .subject(TEST_USER)
                .issuedAt(new Date(System.currentTimeMillis() - 2 * expirationMs))
                .expiration(new Date(System.currentTimeMillis() - expirationMs))
                .signWith(signingKey)
                .compact();

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> jwtTokenProvider.getUsernameFromToken(expiredToken));
        assertThat(exception.getMessage()).contains(INVALID_OR_EXPIRED_TOKEN);
        verify(jwtProperties).getSecret();
    }

    /**
     * Tests successful validation of a JWT token.
     * @since 1.0
     */
    @Test
    void shouldValidateTokenSuccessfully() {
        // Arrange
        String token = Jwts.builder()
                .subject(TEST_USER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
        verify(jwtProperties).getSecret();
    }

    /**
     * Tests throwing AuthenticationException for an invalid JWT token in validateToken.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForInvalidTokenInValidate() {
        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> jwtTokenProvider.validateToken(INVALID_TOKEN));
        assertThat(exception.getMessage()).contains(INVALID_OR_EXPIRED_TOKEN);
        verify(jwtProperties).getSecret();
    }

    /**
     * Tests throwing AuthenticationException for an expired JWT token in validateToken.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForExpiredTokenInValidate() {
        // Arrange
        String expiredToken = Jwts.builder()
                .subject(TEST_USER)
                .issuedAt(new Date(System.currentTimeMillis() - 2 * expirationMs))
                .expiration(new Date(System.currentTimeMillis() - expirationMs))
                .signWith(signingKey)
                .compact();

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> jwtTokenProvider.validateToken(expiredToken));
        assertThat(exception.getMessage()).contains(INVALID_OR_EXPIRED_TOKEN);
        verify(jwtProperties).getSecret();
    }

    /**
     * Tests throwing AuthenticationException for a null or empty JWT token in validateToken.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForNullOrEmptyTokenInValidate() {
        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> jwtTokenProvider.validateToken(""));
        assertThat(exception.getMessage()).contains(JWT_TOKEN_EMPTY_OR_NULL);
        verify(jwtProperties).getSecret();

        exception = assertThrows(AuthenticationException.class,
                () -> jwtTokenProvider.validateToken(null));
        assertThat(exception.getMessage()).contains(JWT_TOKEN_EMPTY_OR_NULL);
        verify(jwtProperties, times(2)).getSecret();
    }

    @Test
    void shouldValidateJwtPropertiesSuccessfully() {
        // Arrange
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secretKey);
        properties.setExpirationMs(expirationMs);
        properties.setRefreshExpirationDays(1L);

        // Act & Assert
        properties.validate();
        assertThat(properties.getSecret()).isEqualTo(secretKey);
        assertThat(properties.getExpirationMs()).isEqualTo(expirationMs);
        assertThat(properties.getRefreshExpirationDays()).isEqualTo(1L);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForInvalidSecretInJwtProperties() {
        // Arrange
        JwtProperties properties = new JwtProperties();
        properties.setSecret("short");
        properties.setExpirationMs(expirationMs);
        properties.setRefreshExpirationDays(1L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, properties::validate);
        assertThat(exception.getMessage()).contains("JWT secret must be at least 32 characters long");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForInvalidExpirationMsInJwtProperties() {
        // Arrange
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secretKey);
        properties.setExpirationMs(0L);
        properties.setRefreshExpirationDays(1L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, properties::validate);
        assertThat(exception.getMessage()).contains("JWT access token expirationMs must be positive");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForInvalidRefreshExpirationDaysInJwtProperties() {
        // Arrange
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secretKey);
        properties.setExpirationMs(expirationMs);
        properties.setRefreshExpirationDays(0L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, properties::validate);
        assertThat(exception.getMessage()).contains("JWT refresh token expirationDays must be positive");
    }
}
