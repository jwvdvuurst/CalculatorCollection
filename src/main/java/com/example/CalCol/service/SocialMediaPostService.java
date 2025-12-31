package com.example.CalCol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating social media posts for different platforms
 */
@Service
@Slf4j
public class SocialMediaPostService {

	/**
	 * Generate a social media post for a specific platform
	 */
	public SocialMediaPost generatePost(String platform, CalculatorInfo calculatorInfo, 
			EnrichmentData enrichmentData) {
		switch (platform.toLowerCase()) {
			case "twitter":
			case "x":
				return generateTwitterPost(calculatorInfo, enrichmentData);
			case "facebook":
				return generateFacebookPost(calculatorInfo, enrichmentData);
			case "instagram":
				return generateInstagramPost(calculatorInfo, enrichmentData);
			case "linkedin":
				return generateLinkedInPost(calculatorInfo, enrichmentData);
			case "reddit":
				return generateRedditPost(calculatorInfo, enrichmentData);
			case "mastodon":
				return generateMastodonPost(calculatorInfo, enrichmentData);
			default:
				return generateGenericPost(calculatorInfo, enrichmentData);
		}
	}

	private SocialMediaPost generateTwitterPost(CalculatorInfo info, EnrichmentData enrichment) {
		SocialMediaPost post = new SocialMediaPost();
		post.setPlatform("Twitter/X");
		post.setMaxLength(280);
		
		StringBuilder content = new StringBuilder();
		
		// Start with emoji and calculator name
		content.append("🧮 ").append(info.getManufacturer()).append(" ").append(info.getModel());
		
		// Add key labels if they fit
		if (info.getLabels() != null && !info.getLabels().isEmpty()) {
			String labels = String.join(" ", info.getLabels().subList(0, Math.min(2, info.getLabels().size())));
			if (content.length() + labels.length() + 3 < 250) {
				content.append(" #").append(labels.replace(" ", ""));
			}
		}
		
		// Add interesting fact from enrichment if available
		if (enrichment != null && enrichment.getAiContent() != null && enrichment.getAiContent().getContent() != null) {
			String fact = extractShortFact(enrichment.getAiContent().getContent());
			if (fact != null && content.length() + fact.length() + 10 < 280) {
				content.append("\n\n").append(fact);
			}
		}
		
		// Add hashtags
		content.append("\n\n#Calculator #VintageTech");
		if (info.getLabels() != null) {
			for (String label : info.getLabels()) {
				if (content.length() + label.length() + 2 < 280) {
					content.append(" #").append(label.replace(" ", ""));
				}
			}
		}
		
		// Truncate if needed
		if (content.length() > 280) {
			content.setLength(277);
			content.append("...");
		}
		
		post.setContent(content.toString());
		return post;
	}

	private SocialMediaPost generateFacebookPost(CalculatorInfo info, EnrichmentData enrichment) {
		SocialMediaPost post = new SocialMediaPost();
		post.setPlatform("Facebook");
		post.setMaxLength(5000);
		
		StringBuilder content = new StringBuilder();
		
		content.append("🧮 ").append(info.getManufacturer()).append(" ").append(info.getModel()).append("\n\n");
		
		if (info.getDescription() != null && !info.getDescription().trim().isEmpty()) {
			content.append(info.getDescription()).append("\n\n");
		}
		
		if (info.getLabels() != null && !info.getLabels().isEmpty()) {
			content.append("Labels: ").append(String.join(", ", info.getLabels())).append("\n\n");
		}
		
		if (enrichment != null && enrichment.getAiContent() != null) {
			content.append("Interesting facts:\n").append(enrichment.getAiContent()).append("\n\n");
		}
		
		if (enrichment != null && enrichment.getMuseumResults() != null && !enrichment.getMuseumResults().isEmpty()) {
			content.append("Related resources:\n");
			for (CalculatorMuseumSearchService.MuseumSearchResult museum : enrichment.getMuseumResults()) {
				if (museum.getFound()) {
					content.append("• ").append(museum.getSiteUrl()).append("\n");
				}
			}
		}
		
		post.setContent(content.toString());
		return post;
	}

