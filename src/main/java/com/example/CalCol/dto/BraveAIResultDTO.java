package com.example.CalCol.dto;

import lombok.Data;

import java.util.Map;

/**
 * DTO for Brave AI Search Result
 */
@Data
public class BraveAIResultDTO {
	private String manufacturer;
	private String model;
	private String rawResponse;
	private String sourceUrl;
	private Map<String, String> structuredData;
}

