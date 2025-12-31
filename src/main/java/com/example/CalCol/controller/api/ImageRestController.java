package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.ImageDTO;
import com.example.CalCol.service.CalculatorService;
import com.example.CalCol.service.DtoMapperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for image operations
 */
@RestController
@RequestMapping("/api/calculators/{calculatorId}/images")
@RequiredArgsConstructor
@Tag(name = "Images", description = "API for managing calculator images")
@SecurityRequirement(name = "basicAuth")
public class ImageRestController {

	private final CalculatorService calculatorService;
	private final com.example.CalCol.service.ImageService imageService;
	private final DtoMapperService dtoMapper;

	@GetMapping
	@Operation(summary = "Get calculator images", description = "Get all approved images for a calculator. Users can also see their own pending proposals.")
	public ResponseEntity<ApiResponse<List<ImageDTO>>> getCalculatorImages(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			Authentication authentication) {

		List<com.example.CalCol.entity.CalculatorImage> images;
		
		// If authenticated, include user's own proposals
		if (authentication != null && authentication.isAuthenticated()) {
			String username = authentication.getName();
			images = calculatorService.getImagesForUser(calculatorId, username);
		} else {
			images = calculatorService.getApprovedImages(calculatorId);
		}

		List<ImageDTO> dtos = images.stream()
			.map(dtoMapper::toImageDTO)
			.collect(Collectors.toList());

		return ResponseEntity.ok(ApiResponse.success(dtos));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload calculator image", description = "Upload an image for a calculator. Can be proposed for approval or directly approved if user is admin.")
	public ResponseEntity<ApiResponse<ImageDTO>> uploadImage(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Image file") @RequestParam("file") MultipartFile file,
			@Parameter(description = "Propose for approval (true) or auto-approve if admin (false)") 
				@RequestParam(defaultValue = "true") boolean propose,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();
		boolean isAdmin = authentication.getAuthorities().stream()
			.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

		try {
			if (file.isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ApiResponse.error("File is empty"));
			}

			boolean isProposal = propose && !isAdmin;
			com.example.CalCol.entity.CalculatorImage image = imageService.uploadImage(
				calculatorId, file, username, isProposal);

			if (!isProposal && isAdmin) {
				imageService.approveImage(image.getId(), username);
			}

			ImageDTO dto = dtoMapper.toImageDTO(image);
			return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", dto));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to upload image: " + e.getMessage()));
		}
	}

	@DeleteMapping("/{imageId}")
	@Operation(summary = "Delete image", description = "Delete an image. Users can only delete their own images.")
	public ResponseEntity<ApiResponse<Void>> deleteImage(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Image ID to delete") @PathVariable Long imageId,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		String username = authentication.getName();

		try {
			boolean deleted = imageService.deleteImage(imageId, username);
			if (deleted) {
				return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", null));
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponse.error("Image not found or you don't have permission to delete it"));
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to delete image: " + e.getMessage()));
		}
	}
}

