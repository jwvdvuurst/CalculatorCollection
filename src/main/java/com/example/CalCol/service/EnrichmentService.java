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
		
		// Build search query
		String searchQuery = calculator.getManufacturer().getName() + " " + calculator.getModel();
		if (calculator.getRawRowText() != null && !calculator.getRawRowText().trim().isEmpty()) {
			searchQuery += " " + calculator.getRawRowText();
		}
		
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
			List<WebSearchService.SearchResult> googleResults = webSearchService.searchGoogle(searchQuery, 5);
			List<WebSearchService.SearchResult> bingResults = webSearchService.searchBing(searchQuery, 5);
			List<WebSearchService.SearchResult> braveResults = webSearchService.searchBrave(searchQuery, 5);
			List<WebSearchService.SearchResult> allWebResults = new ArrayList<>();
			allWebResults.addAll(googleResults);
			allWebResults.addAll(bingResults);
			allWebResults.addAll(braveResults);
			enrichment.setWebResults(allWebResults);
			log.info("Web search results: Google={}, Bing={}, Brave={}, Total={}", 
				googleResults.size(), bingResults.size(), braveResults.size(), allWebResults.size());
		} catch (Exception e) {
			log.error("Error performing web search: {}", e.getMessage(), e);
		}

		// Perform image searches
		try {
			List<WebSearchService.ImageSearchResult> googleImages = webSearchService.searchGoogleImages(searchQuery, 10);
			List<WebSearchService.ImageSearchResult> bingImages = webSearchService.searchBingImages(searchQuery, 10);
			List<WebSearchService.ImageSearchResult> braveImages = webSearchService.searchBraveImages(searchQuery, 10);
			List<WebSearchService.ImageSearchResult> allImageResults = new ArrayList<>();
			allImageResults.addAll(googleImages);
			allImageResults.addAll(bingImages);
			allImageResults.addAll(braveImages);
			enrichment.setImageResults(allImageResults);
			log.info("Image search results: Google={}, Bing={}, Brave={}, Total={}", 
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

