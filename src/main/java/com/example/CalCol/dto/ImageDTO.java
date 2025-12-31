package com.example.CalCol.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for Calculator Image information
 */
@Data
public class ImageDTO {
	private Long id;
	private String imagePath;
	private String imageUrl;
	private String uploadedBy;
	private Boolean isProposal;
	private Boolean isApproved;
	private String approvedBy;
	private LocalDateTime uploadedAt;
	private LocalDateTime approvedAt;
}

