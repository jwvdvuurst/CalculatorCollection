package com.example.CalCol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_calculator_collections", 
	uniqueConstraints = @UniqueConstraint(columnNames = {"username", "calculator_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "calculator")
@EqualsAndHashCode(exclude = "calculator")
public class UserCalculatorCollection {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String username;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "calculator_id", nullable = false)
	private Calculator calculator;

	@Column(name = "added_at", nullable = false)
	private LocalDateTime addedAt;

	@PrePersist
	protected void onCreate() {
		addedAt = LocalDateTime.now();
	}
}

