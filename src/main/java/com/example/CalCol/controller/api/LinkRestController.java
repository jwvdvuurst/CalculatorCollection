package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.LinkDTO;
import com.example.CalCol.service.DtoMapperService;
import com.example.CalCol.service.LinkService;
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
 * REST API controller for link operations
 */
@RestController
@RequestMapping("/api/calculators/{calculatorId}/links")
@RequiredArgsConstructor
@Tag(name = "Links", description = "API for managing external links on calculators")
@SecurityRequirement(name = "basicAuth")
public class LinkRestController {

	private final LinkService linkService;
	private final DtoMapperService dtoMapper;

	@GetMapping
	@Operation(summary = "Get calculator links", description = "Get all external links for a calculator")
	public ResponseEntity<ApiResponse<List<LinkDTO>>> getCalculatorLinks(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId) {

		List<com.example.CalCol.entity.CalculatorLink> links = 
			linkService.getCalculatorLinks(calculatorId);
		List<LinkDTO> dtos = links.stream()
			.map(dtoMapper::toLinkDTO)
			.collect(Collectors.toList());

		return ResponseEntity.ok(ApiResponse.success(dtos));
	}

	@PostMapping
	@Operation(summary = "Add link to calculator", description = "Add an external link to a calculator")
	public ResponseEntity<ApiResponse<LinkDTO>> addLink(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Link URL") @RequestParam String url,
			@Parameter(description = "Link title") @RequestParam String title,
			@Parameter(description = "Link description (optional)") @RequestParam(required = false) String description,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		
		try {
			com.example.CalCol.entity.CalculatorLink link = linkService.addLink(
				calculatorId, url, title, description, username);
			LinkDTO dto = dtoMapper.toLinkDTO(link);
			return ResponseEntity.ok(ApiResponse.success("Link added successfully", dto));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to add link: " + e.getMessage()));
		}
	}

	@DeleteMapping("/{linkId}")
	@Operation(summary = "Delete link", description = "Delete a link from a calculator")
	public ResponseEntity<ApiResponse<Void>> deleteLink(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Link ID to delete") @PathVariable Long linkId,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		boolean deleted = linkService.deleteLink(linkId, username);

		if (deleted) {
			return ResponseEntity.ok(ApiResponse.success("Link deleted successfully", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Link not found or you don't have permission to delete it"));
		}
	}
}

