package com.example.CalCol.dto;

import lombok.Data;

import java.util.List;

/**
 * DTO for AI Search Result
 */
@Data
public class AISearchResultDTO {
	private String content;
	private List<String> links;
}

