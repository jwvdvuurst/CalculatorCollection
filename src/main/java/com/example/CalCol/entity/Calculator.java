package com.example.CalCol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "calculators")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "manufacturer")
@EqualsAndHashCode(exclude = "manufacturer")
public class Calculator {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 500)
	private String model;

	@Column(name = "sold_from")
	private Integer soldFrom;

	@Column(name = "sold_to")
	private Integer soldTo;

	@Column(name = "source_url", length = 1000)
	private String sourceUrl;

	@Column(name = "raw_row_text", length = 2000)
	private String rawRowText;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "enriched_data", columnDefinition = "TEXT")
	private String enrichedData; // JSON string with structured enriched data

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "manufacturer_id", nullable = false)
	private Manufacturer manufacturer;
}

