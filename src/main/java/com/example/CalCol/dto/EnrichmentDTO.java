package com.example.CalCol.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO for Enrichment Data
 */
@Data
public class EnrichmentDTO {
	private List<WebSearchResultDTO> webResults;
	private List<MuseumSearchResultDTO> museumResults;
	private AISearchResultDTO aiContent;
	private BraveAIResultDTO braveAIResult;
}

