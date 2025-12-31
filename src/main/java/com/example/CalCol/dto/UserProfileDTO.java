package com.example.CalCol.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for User Profile information
 */
@Data
public class UserProfileDTO {
	private Long id;
	private String username;
	private String email;
	private String role;
	private Boolean enabled;
	private LocalDateTime createdAt;
	private LocalDateTime lastLogin;
}

