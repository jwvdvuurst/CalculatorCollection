package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.UserProfileDTO;
import com.example.CalCol.service.DtoMapperService;
import com.example.CalCol.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for user profile operations
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "API for managing user profile and settings")
@SecurityRequirement(name = "basicAuth")
public class UserRestController {

	private final UserService userService;
	private final DtoMapperService dtoMapper;

	@GetMapping("/profile")
	@Operation(summary = "Get user profile", description = "Get the authenticated user's profile information")
	public ResponseEntity<ApiResponse<UserProfileDTO>> getProfile(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		return userService.getUserByUsername(username)
			.map(user -> {
				UserProfileDTO dto = dtoMapper.toUserProfileDTO(user);
				return ResponseEntity.ok(ApiResponse.success(dto));
			})
			.orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("User not found")));
	}

	@PutMapping("/profile/email")
	@Operation(summary = "Update email", description = "Update the authenticated user's email address")
	public ResponseEntity<ApiResponse<Void>> updateEmail(
			@Parameter(description = "New email address") @RequestParam String email,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		try {
			boolean updated = userService.updateProfile(username, email);
			if (updated) {
				return ResponseEntity.ok(ApiResponse.success("Email updated successfully", null));
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ApiResponse.error("Email already exists or user not found"));
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to update email: " + e.getMessage()));
		}
	}

	@PutMapping("/profile/password")
	@Operation(summary = "Change password", description = "Change the authenticated user's password")
	public ResponseEntity<ApiResponse<Void>> changePassword(
			@Parameter(description = "Current password") @RequestParam String oldPassword,
			@Parameter(description = "New password") @RequestParam String newPassword,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		try {
			boolean changed = userService.changePassword(username, oldPassword, newPassword);
			if (changed) {
				return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ApiResponse.error("Current password is incorrect"));
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to change password: " + e.getMessage()));
		}
	}
}

