package com.wojtasj.aichatbridge.configuration;

import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
import jakarta.servlet.http.HttpServletResponse;
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
     * Security rules:
     * <ul>
     *     <li>Public access to <code>/api/auth/login</code> and <code>/api/auth/register</code> for authentication and registration.</li>
     *     <li>Authenticated access to <code>/api/auth/refresh</code> for token refresh.</li>
     *     <li>Admin-only access to <code>/api/messages/admin</code>.</li>
     *     <li>Authenticated access to <code>/api/messages/**</code> and <code>/api/openai/**</code>.</li>
     *     <li>Public access to <code>/actuator/health</code>.</li>
     *     <li>Admin-only access to <code>/actuator/**</code>.</li>
     * </ul>
     * <p>
     * Security behavior:
     * <ul>
     *     <li>Disables CSRF, form login, and HTTP basic authentication (JWT-based auth is used).</li>
     *     <li>Stateless session management.</li>
     *     <li>Registers a JWT authentication filter before <code>UsernamePasswordAuthenticationFilter</code>.</li>
     *     <li>Returns a JSON response with structured problem details for access denied (HTTP 403) errors.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object used to configure web security
     * @return a {@link SecurityFilterChain} defining the security configuration
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
                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers("/api/messages/admin").hasRole("ADMIN")
                        .requestMatchers("/api/messages/**").authenticated()
                        .requestMatchers("/api/openai/**").authenticated()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("""
                    {
                        "type": "/problems/authentication-failed",
                        "title": "Authentication Failed",
                        "status": 401,
                        "detail": "You must provide a valid token to access this resource"
                    }
                    """);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("""
                    {
                        "type": "/problems/access-denied",
                        "title": "Access Denied",
                        "status": 403,
                        "detail": "You do not have permission to access this resource"
                    }
                    """);
                        })
                )
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
