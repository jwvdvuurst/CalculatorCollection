package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.entity.CalculatorProposal;
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
 * REST API controller for calculator proposals
 */
@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
@Tag(name = "Proposals", description = "API for proposing new calculators")
@SecurityRequirement(name = "basicAuth")
public class ProposalRestController {

	private final com.example.CalCol.service.CalculatorProposalService proposalService;

	@PostMapping
	@Operation(summary = "Propose new calculator", description = "Propose a new calculator to be added to the database")
	public ResponseEntity<ApiResponse<Map<String, Long>>> proposeCalculator(
			@Parameter(description = "Calculator model") @RequestParam String model,
			@Parameter(description = "Manufacturer name") @RequestParam String manufacturer,
			@Parameter(description = "Year sold from (optional)") @RequestParam(required = false) Integer soldFrom,
			@Parameter(description = "Year sold to (optional)") @RequestParam(required = false) Integer soldTo,
			@Parameter(description = "Source URL (optional)") @RequestParam(required = false) String sourceUrl,
			@Parameter(description = "Raw row text (optional)") @RequestParam(required = false) String rawRowText,
			@Parameter(description = "Additional notes (optional)") @RequestParam(required = false) String notes,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		
		try {
			CalculatorProposal proposal = proposalService.createProposal(
				model, manufacturer, soldFrom, soldTo, sourceUrl, rawRowText, notes, username);
			
			Map<String, Long> response = new HashMap<>();
			response.put("proposalId", proposal.getId());
			
			return ResponseEntity.ok(ApiResponse.success(
				"Calculator proposal submitted successfully",
				response));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to submit proposal: " + e.getMessage()));
		}
	}
}

