package com.example.CalCol.controller;

import com.example.CalCol.service.CalculatorService;
import com.example.CalCol.service.CalculatorProposalService;
import com.example.CalCol.service.ExportService;
import com.example.CalCol.service.ImageService;
import com.example.CalCol.service.ImportService;
import com.example.CalCol.service.LabelService;
import com.example.CalCol.service.LinkService;
import com.example.CalCol.service.ShareService;
import com.example.CalCol.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/calculators")
@RequiredArgsConstructor
public class CalculatorController {

	private final CalculatorService calculatorService;
	private final ImageService imageService;
	private final LabelService labelService;
	private final LinkService linkService;
	private final StatisticsService statisticsService;
	private final ExportService exportService;
	private final ImportService importService;
	private final ShareService shareService;
	private final CalculatorProposalService proposalService;
	private static final int PAGE_SIZE = 20;

	@GetMapping
	public String browseCalculators(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Long manufacturerId,
			@RequestParam(defaultValue = "0") int page,
			Model model,
			Authentication authentication) {

		Pageable pageable = PageRequest.of(page, PAGE_SIZE);
		
		org.springframework.data.domain.Page<com.example.CalCol.entity.Calculator> calculatorsPage;
		if (manufacturerId != null) {
			calculatorsPage = calculatorService.getCalculatorsByManufacturer(manufacturerId, pageable);
			calculatorService.getManufacturerById(manufacturerId).ifPresent(m -> 
				model.addAttribute("manufacturer", m));
		} else {
			calculatorsPage = calculatorService.searchCalculators(search, pageable);
		}
		model.addAttribute("calculators", calculatorsPage);

		if (search != null && !search.trim().isEmpty()) {
			model.addAttribute("search", search);
		}

		if (authentication != null && authentication.isAuthenticated()) {
			String username = authentication.getName();
			model.addAttribute("username", username);
			model.addAttribute("collectionCount", calculatorService.getUserCollectionCount(username));
			// Add set of calculator IDs in user's collection for template checking
			var collectionIds = calculatorsPage.getContent().stream()
				.filter(calc -> calculatorService.isInCollection(username, calc.getId()))
				.map(calc -> calc.getId())
				.collect(java.util.stream.Collectors.toSet());
			model.addAttribute("collectionIds", collectionIds);
		}

		return "calculators/browse";
	}

	@GetMapping("/manufacturers")
	public String browseManufacturers(
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page,
			Model model) {

		Pageable pageable = PageRequest.of(page, PAGE_SIZE);
		model.addAttribute("manufacturers", calculatorService.searchManufacturers(search, pageable));

		if (search != null && !search.trim().isEmpty()) {
			model.addAttribute("search", search);
		}

		return "calculators/manufacturers";
	}

	@GetMapping("/collection")
	public String viewCollection(
			@RequestParam(defaultValue = "0") int page,
			Model model,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		String username = authentication.getName();
		Pageable pageable = PageRequest.of(page, PAGE_SIZE);
		
		model.addAttribute("collection", calculatorService.getUserCollection(username, pageable));
		model.addAttribute("collectionCount", calculatorService.getUserCollectionCount(username));
		model.addAttribute("username", username);
		model.addAttribute("statistics", statisticsService.getCollectionStatistics(username));

		return "calculators/collection";
	}

	@PostMapping("/collection/add/{calculatorId}")
	public String addToCollection(
			@PathVariable Long calculatorId,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		String username = authentication.getName();
		boolean added = calculatorService.addToCollection(username, calculatorId);

		if (added) {
			redirectAttributes.addFlashAttribute("successMessage", "Calculator added to your collection!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", 
				"Calculator could not be added. It may already be in your collection.");
		}

		return "redirect:/calculators";
	}

	@PostMapping("/collection/remove/{calculatorId}")
	public String removeFromCollection(
			@PathVariable Long calculatorId,
			@RequestParam(required = false) String redirectTo,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		String username = authentication.getName();
		boolean removed = calculatorService.removeFromCollection(username, calculatorId);

		if (removed) {
			redirectAttributes.addFlashAttribute("successMessage", "Calculator removed from your collection!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Calculator could not be removed.");
		}

		if ("collection".equals(redirectTo)) {
			return "redirect:/calculators/collection";
		}
		return "redirect:/calculators";
	}

