package com.example.CalCol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for performing web searches on Google and Bing
 */
@Service
@Slf4j
public class WebSearchService {

	private final WebClient webClient;
	private final QuotaService quotaService;
	
	@Value("${app.search.google.api-key:}")
	private String googleApiKey;
	
	@Value("${app.search.google.search-engine-id:}")
	private String googleSearchEngineId;
	
	@Value("${app.search.bing.api-key:}")
	private String bingApiKey;
	
	@Value("${app.search.brave.api-key:}")
	private String braveApiKey;

	public WebSearchService(QuotaService quotaService) {
		this.webClient = WebClient.builder().build();
		this.quotaService = quotaService;
	}

	/**
	 * Search Google for calculator information
	 */
	public List<SearchResult> searchGoogle(String query, int maxResults) {
		if (googleApiKey == null || googleApiKey.isEmpty() || 
			googleSearchEngineId == null || googleSearchEngineId.isEmpty()) {
			log.warn("Google API key or search engine ID not configured. Skipping Google search.");
			return new ArrayList<>();
		}

		// Check quota before making request
		if (!quotaService.canMakeRequest("google")) {
			log.warn("Google search quota/rate limit exceeded. Remaining: {}", 
				quotaService.getRemainingQuota("google"));
			return new ArrayList<>();
		}

		try {
			String url = String.format(
				"https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&num=%d",
				googleApiKey, googleSearchEngineId, java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8), maxResults);

			Map<String, Object> response = webClient.get()
				.uri(url)
				.retrieve()
				.bodyToMono(Map.class)
				.block();

			List<SearchResult> results = parseGoogleResults(response);
			// Record successful request
			quotaService.recordRequest("google");
			return results;
		} catch (Exception e) {
			log.error("Error searching Google: {}", e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	/**
	 * Search Bing for calculator information
	 */
	public List<SearchResult> searchBing(String query, int maxResults) {
		if (bingApiKey == null || bingApiKey.isEmpty()) {
			log.warn("Bing API key not configured. Skipping Bing search.");
			return new ArrayList<>();
		}

		// Check quota before making request
		if (!quotaService.canMakeRequest("bing")) {
			log.warn("Bing search quota/rate limit exceeded. Remaining: {}", 
				quotaService.getRemainingQuota("bing"));
			return new ArrayList<>();
		}

		try {
			String url = String.format(
				"https://api.bing.microsoft.com/v7.0/search?q=%s&count=%d",
				java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8), maxResults);

			Map<String, Object> response = webClient.get()
				.uri(url)
				.header("Ocp-Apim-Subscription-Key", bingApiKey)
				.retrieve()
				.bodyToMono(Map.class)
				.block();

		List<SearchResult> results = parseBingResults(response);
		// Record successful request
		quotaService.recordRequest("bing");
		return results;
	} catch (Exception e) {
		log.error("Error searching Bing: {}", e.getMessage(), e);
		return new ArrayList<>();
	}
}

	/**
	 * Search Google for calculator images
	 */
	public List<ImageSearchResult> searchGoogleImages(String query, int maxResults) {
		if (googleApiKey == null || googleApiKey.isEmpty() || 
			googleSearchEngineId == null || googleSearchEngineId.isEmpty()) {
			log.warn("Google API key or search engine ID not configured. Skipping Google image search.");
			return new ArrayList<>();
		}

		// Check quota before making request
		if (!quotaService.canMakeRequest("google")) {
			int remaining = quotaService.getRemainingQuota("google");
			log.warn("Google image search blocked (rate limit or quota exceeded). Remaining monthly quota: {}", remaining);
			return new ArrayList<>();
		}

		try {
			String url = String.format(
				"https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&num=%d&searchType=image",
				googleApiKey, googleSearchEngineId, java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8), maxResults);

			log.debug("Google image search URL: {}", url.replace(googleApiKey, "***"));
			
			Map<String, Object> response = webClient.get()
				.uri(url)
				.retrieve()
				.bodyToMono(Map.class)
				.block();

			if (response == null) {
				log.warn("Google image search returned null response");
				return new ArrayList<>();
			}
			
			log.debug("Google image search response keys: {}", response.keySet());
			
			List<ImageSearchResult> results = parseGoogleImageResults(response);
			log.info("Google image search returned {} results", results.size());
			
			// Record successful request
			quotaService.recordRequest("google");
			return results;
		} catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
			log.error("Google image search API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
			return new ArrayList<>();
		} catch (Exception e) {
			log.error("Error searching Google images: {}", e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	/**
	 * Search Bing for calculator images
	 */
	public List<ImageSearchResult> searchBingImages(String query, int maxResults) {
		if (bingApiKey == null || bingApiKey.isEmpty()) {
			log.warn("Bing API key not configured. Skipping Bing image search.");
			return new ArrayList<>();
		}

		// Check quota before making request
		if (!quotaService.canMakeRequest("bing")) {
			log.warn("Bing image search quota/rate limit exceeded. Remaining: {}", 
				quotaService.getRemainingQuota("bing"));
			return new ArrayList<>();
		}

		try {
			String url = String.format(
				"https://api.bing.microsoft.com/v7.0/images/search?q=%s&count=%d",
				java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8), maxResults);

			log.debug("Bing image search URL: {}", url);

			Map<String, Object> response = webClient.get()
				.uri(url)
				.header("Ocp-Apim-Subscription-Key", bingApiKey)
				.retrieve()
				.bodyToMono(Map.class)
				.block();

			if (response == null) {
				log.warn("Bing image search returned null response");
				return new ArrayList<>();
			}

			log.debug("Bing image search response keys: {}", response.keySet());

			List<ImageSearchResult> results = parseBingImageResults(response);
			log.info("Bing image search returned {} results", results.size());
			
			// Record successful request
			quotaService.recordRequest("bing");
			return results;
		} catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
			log.error("Bing image search API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
			return new ArrayList<>();
		} catch (Exception e) {
			log.error("Error searching Bing images: {}", e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	/**
	 * Search Brave for calculator images
	 */
	public List<ImageSearchResult> searchBraveImages(String query, int maxResults) {
		if (braveApiKey == null || braveApiKey.isEmpty()) {
			log.warn("Brave API key not configured. Skipping Brave image search.");
			return new ArrayList<>();
		}

		// Check quota before making request
		if (!quotaService.canMakeRequest("brave")) {
			int remaining = quotaService.getRemainingQuota("brave");
			log.warn("Brave image search blocked (rate limit or quota exceeded). Remaining monthly quota: {}", remaining);
			return new ArrayList<>();
		}

		try {
			String url = String.format(
				"https://api.search.brave.com/res/v1/images/search?q=%s&count=%d",
				java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8), maxResults);

			log.debug("Brave image search URL: {}", url);

			Map<String, Object> response = webClient.get()
				.uri(url)
				.header("Accept", "application/json")
				.header("Accept-Encoding", "gzip")
				.header("X-Subscription-Token", braveApiKey)
				.retrieve()
				.bodyToMono(Map.class)
				.block();

			if (response == null) {
				log.warn("Brave image search returned null response");
				return new ArrayList<>();
			}

			log.debug("Brave image search response keys: {}", response.keySet());

			List<ImageSearchResult> results = parseBraveImageResults(response);
			log.info("Brave image search returned {} results", results.size());
			
			// Record successful request
			quotaService.recordRequest("brave");
			return results;
		} catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
			log.error("Brave image search API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
			return new ArrayList<>();
		} catch (Exception e) {
			log.error("Error searching Brave images: {}", e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	/**
	 * Search Brave for calculator information
	 */
	public List<SearchResult> searchBrave(String query, int maxResults) {
		if (braveApiKey == null || braveApiKey.isEmpty()) {
			log.warn("Brave API key not configured. Skipping Brave search.");
			return new ArrayList<>();
		}

		// Check quota before making request
		if (!quotaService.canMakeRequest("brave")) {
			log.warn("Brave search quota/rate limit exceeded. Remaining: {}", 
				quotaService.getRemainingQuota("brave"));
			return new ArrayList<>();
		}

		try {
			String url = String.format(
				"https://api.search.brave.com/res/v1/web/search?q=%s&count=%d",
				java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8), maxResults);

			Map<String, Object> response = webClient.get()
				.uri(url)
				.header("Accept", "application/json")
				.header("Accept-Encoding", "gzip")
				.header("X-Subscription-Token", braveApiKey)
				.retrieve()
				.bodyToMono(Map.class)
				.block();

			List<SearchResult> results = parseBraveResults(response);
			// Record successful request
			quotaService.recordRequest("brave");
			return results;
		} catch (Exception e) {
			log.error("Error searching Brave: {}", e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	/**
	 * Search Brave AI for structured calculator information
	 * Uses a specific prompt format to get structured data in table format
	 */
	public BraveAIResult searchBraveAI(String manufacturer, String model) {
		if (braveApiKey == null || braveApiKey.isEmpty()) {
			log.warn("Brave API key not configured. Skipping Brave AI search.");
			return null;
		}

		// Check quota before making request
		if (!quotaService.canMakeRequest("brave")) {
			log.warn("Brave AI search quota/rate limit exceeded. Remaining: {}", 
				quotaService.getRemainingQuota("brave"));
			return null;
		}

		try {
			String prompt = String.format("In a table of keywords and labels describe the vintage calculator %s %s", 
				manufacturer, model);
			
			String url = String.format(
				"https://api.search.brave.com/res/v1/web/search?q=%s",
				java.net.URLEncoder.encode(prompt, java.nio.charset.StandardCharsets.UTF_8));

			Map<String, Object> response = webClient.get()
				.uri(url)
				.header("Accept", "application/json")
				.header("Accept-Encoding", "gzip")
				.header("X-Subscription-Token", braveApiKey)
				.retrieve()
				.bodyToMono(Map.class)
				.block();

			BraveAIResult result = parseBraveAIResults(response, manufacturer, model);
			// Record successful request
			quotaService.recordRequest("brave");
			return result;
		} catch (Exception e) {
			log.error("Error searching Brave AI: {}", e.getMessage(), e);
			return null;
		}
	}

	private List<SearchResult> parseGoogleResults(Map<String, Object> response) {
		List<SearchResult> results = new ArrayList<>();
		if (response == null || !response.containsKey("items")) {
			return results;
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
		if (items == null) {
			return results;
		}

		for (Map<String, Object> item : items) {
			SearchResult result = new SearchResult();
			result.setTitle((String) item.get("title"));
			result.setUrl((String) item.get("link"));
			result.setSnippet((String) item.get("snippet"));
			results.add(result);
		}

		return results;
	}

	private List<SearchResult> parseBingResults(Map<String, Object> response) {
		List<SearchResult> results = new ArrayList<>();
		if (response == null || !response.containsKey("webPages")) {
			return results;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> webPages = (Map<String, Object>) response.get("webPages");
		if (webPages == null || !webPages.containsKey("value")) {
			return results;
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) webPages.get("value");
		if (items == null) {
			return results;
		}

		for (Map<String, Object> item : items) {
			SearchResult result = new SearchResult();
			result.setTitle((String) item.get("name"));
			result.setUrl((String) item.get("url"));
			result.setSnippet((String) item.get("snippet"));
			results.add(result);
		}

		return results;
	}

	private List<SearchResult> parseBraveResults(Map<String, Object> response) {
		List<SearchResult> results = new ArrayList<>();
		if (response == null || !response.containsKey("web")) {
			return results;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> web = (Map<String, Object>) response.get("web");
		if (web == null || !web.containsKey("results")) {
			return results;
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) web.get("results");
		if (items == null) {
			return results;
		}

		for (Map<String, Object> item : items) {
			SearchResult result = new SearchResult();
			result.setTitle((String) item.get("title"));
			result.setUrl((String) item.get("url"));
			result.setSnippet((String) item.get("description"));
			results.add(result);
		}

		return results;
	}

	private BraveAIResult parseBraveAIResults(Map<String, Object> response, String manufacturer, String model) {
		BraveAIResult aiResult = new BraveAIResult();
		aiResult.setManufacturer(manufacturer);
		aiResult.setModel(model);
		
		log.debug("Parsing Brave AI results for {} {}", manufacturer, model);
		
		// Try to extract structured data from the search results
		// The AI response typically appears in the first result's description or snippet
		if (response != null && response.containsKey("web")) {
			@SuppressWarnings("unchecked")
			Map<String, Object> web = (Map<String, Object>) response.get("web");
			if (web != null && web.containsKey("results")) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> items = (List<Map<String, Object>>) web.get("results");
				if (items != null && !items.isEmpty()) {
					log.debug("Found {} results in Brave AI response", items.size());
					
					// Try to find the best result - look for one with description containing table-like data
					for (Map<String, Object> result : items) {
						String description = (String) result.get("description");
						String title = (String) result.get("title");
						
						log.debug("Result title: {}, description length: {}", title, 
							description != null ? description.length() : 0);
						
						if (description != null && !description.trim().isEmpty()) {
							// Check if this looks like structured data (contains keywords/labels or table-like format)
							String lowerDesc = description.toLowerCase();
							if (lowerDesc.contains("keywords") || lowerDesc.contains("labels") || 
								lowerDesc.contains("\t") || lowerDesc.contains("model") || 
								lowerDesc.contains("manufacturer")) {
								log.debug("Found structured data in result: {}", title);
								aiResult.setStructuredData(parseStructuredTable(description));
								aiResult.setRawResponse(description);
								aiResult.setSourceUrl((String) result.get("url"));
								break; // Use first matching result
							}
						}
					}
					
					// If no structured data found, use first result anyway
					if (aiResult.getRawResponse() == null && !items.isEmpty()) {
						Map<String, Object> firstResult = items.get(0);
						String description = (String) firstResult.get("description");
						if (description != null && !description.trim().isEmpty()) {
							log.debug("Using first result as fallback");
							aiResult.setStructuredData(parseStructuredTable(description));
							aiResult.setRawResponse(description);
							aiResult.setSourceUrl((String) firstResult.get("url"));
						}
					}
				} else {
					log.warn("Brave AI response has no results");
				}
			} else {
				log.warn("Brave AI response missing 'results' key");
			}
		} else {
			log.warn("Brave AI response missing 'web' key");
		}
		
		log.debug("Brave AI Result - Structured Data entries: {}, Raw Response: {}", 
			aiResult.getStructuredData() != null ? aiResult.getStructuredData().size() : 0,
			aiResult.getRawResponse() != null ? "present" : "null");
		
		return aiResult;
	}

	/**
	 * Parse structured table data from the AI response
	 * Expected format: "Keywords\tLabels\nModel\t...\nManufacturer\t..."
	 * Also handles markdown-style tables and plain text with colons
	 */
	private java.util.Map<String, String> parseStructuredTable(String text) {
		java.util.Map<String, String> structuredData = new java.util.HashMap<>();
		
		if (text == null || text.trim().isEmpty()) {
			return structuredData;
		}
		
		log.debug("Parsing structured table from text of length: {}", text.length());
		
		// Split by lines
		String[] lines = text.split("\n");
		boolean foundTable = false;
		boolean skipNextSeparator = false; // For markdown tables
		
		for (String line : lines) {
			line = line.trim();
			if (line.isEmpty()) {
				continue;
			}
			
			// Skip markdown table separators (e.g., "|---|---|")
			if (line.matches("^[|\\s-:]+$")) {
				skipNextSeparator = true;
				continue;
			}
			
			// Look for table header
			if (line.toLowerCase().contains("keywords") && line.toLowerCase().contains("labels")) {
				foundTable = true;
				skipNextSeparator = true; // Next line might be separator
				continue;
			}
			
			if (foundTable || skipNextSeparator) {
				skipNextSeparator = false;
				
				// Try different parsing methods
				boolean parsed = false;
				
				// Method 1: Tab-separated
				if (line.contains("\t")) {
					String[] parts = line.split("\t");
					if (parts.length >= 2) {
						String key = parts[0].trim();
						String value = parts[1].trim();
						if (!key.isEmpty() && !value.isEmpty() && !key.equals(value)) {
							structuredData.put(key, value);
							parsed = true;
							log.debug("Parsed tab-separated: {} = {}", key, value);
						}
					}
				}
				
				// Method 2: Pipe-separated (markdown table)
				if (!parsed && line.contains("|")) {
					String[] parts = line.split("\\|");
					if (parts.length >= 3) { // Skip first and last empty parts
						String key = parts[1].trim();
						String value = parts[2].trim();
						if (!key.isEmpty() && !value.isEmpty() && !key.equals(value) && 
							!key.toLowerCase().contains("keyword") && !key.toLowerCase().contains("label")) {
							structuredData.put(key, value);
							parsed = true;
							log.debug("Parsed pipe-separated: {} = {}", key, value);
						}
					}
				}
				
				// Method 3: Colon-separated (key: value format)
				if (!parsed && line.contains(":")) {
					int colonIndex = line.indexOf(':');
					if (colonIndex > 0 && colonIndex < line.length() - 1) {
						String key = line.substring(0, colonIndex).trim();
						String value = line.substring(colonIndex + 1).trim();
						if (!key.isEmpty() && !value.isEmpty() && !key.equals(value)) {
							structuredData.put(key, value);
							parsed = true;
							log.debug("Parsed colon-separated: {} = {}", key, value);
						}
					}
				}
			}
		}
		
		// If no table structure found, try to extract key-value pairs from the whole text
		if (structuredData.isEmpty() && text.length() > 50) {
			log.debug("No table structure found, trying to extract key-value pairs");
			// Look for patterns like "Model: ..." or "Manufacturer: ..."
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
				"(?i)(Model|Manufacturer|Year|Type|Display|Power|Size|Weight|Functions|Keys|Features|Successor|Historical)[:\\s]+([^\\n]+)");
			java.util.regex.Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				String key = matcher.group(1);
				String value = matcher.group(2).trim();
				if (!value.isEmpty()) {
					structuredData.put(key, value);
					log.debug("Extracted pattern: {} = {}", key, value);
				}
			}
		}
		
		log.debug("Parsed {} structured data entries", structuredData.size());
		return structuredData;
	}

	private List<ImageSearchResult> parseGoogleImageResults(Map<String, Object> response) {
		List<ImageSearchResult> results = new ArrayList<>();
		if (response == null) {
			log.warn("Google image search response is null");
			return results;
		}
		
		log.debug("Google image search response keys: {}", response.keySet());
		
		// Check for errors
		if (response.containsKey("error")) {
			@SuppressWarnings("unchecked")
			Map<String, Object> error = (Map<String, Object>) response.get("error");
			log.error("Google image search API error: {}", error);
			return results;
		}
		
		if (!response.containsKey("items")) {
			log.warn("Google image search response has no 'items' key. Available keys: {}. Response: {}", 
				response.keySet(), response);
			return results;
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
		if (items == null || items.isEmpty()) {
			log.debug("Google image search returned empty items list");
			return results;
		}

		log.debug("Parsing {} Google image search results", items.size());
		
		for (Map<String, Object> item : items) {
			try {
				ImageSearchResult result = new ImageSearchResult();
				
				// Google Custom Search API for images returns:
				// - link: direct image URL
				// - title: image title
				// - image.thumbnailLink: thumbnail URL
				// - image.contextLink: source page URL
				// - image.width/height: dimensions
				
				String imageUrl = (String) item.get("link");
				if (imageUrl == null || imageUrl.trim().isEmpty()) {
					log.debug("Skipping Google image result with no link");
					continue;
				}
				result.setImageUrl(imageUrl);
				result.setTitle((String) item.get("title"));
				
				@SuppressWarnings("unchecked")
				Map<String, Object> image = (Map<String, Object>) item.get("image");
				if (image != null) {
					result.setThumbnailUrl((String) image.get("thumbnailLink"));
					result.setSourceUrl((String) image.get("contextLink"));
					
					Object widthObj = image.get("width");
					Object heightObj = image.get("height");
					if (widthObj instanceof Number) {
						result.setWidth(((Number) widthObj).intValue());
					}
					if (heightObj instanceof Number) {
						result.setHeight(((Number) heightObj).intValue());
					}
				} else {
					// Fallback: use link as thumbnail if image object is missing
					result.setThumbnailUrl(imageUrl);
					result.setSourceUrl((String) item.get("displayLink"));
				}
				
				result.setSource("Google");
				results.add(result);
			} catch (Exception e) {
				log.warn("Error parsing Google image search result: {}", e.getMessage(), e);
			}
		}

		log.info("Successfully parsed {} Google image search results", results.size());
		return results;
	}

	private List<ImageSearchResult> parseBingImageResults(Map<String, Object> response) {
		List<ImageSearchResult> results = new ArrayList<>();
		if (response == null) {
			log.warn("Bing image search response is null");
			return results;
		}
		
		log.debug("Bing image search response keys: {}", response.keySet());
		
		if (!response.containsKey("value")) {
			log.warn("Bing image search response has no 'value' key. Available keys: {}. Response: {}", 
				response.keySet(), response);
			return results;
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("value");
		if (items == null || items.isEmpty()) {
			log.debug("Bing image search returned empty items list");
			return results;
		}

		log.debug("Parsing {} Bing image search results", items.size());

		for (Map<String, Object> item : items) {
			try {
				ImageSearchResult result = new ImageSearchResult();
				
				// Bing Image Search API returns:
				// - contentUrl: direct image URL
				// - thumbnailUrl: thumbnail URL
				// - name: image title
				// - hostPageUrl: source page URL
				// - width/height: dimensions
				
				String imageUrl = (String) item.get("contentUrl");
				if (imageUrl == null || imageUrl.trim().isEmpty()) {
					log.debug("Skipping Bing image result with no contentUrl");
					continue;
				}
				result.setImageUrl(imageUrl);
				result.setThumbnailUrl((String) item.get("thumbnailUrl"));
				result.setTitle((String) item.get("name"));
				result.setSourceUrl((String) item.get("hostPageUrl"));
				
				Object widthObj = item.get("width");
				Object heightObj = item.get("height");
				if (widthObj instanceof Number) {
					result.setWidth(((Number) widthObj).intValue());
				}
				if (heightObj instanceof Number) {
					result.setHeight(((Number) heightObj).intValue());
				}
				
				result.setSource("Bing");
				results.add(result);
			} catch (Exception e) {
				log.warn("Error parsing Bing image search result: {}", e.getMessage(), e);
			}
		}

		log.info("Successfully parsed {} Bing image search results", results.size());
		return results;
	}

	private List<ImageSearchResult> parseBraveImageResults(Map<String, Object> response) {
		List<ImageSearchResult> results = new ArrayList<>();
		if (response == null) {
			log.warn("Brave image search response is null");
			return results;
		}
		
		log.debug("Brave image search response keys: {}", response.keySet());
		
		if (!response.containsKey("results")) {
			log.warn("Brave image search response has no 'results' key. Available keys: {}. Response: {}", 
				response.keySet(), response);
			return results;
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("results");
		if (items == null || items.isEmpty()) {
			log.debug("Brave image search returned empty items list");
			return results;
		}

		log.debug("Parsing {} Brave image search results", items.size());

		for (Map<String, Object> item : items) {
			try {
				ImageSearchResult result = new ImageSearchResult();
				
				// Brave Image Search API returns:
				// - url: direct image URL
				// - thumbnail: thumbnail URL
				// - title: image title
				// - source: source page URL
				// - width/height: dimensions
				
				String imageUrl = (String) item.get("url");
				if (imageUrl == null || imageUrl.trim().isEmpty()) {
					log.debug("Skipping Brave image result with no url");
					continue;
				}
				result.setImageUrl(imageUrl);
				result.setThumbnailUrl((String) item.get("thumbnail"));
				result.setTitle((String) item.get("title"));
				result.setSourceUrl((String) item.get("source"));
				
				Object widthObj = item.get("width");
				Object heightObj = item.get("height");
				if (widthObj instanceof Number) {
					result.setWidth(((Number) widthObj).intValue());
				}
				if (heightObj instanceof Number) {
					result.setHeight(((Number) heightObj).intValue());
				}
				
				result.setSource("Brave");
				results.add(result);
			} catch (Exception e) {
				log.warn("Error parsing Brave image search result: {}", e.getMessage(), e);
			}
		}

		log.info("Successfully parsed {} Brave image search results", results.size());
		return results;
	}

	/**
	 * Filter search results to only include those that contain:
	 * - The word "calculator" (case-insensitive)
	 * - The manufacturer name (case-insensitive)
	 * - The model/type (case-insensitive)
	 */
	public List<SearchResult> filterSearchResults(List<SearchResult> results, String manufacturer, String model) {
		if (results == null || results.isEmpty()) {
			return results;
		}
		
		if (manufacturer == null || manufacturer.trim().isEmpty() || 
			model == null || model.trim().isEmpty()) {
			log.warn("Cannot filter results: manufacturer or model is null/empty");
			return results;
		}
		
		String manufacturerLower = manufacturer.toLowerCase().trim();
		String modelLower = model.toLowerCase().trim();
		
		List<SearchResult> filtered = new ArrayList<>();
		
		for (SearchResult result : results) {
			if (result == null) {
				continue;
			}
			
			// Combine title, snippet, and URL for checking
			String combinedText = "";
			if (result.getTitle() != null) {
				combinedText += result.getTitle() + " ";
			}
			if (result.getSnippet() != null) {
				combinedText += result.getSnippet() + " ";
			}
			if (result.getUrl() != null) {
				combinedText += result.getUrl() + " ";
			}
			
			String combinedLower = combinedText.toLowerCase();
			
			// Check if result contains all required terms
			boolean hasCalculator = combinedLower.contains("calculator");
			boolean hasManufacturer = combinedLower.contains(manufacturerLower);
			boolean hasModel = combinedLower.contains(modelLower);
			
			if (hasCalculator && hasManufacturer && hasModel) {
				filtered.add(result);
				log.debug("Result passed filter: {}", result.getTitle());
			} else {
				log.debug("Result filtered out - Calculator: {}, Manufacturer: {}, Model: {} - Title: {}", 
					hasCalculator, hasManufacturer, hasModel, result.getTitle());
			}
		}
		
		log.info("Filtered {} results from {} total (manufacturer: {}, model: {})", 
			filtered.size(), results.size(), manufacturer, model);
		
		return filtered;
	}

	/**
	 * Filter image search results to only include those that contain:
	 * - The word "calculator" (case-insensitive) AND
	 * - Either the manufacturer name OR the model/type (case-insensitive)
	 * 
	 * Note: Image filtering is less strict than web search filtering because image metadata
	 * often doesn't contain all three terms, but if the query already includes these terms,
	 * the images should be relevant.
	 */
	public List<ImageSearchResult> filterImageResults(List<ImageSearchResult> results, String manufacturer, String model) {
		if (results == null || results.isEmpty()) {
			return results;
		}
		
		if (manufacturer == null || manufacturer.trim().isEmpty() || 
			model == null || model.trim().isEmpty()) {
			log.warn("Cannot filter image results: manufacturer or model is null/empty");
			return results;
		}
		
		String manufacturerLower = manufacturer.toLowerCase().trim();
		String modelLower = model.toLowerCase().trim();
		
		List<ImageSearchResult> filtered = new ArrayList<>();
		
		for (ImageSearchResult result : results) {
			if (result == null) {
				continue;
			}
			
			// Combine title, sourceUrl, and imageUrl for checking
			String combinedText = "";
			if (result.getTitle() != null) {
				combinedText += result.getTitle() + " ";
			}
			if (result.getSourceUrl() != null) {
				combinedText += result.getSourceUrl() + " ";
			}
			if (result.getImageUrl() != null) {
				combinedText += result.getImageUrl() + " ";
			}
			
			String combinedLower = combinedText.toLowerCase();
			
			// Less strict filtering for images: require "calculator" AND (manufacturer OR model)
			boolean hasCalculator = combinedLower.contains("calculator");
			boolean hasManufacturer = combinedLower.contains(manufacturerLower);
			boolean hasModel = combinedLower.contains(modelLower);
			
			// Accept if it has "calculator" and at least one of manufacturer or model
			if (hasCalculator && (hasManufacturer || hasModel)) {
				filtered.add(result);
				log.debug("Image result passed filter: {} (Calculator: {}, Manufacturer: {}, Model: {})", 
					result.getTitle(), hasCalculator, hasManufacturer, hasModel);
			} else {
				log.debug("Image result filtered out - Calculator: {}, Manufacturer: {}, Model: {} - Title: {}", 
					hasCalculator, hasManufacturer, hasModel, result.getTitle());
			}
		}
		
		log.info("Filtered {} image results from {} total (manufacturer: {}, model: {})", 
			filtered.size(), results.size(), manufacturer, model);
		
		return filtered;
	}

	public static class SearchResult {
		private String title;
		private String url;
		private String snippet;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getSnippet() {
			return snippet;
		}

		public void setSnippet(String snippet) {
			this.snippet = snippet;
		}
	}

	/**
	 * Result from image search
	 */
	public static class ImageSearchResult {
		private String imageUrl;
		private String thumbnailUrl;
		private String title;
		private String sourceUrl;
		private Integer width;
		private Integer height;
		private String source;

		public String getImageUrl() {
			return imageUrl;
		}

		public void setImageUrl(String imageUrl) {
			this.imageUrl = imageUrl;
		}

		public String getThumbnailUrl() {
			return thumbnailUrl;
		}

		public void setThumbnailUrl(String thumbnailUrl) {
			this.thumbnailUrl = thumbnailUrl;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getSourceUrl() {
			return sourceUrl;
		}

		public void setSourceUrl(String sourceUrl) {
			this.sourceUrl = sourceUrl;
		}

		public Integer getWidth() {
			return width;
		}

		public void setWidth(Integer width) {
			this.width = width;
		}

		public Integer getHeight() {
			return height;
		}

		public void setHeight(Integer height) {
			this.height = height;
		}

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}
	}

	/**
	 * Result from Brave AI search containing structured data
	 */
	public static class BraveAIResult {
		private String manufacturer;
		private String model;
		private String rawResponse;
		private String sourceUrl;
		private java.util.Map<String, String> structuredData;

		public String getManufacturer() {
			return manufacturer;
		}

		public void setManufacturer(String manufacturer) {
			this.manufacturer = manufacturer;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public String getRawResponse() {
			return rawResponse;
		}

		public void setRawResponse(String rawResponse) {
			this.rawResponse = rawResponse;
		}

		public String getSourceUrl() {
			return sourceUrl;
		}

		public void setSourceUrl(String sourceUrl) {
			this.sourceUrl = sourceUrl;
		}

		public java.util.Map<String, String> getStructuredData() {
			return structuredData;
		}

		public void setStructuredData(java.util.Map<String, String> structuredData) {
			this.structuredData = structuredData;
		}
	}
}

