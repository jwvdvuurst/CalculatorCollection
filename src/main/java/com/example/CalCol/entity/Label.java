package com.example.CalCol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "labels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Label {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Column(name = "is_curated", nullable = false)
	private Boolean isCurated = false;

	@Column(length = 500)
	private String description;
}

