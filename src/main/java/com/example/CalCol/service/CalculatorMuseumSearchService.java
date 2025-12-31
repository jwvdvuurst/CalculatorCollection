package com.example.CalCol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for searching specialized calculator museum websites
 */
@Service
@Slf4j
public class CalculatorMuseumSearchService {

	private final WebClient webClient;
	
	private static final List<String> MUSEUM_SITES = List.of(
		"https://calculator-museum.nl/",
		"http://www.calcuseum.com/",
		"https://www.hpmuseum.org/",
		"http://www.vintagecalculators.com/",
		"https://www.calculators.de/",
		"http://www.datamath.org/",
		"http://www.arithmomuseum.com/",
		"https://www.oldcalculatormuseum.com/"
	);

	public CalculatorMuseumSearchService() {
		this.webClient = WebClient.builder()
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
			.build();
	}

	/**
	 * Search calculator museum sites for information about a calculator
	 */
	public List<MuseumSearchResult> searchMuseums(String manufacturer, String model) {
		List<MuseumSearchResult> results = new ArrayList<>();
		String searchQuery = (manufacturer + " " + model).trim();

		for (String site : MUSEUM_SITES) {
			try {
				MuseumSearchResult result = searchSite(site, searchQuery);
				if (result != null && result.getFound()) {
					results.add(result);
				}
			} catch (Exception e) {
				log.warn("Error searching museum site {}: {}", site, e.getMessage());
			}
		}

		return results;
	}

	private MuseumSearchResult searchSite(String baseUrl, String query) {
		try {
			// Try to search the site - this is a simplified approach
			// In a real implementation, you might use site-specific search APIs or scrape search results
			String searchUrl = buildSearchUrl(baseUrl, query);
			
			String html = webClient.get()
				.uri(searchUrl)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(java.time.Duration.ofSeconds(5))
				.block();

			MuseumSearchResult result = new MuseumSearchResult();
			result.setSiteUrl(baseUrl);
			result.setSearchUrl(searchUrl);
			
			// Simple check if the calculator model/manufacturer appears in the page
			String lowerQuery = query.toLowerCase();
			String lowerHtml = html.toLowerCase();
			
			if (lowerHtml.contains(lowerQuery)) {
				result.setFound(true);
				// Extract a snippet around the match
				int index = lowerHtml.indexOf(lowerQuery);
				int start = Math.max(0, index - 100);
				int end = Math.min(html.length(), index + query.length() + 100);
				result.setSnippet(html.substring(start, end).replaceAll("\\s+", " ").trim());
			} else {
				result.setFound(false);
			}

			return result;
		} catch (Exception e) {
			log.debug("Could not search site {}: {}", baseUrl, e.getMessage());
			MuseumSearchResult result = new MuseumSearchResult();
			result.setSiteUrl(baseUrl);
			result.setFound(false);
			return result;
		}
	}

	private String buildSearchUrl(String baseUrl, String query) {
		// Different sites have different search URL patterns
		// This is a simplified approach - you might need to customize per site
		if (baseUrl.contains("calculator-museum.nl")) {
			return baseUrl + "?s=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
		} else if (baseUrl.contains("hpmuseum.org")) {
			return baseUrl + "search.php?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
		} else {
			// For other sites, try to find the calculator page directly
			return baseUrl;
		}
	}

	public static class MuseumSearchResult {
		private String siteUrl;
		private String searchUrl;
		private boolean found;
		private String snippet;

		public String getSiteUrl() {
			return siteUrl;
		}

		public void setSiteUrl(String siteUrl) {
			this.siteUrl = siteUrl;
		}

		public String getSearchUrl() {
			return searchUrl;
		}

		public void setSearchUrl(String searchUrl) {
			this.searchUrl = searchUrl;
		}

		public boolean getFound() {
			return found;
		}

		public void setFound(boolean found) {
			this.found = found;
		}

		public String getSnippet() {
			return snippet;
		}

		public void setSnippet(String snippet) {
			this.snippet = snippet;
		}
	}
}