	private SocialMediaPost generateInstagramPost(CalculatorInfo info, EnrichmentData enrichment) {
		SocialMediaPost post = new SocialMediaPost();
		post.setPlatform("Instagram");
		post.setMaxLength(2200);
		
		StringBuilder content = new StringBuilder();
		
		content.append("🧮 ").append(info.getManufacturer()).append(" ").append(info.getModel()).append("\n\n");
		
		if (info.getDescription() != null && !info.getDescription().trim().isEmpty()) {
			content.append(info.getDescription()).append("\n\n");
		}
		
		// Instagram loves emojis
		if (info.getLabels() != null && !info.getLabels().isEmpty()) {
			content.append("✨ Features: ").append(String.join(" • ", info.getLabels())).append("\n\n");
		}
		
		if (enrichment != null && enrichment.getAiContent() != null && enrichment.getAiContent().getContent() != null) {
			String fact = extractShortFact(enrichment.getAiContent().getContent());
			if (fact != null) {
				content.append("💡 ").append(fact).append("\n\n");
			}
		}
		
		// Hashtags (Instagram allows up to 30)
		content.append("#Calculator #VintageTech #RetroTech");
		if (info.getLabels() != null) {
			int hashtagCount = 3;
			for (String label : info.getLabels()) {
				if (hashtagCount < 30) {
					content.append(" #").append(label.replace(" ", ""));
					hashtagCount++;
				}
			}
		}
		
		post.setContent(content.toString());
		return post;
	}

	private SocialMediaPost generateLinkedInPost(CalculatorInfo info, EnrichmentData enrichment) {
		SocialMediaPost post = new SocialMediaPost();
		post.setPlatform("LinkedIn");
		post.setMaxLength(3000);
		
		StringBuilder content = new StringBuilder();
		
		content.append("Vintage Calculator: ").append(info.getManufacturer()).append(" ").append(info.getModel()).append("\n\n");
		
		if (info.getDescription() != null && !info.getDescription().trim().isEmpty()) {
			content.append(info.getDescription()).append("\n\n");
		}
		
		if (enrichment != null && enrichment.getAiContent() != null && enrichment.getAiContent().getContent() != null) {
			content.append("Historical Context:\n").append(enrichment.getAiContent().getContent()).append("\n\n");
		}
		
		if (info.getLabels() != null && !info.getLabels().isEmpty()) {
			content.append("Key Features: ").append(String.join(", ", info.getLabels())).append("\n\n");
		}
		
		content.append("What's your favorite vintage calculator? Share your thoughts in the comments!\n\n");
		content.append("#VintageTechnology #Calculator #TechHistory #STEM");
		
		post.setContent(content.toString());
		return post;
	}

	private SocialMediaPost generateRedditPost(CalculatorInfo info, EnrichmentData enrichment) {
		SocialMediaPost post = new SocialMediaPost();
		post.setPlatform("Reddit");
		post.setMaxLength(40000);
		
		StringBuilder content = new StringBuilder();
		
		content.append("**").append(info.getManufacturer()).append(" ").append(info.getModel()).append("**\n\n");
		
		if (info.getDescription() != null && !info.getDescription().trim().isEmpty()) {
			content.append(info.getDescription()).append("\n\n");
		}
		
		if (info.getLabels() != null && !info.getLabels().isEmpty()) {
			content.append("**Features:** ").append(String.join(", ", info.getLabels())).append("\n\n");
		}
		
		if (enrichment != null && enrichment.getAiContent() != null && enrichment.getAiContent().getContent() != null) {
			content.append("**Interesting Facts:**\n\n").append(enrichment.getAiContent().getContent()).append("\n\n");
		}
		
		if (enrichment != null && enrichment.getMuseumResults() != null && !enrichment.getMuseumResults().isEmpty()) {
			content.append("**Related Resources:**\n\n");
			for (CalculatorMuseumSearchService.MuseumSearchResult museum : enrichment.getMuseumResults()) {
				if (museum.getFound()) {
					content.append("* [").append(museum.getSiteUrl()).append("](").append(museum.getSiteUrl()).append(")\n");
				}
			}
		}
		
		post.setContent(content.toString());
		return post;
	}

	private SocialMediaPost generateMastodonPost(CalculatorInfo info, EnrichmentData enrichment) {
		SocialMediaPost post = new SocialMediaPost();
		post.setPlatform("Mastodon");
		post.setMaxLength(500);
		
		StringBuilder content = new StringBuilder();
		
		content.append("🧮 ").append(info.getManufacturer()).append(" ").append(info.getModel());
		
		if (info.getDescription() != null && !info.getDescription().trim().isEmpty()) {
			String desc = info.getDescription();
			if (content.length() + desc.length() + 10 < 450) {
				content.append("\n\n").append(desc);
			}
		}
		
		content.append("\n\n#Calculator #VintageTech");
		
		if (content.length() > 500) {
			content.setLength(497);
			content.append("...");
		}
		
		post.setContent(content.toString());
		return post;
	}

