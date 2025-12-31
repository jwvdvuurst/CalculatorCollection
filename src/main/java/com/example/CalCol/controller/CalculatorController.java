package com.example.CalCol.controller;

import com.example.CalCol.service.CalculatorService;
import com.example.CalCol.service.CalculatorProposalService;
import com.example.CalCol.service.EnrichmentService;
import com.example.CalCol.service.ExportService;
import com.example.CalCol.service.ImageService;
import com.example.CalCol.service.ImportService;
import com.example.CalCol.service.LabelService;
import com.example.CalCol.service.LinkService;
import com.example.CalCol.service.ShareService;
import com.example.CalCol.service.SocialMediaPostService;
import com.example.CalCol.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/calculators")
@RequiredArgsConstructor
@Slf4j
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
	private final EnrichmentService enrichmentService;
	private final SocialMediaPostService socialMediaPostService;
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
				// Check if user is admin
				boolean isAdmin = authentication.getAuthorities().stream()
					.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
				model.addAttribute("isAdmin", isAdmin);
				
				// Parse enriched data to extract image results (for admins)
				if (isAdmin && calc.getEnrichedData() != null && !calc.getEnrichedData().trim().isEmpty()) {
					try {
						com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
							new com.fasterxml.jackson.databind.ObjectMapper();
						com.fasterxml.jackson.databind.JsonNode enrichedData = 
							objectMapper.readTree(calc.getEnrichedData());
						
						if (enrichedData.has("imageResults")) {
							com.fasterxml.jackson.databind.JsonNode imageResults = enrichedData.get("imageResults");
							java.util.List<java.util.Map<String, Object>> foundImages = new java.util.ArrayList<>();
							
							if (imageResults.isArray()) {
								for (com.fasterxml.jackson.databind.JsonNode imageNode : imageResults) {
									java.util.Map<String, Object> imageMap = new java.util.HashMap<>();
									imageMap.put("imageUrl", imageNode.has("imageUrl") ? imageNode.get("imageUrl").asText() : null);
									imageMap.put("thumbnailUrl", imageNode.has("thumbnailUrl") ? imageNode.get("thumbnailUrl").asText() : null);
									imageMap.put("title", imageNode.has("title") ? imageNode.get("title").asText() : null);
									imageMap.put("sourceUrl", imageNode.has("sourceUrl") ? imageNode.get("sourceUrl").asText() : null);
									imageMap.put("width", imageNode.has("width") ? imageNode.get("width").asInt() : null);
									imageMap.put("height", imageNode.has("height") ? imageNode.get("height").asInt() : null);
									imageMap.put("source", imageNode.has("source") ? imageNode.get("source").asText() : null);
									foundImages.add(imageMap);
								}
							}
							
							model.addAttribute("foundImages", foundImages);
						}
					} catch (Exception e) {
						log.error("Error parsing enriched data for image results: {}", e.getMessage(), e);
					}
				}
			} else {
				model.addAttribute("images", calculatorService.getApprovedImages(id));
			}
		});
		return "calculators/detail";
	}

	@PostMapping("/{id}/images/from-url")
	@PreAuthorize("hasRole('ADMIN')")
	public String addImageFromUrl(
			@PathVariable Long id,
			@RequestParam("imageUrl") String imageUrl,
			@RequestParam(value = "proposeForRepository", defaultValue = "false") boolean proposeForRepository,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		String username = authentication.getName();
		try {
			imageService.addImageFromUrl(id, imageUrl, username, proposeForRepository);
			redirectAttributes.addFlashAttribute("successMessage", 
				"Image downloaded and added successfully!");
		} catch (Exception e) {
			log.error("Error adding image from URL: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", 
				"Failed to add image from URL: " + e.getMessage());
		}

		return "redirect:/calculators/" + id;
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

	@GetMapping("/{id}/social-share")
	public String socialShareForm(
			@PathVariable Long id,
			Model model,
			Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		java.util.Optional<com.example.CalCol.entity.Calculator> calcOpt = 
			calculatorService.getCalculatorById(id);
		
		if (calcOpt.isEmpty()) {
			model.addAttribute("errorMessage", "Calculator not found.");
			return "error";
		}

		com.example.CalCol.entity.Calculator calculator = calcOpt.get();
		model.addAttribute("calculator", calculator);
		
		// Get labels for the calculator
		java.util.List<com.example.CalCol.entity.Label> labels = labelService.getCalculatorLabels(id);
		model.addAttribute("labels", labels);
		
		// Get images for the calculator
		java.util.List<com.example.CalCol.entity.CalculatorImage> images = 
			calculatorService.getApprovedImages(id);
		model.addAttribute("images", images);
		
		return "calculators/social-share";
	}

	@PostMapping("/{id}/social-share/generate")
	public String generateSocialMediaPost(
			@PathVariable Long id,
			@RequestParam String platform,
			@RequestParam(required = false, defaultValue = "false") boolean enrich,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		try {
			java.util.Optional<com.example.CalCol.entity.Calculator> calcOpt = 
				calculatorService.getCalculatorById(id);
			
			if (calcOpt.isEmpty()) {
				redirectAttributes.addFlashAttribute("errorMessage", "Calculator not found.");
				return "redirect:/calculators";
			}

			com.example.CalCol.entity.Calculator calculator = calcOpt.get();
			
			// Build calculator info
			com.example.CalCol.service.SocialMediaPostService.CalculatorInfo calcInfo = 
				enrichmentService.buildCalculatorInfo(calculator);
			
			// Enrich if requested
			com.example.CalCol.service.SocialMediaPostService.EnrichmentData enrichment = null;
			if (enrich) {
				enrichment = enrichmentService.enrichCalculator(calculator);
			}
			
			// Generate post
			com.example.CalCol.service.SocialMediaPostService.SocialMediaPost post = 
				socialMediaPostService.generatePost(platform, calcInfo, enrichment);
			
			model.addAttribute("calculator", calculator);
			model.addAttribute("post", post);
			model.addAttribute("platform", platform);
			model.addAttribute("enrichment", enrichment);
			
			return "calculators/social-share-result";
		} catch (Exception e) {
			log.error("Error generating social media post: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", 
				"Failed to generate social media post: " + e.getMessage());
			return "redirect:/calculators/" + id;
		}
	}

	@PostMapping("/{id}/enrich")
	@PreAuthorize("hasRole('ADMIN')")
	public String enrichCalculator(
			@PathVariable Long id,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		try {
			java.util.Optional<com.example.CalCol.entity.Calculator> calcOpt = 
				calculatorService.getCalculatorById(id);
			
			if (calcOpt.isEmpty()) {
				redirectAttributes.addFlashAttribute("errorMessage", "Calculator not found.");
				return "redirect:/calculators";
			}

			com.example.CalCol.entity.Calculator calculator = calcOpt.get();
			
			log.info("Starting enrichment for calculator ID: {}, Model: {}, Manufacturer: {}", 
				id, calculator.getModel(), calculator.getManufacturer().getName());
			
			// Perform enrichment
			com.example.CalCol.service.SocialMediaPostService.EnrichmentData enrichment = 
				enrichmentService.enrichCalculator(calculator);
			
			log.info("Enrichment completed. AI Content: {}, Brave AI: {}, Web Results: {}, Museum Results: {}, Images: {}", 
				enrichment.getAiContent() != null ? "present" : "null",
				enrichment.getBraveAIResult() != null ? "present" : "null",
				enrichment.getWebResults() != null ? enrichment.getWebResults().size() : 0,
				enrichment.getMuseumResults() != null ? enrichment.getMuseumResults().size() : 0,
				enrichment.getImageResults() != null ? enrichment.getImageResults().size() : 0);
			
			// Track which URLs were converted to links (to exclude from enriched data)
			java.util.Set<String> convertedLinkUrls = new java.util.HashSet<>();
			
			// Build description from enrichment data
			StringBuilder description = new StringBuilder();
			
			// Add Brave AI structured data first (most structured)
			if (enrichment.getBraveAIResult() != null) {
				com.example.CalCol.service.WebSearchService.BraveAIResult braveResult = 
					enrichment.getBraveAIResult();
				log.info("Brave AI Result - Structured Data: {}, Raw Response: {}", 
					braveResult.getStructuredData() != null ? braveResult.getStructuredData().size() : 0,
					braveResult.getRawResponse() != null ? "present" : "null");
				
				if (braveResult.getStructuredData() != null && !braveResult.getStructuredData().isEmpty()) {
					description.append("Structured Information:\n");
					for (java.util.Map.Entry<String, String> entry : braveResult.getStructuredData().entrySet()) {
						description.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
					}
					description.append("\n");
				}
				if (braveResult.getRawResponse() != null && !braveResult.getRawResponse().trim().isEmpty()) {
					description.append("Additional Information:\n").append(braveResult.getRawResponse()).append("\n\n");
				}
			}
			
			// Add AI content if available
			if (enrichment.getAiContent() != null && enrichment.getAiContent().getContent() != null 
				&& !enrichment.getAiContent().getContent().trim().isEmpty()) {
				log.info("AI Content length: {}", enrichment.getAiContent().getContent().length());
				description.append(enrichment.getAiContent().getContent()).append("\n\n");
			}
			
			// Extract technical specifications from remaining web results (not converted to links)
			// and from other sources (AI content, Brave AI, museum results)
			java.util.Map<String, String> technicalSpecs = new java.util.LinkedHashMap<>();
			
			// Collect all text content for analysis
			java.util.List<String> textSources = new java.util.ArrayList<>();
			
			// Add snippets from web results that weren't converted to links
			if (enrichment.getWebResults() != null && !enrichment.getWebResults().isEmpty()) {
				for (com.example.CalCol.service.WebSearchService.SearchResult result : enrichment.getWebResults()) {
					if (!convertedLinkUrls.contains(result.getUrl()) && 
						result.getSnippet() != null && !result.getSnippet().trim().isEmpty()) {
						textSources.add(cleanHtmlTags(result.getSnippet()));
					}
				}
			}
			
			// Add AI content
			if (enrichment.getAiContent() != null && enrichment.getAiContent().getContent() != null) {
				textSources.add(enrichment.getAiContent().getContent());
			}
			
			// Add Brave AI raw response
			if (enrichment.getBraveAIResult() != null && enrichment.getBraveAIResult().getRawResponse() != null) {
				textSources.add(enrichment.getBraveAIResult().getRawResponse());
			}
			
			// Add museum results snippets
			if (enrichment.getMuseumResults() != null && !enrichment.getMuseumResults().isEmpty()) {
				for (com.example.CalCol.service.CalculatorMuseumSearchService.MuseumSearchResult result : 
					enrichment.getMuseumResults()) {
					if (result.getSnippet() != null && !result.getSnippet().trim().isEmpty()) {
						textSources.add(cleanHtmlTags(result.getSnippet()));
					}
				}
			}
			
			// Extract technical specifications from all text sources
			for (String text : textSources) {
				if (text == null || text.trim().isEmpty()) {
					continue;
				}
				
				// Extract display type
				java.util.regex.Pattern displayPattern = java.util.regex.Pattern.compile(
					"(?i)\\b(lcd|led|vfd|oled|vacuum\\s+fluorescent|dot\\s+matrix|segmented)\\b");
				java.util.regex.Matcher displayMatcher = displayPattern.matcher(text);
				if (displayMatcher.find() && !technicalSpecs.containsKey("Display Type")) {
					technicalSpecs.put("Display Type", displayMatcher.group(1).toUpperCase());
				}
				
				// Extract display width/dimensions
				java.util.regex.Pattern widthPattern = java.util.regex.Pattern.compile(
					"(?i)(?:display|screen|width|size)[^\\d]*(\\d+)\\s*(?:digit|char|character|dot|pixel|segments?|\\s*x\\s*\\d+)");
				java.util.regex.Matcher widthMatcher = widthPattern.matcher(text);
				if (widthMatcher.find() && !technicalSpecs.containsKey("Display Width")) {
					technicalSpecs.put("Display Width", widthMatcher.group(1) + " digits/characters");
				}
				
				// Extract resolution
				java.util.regex.Pattern resolutionPattern = java.util.regex.Pattern.compile(
					"(?i)(\\d+)\\s*x\\s*(\\d+)\\s*(?:pixel|dot|resolution)");
				java.util.regex.Matcher resolutionMatcher = resolutionPattern.matcher(text);
				if (resolutionMatcher.find() && !technicalSpecs.containsKey("Resolution")) {
					technicalSpecs.put("Resolution", resolutionMatcher.group(1) + "x" + resolutionMatcher.group(2));
				}
				
				// Extract processor/CPU information
				java.util.regex.Pattern processorPattern = java.util.regex.Pattern.compile(
					"(?i)\\b(cpu|processor|microprocessor|chip|ic|integrated\\s+circuit)[^\\w]*(?:type|model|name)?[^\\w]*([A-Z0-9\\-]+)");
				java.util.regex.Matcher processorMatcher = processorPattern.matcher(text);
				if (processorMatcher.find() && !technicalSpecs.containsKey("Processor")) {
					technicalSpecs.put("Processor", processorMatcher.group(2));
				}
				
				// Extract OS/operating system
				java.util.regex.Pattern osPattern = java.util.regex.Pattern.compile(
					"(?i)\\b(os|operating\\s+system|firmware|rom|software)[^\\w]*(?:version|type|name)?[^\\w]*([A-Z0-9\\-\\.]+)");
				java.util.regex.Matcher osMatcher = osPattern.matcher(text);
				if (osMatcher.find() && !technicalSpecs.containsKey("OS/Firmware")) {
					technicalSpecs.put("OS/Firmware", osMatcher.group(2));
				}
				
				// Extract power supply type
				if (text.toLowerCase().contains("batter") && !technicalSpecs.containsKey("Power Supply")) {
					java.util.regex.Pattern batteryPattern = java.util.regex.Pattern.compile(
						"(?i)(\\d+)\\s*(?:x\\s*)?(?:AA|AAA|button|coin|cell|batteries?)");
					java.util.regex.Matcher batteryMatcher = batteryPattern.matcher(text);
					if (batteryMatcher.find()) {
						technicalSpecs.put("Power Supply", batteryMatcher.group(1) + "x " + 
							(batteryMatcher.group(2) != null ? batteryMatcher.group(2) : "batteries"));
					} else {
						technicalSpecs.put("Power Supply", "Batteries");
					}
				} else if (text.toLowerCase().contains("solar") && !technicalSpecs.containsKey("Power Supply")) {
					technicalSpecs.put("Power Supply", "Solar powered");
				} else if (text.toLowerCase().contains("usb") && text.toLowerCase().contains("power") && 
					!technicalSpecs.containsKey("Power Supply")) {
					technicalSpecs.put("Power Supply", "USB powered");
				} else if (text.toLowerCase().contains("external") && text.toLowerCase().contains("power") && 
					!technicalSpecs.containsKey("Power Supply")) {
					technicalSpecs.put("Power Supply", "External power supply");
				} else if (text.toLowerCase().contains("ac") && text.toLowerCase().contains("adapter") && 
					!technicalSpecs.containsKey("Power Supply")) {
					technicalSpecs.put("Power Supply", "AC adapter");
				}
			}
			
			// Extract dates and prices (keep existing logic)
			java.util.Set<String> extractedFacts = new java.util.LinkedHashSet<>();
			for (String text : textSources) {
				if (text == null || text.trim().isEmpty()) {
					continue;
				}
				String cleanSnippet = cleanHtmlTags(text);
				if (cleanSnippet != null && !cleanSnippet.trim().isEmpty()) {
					// Extract dates
					java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile(
						"(?i)(introduced|released|discontinued|manufactured|produced)\\s+(?:in\\s+)?(\\d{4})");
					java.util.regex.Matcher dateMatcher = datePattern.matcher(cleanSnippet);
					while (dateMatcher.find()) {
						extractedFacts.add(dateMatcher.group(1) + " " + dateMatcher.group(2));
					}
					
					// Extract prices
					java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile(
						"(?i)(?:retail\\s+)?price(?:\\s+of)?\\s+\\$?([\\d,]+(?:\\.\\d{2})?)");
					java.util.regex.Matcher priceMatcher = pricePattern.matcher(cleanSnippet);
					if (priceMatcher.find()) {
						extractedFacts.add("Retail price: $" + priceMatcher.group(1));
					}
				}
			}
				
			// Add technical specifications to description
			if (!technicalSpecs.isEmpty()) {
				if (description.length() == 0) {
					description.append("Technical Specifications:\n");
				} else {
					description.append("\nTechnical Specifications:\n");
				}
				for (java.util.Map.Entry<String, String> spec : technicalSpecs.entrySet()) {
					description.append("- ").append(spec.getKey()).append(": ").append(spec.getValue()).append("\n");
				}
				description.append("\n");
			}
			
			// Add extracted facts (dates, prices) to description
			if (!extractedFacts.isEmpty()) {
				if (description.length() == 0) {
					description.append("Key Information:\n");
				} else {
					description.append("\nAdditional Information:\n");
				}
				for (String fact : extractedFacts) {
					description.append("- ").append(fact).append("\n");
				}
				description.append("\n");
			}
			
			// If still no description, add summary from remaining web results
			if (description.length() == 0 && enrichment.getWebResults() != null && !enrichment.getWebResults().isEmpty()) {
				description.append("Additional Information:\n");
				int count = 0;
				for (com.example.CalCol.service.WebSearchService.SearchResult result : enrichment.getWebResults()) {
					if (!convertedLinkUrls.contains(result.getUrl()) && count < 3) {
						String cleanSnippet = cleanHtmlTags(result.getSnippet());
						if (cleanSnippet != null && !cleanSnippet.trim().isEmpty()) {
							String snippet = cleanSnippet.length() > 200 ? 
								cleanSnippet.substring(0, 200) + "..." : cleanSnippet;
							description.append("- ").append(snippet).append("\n");
							count++;
						}
					}
				}
				description.append("\n");
			}
			
			// Store enriched data as JSON (excluding results that were converted to links)
			com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
				new com.fasterxml.jackson.databind.ObjectMapper();
			java.util.Map<String, Object> enrichedDataMap = new java.util.HashMap<>();
			
			// Filter out web results that were converted to links
			if (enrichment.getWebResults() != null && !enrichment.getWebResults().isEmpty()) {
				java.util.List<java.util.Map<String, String>> webResults = new java.util.ArrayList<>();
				for (com.example.CalCol.service.WebSearchService.SearchResult result : enrichment.getWebResults()) {
					// Only include if not converted to a link
					if (!convertedLinkUrls.contains(result.getUrl())) {
						java.util.Map<String, String> resultMap = new java.util.HashMap<>();
						resultMap.put("title", result.getTitle());
						resultMap.put("url", result.getUrl());
						resultMap.put("snippet", result.getSnippet());
						webResults.add(resultMap);
					}
				}
				if (!webResults.isEmpty()) {
					enrichedDataMap.put("webResults", webResults);
				}
			}
			
			if (enrichment.getMuseumResults() != null && !enrichment.getMuseumResults().isEmpty()) {
				java.util.List<java.util.Map<String, Object>> museumResults = new java.util.ArrayList<>();
				for (com.example.CalCol.service.CalculatorMuseumSearchService.MuseumSearchResult result : 
					enrichment.getMuseumResults()) {
					java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
					resultMap.put("siteUrl", result.getSiteUrl());
					resultMap.put("searchUrl", result.getSearchUrl());
					resultMap.put("found", result.getFound());
					resultMap.put("snippet", result.getSnippet());
					museumResults.add(resultMap);
				}
				enrichedDataMap.put("museumResults", museumResults);
			}
			
			if (enrichment.getAiContent() != null) {
				java.util.Map<String, Object> aiContent = new java.util.HashMap<>();
				aiContent.put("content", enrichment.getAiContent().getContent());
				aiContent.put("links", enrichment.getAiContent().getLinks());
				enrichedDataMap.put("aiContent", aiContent);
			}
			
			if (enrichment.getBraveAIResult() != null) {
				java.util.Map<String, Object> braveData = new java.util.HashMap<>();
				braveData.put("structuredData", enrichment.getBraveAIResult().getStructuredData());
				braveData.put("rawResponse", enrichment.getBraveAIResult().getRawResponse());
				braveData.put("sourceUrl", enrichment.getBraveAIResult().getSourceUrl());
				enrichedDataMap.put("braveAIResult", braveData);
			}
			
			// Add image search results
			if (enrichment.getImageResults() != null && !enrichment.getImageResults().isEmpty()) {
				java.util.List<java.util.Map<String, Object>> imageResults = new java.util.ArrayList<>();
				for (com.example.CalCol.service.WebSearchService.ImageSearchResult result : enrichment.getImageResults()) {
					java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
					resultMap.put("imageUrl", result.getImageUrl());
					resultMap.put("thumbnailUrl", result.getThumbnailUrl());
					resultMap.put("title", result.getTitle());
					resultMap.put("sourceUrl", result.getSourceUrl());
					resultMap.put("width", result.getWidth());
					resultMap.put("height", result.getHeight());
					resultMap.put("source", result.getSource());
					imageResults.add(resultMap);
				}
				enrichedDataMap.put("imageResults", imageResults);
			}
			
			String enrichedDataJson = objectMapper.writeValueAsString(enrichedDataMap);
			String finalDescription = description.toString().trim();
			
			log.info("Preparing to save enrichment. Description length: {}, EnrichedData JSON length: {}", 
				finalDescription.length(), enrichedDataJson.length());
			log.debug("EnrichedData JSON: {}", enrichedDataJson);
			
			// Check if we have any data to save
			boolean hasEnrichedData = !enrichedDataJson.isEmpty() && !enrichedDataJson.equals("{}");
			boolean hasDescription = !finalDescription.isEmpty();
			
			log.info("Enrichment data check - Description: {} ({} chars), EnrichedData: {} ({} chars)", 
				hasDescription ? "present" : "empty", finalDescription.length(),
				hasEnrichedData ? "present" : "empty", enrichedDataJson.length());
			
			// Update calculator with enriched data
			if (hasDescription) {
				calculator.setDescription(finalDescription);
				log.info("Setting description (length: {})", finalDescription.length());
			} else {
				log.warn("Description is empty");
			}
			
			if (hasEnrichedData) {
				calculator.setEnrichedData(enrichedDataJson);
				log.info("Setting enrichedData JSON (length: {})", enrichedDataJson.length());
			} else {
				log.warn("EnrichedData JSON is empty or only contains empty objects");
			}
			
			// Only save if we have something to save
			if (!hasDescription && !hasEnrichedData) {
				log.error("No enrichment data to save! All sources returned empty results.");
				redirectAttributes.addFlashAttribute("errorMessage", 
					"Enrichment completed but no data was retrieved. Please check API configurations and quota limits.");
				return "redirect:/calculators/" + id;
			}
			
			// Automatically add web search results as links and track which ones were added
			int linksAdded = 0;
			if (enrichment.getWebResults() != null && !enrichment.getWebResults().isEmpty()) {
				String adminUsername = authentication.getName();
				for (com.example.CalCol.service.WebSearchService.SearchResult webResult : enrichment.getWebResults()) {
					if (webResult.getUrl() != null && !webResult.getUrl().trim().isEmpty()) {
						// Clean HTML tags from snippet for description
						String cleanSnippet = cleanHtmlTags(webResult.getSnippet());
						String title = webResult.getTitle() != null ? webResult.getTitle() : "Web Search Result";
						
						// Truncate title if too long
						if (title.length() > 200) {
							title = title.substring(0, 197) + "...";
						}
						
						// Truncate description if too long
						if (cleanSnippet != null && cleanSnippet.length() > 500) {
							cleanSnippet = cleanSnippet.substring(0, 497) + "...";
						}
						
						com.example.CalCol.entity.CalculatorLink link = 
							linkService.addLinkIfNotExists(id, webResult.getUrl(), title, cleanSnippet, adminUsername);
						if (link != null) {
							linksAdded++;
							convertedLinkUrls.add(webResult.getUrl());
							log.debug("Added link: {} - {}", title, webResult.getUrl());
						}
					}
				}
				log.info("Added {} new links from web search results", linksAdded);
			}
			
			// Save the calculator
			com.example.CalCol.entity.Calculator savedCalculator = calculatorService.saveCalculator(calculator);
			
			// Verify the save
			log.info("Calculator saved. ID: {}, Description: {}, EnrichedData: {}", 
				savedCalculator.getId(),
				savedCalculator.getDescription() != null ? "set (" + savedCalculator.getDescription().length() + " chars)" : "null",
				savedCalculator.getEnrichedData() != null ? "set (" + savedCalculator.getEnrichedData().length() + " chars)" : "null");
			
			// Double-check by reloading from database
			java.util.Optional<com.example.CalCol.entity.Calculator> verifyOpt = 
				calculatorService.getCalculatorById(savedCalculator.getId());
			if (verifyOpt.isPresent()) {
				com.example.CalCol.entity.Calculator verified = verifyOpt.get();
				log.info("Verified from DB - Description: {}, EnrichedData: {}", 
					verified.getDescription() != null ? "present" : "null",
					verified.getEnrichedData() != null ? "present" : "null");
			}
			
			StringBuilder successMsg = new StringBuilder("Calculator data enriched successfully! ");
			if (hasDescription) {
				successMsg.append("Description saved. ");
			}
			if (hasEnrichedData) {
				successMsg.append("Enriched data JSON saved. ");
			}
			if (linksAdded > 0) {
				successMsg.append(linksAdded).append(" new link(s) added from web search results.");
			}
			
			if (finalDescription.isEmpty() && enrichedDataJson.isEmpty() && linksAdded == 0) {
				redirectAttributes.addFlashAttribute("warningMessage", 
					"Enrichment completed but no data was retrieved. This may be due to API quota limits or no results found.");
			} else {
				redirectAttributes.addFlashAttribute("successMessage", successMsg.toString());
			}
		} catch (Exception e) {
			log.error("Error enriching calculator: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", 
				"Failed to enrich calculator: " + e.getMessage());
		}

		return "redirect:/calculators/" + id;
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
	
	/**
	 * Clean HTML tags from text
	 */
	private String cleanHtmlTags(String html) {
		if (html == null || html.trim().isEmpty()) {
			return null;
		}
		
		// Remove HTML tags
		String cleaned = html.replaceAll("<[^>]+>", "");
		
		// Decode common HTML entities
		cleaned = cleaned.replace("&amp;", "&")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&quot;", "\"")
			.replace("&#39;", "'")
			.replace("&nbsp;", " ")
			.replace("&apos;", "'");
		
		// Clean up multiple spaces
		cleaned = cleaned.replaceAll("\\s+", " ").trim();
		
		return cleaned;
	}
}

