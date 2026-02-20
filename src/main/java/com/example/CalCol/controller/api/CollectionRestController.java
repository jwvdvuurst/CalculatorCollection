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
import org.springframework.beans.factory.annotation.Autowired;

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
	private final com.example.CalCol.service.ExportService exportService;
	private final com.example.CalCol.service.ImportService importService;
	@Autowired(required = false)
	private com.example.CalCol.service.EmailService emailService;
	private final com.example.CalCol.service.UserService userService;

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
			@Parameter(description = "Optional notes") @RequestParam(required = false) String notes,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		boolean added = calculatorService.addToCollection(username, calculatorId, notes);

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

	@PutMapping("/{calculatorId}/notes")
	@Operation(summary = "Update collection notes", description = "Update notes for a calculator in the collection")
	public ResponseEntity<ApiResponse<Void>> updateCollectionNotes(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Notes") @RequestParam(required = false) String notes,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		boolean updated = calculatorService.updateCollectionNotes(username, calculatorId, notes);

		if (updated) {
			return ResponseEntity.ok(ApiResponse.success("Notes updated successfully", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Calculator not found in collection"));
		}
	}

	@GetMapping("/export")
	@Operation(summary = "Export collection", description = "Export user's collection as JSON or CSV")
	public ResponseEntity<?> exportCollection(
			@Parameter(description = "Export format (json or csv)") @RequestParam(defaultValue = "json") String format,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		try {
			String username = authentication.getName();
			String content;
			String contentType;
			String filename;

			if ("csv".equalsIgnoreCase(format)) {
				content = exportService.exportUserCollectionAsCsv(username);
				contentType = "text/csv";
				filename = "collection_" + username + "_" + java.time.LocalDate.now() + ".csv";
			} else {
				content = exportService.exportUserCollectionAsJson(username);
				contentType = "application/json";
				filename = "collection_" + username + "_" + java.time.LocalDate.now() + ".json";
			}

			return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
				.contentType(org.springframework.http.MediaType.parseMediaType(contentType))
				.body(content);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to export collection: " + e.getMessage()));
		}
	}

	@PostMapping("/import")
	@Operation(summary = "Import collection", description = "Import calculators to user's collection from JSON file")
	public ResponseEntity<ApiResponse<Map<String, Integer>>> importCollection(
			@Parameter(description = "JSON file containing collection data") 
				@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		if (file.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("File is empty"));
		}

		try {
			String username = authentication.getName();
			String jsonData = new String(file.getBytes());
			int imported = importService.importUserCollection(jsonData, username);
			return ResponseEntity.ok(ApiResponse.success(
				"Successfully imported " + imported + " calculator(s)", 
				Map.of("imported", imported)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to import collection: " + e.getMessage()));
		}
	}

	@PostMapping("/send-summary-email")
	@Operation(summary = "Send collection summary email", 
		description = "Send an email with collection statistics to the user's email address")
	public ResponseEntity<ApiResponse<Void>> sendCollectionSummaryEmail(
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		if (emailService == null) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ApiResponse.error("Email service is not configured"));
		}

		String username = authentication.getName();
		try {
			com.example.CalCol.entity.AppUser user = userService.getUserByUsername(username)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
			
			if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ApiResponse.error("User email address is not configured"));
			}

			Map<String, Object> stats = statisticsService.getCollectionStatistics(username);
			long collectionCount = calculatorService.getUserCollectionCount(username);
			Map<String, Long> byManufacturer = (Map<String, Long>) stats.get("byManufacturer");
			List<String> recentAdditions = (List<String>) stats.get("recentAdditions");
			emailService.sendCollectionSummaryEmail(user.getEmail(), username, collectionCount, 
				byManufacturer != null ? byManufacturer : Map.of(), 
				recentAdditions != null ? recentAdditions : List.of());
			
			return ResponseEntity.ok(ApiResponse.success("Collection summary email sent successfully", null));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to send email: " + e.getMessage()));
		}
	}
}

