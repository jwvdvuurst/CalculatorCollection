package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.Label;
import com.example.CalCol.repository.CalculatorLabelRepository;
import com.example.CalCol.repository.UserCalculatorCollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

	private final UserCalculatorCollectionRepository userCollectionRepository;
	private final CalculatorLabelRepository calculatorLabelRepository;

	public Map<String, Object> getCollectionStatistics(String username) {
		Map<String, Object> stats = new HashMap<>();

		// Get all calculators in user's collection
		var collections = userCollectionRepository.findByUsernameOrderByAddedAtDesc(username, 
			org.springframework.data.domain.Pageable.unpaged()).getContent();

		List<Calculator> calculators = collections.stream()
			.map(uc -> uc.getCalculator())
			.collect(Collectors.toList());

		// Statistics by Manufacturer
		Map<String, Long> byManufacturer = calculators.stream()
			.collect(Collectors.groupingBy(
				calc -> calc.getManufacturer().getName(),
				Collectors.counting()
			));
		stats.put("byManufacturer", byManufacturer);

		// Statistics by Time Period
		Map<String, Long> byPeriod = new LinkedHashMap<>();
		byPeriod.put("Unknown", calculators.stream()
			.filter(calc -> calc.getSoldFrom() == null && calc.getSoldTo() == null)
			.count());
		byPeriod.put("Pre-1970", calculators.stream()
			.filter(calc -> calc.getSoldTo() != null && calc.getSoldTo() < 1970)
			.count());
		byPeriod.put("1970s", calculators.stream()
			.filter(calc -> {
				Integer from = calc.getSoldFrom() != null ? calc.getSoldFrom() : calc.getSoldTo();
				Integer to = calc.getSoldTo() != null ? calc.getSoldTo() : calc.getSoldFrom();
				return from != null && to != null && 
					((from >= 1970 && from < 1980) || (to >= 1970 && to < 1980) ||
					 (from < 1970 && to >= 1980));
			})
			.count());
		byPeriod.put("1980s", calculators.stream()
			.filter(calc -> {
				Integer from = calc.getSoldFrom() != null ? calc.getSoldFrom() : calc.getSoldTo();
				Integer to = calc.getSoldTo() != null ? calc.getSoldTo() : calc.getSoldFrom();
				return from != null && to != null && 
					((from >= 1980 && from < 1990) || (to >= 1980 && to < 1990) ||
					 (from < 1980 && to >= 1990));
			})
			.count());
		byPeriod.put("1990s", calculators.stream()
			.filter(calc -> {
				Integer from = calc.getSoldFrom() != null ? calc.getSoldFrom() : calc.getSoldTo();
				Integer to = calc.getSoldTo() != null ? calc.getSoldTo() : calc.getSoldFrom();
				return from != null && to != null && 
					((from >= 1990 && from < 2000) || (to >= 1990 && to < 2000) ||
					 (from < 1990 && to >= 2000));
			})
			.count());
		byPeriod.put("2000s", calculators.stream()
			.filter(calc -> {
				Integer from = calc.getSoldFrom() != null ? calc.getSoldFrom() : calc.getSoldTo();
				Integer to = calc.getSoldTo() != null ? calc.getSoldTo() : calc.getSoldFrom();
				return from != null && to != null && 
					((from >= 2000 && from < 2010) || (to >= 2000 && to < 2010) ||
					 (from < 2000 && to >= 2010));
			})
			.count());
		byPeriod.put("2010s+", calculators.stream()
			.filter(calc -> {
				Integer from = calc.getSoldFrom() != null ? calc.getSoldFrom() : calc.getSoldTo();
				return from != null && from >= 2010;
			})
			.count());
		stats.put("byPeriod", byPeriod);

		// Statistics by Labels
		Map<String, Long> byLabel = new HashMap<>();
		for (Calculator calc : calculators) {
			List<Label> labels = calculatorLabelRepository.findLabelsByCalculatorId(calc.getId());
			for (Label label : labels) {
				byLabel.put(label.getName(), byLabel.getOrDefault(label.getName(), 0L) + 1);
			}
		}
		stats.put("byLabel", byLabel);

		// Total count
		stats.put("total", (long) calculators.size());

		return stats;
	}
}

