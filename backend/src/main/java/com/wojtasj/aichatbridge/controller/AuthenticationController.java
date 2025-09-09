package com.wojtasj.aichatbridge.controller;

import com.wojtasj.aichatbridge.dto.LoginRequest;
import com.wojtasj.aichatbridge.dto.LoginResponse;
import com.wojtasj.aichatbridge.dto.RegisterRequest;
import com.wojtasj.aichatbridge.dto.UserDTO;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.service.AuthenticationService;
import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
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

    /**
     * Retrieves information about the currently authenticated user.
     *
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
     * Authenticates a user and returns a JWT token.
     * @param request the login request containing username or email and password
     * @return a ResponseEntity containing the JWT token
     * @throws ResponseStatusException if an error occurs during authentication
     * @since 1.0
     */
    @Operation(summary = "Authenticates a user and returns a JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User authenticated successfully", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid username or password", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Authenticating user: {}", request.username());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtTokenProvider.generateToken(authentication);
            log.info("User authenticated successfully: {}", request.username());
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (Exception e) {
            log.error("Failed to authenticate user: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password", e);
        }
    }
}
