package com.example.CalCol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "shared_collection_calculators")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"sharedCollection", "calculator"})
@EqualsAndHashCode(exclude = {"sharedCollection", "calculator"})
public class SharedCollectionCalculator {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shared_collection_id", nullable = false)
	private SharedCollection sharedCollection;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "calculator_id", nullable = false)
	private Calculator calculator;
}