	@GetMapping("/{id}")
	public String calculatorDetail(
			@PathVariable Long id,
			Model model,
			Authentication authentication) {
		calculatorService.getCalculatorById(id).ifPresent(calc -> {
			model.addAttribute("calculator", calc);
			model.addAttribute("labels", labelService.getCalculatorLabels(id));
			model.addAttribute("links", calculatorService.getCalculatorLinks(id));
			model.addAttribute("curatedLabels", labelService.getAllCuratedLabels());
			if (authentication != null && authentication.isAuthenticated()) {
				String username = authentication.getName();
				model.addAttribute("images", calculatorService.getImagesForUser(id, username));
				model.addAttribute("username", username);
			} else {
				model.addAttribute("images", calculatorService.getApprovedImages(id));
			}
		});
		return "calculators/detail";
	}

	@PostMapping("/{id}/images")
	public String uploadImage(
			@PathVariable Long id,
			@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
			@RequestParam(value = "proposeForRepository", defaultValue = "false") boolean proposeForRepository,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		if (file.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload.");
			return "redirect:/calculators/" + id;
		}

		try {
			String username = authentication.getName();
			imageService.uploadImage(id, file, username, proposeForRepository);
			if (proposeForRepository) {
				redirectAttributes.addFlashAttribute("successMessage", 
					"Image uploaded and proposed for the central repository. Waiting for admin approval.");
			} else {
				redirectAttributes.addFlashAttribute("successMessage", "Image uploaded successfully!");
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload image: " + e.getMessage());
		}

		return "redirect:/calculators/" + id;
	}

	@PostMapping("/images/{imageId}/delete")
	public String deleteImage(
			@PathVariable Long imageId,
			Authentication authentication,
			@RequestParam(required = false) Long calculatorId,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		String username = authentication.getName();
		if (imageService.deleteImage(imageId, username)) {
			redirectAttributes.addFlashAttribute("successMessage", "Image deleted successfully!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete image.");
		}

		if (calculatorId != null) {
			return "redirect:/calculators/" + calculatorId;
		}
		return "redirect:/calculators";
	}

	@PostMapping("/{id}/labels")
	public String addLabel(
			@PathVariable Long id,
			@RequestParam(required = false) Long labelId,
			@RequestParam(required = false) String newLabelName,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		try {
			if (labelId != null) {
				// Add existing label
				if (labelService.addLabelToCalculator(id, labelId)) {
					redirectAttributes.addFlashAttribute("successMessage", "Label added successfully!");
				} else {
					redirectAttributes.addFlashAttribute("errorMessage", "Label could not be added.");
				}
			} else if (newLabelName != null && !newLabelName.trim().isEmpty()) {
				// Create new free-form label and add it
				com.example.CalCol.entity.Label label = labelService.createOrGetLabel(newLabelName.trim(), false);
				if (labelService.addLabelToCalculator(id, label.getId())) {
					redirectAttributes.addFlashAttribute("successMessage", "Label added successfully!");
				} else {
					redirectAttributes.addFlashAttribute("errorMessage", "Label could not be added.");
				}
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to add label: " + e.getMessage());
		}

		return "redirect:/calculators/" + id;
	}

	@PostMapping("/{id}/labels/{labelId}/remove")
	public String removeLabel(
			@PathVariable Long id,
			@PathVariable Long labelId,
			RedirectAttributes redirectAttributes) {

		if (labelService.removeLabelFromCalculator(id, labelId)) {
			redirectAttributes.addFlashAttribute("successMessage", "Label removed successfully!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove label.");
		}

		return "redirect:/calculators/" + id;
	}

	@PostMapping("/{id}/links")
	public String addLink(
			@PathVariable Long id,
			@RequestParam String url,
			@RequestParam String title,
			@RequestParam(required = false) String description,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		try {
			String username = authentication.getName();
			linkService.addLink(id, url, title, description, username);
			redirectAttributes.addFlashAttribute("successMessage", "Link added successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to add link: " + e.getMessage());
		}

		return "redirect:/calculators/" + id;
	}

	@PostMapping("/links/{linkId}/delete")
	public String deleteLink(
			@PathVariable Long linkId,
			@RequestParam(required = false) Long calculatorId,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		String username = authentication.getName();
		if (linkService.deleteLink(linkId, username)) {
			redirectAttributes.addFlashAttribute("successMessage", "Link deleted successfully!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete link.");
		}

		if (calculatorId != null) {
			return "redirect:/calculators/" + calculatorId;
		}
		return "redirect:/calculators";
	}

	@GetMapping("/collection/export")
	public org.springframework.http.ResponseEntity<String> exportCollection(
			@RequestParam(defaultValue = "json") String format,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
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

			return org.springframework.http.ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
				.contentType(org.springframework.http.MediaType.parseMediaType(contentType))
				.body(content);
		} catch (Exception e) {
			return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping("/collection/import")
	public String importCollection(
			@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		if (file.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to import.");
			return "redirect:/calculators/collection";
		}

		try {
			String username = authentication.getName();
			String jsonData = new String(file.getBytes());
			int imported = importService.importUserCollection(jsonData, username);
			redirectAttributes.addFlashAttribute("successMessage", 
				"Successfully imported " + imported + " calculator(s) to your collection!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", 
				"Failed to import collection: " + e.getMessage());
		}

		return "redirect:/calculators/collection";
	}

	@GetMapping("/propose")
	public String proposeCalculatorForm(Model model) {
		model.addAttribute("proposal", new com.example.CalCol.entity.CalculatorProposal());
		return "calculators/propose-form";
	}

	@PostMapping("/propose")
	public String submitProposal(
			@RequestParam String model,
			@RequestParam String manufacturerName,
			@RequestParam(required = false) Integer soldFrom,
			@RequestParam(required = false) Integer soldTo,
			@RequestParam(required = false) String sourceUrl,
			@RequestParam(required = false) String rawRowText,
			@RequestParam(required = false) String notes,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		try {
			String username = authentication.getName();
			proposalService.createProposal(model, manufacturerName, soldFrom, soldTo, 
				sourceUrl, rawRowText, notes, username);
			redirectAttributes.addFlashAttribute("successMessage", 
				"Calculator proposal submitted! It will be reviewed by an administrator.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", 
				"Failed to submit proposal: " + e.getMessage());
		}

		return "redirect:/calculators";
	}

	@GetMapping("/collection/share")
	public String shareCollectionForm(
			Model model,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		String username = authentication.getName();
		model.addAttribute("collection", calculatorService.getUserCollection(username, 
			org.springframework.data.domain.Pageable.unpaged()).getContent());
		model.addAttribute("shares", shareService.getUserShares(username));
		return "calculators/share";
	}

	@PostMapping("/collection/share")
	public String createShare(
			@RequestParam("calculatorIds") java.util.List<Long> calculatorIds,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String description,
			@RequestParam(required = false) Integer daysValid,
			@RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		try {
			String username = authentication.getName();
			com.example.CalCol.entity.SharedCollection share = shareService.createShare(
				username, calculatorIds, title, description, daysValid, isPublic);
			redirectAttributes.addFlashAttribute("successMessage", 
				"Collection shared! Share link: " + 
				org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
					.path("/share/" + share.getShareToken()).toUriString());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", 
				"Failed to create share: " + e.getMessage());
		}

		return "redirect:/calculators/collection/share";
	}

	@GetMapping("/share/{token}")
	public String viewSharedCollection(
			@PathVariable String token,
			Model model) {

		java.util.Optional<com.example.CalCol.entity.SharedCollection> shareOpt = 
			shareService.getSharedCollection(token);
		
		if (shareOpt.isEmpty()) {
			model.addAttribute("errorMessage", "Shared collection not found or expired.");
			return "error";
		}

		com.example.CalCol.entity.SharedCollection share = shareOpt.get();
		model.addAttribute("share", share);
		model.addAttribute("calculators", shareService.getSharedCalculators(token));
		return "calculators/shared-view";
	}

	@PostMapping("/share/{token}/delete")
	public String deleteShare(
			@PathVariable String token,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		String username = authentication.getName();
		if (shareService.deleteShare(token, username)) {
			redirectAttributes.addFlashAttribute("successMessage", "Share deleted successfully!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete share.");
		}

		return "redirect:/calculators/collection/share";
	}
}

