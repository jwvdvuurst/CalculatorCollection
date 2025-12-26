package com.example.CalCol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "calculator_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "calculator")
@EqualsAndHashCode(exclude = "calculator")
public class CalculatorLink {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "calculator_id", nullable = false)
	private Calculator calculator;

	@Column(nullable = false, length = 500)
	private String url;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 500)
	private String description;

	@Column(name = "added_by", nullable = false, length = 100)
	private String addedBy;

	@Column(name = "added_at", nullable = false)
	private LocalDateTime addedAt;

	@PrePersist
	protected void onCreate() {
		if (addedAt == null) {
			addedAt = LocalDateTime.now();
		}
	}
}

