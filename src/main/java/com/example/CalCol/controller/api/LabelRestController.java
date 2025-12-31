package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.LabelDTO;
import com.example.CalCol.service.DtoMapperService;
import com.example.CalCol.service.LabelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for label operations
 */
@RestController
@RequestMapping("/api/calculators/{calculatorId}/labels")
@RequiredArgsConstructor
@Tag(name = "Labels", description = "API for managing labels on calculators")
@SecurityRequirement(name = "basicAuth")
public class LabelRestController {

	private final LabelService labelService;
	private final DtoMapperService dtoMapper;

	@GetMapping
	@Operation(summary = "Get calculator labels", description = "Get all labels assigned to a calculator")
	public ResponseEntity<ApiResponse<List<LabelDTO>>> getCalculatorLabels(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId) {

		List<com.example.CalCol.entity.Label> labels = labelService.getCalculatorLabels(calculatorId);
		List<LabelDTO> dtos = labels.stream()
			.map(dtoMapper::toLabelDTO)
			.collect(Collectors.toList());

		return ResponseEntity.ok(ApiResponse.success(dtos));
	}

	@GetMapping("/curated")
	@Operation(summary = "Get curated labels", description = "Get all curated labels available in the system")
	public ResponseEntity<ApiResponse<List<LabelDTO>>> getCuratedLabels() {
		List<com.example.CalCol.entity.Label> labels = labelService.getAllCuratedLabels();
		List<LabelDTO> dtos = labels.stream()
			.map(dtoMapper::toLabelDTO)
			.collect(Collectors.toList());

		return ResponseEntity.ok(ApiResponse.success(dtos));
	}

	@PostMapping
	@Operation(summary = "Add label to calculator", description = "Add a label to a calculator. Can use existing label ID or create a new free-form label")
	public ResponseEntity<ApiResponse<Void>> addLabel(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Existing label ID (optional)") @RequestParam(required = false) Long labelId,
			@Parameter(description = "New label name (if creating new free-form label)") @RequestParam(required = false) String newLabelName,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		try {
			if (labelId != null) {
				boolean added = labelService.addLabelToCalculator(calculatorId, labelId);
				if (added) {
					return ResponseEntity.ok(ApiResponse.success("Label added successfully", null));
				} else {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(ApiResponse.error("Label could not be added"));
				}
			} else if (newLabelName != null && !newLabelName.trim().isEmpty()) {
				com.example.CalCol.entity.Label label = labelService.createOrGetLabel(newLabelName.trim(), false);
				boolean added = labelService.addLabelToCalculator(calculatorId, label.getId());
				if (added) {
					return ResponseEntity.ok(ApiResponse.success("Label added successfully", null));
				} else {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(ApiResponse.error("Label could not be added"));
				}
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ApiResponse.error("Either labelId or newLabelName must be provided"));
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to add label: " + e.getMessage()));
		}
	}

	@DeleteMapping("/{labelId}")
	@Operation(summary = "Remove label from calculator", description = "Remove a label from a calculator")
	public ResponseEntity<ApiResponse<Void>> removeLabel(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Label ID to remove") @PathVariable Long labelId,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		boolean removed = labelService.removeLabelFromCalculator(calculatorId, labelId);
		if (removed) {
			return ResponseEntity.ok(ApiResponse.success("Label removed successfully", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Label not found on calculator"));
		}
	}
}

