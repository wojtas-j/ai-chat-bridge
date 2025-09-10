package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.dto.*;
import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.service.AuthenticationService;
import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
import com.wojtasj.aichatbridge.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller for handling authentication-related endpoints in the AI Chat Bridge application.
 * @since 1.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProviderImpl jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * Retrieves information about the currently authenticated user.
     * @param userDetails the authenticated user's details
     * @return a ResponseEntity containing the current user's basic info
     * @throws ResponseStatusException if an error occurs during user retrieval
     * @since 1.0
     */
    @Operation(summary = "Get current authenticated user's info")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user info", content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Retrieving info for authenticated user: {}", userDetails != null ? userDetails.getUsername() : "none");
        try {
            if (userDetails == null) {
                log.warn("No authenticated user found");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
            }

            UserEntity user = authenticationService.findByUsername(userDetails.getUsername());
            UserDTO userDTO = new UserDTO(user.getUsername(), user.getEmail(), user.getRoles());
            log.info("Successfully retrieved info for user: {}", user.getUsername());
            return ResponseEntity.ok(userDTO);
        } catch (AuthenticationException e) {
            log.error("Failed to retrieve user info: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving user info: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve user info", e);
        }
    }

    /**
     * Registers a new user with the provided details.
     * @param request the registration request containing username, email, and password
     * @return a ResponseEntity containing the registered user's details
     * @throws ResponseStatusException if an error occurs during registration
     * @since 1.0
     */
    @Operation(summary = "Registers a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username or email already taken", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering new user: {}", request.username());
        try {
            UserEntity user = authenticationService.register(request);
            UserDTO userDTO = new UserDTO(user.getUsername(), user.getEmail(), user.getRoles());
            log.info("User registered successfully: {}", user.getUsername());
            return ResponseEntity.ok(userDTO);
        } catch (AuthenticationException e) {
            log.error("Failed to register user: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to register user", e);
        }
    }

    /**
     * Authenticates a user and returns access and refresh tokens.
     * @param request the login request containing username or email and password
     * @return a ResponseEntity containing the access and refresh tokens
     * @throws ResponseStatusException if an error occurs during authentication
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
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String accessToken = jwtTokenProvider.generateToken(authentication);
            UserEntity user = authenticationService.findByUsername(request.username());
            RefreshTokenEntity refreshToken = refreshTokenService.generateRefreshToken(user);
            log.info("User authenticated successfully: {}", request.username());
            return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken.getToken()));
        } catch (Exception e) {
            log.error("Failed to authenticate user: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password", e);
        }
    }

    /**
     * Refreshes an access token using a valid refresh token and rotates the refresh token.
     * @param request the refresh token request containing the refresh token
     * @param userDetails the authenticated user's details
     * @return a ResponseEntity containing a new access token and a new refresh token
     * @throws ResponseStatusException if the refresh token is invalid, expired, or does not match the authenticated user
     * @since 1.0
     */
    @Operation(summary = "Refreshes an access token using a refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access token refreshed successfully", content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token or unauthorized user", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - missing or invalid authentication token", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Refreshing access token for user: {}", userDetails.getUsername());
        try {
            RefreshTokenEntity tokenEntity = refreshTokenService.validateRefreshToken(request.refreshToken());
            UserEntity user = tokenEntity.getUser();
            if (!userDetails.getUsername().equals(user.getUsername())) {
                log.error("Refresh token does not belong to authenticated user: {}", userDetails.getUsername());
                throw new AuthenticationException("Refresh token does not match authenticated user");
            }
            // Rotate refresh token
            refreshTokenService.deleteByUser(user);
            // Delete old refresh tokens
            RefreshTokenEntity newRefreshToken = refreshTokenService.generateRefreshToken(user);
            // Generate new refresh token
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    user.getAuthorities()
            );
            String newAccessToken = jwtTokenProvider.generateToken(authentication);
            log.info("Access token and refresh token rotated successfully for user: {}", user.getUsername());
            return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken.getToken()));
        } catch (AuthenticationException e) {
            log.error("Failed to refresh token: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error refreshing token: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh token", e);
        }
    }

    /**
     * Logs out the authenticated user by invalidating all their refresh tokens.
     * @param userDetails the authenticated user's details
     * @return a ResponseEntity indicating successful logout
     * @since 1.0
     */
    @Operation(summary = "Logs out the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User logged out successfully", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - missing or invalid authentication token", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Logging out user: {}", userDetails.getUsername());
        try {
            UserEntity user = authenticationService.findByUsername(userDetails.getUsername());
            refreshTokenService.deleteByUser(user);
            log.info("User logged out successfully: {}", user.getUsername());
            return ResponseEntity.ok().build();
        } catch (AuthenticationException e) {
            log.error("Failed to log out user: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during logout: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to log out user", e);
        }
    }
}
