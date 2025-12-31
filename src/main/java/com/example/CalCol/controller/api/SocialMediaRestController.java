package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.EnrichmentDTO;
import com.example.CalCol.dto.SocialMediaPostDTO;
import com.example.CalCol.service.CalculatorService;
import com.example.CalCol.service.DtoMapperService;
import com.example.CalCol.service.EnrichmentService;
import com.example.CalCol.service.SocialMediaPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for social media sharing
 */
@RestController
@RequestMapping("/api/calculators/{calculatorId}/social-share")
@RequiredArgsConstructor
@Tag(name = "Social Media", description = "API for generating social media posts")
@SecurityRequirement(name = "basicAuth")
public class SocialMediaRestController {

	private final CalculatorService calculatorService;
	private final EnrichmentService enrichmentService;
	private final SocialMediaPostService socialMediaPostService;
	private final DtoMapperService dtoMapper;

	@PostMapping("/generate")
	@Operation(summary = "Generate social media post", description = "Generate a social media post for a calculator, optionally with enrichment")
	public ResponseEntity<ApiResponse<Map<String, Object>>> generatePost(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Social media platform (twitter, facebook, instagram, linkedin, reddit, mastodon)") 
				@RequestParam String platform,
			@Parameter(description = "Enable enrichment (web search, museum search, AI)") 
				@RequestParam(defaultValue = "false") boolean enableEnrichment,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		java.util.Optional<com.example.CalCol.entity.Calculator> calcOpt = 
			calculatorService.getCalculatorById(calculatorId);
		
		if (calcOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Calculator not found"));
		}

		com.example.CalCol.entity.Calculator calculator = calcOpt.get();
		
		try {
			// Build calculator info
			SocialMediaPostService.CalculatorInfo calcInfo = 
				enrichmentService.buildCalculatorInfo(calculator);
			
			// Enrich if requested
			SocialMediaPostService.EnrichmentData enrichment = null;
			if (enableEnrichment) {
				enrichment = enrichmentService.enrichCalculator(calculator);
			}

			// Generate post
			SocialMediaPostService.SocialMediaPost post = socialMediaPostService.generatePost(
				platform, calcInfo, enrichment);

			SocialMediaPostDTO postDto = dtoMapper.toSocialMediaPostDTO(post);
			EnrichmentDTO enrichmentDto = enrichment != null ? 
				dtoMapper.toEnrichmentDTO(enrichment) : null;

			Map<String, Object> response = new HashMap<>();
			response.put("post", postDto);
			response.put("enrichment", enrichmentDto != null ? enrichmentDto : Map.of());

			return ResponseEntity.ok(ApiResponse.success("Post generated successfully", response));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to generate post: " + e.getMessage()));
		}
	}
}

