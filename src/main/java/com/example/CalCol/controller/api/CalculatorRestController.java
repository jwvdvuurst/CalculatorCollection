package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.CalculatorDTO;
import com.example.CalCol.entity.Calculator;
import com.example.CalCol.service.CalculatorService;
import com.example.CalCol.service.DtoMapperService;
import com.example.CalCol.service.LabelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.stream.Collectors;

/**
 * REST API controller for calculator operations
 */
@RestController
@RequestMapping("/api/calculators")
@RequiredArgsConstructor
@Tag(name = "Calculators", description = "API for browsing and viewing calculators")
public class CalculatorRestController {

	private final CalculatorService calculatorService;
	private final LabelService labelService;
	private final DtoMapperService dtoMapper;
	private final com.example.CalCol.service.EnrichmentService enrichmentService;

	@GetMapping
	@Operation(summary = "Browse calculators", description = "Get a paginated list of calculators with optional search, manufacturer, price filters, and sorting")
	public ResponseEntity<ApiResponse<Page<CalculatorDTO>>> browseCalculators(
			@Parameter(description = "Search term for model or manufacturer") @RequestParam(required = false) String search,
			@Parameter(description = "Filter by manufacturer ID") @RequestParam(required = false) Long manufacturerId,
			@Parameter(description = "Minimum price filter (EUR)") @RequestParam(required = false) java.math.BigDecimal minPrice,
			@Parameter(description = "Maximum price filter (EUR)") @RequestParam(required = false) java.math.BigDecimal maxPrice,
			@Parameter(description = "Sort option: price_asc, price_desc, model_asc, model_desc, manufacturer_asc, manufacturer_desc, year_asc, year_desc") 
				@RequestParam(required = false) String sort,
			@Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<Calculator> calculatorsPage;

		if (manufacturerId != null) {
			calculatorsPage = calculatorService.getCalculatorsByManufacturer(manufacturerId, minPrice, maxPrice, sort, pageable);
		} else {
			calculatorsPage = calculatorService.searchCalculators(search, minPrice, maxPrice, sort, pageable);
		}

		Page<CalculatorDTO> dtoPage = calculatorsPage.map(calc -> {
			List<com.example.CalCol.entity.Label> labels = labelService.getCalculatorLabels(calc.getId());
			List<com.example.CalCol.entity.CalculatorImage> images = calculatorService.getApprovedImages(calc.getId());
			List<com.example.CalCol.entity.CalculatorLink> links = calculatorService.getCalculatorLinks(calc.getId());
			return dtoMapper.toCalculatorDTO(calc, labels, images, links);
		});

		return ResponseEntity.ok(ApiResponse.success(dtoPage));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get calculator details", description = "Get detailed information about a specific calculator including labels, images, and links")
	public ResponseEntity<ApiResponse<CalculatorDTO>> getCalculator(
			@Parameter(description = "Calculator ID") @PathVariable Long id) {

		return calculatorService.getCalculatorById(id)
			.map(calc -> {
				List<com.example.CalCol.entity.Label> labels = labelService.getCalculatorLabels(id);
				List<com.example.CalCol.entity.CalculatorImage> images = calculatorService.getApprovedImages(id);
				List<com.example.CalCol.entity.CalculatorLink> links = calculatorService.getCalculatorLinks(id);
				CalculatorDTO dto = dtoMapper.toCalculatorDTO(calc, labels, images, links);
				return ResponseEntity.ok(ApiResponse.success(dto));
			})
			.orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Calculator not found")));
	}

	@GetMapping("/manufacturers")
	@Operation(summary = "Get all manufacturers", description = "Get a list of all calculator manufacturers")
	public ResponseEntity<ApiResponse<List<com.example.CalCol.dto.ManufacturerDTO>>> getManufacturers() {
		List<com.example.CalCol.entity.Manufacturer> manufacturers = 
			calculatorService.getAllManufacturers(Pageable.unpaged()).getContent();
		
		List<com.example.CalCol.dto.ManufacturerDTO> dtos = manufacturers.stream()
			.map(m -> {
				com.example.CalCol.dto.ManufacturerDTO dto = new com.example.CalCol.dto.ManufacturerDTO();
				dto.setId(m.getId());
				dto.setName(m.getName());
				return dto;
			})
			.collect(Collectors.toList());

		return ResponseEntity.ok(ApiResponse.success(dtos));
	}

	@PostMapping("/{id}/enrich")
	@Operation(summary = "Enrich calculator", 
		description = "Enrich a calculator with web search data, museum search, and AI enhancement. Admin only.")
	public ResponseEntity<ApiResponse<com.example.CalCol.dto.EnrichmentDTO>> enrichCalculator(
			@Parameter(description = "Calculator ID") @PathVariable Long id,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		boolean isAdmin = authentication.getAuthorities().stream()
			.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

		if (!isAdmin) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiResponse.error("Admin access required"));
		}

		try {
			java.util.Optional<Calculator> calcOpt = calculatorService.getCalculatorById(id);
			if (calcOpt.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponse.error("Calculator not found"));
			}

			Calculator calculator = calcOpt.get();
			com.example.CalCol.service.SocialMediaPostService.EnrichmentData enrichment = 
				enrichmentService.enrichCalculator(calculator);

			com.example.CalCol.dto.EnrichmentDTO dto = dtoMapper.toEnrichmentDTO(enrichment);

			return ResponseEntity.ok(ApiResponse.success("Calculator enriched successfully", dto));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to enrich calculator: " + e.getMessage()));
		}
	}
}

