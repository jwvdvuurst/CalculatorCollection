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
	private final com.example.CalCol.service.FileStorageService fileStorageService;

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
	@Operation(summary = "Upload calculator image (multipart/form-data)", 
		description = "Upload an image for a calculator using multipart/form-data. Can be proposed for approval or directly approved if user is admin.")
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

	@PostMapping(value = "/binary", consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, 
		MediaType.IMAGE_GIF_VALUE, "image/webp", MediaType.APPLICATION_OCTET_STREAM_VALUE})
	@Operation(summary = "Upload calculator image (binary data)", 
		description = "Upload an image for a calculator using raw binary data in the request body. " +
			"Content-Type should be image/jpeg, image/png, image/gif, image/webp, or application/octet-stream. " +
			"Can be proposed for approval or directly approved if user is admin.")
	public ResponseEntity<ApiResponse<ImageDTO>> uploadImageBinary(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Image binary data") @RequestBody byte[] imageData,
			@Parameter(description = "Content-Type header (image/jpeg, image/png, image/gif, image/webp)") 
				@RequestHeader(value = "Content-Type", required = false) String contentType,
			@Parameter(description = "Propose for approval (true) or auto-approve if admin (false)") 
				@RequestParam(defaultValue = "true") boolean propose,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Authentication required"));
		}

		if (imageData == null || imageData.length == 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Image data is empty"));
		}

		String username = authentication.getName();
		boolean isAdmin = authentication.getAuthorities().stream()
			.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

		try {
			// Determine file extension and MIME type from Content-Type
			String extension;
			final String mimeType;
			if (contentType != null) {
				if (contentType.contains("image/png")) {
					extension = ".png";
					mimeType = MediaType.IMAGE_PNG_VALUE;
				} else if (contentType.contains("image/gif")) {
					extension = ".gif";
					mimeType = MediaType.IMAGE_GIF_VALUE;
				} else if (contentType.contains("image/webp")) {
					extension = ".webp";
					mimeType = "image/webp";
				} else if (contentType.contains("image/jpeg") || contentType.contains("image/jpg")) {
					extension = ".jpg";
					mimeType = MediaType.IMAGE_JPEG_VALUE;
				} else {
					extension = ".jpg";
					mimeType = MediaType.IMAGE_JPEG_VALUE;
				}
			} else {
				extension = ".jpg";
				mimeType = MediaType.IMAGE_JPEG_VALUE;
			}

			// Create a MultipartFile from the binary data
			String filename = java.util.UUID.randomUUID().toString() + extension;
			MultipartFile multipartFile = new MultipartFile() {
				@Override
				public String getName() { return "file"; }
				@Override
				public String getOriginalFilename() { return filename; }
				@Override
				public String getContentType() { return mimeType; }
				@Override
				public boolean isEmpty() { return imageData.length == 0; }
				@Override
				public long getSize() { return imageData.length; }
				@Override
				public byte[] getBytes() { return imageData; }
				@Override
				public java.io.InputStream getInputStream() { 
					return new java.io.ByteArrayInputStream(imageData); 
				}
				@Override
				public void transferTo(java.io.File dest) throws java.io.IOException, IllegalStateException {
					java.nio.file.Files.write(dest.toPath(), imageData);
				}
			};

			boolean isProposal = propose && !isAdmin;
			com.example.CalCol.entity.CalculatorImage image = imageService.uploadImage(
				calculatorId, multipartFile, username, isProposal);

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

	@GetMapping("/{imageId}/data")
	@Operation(summary = "Get image binary data", description = "Get the binary data for an image file. Returns the image file with appropriate content type.")
	public ResponseEntity<?> getImageData(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Image ID") @PathVariable Long imageId,
			Authentication authentication) {

		try {
			// Get image entity
			java.util.Optional<com.example.CalCol.entity.CalculatorImage> imageOpt = 
				imageService.getImageById(imageId);
			
			if (imageOpt.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponse.error("Image not found"));
			}

			com.example.CalCol.entity.CalculatorImage image = imageOpt.get();
			
			// Verify image belongs to the calculator
			if (!image.getCalculator().getId().equals(calculatorId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ApiResponse.error("Image does not belong to this calculator"));
			}

			// Check visibility: approved images are public, unapproved images only visible to uploader
			if (!image.getIsApproved()) {
				if (authentication == null || !authentication.isAuthenticated()) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(ApiResponse.error("Image is pending approval and not accessible"));
				}
				if (!image.getUploadedBy().equals(authentication.getName())) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(ApiResponse.error("Image is pending approval and not accessible"));
				}
			}

			// Load file from storage
			java.nio.file.Path imagePath = fileStorageService.loadFile(image.getImagePath());
			
			if (!java.nio.file.Files.exists(imagePath)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponse.error("Image file not found on server"));
			}

			// Determine content type from file extension
			String contentType = "image/jpeg"; // default
			String filename = image.getImagePath().toLowerCase();
			if (filename.endsWith(".png")) {
				contentType = "image/png";
			} else if (filename.endsWith(".gif")) {
				contentType = "image/gif";
			} else if (filename.endsWith(".webp")) {
				contentType = "image/webp";
			} else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
				contentType = "image/jpeg";
			}

			// Read file bytes
			byte[] imageBytes = java.nio.file.Files.readAllBytes(imagePath);

			// Return binary data with appropriate headers
			return ResponseEntity.ok()
				.contentType(org.springframework.http.MediaType.parseMediaType(contentType))
				.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
					"inline; filename=\"" + image.getImagePath() + "\"")
				.body(imageBytes);

		} catch (java.nio.file.NoSuchFileException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Image file not found"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to retrieve image: " + e.getMessage()));
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

	@PostMapping("/from-url")
	@Operation(summary = "Upload image from URL", 
		description = "Download an image from a URL and add it to a calculator. Admin only.")
	public ResponseEntity<ApiResponse<ImageDTO>> uploadImageFromUrl(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Image URL") @RequestParam String imageUrl,
			@Parameter(description = "Propose for approval (true) or auto-approve if admin (false)") 
				@RequestParam(defaultValue = "false") boolean proposeForRepository,
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

		String username = authentication.getName();

		try {
			com.example.CalCol.entity.CalculatorImage image = 
				imageService.addImageFromUrl(calculatorId, imageUrl, username, proposeForRepository);
			ImageDTO dto = dtoMapper.toImageDTO(image);
			return ResponseEntity.ok(ApiResponse.success("Image downloaded and added successfully", dto));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to add image from URL: " + e.getMessage()));
		}
	}
}

