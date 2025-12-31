package com.example.CalCol.dto;

import lombok.Data;

/**
 * DTO for Social Media Post
 */
@Data
public class SocialMediaPostDTO {
	private String platform;
	private String content;
	private Integer maxLength;
	private Integer currentLength;
}

