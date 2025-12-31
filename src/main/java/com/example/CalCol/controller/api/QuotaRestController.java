package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.service.QuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST API controller for quota management
 */
@RestController
@RequestMapping("/api/quota")
@RequiredArgsConstructor
@Tag(name = "Quota", description = "API for checking quota status")
@SecurityRequirement(name = "basicAuth")
public class QuotaRestController {

	private final QuotaService quotaService;

	@GetMapping("/status")
	@Operation(summary = "Get quota status", description = "Get quota and rate limit status for all services")
	public ResponseEntity<ApiResponse<Map<String, QuotaService.QuotaStatus>>> getQuotaStatus(
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		Map<String, QuotaService.QuotaStatus> status = quotaService.getAllQuotaStatus();
		return ResponseEntity.ok(ApiResponse.success(status));
	}
}

