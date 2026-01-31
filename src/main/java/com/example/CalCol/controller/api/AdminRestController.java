package com.example.CalCol.controller.api;

import com.example.CalCol.dto.ApiResponse;
import com.example.CalCol.dto.CalculatorDTO;
import com.example.CalCol.dto.LabelDTO;
import com.example.CalCol.dto.UserProfileDTO;
import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.Label;
import com.example.CalCol.service.AdminService;
import com.example.CalCol.service.CalculatorProposalService;
import com.example.CalCol.service.CalculatorService;
import com.example.CalCol.service.DtoMapperService;
import com.example.CalCol.service.ExportService;
import com.example.CalCol.service.ImageService;
import com.example.CalCol.service.ImportService;
import com.example.CalCol.service.LabelDerivationService;
import com.example.CalCol.service.LabelService;
import com.example.CalCol.service.LinkService;
import com.example.CalCol.service.UserService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for admin operations
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "API for administrative operations (Admin only)")
@SecurityRequirement(name = "basicAuth")
public class AdminRestController {

	private final AdminService adminService;
	private final CalculatorService calculatorService;
	private final ImageService imageService;
	private final LabelService labelService;
	private final LabelDerivationService labelDerivationService;
	private final ExportService exportService;
	private final ImportService importService;
	private final CalculatorProposalService proposalService;
	private final UserService userService;
	private final LinkService linkService;
	private final DtoMapperService dtoMapper;

	// ========== Calculator Management ==========

	@GetMapping("/calculators")
	@Operation(summary = "Get all calculators (admin)", description = "Get a paginated list of all calculators")
	public ResponseEntity<ApiResponse<Page<CalculatorDTO>>> getAllCalculators(
			@Parameter(description = "Search term") @RequestParam(required = false) String search,
			@Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<Calculator> calculatorsPage = adminService.getAllCalculators(pageable);

		Page<CalculatorDTO> dtoPage = calculatorsPage.map(calc -> {
			List<com.example.CalCol.entity.Label> labels = labelService.getCalculatorLabels(calc.getId());
			List<com.example.CalCol.entity.CalculatorImage> images = calculatorService.getApprovedImages(calc.getId());
			List<com.example.CalCol.entity.CalculatorLink> links = calculatorService.getCalculatorLinks(calc.getId());
			return dtoMapper.toCalculatorDTO(calc, labels, images, links);
		});

		return ResponseEntity.ok(ApiResponse.success(dtoPage));
	}

	@PostMapping("/calculators")
	@Operation(summary = "Create calculator", description = "Create a new calculator")
	public ResponseEntity<ApiResponse<CalculatorDTO>> createCalculator(
			@RequestBody Calculator calculator,
			@Parameter(description = "Manufacturer ID") @RequestParam(required = false) Long manufacturerId,
			Authentication authentication) {

		if (manufacturerId != null) {
			adminService.getManufacturerById(manufacturerId).ifPresent(calculator::setManufacturer);
		}

		Calculator created = adminService.createCalculator(calculator);
		List<com.example.CalCol.entity.Label> labels = labelService.getCalculatorLabels(created.getId());
		List<com.example.CalCol.entity.CalculatorImage> images = calculatorService.getApprovedImages(created.getId());
		List<com.example.CalCol.entity.CalculatorLink> links = calculatorService.getCalculatorLinks(created.getId());
		CalculatorDTO dto = dtoMapper.toCalculatorDTO(created, labels, images, links);

		return ResponseEntity.ok(ApiResponse.success("Calculator created successfully", dto));
	}

