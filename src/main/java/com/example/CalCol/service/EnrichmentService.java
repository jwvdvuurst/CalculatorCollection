package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.CalculatorImage;
import com.example.CalCol.entity.Label;
import com.example.CalCol.repository.CalculatorImageRepository;
import com.example.CalCol.repository.CalculatorLabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that coordinates all enrichment services (web search, museum search, AI search)
 * and provides enriched data for social media post generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrichmentService {

	private final WebSearchService webSearchService;
	private final CalculatorMuseumSearchService museumSearchService;
	private final AISearchService aiSearchService;
	private final CalculatorLabelRepository calculatorLabelRepository;
	private final CalculatorImageRepository calculatorImageRepository;
	
	@Value("${app.base-url:}")
	private String baseUrl;

	/**
	 * Enrich calculator data with information from various sources
	 */
	public SocialMediaPostService.EnrichmentData enrichCalculator(Calculator calculator) {
		log.info("Enriching calculator: {} {}", calculator.getManufacturer().getName(), calculator.getModel());
		
		SocialMediaPostService.EnrichmentData enrichment = new SocialMediaPostService.EnrichmentData();
		
		// Build enhanced search query
		String searchQuery = buildEnhancedSearchQuery(calculator);
		
		// Get labels and their descriptions
		List<Label> labels = calculatorLabelRepository.findLabelsByCalculatorId(calculator.getId());
		List<String> labelNames = labels.stream().map(Label::getName).collect(Collectors.toList());
		List<String> labelDescriptions = labels.stream()
			.map(l -> l.getDescription() != null ? l.getDescription() : "")
			.collect(Collectors.toList());
		
		// Get approved images
		List<CalculatorImage> images = calculatorImageRepository.findByCalculatorIdAndIsApprovedTrue(calculator.getId());
		List<String> imageUrls = new ArrayList<>();
		for (CalculatorImage image : images) {
			String imageUrl = buildImageUrl(image.getImagePath());
			imageUrls.add(imageUrl);
		}
		
		// Perform web searches
		try {
			String manufacturer = calculator.getManufacturer().getName();
			String model = calculator.getModel();
			
			List<WebSearchService.SearchResult> googleResults = webSearchService.searchGoogle(searchQuery, 5);
			googleResults = webSearchService.filterSearchResults(googleResults, manufacturer, model);
			
			List<WebSearchService.SearchResult> bingResults = webSearchService.searchBing(searchQuery, 5);
			bingResults = webSearchService.filterSearchResults(bingResults, manufacturer, model);
			
			List<WebSearchService.SearchResult> braveResults = webSearchService.searchBrave(searchQuery, 5);
			braveResults = webSearchService.filterSearchResults(braveResults, manufacturer, model);
			
			List<WebSearchService.SearchResult> allWebResults = new ArrayList<>();
			allWebResults.addAll(googleResults);
			allWebResults.addAll(bingResults);
			allWebResults.addAll(braveResults);
			enrichment.setWebResults(allWebResults);
			log.info("Web search results (after filtering): Google={}, Bing={}, Brave={}, Total={}", 
				googleResults.size(), bingResults.size(), braveResults.size(), allWebResults.size());
		} catch (Exception e) {
			log.error("Error performing web search: {}", e.getMessage(), e);
		}

		// Perform image searches
		try {
			String imageSearchQuery = buildImageSearchQuery(calculator);
			String manufacturer = calculator.getManufacturer().getName();
			String model = calculator.getModel();
			
			List<WebSearchService.ImageSearchResult> googleImages = webSearchService.searchGoogleImages(imageSearchQuery, 10);
			googleImages = webSearchService.filterImageResults(googleImages, manufacturer, model);
			
			List<WebSearchService.ImageSearchResult> bingImages = webSearchService.searchBingImages(imageSearchQuery, 10);
			bingImages = webSearchService.filterImageResults(bingImages, manufacturer, model);
			
			List<WebSearchService.ImageSearchResult> braveImages = webSearchService.searchBraveImages(imageSearchQuery, 10);
			braveImages = webSearchService.filterImageResults(braveImages, manufacturer, model);
			
			List<WebSearchService.ImageSearchResult> allImageResults = new ArrayList<>();
			allImageResults.addAll(googleImages);
			allImageResults.addAll(bingImages);
			allImageResults.addAll(braveImages);
			enrichment.setImageResults(allImageResults);
			log.info("Image search results (after filtering): Google={}, Bing={}, Brave={}, Total={}", 
				googleImages.size(), bingImages.size(), braveImages.size(), allImageResults.size());
		} catch (Exception e) {
			log.error("Error performing image search: {}", e.getMessage(), e);
		}
		
		// Perform Brave AI search for structured data
		try {
			WebSearchService.BraveAIResult braveAIResult = webSearchService.searchBraveAI(
				calculator.getManufacturer().getName(), calculator.getModel());
			if (braveAIResult != null) {
				enrichment.setBraveAIResult(braveAIResult);
			}
		} catch (Exception e) {
			log.error("Error performing Brave AI search: {}", e.getMessage(), e);
		}
		
		// Search calculator museums
		try {
			List<CalculatorMuseumSearchService.MuseumSearchResult> museumResults = 
				museumSearchService.searchMuseums(calculator.getManufacturer().getName(), calculator.getModel());
			enrichment.setMuseumResults(museumResults);
		} catch (Exception e) {
			log.error("Error searching calculator museums: {}", e.getMessage(), e);
		}
		
		// Perform AI search
		try {
			String description = calculator.getRawRowText() != null ? calculator.getRawRowText() : "";
			AISearchService.AISearchResult aiResult = aiSearchService.searchWithAI(
				description, labelNames, labelDescriptions, imageUrls);
			enrichment.setAiContent(aiResult);
			log.info("AI search completed. Content: {}", 
				aiResult.getContent() != null && !aiResult.getContent().trim().isEmpty() ? "present" : "empty");
		} catch (Exception e) {
			log.error("Error performing AI search: {}", e.getMessage(), e);
		}

		log.info("Enrichment summary - Web: {}, Museum: {}, AI: {}, Brave AI: {}, Images: {}", 
			enrichment.getWebResults() != null ? enrichment.getWebResults().size() : 0,
			enrichment.getMuseumResults() != null ? enrichment.getMuseumResults().size() : 0,
			enrichment.getAiContent() != null && enrichment.getAiContent().getContent() != null ? "present" : "null",
			enrichment.getBraveAIResult() != null ? "present" : "null",
			enrichment.getImageResults() != null ? enrichment.getImageResults().size() : 0);

		return enrichment;
	}

	/**
	 * Build calculator info object for social media post generation
	 */
	public SocialMediaPostService.CalculatorInfo buildCalculatorInfo(Calculator calculator) {
		SocialMediaPostService.CalculatorInfo info = new SocialMediaPostService.CalculatorInfo();
		info.setManufacturer(calculator.getManufacturer().getName());
		info.setModel(calculator.getModel());
		info.setDescription(calculator.getRawRowText());
		
		// Get labels
		List<Label> labels = calculatorLabelRepository.findLabelsByCalculatorId(calculator.getId());
		info.setLabels(labels.stream().map(Label::getName).collect(Collectors.toList()));
		info.setLabelDescriptions(labels.stream()
			.map(l -> l.getDescription() != null ? l.getDescription() : "")
			.collect(Collectors.toList()));
		
		// Get image URLs
		List<CalculatorImage> images = calculatorImageRepository.findByCalculatorIdAndIsApprovedTrue(calculator.getId());
		List<String> imageUrls = new ArrayList<>();
		for (CalculatorImage image : images) {
			imageUrls.add(buildImageUrl(image.getImagePath()));
		}
		info.setImageUrls(imageUrls);
		
		return info;
	}

	/**
	 * Build enhanced search query with manufacturer, model, years, calculator, and vintage keywords
	 */
	private String buildEnhancedSearchQuery(Calculator calculator) {
		StringBuilder query = new StringBuilder();
		
		// Get calculator labels to check for Mechanical/Electromechanical
		List<Label> labels = calculatorLabelRepository.findLabelsByCalculatorId(calculator.getId());
		String calculatorType = "electronic"; // default
		
		// Check if calculator has Mechanical or Electromechanical label
		for (Label label : labels) {
			String labelName = label.getName();
			if ("Mechanical".equalsIgnoreCase(labelName)) {
				calculatorType = "mechanical";
				break; // Prefer Mechanical over Electromechanical if both exist
			} else if ("Electromechanical".equalsIgnoreCase(labelName)) {
				calculatorType = "electromechanical";
				// Don't break, in case Mechanical is also present (which would override)
			}
		}
		
		// Add manufacturer and model
		query.append(calculator.getManufacturer().getName()).append(" ");
		query.append(calculator.getModel());
		
		// Add years if present
		if (calculator.getSoldFrom() != null) {
			query.append(" ").append(calculator.getSoldFrom());
		}
		if (calculator.getSoldTo() != null) {
			query.append(" ").append(calculator.getSoldTo());
		}
		
		// Add calculator type (electronic, mechanical, or electromechanical)
		query.append(" ").append(calculatorType).append(" calculator");
		
		// Add "vintage" keyword, except when from year > 2000
		if (calculator.getSoldFrom() == null || calculator.getSoldFrom() <= 2000) {
			query.append(" vintage");
		}
		
		// Add raw row text if present
		if (calculator.getRawRowText() != null && !calculator.getRawRowText().trim().isEmpty()) {
			query.append(" ").append(calculator.getRawRowText());
		}
		
		// Clean and deduplicate the query
		return cleanAndDeduplicateQuery(query.toString().trim());
	}

	/**
	 * Clean search query by removing special characters and deduplicating terms
	 */
	private String cleanAndDeduplicateQuery(String query) {
		if (query == null || query.trim().isEmpty()) {
			return "";
		}
		
		// Remove special characters: comma, semicolon, colon, and other punctuation
		// Keep alphanumeric, spaces, hyphens, and apostrophes (for words like "don't")
		String cleaned = query.replaceAll("[,\\;:!@#$%^&*()\\[\\]{}_+=<>?/\\\\|\"`~]", " ");
		
		// Split into terms, normalize whitespace
		String[] terms = cleaned.split("\\s+");
		
		// Use LinkedHashSet to preserve order while removing duplicates (case-insensitive)
		java.util.LinkedHashSet<String> uniqueTerms = new java.util.LinkedHashSet<>();
		java.util.Set<String> seenLower = new java.util.HashSet<>();
		
		for (String term : terms) {
			if (term != null && !term.trim().isEmpty()) {
				String trimmed = term.trim();
				String lower = trimmed.toLowerCase();
				
				// Only add if we haven't seen this term (case-insensitive)
				if (!seenLower.contains(lower)) {
					uniqueTerms.add(trimmed);
					seenLower.add(lower);
				}
			}
		}
		
		// Join unique terms with spaces
		return String.join(" ", uniqueTerms);
	}

	/**
	 * Build enhanced image search query with manufacturer, model, years, calculator, vintage keywords, and image specification
	 */
	private String buildImageSearchQuery(Calculator calculator) {
		// Start with the base enhanced query (already cleaned and deduplicated)
		String baseQuery = buildEnhancedSearchQuery(calculator);
		
		// Add image-specific keyword and clean again to avoid duplicates
		String imageQuery = baseQuery + " image";
		return cleanAndDeduplicateQuery(imageQuery);
	}

	private String buildImageUrl(String imagePath) {
		if (baseUrl != null && !baseUrl.isEmpty()) {
			return baseUrl + "/uploads/" + imagePath;
		}
		// Fallback to relative URL
		return ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/uploads/")
			.path(imagePath)
			.toUriString();
	}
}

