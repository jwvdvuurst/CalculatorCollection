package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.CalculatorDTO;
import com.example.CalCol.dto.CollectionStatisticsDTO;
import com.example.CalCol.entity.Calculator;
import com.example.CalCol.service.CalculatorService;
import com.example.CalCol.service.DtoMapperService;
import com.example.CalCol.service.LabelService;
import com.example.CalCol.service.StatisticsService;
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

/**
 * REST API controller for user collection management
 */
@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
@Tag(name = "Collection", description = "API for managing user calculator collections")
@SecurityRequirement(name = "basicAuth")
public class CollectionRestController {

	private final CalculatorService calculatorService;
	private final LabelService labelService;
	private final DtoMapperService dtoMapper;
	private final StatisticsService statisticsService;

	@GetMapping
	@Operation(summary = "Get user collection", description = "Get all calculators in the authenticated user's collection")
	public ResponseEntity<ApiResponse<Page<CalculatorDTO>>> getCollection(
			@Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		Pageable pageable = PageRequest.of(page, size);
		Page<com.example.CalCol.entity.UserCalculatorCollection> collection = 
			calculatorService.getUserCollection(username, pageable);

		Page<CalculatorDTO> dtoPage = collection.map(item -> {
			Calculator calc = item.getCalculator();
			List<com.example.CalCol.entity.Label> labels = labelService.getCalculatorLabels(calc.getId());
			List<com.example.CalCol.entity.CalculatorImage> images = calculatorService.getApprovedImages(calc.getId());
			List<com.example.CalCol.entity.CalculatorLink> links = calculatorService.getCalculatorLinks(calc.getId());
			return dtoMapper.toCalculatorDTO(calc, labels, images, links);
		});

		return ResponseEntity.ok(ApiResponse.success(dtoPage));
	}

	@PostMapping("/{calculatorId}")
	@Operation(summary = "Add calculator to collection", description = "Add a calculator to the authenticated user's collection")
	public ResponseEntity<ApiResponse<Void>> addToCollection(
			@Parameter(description = "Calculator ID to add") @PathVariable Long calculatorId,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		boolean added = calculatorService.addToCollection(username, calculatorId);

		if (added) {
			return ResponseEntity.ok(ApiResponse.success("Calculator added to collection", null));
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Calculator could not be added. It may already be in your collection."));
		}
	}

	@DeleteMapping("/{calculatorId}")
	@Operation(summary = "Remove calculator from collection", description = "Remove a calculator from the authenticated user's collection")
	public ResponseEntity<ApiResponse<Void>> removeFromCollection(
			@Parameter(description = "Calculator ID to remove") @PathVariable Long calculatorId,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		boolean removed = calculatorService.removeFromCollection(username, calculatorId);

		if (removed) {
			return ResponseEntity.ok(ApiResponse.success("Calculator removed from collection", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Calculator not found in collection"));
		}
	}

	@GetMapping("/statistics")
	@Operation(summary = "Get collection statistics", description = "Get statistics about the user's collection (by manufacturer, period, labels)")
	public ResponseEntity<ApiResponse<CollectionStatisticsDTO>> getStatistics(
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		
		Map<String, Object> statsMap = statisticsService.getCollectionStatistics(username);
		
		CollectionStatisticsDTO stats = new CollectionStatisticsDTO();
		stats.setTotalCalculators(calculatorService.getUserCollectionCount(username));
		stats.setCalculatorsByManufacturer((Map<String, Long>) statsMap.get("byManufacturer"));
		stats.setCalculatorsByPeriod((Map<String, Long>) statsMap.get("byPeriod"));
		stats.setCalculatorsByLabel((Map<String, Long>) statsMap.get("byLabel"));

		return ResponseEntity.ok(ApiResponse.success(stats));
	}

	@GetMapping("/count")
	@Operation(summary = "Get collection count", description = "Get the total number of calculators in the user's collection")
	public ResponseEntity<ApiResponse<Map<String, Long>>> getCollectionCount(
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		Long count = calculatorService.getUserCollectionCount(username);

		return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
	}
}

