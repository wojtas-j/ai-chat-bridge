package com.wojtasj.aichatbridge.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Configuration for Swagger/OpenAPI documentation in the AI Chat Bridge application.
 * @since 1.0
 */
@Configuration
public class SwaggerConfig {

    /**
     * Configures the OpenAPI documentation with JWT authentication support.
     * @return an OpenAPI instance with API info and security scheme
     * @since 1.0
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Chat Bridge API")
                        .version("1.0")
                        .description("API for the AI Chat Bridge application, providing user authentication and message processing with OpenAI integration"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}