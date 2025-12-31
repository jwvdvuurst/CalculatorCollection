package com.example.CalCol.service;

import com.example.CalCol.dto.*;
import com.example.CalCol.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for mapping entities to DTOs
 */
@Service
@RequiredArgsConstructor
public class DtoMapperService {

	@Value("${app.base-url:}")
	private String baseUrl;

	public CalculatorDTO toCalculatorDTO(Calculator calculator, List<Label> labels, 
			List<CalculatorImage> images, List<CalculatorLink> links) {
		CalculatorDTO dto = new CalculatorDTO();
		dto.setId(calculator.getId());
		dto.setModel(calculator.getModel());
		dto.setManufacturer(calculator.getManufacturer().getName());
		dto.setManufacturerId(calculator.getManufacturer().getId());
		dto.setSoldFrom(calculator.getSoldFrom());
		dto.setSoldTo(calculator.getSoldTo());
		dto.setSourceUrl(calculator.getSourceUrl());
		dto.setRawRowText(calculator.getRawRowText());
		
		if (labels != null) {
			dto.setLabels(labels.stream().map(this::toLabelDTO).collect(Collectors.toList()));
		}
		
		if (images != null) {
			dto.setImages(images.stream().map(this::toImageDTO).collect(Collectors.toList()));
		}
		
		if (links != null) {
			dto.setLinks(links.stream().map(this::toLinkDTO).collect(Collectors.toList()));
		}
		
		return dto;
	}

	public LabelDTO toLabelDTO(Label label) {
		LabelDTO dto = new LabelDTO();
		dto.setId(label.getId());
		dto.setName(label.getName());
		dto.setDescription(label.getDescription());
		dto.setIsCurated(label.getIsCurated());
		return dto;
	}

	public ImageDTO toImageDTO(CalculatorImage image) {
		ImageDTO dto = new ImageDTO();
		dto.setId(image.getId());
		dto.setImagePath(image.getImagePath());
		dto.setImageUrl(buildImageUrl(image.getImagePath()));
		dto.setUploadedBy(image.getUploadedBy());
		dto.setIsProposal(image.getIsProposal());
		dto.setIsApproved(image.getIsApproved());
		dto.setApprovedBy(image.getApprovedBy());
		dto.setUploadedAt(image.getUploadedAt());
		dto.setApprovedAt(image.getApprovedAt());
		return dto;
	}

	public LinkDTO toLinkDTO(CalculatorLink link) {
		LinkDTO dto = new LinkDTO();
		dto.setId(link.getId());
		dto.setUrl(link.getUrl());
		dto.setTitle(link.getTitle());
		dto.setDescription(link.getDescription());
		dto.setAddedBy(link.getAddedBy());
		dto.setAddedAt(link.getAddedAt());
		return dto;
	}

	public UserProfileDTO toUserProfileDTO(AppUser user) {
		UserProfileDTO dto = new UserProfileDTO();
		dto.setId(user.getId());
		dto.setUsername(user.getUsername());
		dto.setEmail(user.getEmail());
		dto.setRole(user.getRole());
		dto.setEnabled(user.getEnabled());
		dto.setCreatedAt(user.getCreatedAt());
		dto.setLastLogin(user.getLastLogin());
		return dto;
	}

	public SocialMediaPostDTO toSocialMediaPostDTO(SocialMediaPostService.SocialMediaPost post) {
		SocialMediaPostDTO dto = new SocialMediaPostDTO();
		dto.setPlatform(post.getPlatform());
		dto.setContent(post.getContent());
		dto.setMaxLength(post.getMaxLength());
		dto.setCurrentLength(post.getContent() != null ? post.getContent().length() : 0);
		return dto;
	}

	public EnrichmentDTO toEnrichmentDTO(SocialMediaPostService.EnrichmentData enrichment) {
		EnrichmentDTO dto = new EnrichmentDTO();
		
		if (enrichment.getWebResults() != null) {
			dto.setWebResults(enrichment.getWebResults().stream()
				.map(this::toWebSearchResultDTO)
				.collect(Collectors.toList()));
		}
		
		if (enrichment.getMuseumResults() != null) {
			dto.setMuseumResults(enrichment.getMuseumResults().stream()
				.map(this::toMuseumSearchResultDTO)
				.collect(Collectors.toList()));
		}
		
		if (enrichment.getAiContent() != null) {
			dto.setAiContent(toAISearchResultDTO(enrichment.getAiContent()));
		}
		
		if (enrichment.getBraveAIResult() != null) {
			dto.setBraveAIResult(toBraveAIResultDTO(enrichment.getBraveAIResult()));
		}
		
		return dto;
	}

	private WebSearchResultDTO toWebSearchResultDTO(WebSearchService.SearchResult result) {
		WebSearchResultDTO dto = new WebSearchResultDTO();
		dto.setTitle(result.getTitle());
		dto.setUrl(result.getUrl());
		dto.setSnippet(result.getSnippet());
		return dto;
	}

	private MuseumSearchResultDTO toMuseumSearchResultDTO(CalculatorMuseumSearchService.MuseumSearchResult result) {
		MuseumSearchResultDTO dto = new MuseumSearchResultDTO();
		dto.setSiteUrl(result.getSiteUrl());
		dto.setSearchUrl(result.getSearchUrl());
		dto.setFound(result.getFound());
		dto.setSnippet(result.getSnippet());
		return dto;
	}

	private AISearchResultDTO toAISearchResultDTO(AISearchService.AISearchResult result) {
		AISearchResultDTO dto = new AISearchResultDTO();
		dto.setContent(result.getContent());
		dto.setLinks(result.getLinks());
		return dto;
	}

	private com.example.CalCol.dto.BraveAIResultDTO toBraveAIResultDTO(WebSearchService.BraveAIResult result) {
		com.example.CalCol.dto.BraveAIResultDTO dto = new com.example.CalCol.dto.BraveAIResultDTO();
		dto.setManufacturer(result.getManufacturer());
		dto.setModel(result.getModel());
		dto.setRawResponse(result.getRawResponse());
		dto.setSourceUrl(result.getSourceUrl());
		dto.setStructuredData(result.getStructuredData());
		return dto;
	}

	private String buildImageUrl(String imagePath) {
		if (baseUrl != null && !baseUrl.isEmpty()) {
			return baseUrl + "/uploads/" + imagePath;
		}
		return ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/uploads/")
			.path(imagePath)
			.toUriString();
	}
}

