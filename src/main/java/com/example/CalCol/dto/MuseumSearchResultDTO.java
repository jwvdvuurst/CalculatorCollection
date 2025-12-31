package com.example.CalCol.dto;

import lombok.Data;

/**
 * DTO for Museum Search Result
 */
@Data
public class MuseumSearchResultDTO {
	private String siteUrl;
	private String searchUrl;
	private Boolean found;
	private String snippet;
}