	@PutMapping("/calculators/{id}")
	@Operation(summary = "Update calculator", description = "Update an existing calculator")
	public ResponseEntity<ApiResponse<CalculatorDTO>> updateCalculator(
			@Parameter(description = "Calculator ID") @PathVariable Long id,
			@RequestBody Calculator calculator,
			@Parameter(description = "Manufacturer ID") @RequestParam(required = false) Long manufacturerId) {

		if (manufacturerId != null) {
			adminService.getManufacturerById(manufacturerId).ifPresent(calculator::setManufacturer);
		}

		Calculator updated = adminService.updateCalculator(id, calculator);
		List<com.example.CalCol.entity.Label> labels = labelService.getCalculatorLabels(updated.getId());
		List<com.example.CalCol.entity.CalculatorImage> images = calculatorService.getApprovedImages(updated.getId());
		List<com.example.CalCol.entity.CalculatorLink> links = calculatorService.getCalculatorLinks(updated.getId());
		CalculatorDTO dto = dtoMapper.toCalculatorDTO(updated, labels, images, links);

		return ResponseEntity.ok(ApiResponse.success("Calculator updated successfully", dto));
	}

	@DeleteMapping("/calculators/{id}")
	@Operation(summary = "Delete calculator", description = "Delete a calculator")
	public ResponseEntity<ApiResponse<Void>> deleteCalculator(
			@Parameter(description = "Calculator ID") @PathVariable Long id) {

		boolean deleted = adminService.deleteCalculator(id);
		if (deleted) {
			return ResponseEntity.ok(ApiResponse.success("Calculator deleted successfully", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Calculator not found"));
		}
	}

	// ========== Label Management ==========

	@GetMapping("/labels")
	@Operation(summary = "Get all labels", description = "Get a list of all labels")
	public ResponseEntity<ApiResponse<List<LabelDTO>>> getAllLabels() {
		List<Label> labels = labelService.getAllLabels();
		List<LabelDTO> dtos = labels.stream()
			.map(l -> {
				LabelDTO dto = new LabelDTO();
				dto.setId(l.getId());
				dto.setName(l.getName());
				dto.setDescription(l.getDescription());
				dto.setIsCurated(l.getIsCurated());
				return dto;
			})
			.collect(Collectors.toList());

		return ResponseEntity.ok(ApiResponse.success(dtos));
	}

	@PostMapping("/labels")
	@Operation(summary = "Create label", description = "Create a new curated label")
	public ResponseEntity<ApiResponse<LabelDTO>> createLabel(
			@Parameter(description = "Label name") @RequestParam String name,
			@Parameter(description = "Label description") @RequestParam(required = false) String description,
			@Parameter(description = "Is curated") @RequestParam(defaultValue = "true") boolean isCurated) {

		Label label = labelService.createCuratedLabel(name, description);
		LabelDTO dto = new LabelDTO();
		dto.setId(label.getId());
		dto.setName(label.getName());
		dto.setDescription(label.getDescription());
		dto.setIsCurated(label.getIsCurated());

		return ResponseEntity.ok(ApiResponse.success("Label created successfully", dto));
	}

	@PutMapping("/labels/{id}")
	@Operation(summary = "Update label", description = "Update an existing label")
	public ResponseEntity<ApiResponse<LabelDTO>> updateLabel(
			@Parameter(description = "Label ID") @PathVariable Long id,
			@Parameter(description = "Label name") @RequestParam String name,
			@Parameter(description = "Label description") @RequestParam(required = false) String description,
			@Parameter(description = "Is curated") @RequestParam(defaultValue = "false") boolean isCurated) {

		try {
			labelService.updateLabel(id, name, description, isCurated);
			Label label = labelService.getLabelById(id)
				.orElseThrow(() -> new RuntimeException("Label not found after update"));
			
			LabelDTO dto = new LabelDTO();
			dto.setId(label.getId());
			dto.setName(label.getName());
			dto.setDescription(label.getDescription());
			dto.setIsCurated(label.getIsCurated());

			return ResponseEntity.ok(ApiResponse.success("Label updated successfully", dto));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Failed to update label: " + e.getMessage()));
		}
	}

	@DeleteMapping("/labels/{id}")
	@Operation(summary = "Delete label", description = "Delete a label")
	public ResponseEntity<ApiResponse<Void>> deleteLabel(
			@Parameter(description = "Label ID") @PathVariable Long id) {

		boolean deleted = labelService.deleteLabel(id);
		if (deleted) {
			return ResponseEntity.ok(ApiResponse.success("Label deleted successfully", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Label not found or cannot be deleted"));
		}
	}

	@PostMapping("/labels/derive")
	@Operation(summary = "Derive labels", description = "Automatically derive and assign labels to calculators")
	public ResponseEntity<ApiResponse<Map<String, Integer>>> deriveLabels() {
		try {
			int assigned = labelDerivationService.deriveLabelsFromCalculators();
			return ResponseEntity.ok(ApiResponse.success(
				"Label derivation completed! Assigned " + assigned + " labels to calculators.",
				Map.of("assigned", assigned)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to derive labels: " + e.getMessage()));
		}
	}

	// ========== User Management ==========

	@GetMapping("/users")
	@Operation(summary = "Get all users", description = "Get a paginated list of all users")
	public ResponseEntity<ApiResponse<Page<UserProfileDTO>>> getAllUsers(
			@Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<com.example.CalCol.entity.AppUser> usersPage = userService.getAllUsers(pageable);

		Page<UserProfileDTO> dtoPage = usersPage.map(user -> {
			UserProfileDTO dto = new UserProfileDTO();
			dto.setId(user.getId());
			dto.setUsername(user.getUsername());
			dto.setEmail(user.getEmail());
			dto.setRole(user.getRole());
			dto.setEnabled(user.getEnabled());
			dto.setCreatedAt(user.getCreatedAt());
			dto.setLastLogin(user.getLastLogin());
			return dto;
		});

		return ResponseEntity.ok(ApiResponse.success(dtoPage));
	}

	@PostMapping("/users")
	@Operation(summary = "Create user", description = "Create a new user")
	public ResponseEntity<ApiResponse<UserProfileDTO>> createUser(
			@Parameter(description = "Username") @RequestParam String username,
			@Parameter(description = "Email") @RequestParam String email,
			@Parameter(description = "Password") @RequestParam String password,
			@Parameter(description = "Role (USER or ADMIN)") @RequestParam String role,
			@Parameter(description = "Enabled") @RequestParam(defaultValue = "true") boolean enabled) {

		try {
			com.example.CalCol.entity.AppUser user = userService.createUser(username, email, password, role);
			UserProfileDTO dto = new UserProfileDTO();
			dto.setId(user.getId());
			dto.setUsername(user.getUsername());
			dto.setEmail(user.getEmail());
			dto.setRole(user.getRole());
			dto.setEnabled(user.getEnabled());
			dto.setCreatedAt(user.getCreatedAt());
			dto.setLastLogin(user.getLastLogin());

			return ResponseEntity.ok(ApiResponse.success("User created successfully", dto));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Failed to create user: " + e.getMessage()));
		}
	}

	@PutMapping("/users/{id}")
	@Operation(summary = "Update user", description = "Update an existing user")
	public ResponseEntity<ApiResponse<UserProfileDTO>> updateUser(
			@Parameter(description = "User ID") @PathVariable Long id,
			@Parameter(description = "Email") @RequestParam String email,
			@Parameter(description = "Role (USER or ADMIN)") @RequestParam String role,
			@Parameter(description = "Enabled") @RequestParam(defaultValue = "true") boolean enabled) {

		try {
			com.example.CalCol.entity.AppUser user = userService.updateUser(id, email, role, enabled);
			UserProfileDTO dto = new UserProfileDTO();
			dto.setId(user.getId());
			dto.setUsername(user.getUsername());
			dto.setEmail(user.getEmail());
			dto.setRole(user.getRole());
			dto.setEnabled(user.getEnabled());
			dto.setCreatedAt(user.getCreatedAt());
			dto.setLastLogin(user.getLastLogin());

			return ResponseEntity.ok(ApiResponse.success("User updated successfully", dto));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Failed to update user: " + e.getMessage()));
		}
	}

	@DeleteMapping("/users/{id}")
	@Operation(summary = "Delete user", description = "Delete a user")
	public ResponseEntity<ApiResponse<Void>> deleteUser(
			@Parameter(description = "User ID") @PathVariable Long id) {

		boolean deleted = userService.deleteUser(id);
		if (deleted) {
			return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("User not found or cannot be deleted"));
		}
	}

	// ========== Proposal Management ==========

	@GetMapping("/proposals/calculators")
	@Operation(summary = "Get calculator proposals", description = "Get pending calculator proposals")
	public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getCalculatorProposals(
			@Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<com.example.CalCol.entity.CalculatorProposal> proposalsPage = proposalService.getPendingProposals(pageable);

		Page<Map<String, Object>> dtoPage = proposalsPage.map(proposal -> Map.of(
			"id", proposal.getId(),
			"manufacturerName", proposal.getManufacturerName() != null ? proposal.getManufacturerName() : "",
			"model", proposal.getModel() != null ? proposal.getModel() : "",
			"soldFrom", proposal.getSoldFrom() != null ? proposal.getSoldFrom() : 0,
			"soldTo", proposal.getSoldTo() != null ? proposal.getSoldTo() : 0,
			"sourceUrl", proposal.getSourceUrl() != null ? proposal.getSourceUrl() : "",
			"rawRowText", proposal.getRawRowText() != null ? proposal.getRawRowText() : "",
			"proposedBy", proposal.getProposedBy() != null ? proposal.getProposedBy() : "",
			"proposedAt", proposal.getProposedAt() != null ? proposal.getProposedAt().toString() : ""
		));

		return ResponseEntity.ok(ApiResponse.success(dtoPage));
	}

	@PostMapping("/proposals/calculators/{id}/approve")
	@Operation(summary = "Approve calculator proposal", description = "Approve a calculator proposal and add it to the database")
	public ResponseEntity<ApiResponse<CalculatorDTO>> approveCalculatorProposal(
			@Parameter(description = "Proposal ID") @PathVariable Long id,
			Authentication authentication) {

		boolean approved = proposalService.approveProposal(id, authentication.getName());
		if (!approved) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Failed to approve proposal"));
		}

		// Get the created calculator (this would need to be returned from approveProposal)
		return ResponseEntity.ok(ApiResponse.success("Calculator proposal approved and added to database", null));
	}

	@PostMapping("/proposals/calculators/{id}/reject")
	@Operation(summary = "Reject calculator proposal", description = "Reject a calculator proposal")
	public ResponseEntity<ApiResponse<Void>> rejectCalculatorProposal(
			@Parameter(description = "Proposal ID") @PathVariable Long id) {

		boolean rejected = proposalService.rejectProposal(id);
		if (rejected) {
			return ResponseEntity.ok(ApiResponse.success("Calculator proposal rejected", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Proposal not found"));
		}
	}

	@GetMapping("/proposals/images")
	@Operation(summary = "Get image proposals", description = "Get pending image proposals")
	public ResponseEntity<ApiResponse<Page<com.example.CalCol.dto.ImageDTO>>> getImageProposals(
			@Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<com.example.CalCol.entity.CalculatorImage> imagesPage = imageService.getPendingProposals(pageable);

		Page<com.example.CalCol.dto.ImageDTO> dtoPage = imagesPage.map(dtoMapper::toImageDTO);

		return ResponseEntity.ok(ApiResponse.success(dtoPage));
	}

	@PostMapping("/proposals/images/{imageId}/approve")
	@Operation(summary = "Approve image proposal", description = "Approve an image proposal")
	public ResponseEntity<ApiResponse<com.example.CalCol.dto.ImageDTO>> approveImageProposal(
			@Parameter(description = "Image ID") @PathVariable Long imageId,
			Authentication authentication) {

		boolean approved = imageService.approveImage(imageId, authentication.getName());
		if (!approved) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Failed to approve image proposal"));
		}

		com.example.CalCol.entity.CalculatorImage image = imageService.getImageById(imageId)
			.orElseThrow(() -> new RuntimeException("Image not found after approval"));
		com.example.CalCol.dto.ImageDTO dto = dtoMapper.toImageDTO(image);

		return ResponseEntity.ok(ApiResponse.success("Image proposal approved", dto));
	}

	@PostMapping("/proposals/images/{imageId}/reject")
	@Operation(summary = "Reject image proposal", description = "Reject an image proposal")
	public ResponseEntity<ApiResponse<Void>> rejectImageProposal(
			@Parameter(description = "Image ID") @PathVariable Long imageId) {

		boolean rejected = imageService.rejectImage(imageId);
		if (rejected) {
			return ResponseEntity.ok(ApiResponse.success("Image proposal rejected", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Image proposal not found"));
		}
	}

	// ========== Data Export/Import ==========

	@GetMapping("/export")
	@Operation(summary = "Export all data", description = "Export all data (manufacturers, calculators, labels) as JSON")
	public ResponseEntity<?> exportAllData() {
		try {
			String jsonData = exportService.exportAllDataAsJson();
			String filename = "calculator_collector_export_" + java.time.LocalDate.now() + ".json";
			return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
				.contentType(org.springframework.http.MediaType.parseMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE))
				.body(jsonData);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to export data: " + e.getMessage()));
		}
	}

	@PostMapping("/import")
	@Operation(summary = "Import all data", description = "Import all data (manufacturers, calculators, labels) from JSON file")
	public ResponseEntity<ApiResponse<Map<String, Integer>>> importAllData(
			@Parameter(description = "JSON file containing data") @RequestParam("file") MultipartFile file) {

		if (file.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("File is empty"));
		}

		try {
			String jsonData = new String(file.getBytes());
			ImportService.ImportResult result = importService.importAllData(jsonData);
			return ResponseEntity.ok(ApiResponse.success(
				"Import completed! Created: " + result.manufacturersCreated + " manufacturers, " +
				result.calculatorsCreated + " calculators, " + result.labelsCreated + " labels.",
				Map.of(
					"manufacturersCreated", result.manufacturersCreated,
					"calculatorsCreated", result.calculatorsCreated,
					"labelsCreated", result.labelsCreated)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to import data: " + e.getMessage()));
		}
	}

	// ========== Link Management (Admin) ==========

	@PostMapping("/calculators/{calculatorId}/links")
	@Operation(summary = "Add link (admin)", description = "Add a link to a calculator (admin can add any link)")
	public ResponseEntity<ApiResponse<com.example.CalCol.dto.LinkDTO>> addLink(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Link URL") @RequestParam String url,
			@Parameter(description = "Link title") @RequestParam String title,
			@Parameter(description = "Link description (optional)") @RequestParam(required = false) String description,
			Authentication authentication) {

		try {
			com.example.CalCol.entity.CalculatorLink link = linkService.addLink(
				calculatorId, url, title, description, authentication.getName());
			com.example.CalCol.dto.LinkDTO dto = dtoMapper.toLinkDTO(link);
			return ResponseEntity.ok(ApiResponse.success("Link added successfully", dto));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to add link: " + e.getMessage()));
		}
	}

	@PutMapping("/calculators/{calculatorId}/links/{linkId}")
	@Operation(summary = "Update link (admin)", description = "Update a link on a calculator (admin can update any link)")
	public ResponseEntity<ApiResponse<com.example.CalCol.dto.LinkDTO>> updateLink(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Link ID") @PathVariable Long linkId,
			@Parameter(description = "Link URL") @RequestParam String url,
			@Parameter(description = "Link title") @RequestParam String title,
			@Parameter(description = "Link description (optional)") @RequestParam(required = false) String description,
			Authentication authentication) {

		try {
			com.example.CalCol.entity.CalculatorLink link = linkService.updateLink(
				linkId, url, title, description, authentication.getName());
			com.example.CalCol.dto.LinkDTO dto = dtoMapper.toLinkDTO(link);
			return ResponseEntity.ok(ApiResponse.success("Link updated successfully", dto));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to update link: " + e.getMessage()));
		}
	}

	@DeleteMapping("/calculators/{calculatorId}/links/{linkId}")
	@Operation(summary = "Delete link (admin)", description = "Delete a link from a calculator (admin can delete any link)")
	public ResponseEntity<ApiResponse<Void>> deleteLink(
			@Parameter(description = "Calculator ID") @PathVariable Long calculatorId,
			@Parameter(description = "Link ID") @PathVariable Long linkId) {

		boolean deleted = linkService.adminDeleteLink(linkId);
		if (deleted) {
			return ResponseEntity.ok(ApiResponse.success("Link deleted successfully", null));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Link not found"));
		}
	}
}