	private SocialMediaPost generateGenericPost(CalculatorInfo info, EnrichmentData enrichment) {
		SocialMediaPost post = new SocialMediaPost();
		post.setPlatform("Generic");
		post.setMaxLength(2000);
		
		StringBuilder content = new StringBuilder();
		
		content.append(info.getManufacturer()).append(" ").append(info.getModel()).append("\n\n");
		
		if (info.getDescription() != null && !info.getDescription().trim().isEmpty()) {
			content.append(info.getDescription()).append("\n\n");
		}
		
		if (info.getLabels() != null && !info.getLabels().isEmpty()) {
			content.append("Labels: ").append(String.join(", ", info.getLabels())).append("\n\n");
		}
		
		if (enrichment != null && enrichment.getAiContent() != null && enrichment.getAiContent().getContent() != null) {
			content.append(enrichment.getAiContent().getContent());
		}
		
		post.setContent(content.toString());
		return post;
	}

	private String extractShortFact(String content) {
		if (content == null || content.trim().isEmpty()) {
			return null;
		}
		
		// Try to extract the first interesting sentence or fact
		String[] sentences = content.split("[.!?]");
		for (String sentence : sentences) {
			sentence = sentence.trim();
			if (sentence.length() > 20 && sentence.length() < 200) {
				return sentence;
			}
		}
		
		// If no good sentence found, return first 150 chars
		if (content.length() > 150) {
			return content.substring(0, 147) + "...";
		}
		return content;
	}

	public static class SocialMediaPost {
		private String platform;
		private String content;
		private int maxLength;

		public String getPlatform() {
			return platform;
		}

		public void setPlatform(String platform) {
			this.platform = platform;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public int getMaxLength() {
			return maxLength;
		}

		public void setMaxLength(int maxLength) {
			this.maxLength = maxLength;
		}
	}

	public static class CalculatorInfo {
		private String manufacturer;
		private String model;
		private String description;
		private List<String> labels;
		private List<String> labelDescriptions;
		private List<String> imageUrls;

		// Getters and setters
		public String getManufacturer() { return manufacturer; }
		public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
		public String getModel() { return model; }
		public void setModel(String model) { this.model = model; }
		public String getDescription() { return description; }
		public void setDescription(String description) { this.description = description; }
		public List<String> getLabels() { return labels; }
		public void setLabels(List<String> labels) { this.labels = labels; }
		public List<String> getLabelDescriptions() { return labelDescriptions; }
		public void setLabelDescriptions(List<String> labelDescriptions) { this.labelDescriptions = labelDescriptions; }
		public List<String> getImageUrls() { return imageUrls; }
		public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
	}

	public static class EnrichmentData {
		private List<WebSearchService.SearchResult> webResults;
		private List<CalculatorMuseumSearchService.MuseumSearchResult> museumResults;
		private AISearchService.AISearchResult aiContent;
		private WebSearchService.BraveAIResult braveAIResult;
		private List<WebSearchService.ImageSearchResult> imageResults;

		public List<WebSearchService.SearchResult> getWebResults() { return webResults; }
		public void setWebResults(List<WebSearchService.SearchResult> webResults) { this.webResults = webResults; }
		public List<CalculatorMuseumSearchService.MuseumSearchResult> getMuseumResults() { return museumResults; }
		public void setMuseumResults(List<CalculatorMuseumSearchService.MuseumSearchResult> museumResults) { this.museumResults = museumResults; }
		public AISearchService.AISearchResult getAiContent() { return aiContent; }
		public void setAiContent(AISearchService.AISearchResult aiContent) { this.aiContent = aiContent; }
		public WebSearchService.BraveAIResult getBraveAIResult() { return braveAIResult; }
		public void setBraveAIResult(WebSearchService.BraveAIResult braveAIResult) { this.braveAIResult = braveAIResult; }
		public List<WebSearchService.ImageSearchResult> getImageResults() { return imageResults; }
		public void setImageResults(List<WebSearchService.ImageSearchResult> imageResults) { this.imageResults = imageResults; }
	}
}

