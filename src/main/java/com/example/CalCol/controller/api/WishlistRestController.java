package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.CalculatorDTO;
import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.WishlistItem;
import com.example.CalCol.service.CalculatorService;
import com.example.CalCol.service.DtoMapperService;
import com.example.CalCol.service.LabelService;
import com.example.CalCol.service.WishlistService;
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
 * REST API controller for wishlist management
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "API for managing user wishlist")
@SecurityRequirement(name = "basicAuth")
public class WishlistRestController {

	private final WishlistService wishlistService;
	private final CalculatorService calculatorService;
	private final LabelService labelService;
	private final DtoMapperService dtoMapper;

	@GetMapping
	@Operation(summary = "Get user wishlist", description = "Get all calculators in the authenticated user's wishlist")
	public ResponseEntity<ApiResponse<Page<CalculatorDTO>>> getWishlist(
			@Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		Pageable pageable = PageRequest.of(page, size);
		Page<WishlistItem> wishlistPage = wishlistService.getUserWishlist(username, pageable);

		Page<CalculatorDTO> dtoPage = wishlistPage.map(item -> {
			Calculator calc = item.getCalculator();
			List<com.example.CalCol.entity.Label> labels = labelService.getCalculatorLabels(calc.getId());
			List<com.example.CalCol.entity.CalculatorImage> images = calculatorService.getApprovedImages(calc.getId());
			List<com.example.CalCol.entity.CalculatorLink> links = calculatorService.getCalculatorLinks(calc.getId());
			CalculatorDTO dto = dtoMapper.toCalculatorDTO(calc, labels, images, links);
			// Note: We'll need to add notes to DTO separately or create a WishlistItemDTO
			return dto;
		});

		return ResponseEntity.ok(ApiResponse.success(dtoPage));
	}

	@PostMapping("/{calculatorId}")
	@Operation(summary = "Add calculator to wishlist", description = "Add a calculator to the authenticated user's wishlist")
	public ResponseEntity<ApiResponse<Void>> addToWishlist(
			@Parameter(description = "Calculator ID to add") @PathVariable Long calculatorId,
			@Parameter(description = "Optional notes") @RequestParam(required = false) String notes,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		boolean added = wishlistService.addToWishlist(username, calculatorId, notes);

		if (added) {
			return ResponseEntity.ok(ApiResponse.success("Calculator added to wishlist", null));
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Calculator could not be added. It may already be in your wishlist."));
		}
	}

	@DeleteMapping("/{calculatorId}")
	@Operation(summary = "Remove calculator from wishlist", description = "Remove a calculator from the authenticated user's wishlist")
	public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
			@Parameter(description = "Calculator ID to remove") @PathVariable Long calculatorId,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		boolean removed = wishlistService.removeFromWishlist(username, calculatorId);

		if (removed) {
			return ResponseEntity.ok(ApiResponse.success("Calculator removed from wishlist", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Calculator not found in wishlist"));
		}
	}

	@PutMapping("/{calculatorId}/notes")
	@Operation(summary = "Update wishlist notes", description = "Update notes for a calculator in the wishlist")
	public ResponseEntity<ApiResponse<Void>> updateWishlistNotes(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Notes") @RequestParam(required = false) String notes,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		boolean updated = wishlistService.updateWishlistNotes(username, calculatorId, notes);

		if (updated) {
			return ResponseEntity.ok(ApiResponse.success("Notes updated successfully", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Calculator not found in wishlist"));
		}
	}

	@PostMapping("/{calculatorId}/move-to-collection")
	@Operation(summary = "Move calculator from wishlist to collection", description = "Move a calculator from wishlist to collection")
	public ResponseEntity<ApiResponse<Void>> moveToCollection(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		
		// Get notes from wishlist item before removing
		java.util.Optional<com.example.CalCol.entity.WishlistItem> itemOpt = 
			wishlistService.getWishlistItem(username, calculatorId);
		
		String notes = itemOpt.map(com.example.CalCol.entity.WishlistItem::getNotes).orElse(null);
		
		// Remove from wishlist
		boolean removed = wishlistService.removeFromWishlist(username, calculatorId);
		if (!removed) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Calculator not found in wishlist"));
		}
		
		// Add to collection with notes
		boolean added = calculatorService.addToCollection(username, calculatorId, notes);
		if (added) {
			return ResponseEntity.ok(ApiResponse.success("Calculator moved to collection", null));
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Calculator could not be added to collection. It may already be in your collection."));
		}
	}

	@GetMapping("/count")
	@Operation(summary = "Get wishlist count", description = "Get the total number of calculators in the user's wishlist")
	public ResponseEntity<ApiResponse<Map<String, Long>>> getWishlistCount(
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		Long count = wishlistService.getUserWishlistCount(username);

		return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
	}
}

