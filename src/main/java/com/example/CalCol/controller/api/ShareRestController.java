package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.CalculatorDTO;
import com.example.CalCol.entity.Calculator;
import com.example.CalCol.service.CalculatorService;
import com.example.CalCol.service.DtoMapperService;
import com.example.CalCol.service.LabelService;
import com.example.CalCol.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for sharing operations
 */
@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
@Tag(name = "Sharing", description = "API for sharing calculator collections")
@SecurityRequirement(name = "basicAuth")
public class ShareRestController {

	private final ShareService shareService;
	private final CalculatorService calculatorService;
	private final LabelService labelService;
	private final DtoMapperService dtoMapper;

	@PostMapping
	@Operation(summary = "Create shared collection", description = "Create a shareable link for selected calculators or entire collection")
	public ResponseEntity<ApiResponse<Map<String, String>>> createShare(
			@Parameter(description = "Title for the shared collection") @RequestParam String title,
			@Parameter(description = "Description (optional)") @RequestParam(required = false) String description,
			@Parameter(description = "Calculator IDs to share (comma-separated). If empty, shares entire collection.") 
				@RequestParam(required = false) List<Long> calculatorIds,
			@Parameter(description = "Days until expiration (default: 30)") @RequestParam(defaultValue = "30") int daysValid,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		
		try {
			// If no calculatorIds provided, get all from user's collection
			if (calculatorIds == null || calculatorIds.isEmpty()) {
				calculatorIds = calculatorService.getUserCollection(username, 
					org.springframework.data.domain.Pageable.unpaged())
					.getContent().stream()
					.map(item -> item.getCalculator().getId())
					.collect(java.util.stream.Collectors.toList());
			}
			
			com.example.CalCol.entity.SharedCollection share = shareService.createShare(
				username, calculatorIds, title, description, daysValid, true);
			
			return ResponseEntity.ok(ApiResponse.success(
				"Share created successfully",
				Map.of("token", share.getShareToken(), "shareUrl", "/api/share/" + share.getShareToken())));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to create share: " + e.getMessage()));
		}
	}

	@GetMapping("/{token}")
	@Operation(summary = "Get shared collection", description = "Get calculators from a shared collection (public endpoint)")
	public ResponseEntity<ApiResponse<Page<CalculatorDTO>>> getSharedCollection(
			@Parameter(description = "Share token") @PathVariable String token,
			@Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size);
		
		return shareService.getSharedCollection(token)
			.map(share -> {
				List<Calculator> calculators = shareService.getSharedCalculators(token);
				
				// Convert to page
				int start = (int) pageable.getOffset();
				int end = Math.min((start + pageable.getPageSize()), calculators.size());
				List<Calculator> pagedCalculators = calculators.subList(start, end);
				
				List<CalculatorDTO> dtos = pagedCalculators.stream()
					.map(calc -> {
						List<com.example.CalCol.entity.Label> labels = labelService.getCalculatorLabels(calc.getId());
						List<com.example.CalCol.entity.CalculatorImage> images = calculatorService.getApprovedImages(calc.getId());
						List<com.example.CalCol.entity.CalculatorLink> links = calculatorService.getCalculatorLinks(calc.getId());
						return dtoMapper.toCalculatorDTO(calc, labels, images, links);
					})
					.collect(Collectors.toList());
				
				Page<CalculatorDTO> dtoPage = new org.springframework.data.domain.PageImpl<>(
					dtos, pageable, calculators.size());
				
				return ResponseEntity.ok(ApiResponse.success(dtoPage));
			})
			.orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Shared collection not found or expired")));
	}
}

