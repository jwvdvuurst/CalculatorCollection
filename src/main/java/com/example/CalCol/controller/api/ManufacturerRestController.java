package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.ManufacturerDTO;
import com.example.CalCol.service.CalculatorService;
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


/**
 * REST API controller for manufacturer operations
 */
@RestController
@RequestMapping("/api/manufacturers")
@RequiredArgsConstructor
@Tag(name = "Manufacturers", description = "API for managing manufacturers")
@SecurityRequirement(name = "basicAuth")
public class ManufacturerRestController {

	private final CalculatorService calculatorService;

	@GetMapping
	@Operation(summary = "Get manufacturers", description = "Get a paginated list of manufacturers with optional search and sorting")
	public ResponseEntity<ApiResponse<Page<ManufacturerDTO>>> getManufacturers(
			@Parameter(description = "Search term for manufacturer name") @RequestParam(required = false) String search,
			@Parameter(description = "Sort option: id, name-asc, name-desc, count-asc, count-desc") 
				@RequestParam(required = false) String sort,
			@Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<com.example.CalCol.entity.Manufacturer> manufacturersPage = 
			calculatorService.searchManufacturersWithSort(search, sort, pageable);

		Page<ManufacturerDTO> dtoPage = manufacturersPage.map(m -> {
			ManufacturerDTO dto = new ManufacturerDTO();
			dto.setId(m.getId());
			dto.setName(m.getName());
			return dto;
		});

		return ResponseEntity.ok(ApiResponse.success(dtoPage));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get manufacturer by ID", description = "Get detailed information about a specific manufacturer")
	public ResponseEntity<ApiResponse<ManufacturerDTO>> getManufacturer(
			@Parameter(description = "Manufacturer ID") @PathVariable Long id) {

		return calculatorService.getManufacturerById(id)
			.map(m -> {
				ManufacturerDTO dto = new ManufacturerDTO();
				dto.setId(m.getId());
				dto.setName(m.getName());
				return ResponseEntity.ok(ApiResponse.success(dto));
			})
			.orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Manufacturer not found")));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update manufacturer name", description = "Update the name of a manufacturer")
	public ResponseEntity<ApiResponse<ManufacturerDTO>> updateManufacturer(
			@Parameter(description = "Manufacturer ID") @PathVariable Long id,
			@Parameter(description = "New manufacturer name") @RequestParam String name,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		boolean updated = calculatorService.updateManufacturer(id, name);
		if (!updated) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Manufacturer not found or update failed"));
		}

		return calculatorService.getManufacturerById(id)
			.map(m -> {
				ManufacturerDTO dto = new ManufacturerDTO();
				dto.setId(m.getId());
				dto.setName(m.getName());
				return ResponseEntity.ok(ApiResponse.success("Manufacturer updated successfully", dto));
			})
			.orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to retrieve updated manufacturer")));
	}

	@PostMapping("/merge")
	@Operation(summary = "Merge manufacturers", description = "Merge two manufacturers, moving all calculators from source to target")
	public ResponseEntity<ApiResponse<ManufacturerDTO>> mergeManufacturers(
			@Parameter(description = "Target manufacturer ID") @RequestParam Long targetManufacturerId,
			@Parameter(description = "Source manufacturer ID (will be deleted)") @RequestParam Long sourceManufacturerId,
			@Parameter(description = "Optional new name for the merged manufacturer") 
				@RequestParam(required = false) String newName,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		boolean merged = calculatorService.mergeManufacturers(targetManufacturerId, sourceManufacturerId, newName);
		if (!merged) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Failed to merge manufacturers. Check that both manufacturers exist and are different."));
		}

		return calculatorService.getManufacturerById(targetManufacturerId)
			.map(m -> {
				ManufacturerDTO dto = new ManufacturerDTO();
				dto.setId(m.getId());
				dto.setName(m.getName());
				return ResponseEntity.ok(ApiResponse.success("Manufacturers merged successfully", dto));
			})
			.orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to retrieve merged manufacturer")));
	}
}
