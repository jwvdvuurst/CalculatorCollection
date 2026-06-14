package com.example.CalCol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
	private String accessToken;
	private String tokenType;
	private long expiresIn;
	private UserProfileDTO user;
}
