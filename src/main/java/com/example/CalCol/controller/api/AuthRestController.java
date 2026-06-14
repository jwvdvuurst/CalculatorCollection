package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.AuthResponse;
import com.example.CalCol.dto.LoginRequest;
import com.example.CalCol.dto.RegisterRequest;
import com.example.CalCol.dto.UserProfileDTO;
import com.example.CalCol.entity.AppUser;
import com.example.CalCol.service.DtoMapperService;
import com.example.CalCol.service.JwtService;
import com.example.CalCol.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and obtain JWT access tokens")
public class AuthRestController {

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final UserService userService;
	private final JwtService jwtService;
	private final DtoMapperService dtoMapper;

	@PostMapping("/register")
	@Operation(summary = "Register a new user", description = "Create a new user account and return a JWT access token")
	public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
		String validationError = validateRegistration(request);
		if (validationError != null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(validationError));
		}

		if (userService.usernameExists(request.getUsername().trim())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Username already exists"));
		}

		if (userService.emailExists(request.getEmail().trim())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Email already registered"));
		}

		try {
			AppUser user = userService.createUser(
				request.getUsername().trim(),
				request.getEmail().trim(),
				request.getPassword(),
				"USER"
			);
			return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Registration successful", buildAuthResponse(user)));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
		}
	}

	@PostMapping("/login")
	@Operation(summary = "Login", description = "Authenticate with username and password and receive a JWT access token")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
		if (request.getUsername() == null || request.getUsername().isBlank()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Username is required"));
		}
		if (request.getPassword() == null || request.getPassword().isBlank()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Password is required"));
		}

		try {
			authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
			);
		} catch (BadCredentialsException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Invalid username or password"));
		}

		userService.updateLastLogin(request.getUsername().trim());
		return userService.getUserByUsername(request.getUsername().trim())
			.map(user -> ResponseEntity.ok(ApiResponse.success(buildAuthResponse(user))))
			.orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Invalid username or password")));
	}

	private AuthResponse buildAuthResponse(AppUser user) {
		UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
		String token = jwtService.generateToken(userDetails);
		UserProfileDTO profile = dtoMapper.toUserProfileDTO(user);
		return new AuthResponse(token, "Bearer", jwtService.getExpirationMs() / 1000, profile);
	}

	private String validateRegistration(RegisterRequest request) {
		if (request.getUsername() == null || request.getUsername().isBlank()) {
			return "Username is required";
		}
		if (request.getEmail() == null || request.getEmail().isBlank() || !request.getEmail().contains("@")) {
			return "Valid email is required";
		}
		if (request.getPassword() == null || request.getPassword().length() < 4) {
			return "Password must be at least 4 characters long";
		}
		if (request.getConfirmPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
			return "Passwords do not match";
		}
		return null;
	}
}
