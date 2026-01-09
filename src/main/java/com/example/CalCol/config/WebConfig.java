package com.example.CalCol.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.upload.dir:uploads}")
	private String uploadDir;

	@Override
	public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
		String uploadPath = Paths.get(uploadDir).toAbsolutePath().toString();
		registry.addResourceHandler("/uploads/**")
			.addResourceLocations("file:" + uploadPath + "/");
	}

	@Override
	public void addCorsMappings(@NonNull CorsRegistry registry) {
		// Allow CORS for REST API endpoints (useful for web clients, Android doesn't need it but doesn't hurt)
		registry.addMapping("/api/**")
			.allowedOrigins("*")
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
			.allowedHeaders("*")
			.allowCredentials(false);
	}
}

