package com.example.CalCol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "calculator_labels", 
	uniqueConstraints = @UniqueConstraint(columnNames = {"calculator_id", "label_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"calculator", "label"})
@EqualsAndHashCode(exclude = {"calculator", "label"})
public class CalculatorLabel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "calculator_id", nullable = false)
	private Calculator calculator;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "label_id", nullable = false)
	private Label label;
}

