package com.example.CalCol.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI/Swagger documentation
 */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI calculatorCollectorOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("Calculator Collector API")
				.description("REST API for Calculator Collector application. " +
					"Provides endpoints for managing calculator collections, labels, images, and more. " +
					"Authenticate via HTTP Basic Auth or obtain a JWT from POST /api/auth/login.")
				.version("1.0.0")
				.contact(new Contact()
					.name("Calculator Collector")
					.email("support@calculatorcollector.example.com"))
				.license(new License()
					.name("GPL-3.0")
					.url("https://www.gnu.org/licenses/gpl-3.0.html")))
			.addSecurityItem(new SecurityRequirement().addList("basicAuth"))
			.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
			.components(new Components()
				.addSecuritySchemes("basicAuth", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("basic")
					.description("HTTP Basic Authentication"))
				.addSecuritySchemes("bearerAuth", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.description("JWT Bearer token from POST /api/auth/login or /api/auth/register")));
	}
}
