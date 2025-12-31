package com.example.CalCol.dto;

import lombok.Data;

/**
 * DTO for Label information
 */
@Data
public class LabelDTO {
	private Long id;
	private String name;
	private String description;
	private Boolean isCurated;
}

