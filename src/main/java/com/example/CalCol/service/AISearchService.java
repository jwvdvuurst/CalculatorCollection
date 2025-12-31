package com.example.CalCol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Service for AI-powered search using description, labels, and images
 */
@Service
@Slf4j
public class AISearchService {

	private final WebClient webClient;
	private final QuotaService quotaService;
	
	@Value("${app.search.ai.api-key:}")
	private String aiApiKey;
	
	@Value("${app.search.ai.provider:openai}")
	private String aiProvider; // openai, anthropic, etc.

	public AISearchService(QuotaService quotaService) {
		this.webClient = WebClient.builder().build();
		this.quotaService = quotaService;
	}

	/**
	 * Perform AI-powered search with calculator information
	 */
	public AISearchResult searchWithAI(String description, List<String> labels, List<String> labelDescriptions, 
			List<String> imageUrls) {
		if (aiApiKey == null || aiApiKey.isEmpty()) {
			log.warn("AI API key not configured. Skipping AI search.");
			return new AISearchResult();
		}

		// Check quota before making request
		if (!quotaService.canMakeRequest("ai")) {
			log.warn("AI search quota/rate limit exceeded. Remaining: {}", 
				quotaService.getRemainingQuota("ai"));
			return new AISearchResult();
		}

		try {
			String prompt = buildSearchPrompt(description, labels, labelDescriptions, imageUrls);
			log.debug("Calling AI with prompt length: {}", prompt.length());
			AISearchResult result = callAI(prompt);
			log.info("AI search completed. Content length: {}, Links: {}", 
				result.getContent() != null ? result.getContent().length() : 0,
				result.getLinks() != null ? result.getLinks().size() : 0);
			// Record successful request
			quotaService.recordRequest("ai");
			return result;
		} catch (Exception e) {
			log.error("Error performing AI search: {}", e.getMessage(), e);
			return new AISearchResult();
		}
	}

	private String buildSearchPrompt(String description, List<String> labels, List<String> labelDescriptions, 
			List<String> imageUrls) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("I have a calculator with the following information:\n\n");
		
		if (description != null && !description.trim().isEmpty()) {
			prompt.append("Description: ").append(description).append("\n\n");
		}
		
		if (labels != null && !labels.isEmpty()) {
			prompt.append("Labels: ").append(String.join(", ", labels)).append("\n");
			if (labelDescriptions != null && !labelDescriptions.isEmpty()) {
				prompt.append("Label descriptions:\n");
				for (int i = 0; i < Math.min(labels.size(), labelDescriptions.size()); i++) {
					if (labelDescriptions.get(i) != null && !labelDescriptions.get(i).trim().isEmpty()) {
						prompt.append("- ").append(labels.get(i)).append(": ").append(labelDescriptions.get(i)).append("\n");
					}
				}
			}
			prompt.append("\n");
		}
		
		if (imageUrls != null && !imageUrls.isEmpty()) {
			prompt.append("Images available: ").append(imageUrls.size()).append(" image(s)\n");
			prompt.append("Image URLs: ").append(String.join(", ", imageUrls)).append("\n\n");
		}
		
		prompt.append("Please provide:\n");
		prompt.append("1. Historical context about this calculator\n");
		prompt.append("2. Technical specifications if known\n");
		prompt.append("3. Interesting facts or trivia\n");
		prompt.append("4. Links to relevant resources or museum pages\n");
		prompt.append("5. Any notable features or innovations\n");
		
		return prompt.toString();
	}

	private AISearchResult callAI(String prompt) {
		if ("openai".equalsIgnoreCase(aiProvider)) {
			return callOpenAI(prompt);
		} else if ("anthropic".equalsIgnoreCase(aiProvider)) {
			return callAnthropic(prompt);
		} else {
			log.warn("Unknown AI provider: {}. Using OpenAI format.", aiProvider);
			return callOpenAI(prompt);
		}
	}

	private AISearchResult callOpenAI(String prompt) {
		try {
			Map<String, Object> request = new HashMap<>();
			request.put("model", "gpt-4o-mini"); // Using a cost-effective model
			request.put("messages", List.of(
				Map.of("role", "system", "content", "You are a calculator historian and expert. Provide detailed, accurate information about vintage calculators."),
				Map.of("role", "user", "content", prompt)
			));
			request.put("max_tokens", 1000);
			request.put("temperature", 0.7);

			Map<String, Object> response = webClient.post()
				.uri("https://api.openai.com/v1/chat/completions")
				.header("Authorization", "Bearer " + aiApiKey)
				.header("Content-Type", "application/json")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Map.class)
				.block();

			return parseOpenAIResponse(response);
		} catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
			log.warn("OpenAI API rate limit exceeded (429). Please wait before retrying.");
			return new AISearchResult();
		} catch (Exception e) {
			log.error("Error calling OpenAI API: {}", e.getMessage(), e);
			return new AISearchResult();
		}
	}

	private AISearchResult callAnthropic(String prompt) {
		try {
			Map<String, Object> request = new HashMap<>();
			request.put("model", "claude-3-haiku-20240307");
			request.put("max_tokens", 1000);
			request.put("messages", List.of(
				Map.of("role", "user", "content", prompt)
			));

			Map<String, Object> response = webClient.post()
				.uri("https://api.anthropic.com/v1/messages")
				.header("x-api-key", aiApiKey)
				.header("anthropic-version", "2023-06-01")
				.header("Content-Type", "application/json")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Map.class)
				.block();

			return parseAnthropicResponse(response);
		} catch (Exception e) {
			log.error("Error calling Anthropic API: {}", e.getMessage(), e);
			return new AISearchResult();
		}
	}

	@SuppressWarnings("unchecked")
	private AISearchResult parseOpenAIResponse(Map<String, Object> response) {
		AISearchResult result = new AISearchResult();
		if (response == null) {
			log.warn("OpenAI response is null");
			return result;
		}
		
		if (response.containsKey("error")) {
			log.error("OpenAI API error: {}", response.get("error"));
			return result;
		}
		
		if (!response.containsKey("choices")) {
			log.warn("OpenAI response missing 'choices' key. Keys: {}", response.keySet());
			return result;
		}

		List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
		if (choices == null || choices.isEmpty()) {
			log.warn("OpenAI response has no choices");
			return result;
		}

		Map<String, Object> firstChoice = choices.get(0);
		Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
		if (message != null) {
			String content = (String) message.get("content");
			if (content != null && !content.trim().isEmpty()) {
				result.setContent(content);
				log.debug("OpenAI content extracted, length: {}", content.length());
			} else {
				log.warn("OpenAI message content is null or empty");
			}
		} else {
			log.warn("OpenAI choice has no message");
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private AISearchResult parseAnthropicResponse(Map<String, Object> response) {
		AISearchResult result = new AISearchResult();
		if (response == null || !response.containsKey("content")) {
			return result;
		}

		List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
		if (content == null || content.isEmpty()) {
			return result;
		}

		Map<String, Object> firstContent = content.get(0);
		result.setContent((String) firstContent.get("text"));

		return result;
	}

	public static class AISearchResult {
		private String content;
		private List<String> links = new ArrayList<>();

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
			// Extract links from content
			if (content != null) {
				java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("https?://[^\\s]+");
				java.util.regex.Matcher matcher = pattern.matcher(content);
				while (matcher.find()) {
					links.add(matcher.group());
				}
			}
		}

		public List<String> getLinks() {
			return links;
		}
	}
}

