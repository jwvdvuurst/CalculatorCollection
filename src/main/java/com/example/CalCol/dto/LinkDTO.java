package com.example.CalCol.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for Calculator Link information
 */
@Data
public class LinkDTO {
	private Long id;
	private String url;
	private String title;
	private String description;
	private String addedBy;
	private LocalDateTime addedAt;
}

