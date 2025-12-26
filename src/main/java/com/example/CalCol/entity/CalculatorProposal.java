package com.example.CalCol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "calculator_proposals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculatorProposal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 500)
	private String model;

	@Column(name = "manufacturer_name", nullable = false, length = 500)
	private String manufacturerName;

	@Column(name = "sold_from")
	private Integer soldFrom;

	@Column(name = "sold_to")
	private Integer soldTo;

	@Column(name = "source_url", length = 1000)
	private String sourceUrl;

	@Column(name = "raw_row_text", length = 2000)
	private String rawRowText;

	@Column(name = "proposed_by", nullable = false, length = 100)
	private String proposedBy;

	@Column(name = "proposed_at", nullable = false)
	private LocalDateTime proposedAt;

	@Column(name = "is_approved", nullable = false)
	private Boolean isApproved = false;

	@Column(name = "approved_by", length = 100)
	private String approvedBy;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(length = 1000)
	private String notes;

	@PrePersist
	protected void onCreate() {
		if (proposedAt == null) {
			proposedAt = LocalDateTime.now();
		}
	}
}

