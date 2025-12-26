package com.example.CalCol.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
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
}

