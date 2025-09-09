package com.wojtasj.aichatbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

/**
 * Main entry point for the AI Chat Bridge application, configuring Spring Boot and enabling JPA auditing, retry, and web support.
 * @since 1.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableRetry
public class AichatbridgeApplication {

	public static void main(String[] args) {

		SpringApplication.run(AichatbridgeApplication.class, args);
	}

	/**
	 * Configures a RestTemplate bean for making HTTP requests.
	 * @return a new RestTemplate instance
	 * @since 1.0
	 */
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
