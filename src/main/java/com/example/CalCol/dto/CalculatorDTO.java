package com.example.CalCol.dto;

import lombok.Data;

import java.util.List;

/**
 * DTO for Calculator information
 */
@Data
public class CalculatorDTO {
	private Long id;
	private String model;
	private String manufacturer;
	private Long manufacturerId;
	private Integer soldFrom;
	private Integer soldTo;
	private String sourceUrl;
	private String rawRowText;
	private java.math.BigDecimal currentPrice;
	private String priceCurrency;
	private java.time.LocalDateTime priceLastUpdated;
	private List<LabelDTO> labels;
	private List<ImageDTO> images;
	private List<LinkDTO> links;
}

