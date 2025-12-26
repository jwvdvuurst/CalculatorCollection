package com.example.CalCol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "calculator_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "calculator")
@EqualsAndHashCode(exclude = "calculator")
public class CalculatorImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "calculator_id", nullable = false)
	private Calculator calculator;

	@Column(nullable = false, length = 500)
	private String imagePath;

	@Column(name = "uploaded_by", nullable = false, length = 100)
	private String uploadedBy;

	@Column(name = "is_proposal", nullable = false)
	private Boolean isProposal = true;

	@Column(name = "is_approved", nullable = false)
	private Boolean isApproved = false;

	@Column(name = "approved_by", length = 100)
	private String approvedBy;

	@Column(name = "uploaded_at", nullable = false)
	private LocalDateTime uploadedAt;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@PrePersist
	protected void onCreate() {
		if (uploadedAt == null) {
			uploadedAt = LocalDateTime.now();
		}
	}
}

