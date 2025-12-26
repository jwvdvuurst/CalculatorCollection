package com.example.CalCol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "manufacturers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "calculators")
@EqualsAndHashCode(exclude = "calculators")
public class Manufacturer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 500)
	private String name;

	@OneToMany(mappedBy = "manufacturer", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Calculator> calculators = new ArrayList<>();
}

