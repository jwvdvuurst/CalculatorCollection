package com.example.CalCol.dto;

import lombok.Data;

import java.util.Map;

/**
 * DTO for Collection Statistics
 */
@Data
public class CollectionStatisticsDTO {
	private Long totalCalculators;
	private Map<String, Long> calculatorsByManufacturer;
	private Map<String, Long> calculatorsByPeriod;
	private Map<String, Long> calculatorsByLabel;
}

