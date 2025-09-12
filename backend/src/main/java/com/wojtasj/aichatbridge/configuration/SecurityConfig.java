package com.wojtasj.aichatbridge.configuration;

import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configures Spring Security for the AI Chat Bridge application, enabling JWT-based authentication.
 * @since 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProviderImpl jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtTokenProviderImpl jwtTokenProvider, @Lazy UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Configures the security filter chain to protect endpoints and enable JWT authentication.
     * <p>
     * - Public access to /api/auth/login and /api/auth/register for authentication and registration.
     * - Authenticated access to /api/auth/refresh for token refresh.
     * - Authenticated access to /api/messages/** for message-related operations.
     * - Authenticated access to /api/openai/** for OpenAI interactions.
     * - Disables CSRF, form login, and HTTP basic authentication as JWT is used.
     * - Adds JWT authentication filter before UsernamePasswordAuthenticationFilter.
     * </p>
     * @param http the HttpSecurity object used to configure web security
     * @return a SecurityFilterChain defining the security configuration
     * @throws Exception if an error occurs during configuration
     * @since 1.0
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/refresh").authenticated()
                        .requestMatchers("/api/messages/**").authenticated()
                        .requestMatchers("/api/openai/**").authenticated()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Provides the JWT authentication filter for validating JWT tokens.
     * @return a JwtAuthenticationFilter instance
     * @since 1.0
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }
}
