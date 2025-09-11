package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.dto.*;
import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AccessDeniedException;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.service.AuthenticationService;
import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
import com.wojtasj.aichatbridge.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling authentication-related endpoints in the AI Chat Bridge application.
 * @since 1.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to access this resource";
    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Invalid refresh token";
    private static final String REFRESH_TOKEN_MISMATCH_MESSAGE = "Refresh token does not match authenticated user";

    private final AuthenticationService authenticationService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProviderImpl jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * Retrieves information about the currently authenticated user.
     * @param userDetails the authenticated user's details
     * @return ResponseEntity with the current user's info (username, email, roles)
     * @throws AuthenticationException if the user is not found
     * @since 1.0
     */
    @Operation(summary = "Get current authenticated user's info", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user info", content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to access", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("No authenticated user found");
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
        log.info("Retrieving info for authenticated user: {}", userDetails.getUsername());
        UserEntity user = authenticationService.findByUsername(userDetails.getUsername());
        log.info("Successfully retrieved info for user: {}", user.getUsername());
        return ResponseEntity.ok(new UserDTO(user.getUsername(), user.getEmail(), user.getRoles()));
    }

    /**
     * Registers a new user with the provided details.
     * @param request the registration request with username, email, and password
     * @return ResponseEntity with the registered user's details
     * @throws AuthenticationException if registration fails (e.g., username or email already taken)
     * @since 1.0
     */
    @Operation(summary = "Registers a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username or email already taken", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering new user: {}", request.username());
        UserEntity user = authenticationService.register(request);
        log.info("User registered successfully: {}", user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(new UserDTO(user.getUsername(), user.getEmail(), user.getRoles()));
    }

    /**
     * Authenticates a user and returns access and refresh tokens.
     * @param request the login request with username or email and password
     * @return ResponseEntity with access and refresh tokens
     * @throws AuthenticationException if authentication fails
     * @since 1.0
     */
    @Operation(summary = "Authenticates a user and returns access and refresh tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User authenticated successfully", content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid username or password", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Authenticating user: {}", request.username());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        String accessToken = jwtTokenProvider.generateToken(authentication);
        UserEntity user = authenticationService.findByUsername(request.username());
        RefreshTokenEntity refreshToken = refreshTokenService.generateRefreshToken(user);
        log.info("User authenticated successfully: {}", request.username());
        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken.getToken()));
    }

    /**
     * Refreshes an access token using a valid refresh token and rotates the refresh token.
     * @param request the refresh token request containing the refresh token
     * @param userDetails the authenticated user's details
     * @return ResponseEntity with new access and refresh tokens
     * @throws AuthenticationException if the refresh token is invalid, expired, or does not match the user
     * @since 1.0
     */
    @Operation(summary = "Refreshes an access token using a refresh token", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access token refreshed successfully", content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token or unauthorized user", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to access", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("No authenticated user tries to refresh token");
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
        log.info("Refreshing access token for user: {}", userDetails.getUsername());
        RefreshTokenEntity tokenEntity = refreshTokenService.validateRefreshToken(request.refreshToken());
        if (tokenEntity == null) {
            log.error("Invalid refresh token: {}", request.refreshToken());
            throw new AuthenticationException(INVALID_REFRESH_TOKEN_MESSAGE);
        }
        UserEntity user = tokenEntity.getUser();
        if (user == null || !user.getUsername().equals(userDetails.getUsername())) {
            log.error("Refresh token does not belong to authenticated user: {}", userDetails.getUsername());
            throw new AuthenticationException(REFRESH_TOKEN_MISMATCH_MESSAGE);
        }
        refreshTokenService.deleteByUser(user);
        RefreshTokenEntity newRefreshToken = refreshTokenService.generateRefreshToken(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        String newAccessToken = jwtTokenProvider.generateToken(authentication);
        log.info("Access token and refresh token rotated successfully for user: {}", user.getUsername());
        return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken.getToken()));
    }

    /**
     * Logs out the authenticated user by invalidating all their refresh tokens.
     * @param userDetails the authenticated user's details
     * @return ResponseEntity indicating successful logout
     * @throws AuthenticationException if the user is not found
     * @since 1.0
     */
    @Operation(summary = "Logs out the authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User logged out successfully", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - no permission to access", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("No authenticated user found tries to logout");
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
        log.info("Logging out user: {}", userDetails.getUsername());
        UserEntity user = authenticationService.findByUsername(userDetails.getUsername());
        refreshTokenService.deleteByUser(user);
        log.info("User logged out successfully: {}", user.getUsername());
        return ResponseEntity.ok().build();
    }
}
