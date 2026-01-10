package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.CalculatorLabel;
import com.example.CalCol.entity.Label;
import com.example.CalCol.repository.CalculatorLabelRepository;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabelDerivationService {

	private final CalculatorRepository calculatorRepository;
	private final LabelRepository labelRepository;
	private final CalculatorLabelRepository calculatorLabelRepository;
	private final LabelService labelService;

	// Define label keywords and their corresponding label names
	private static final Map<String, String> LABEL_KEYWORDS = createLabelKeywords();

	private static Map<String, String> createLabelKeywords() {
		Map<String, String> keywords = new HashMap<>();
		keywords.put("classroom", "Classroom");
		keywords.put("scientific", "Scientific");
		keywords.put("graphic", "Graphic");
		keywords.put("statistical", "Statistical");
		keywords.put("business", "Business");
		keywords.put("led", "LED");
		keywords.put("lcd", "LCD");
		keywords.put("vfd", "VFD");
		keywords.put("oled", "OLED");
		keywords.put("programmable", "Programmable");
		keywords.put("financial", "Financial");
		keywords.put("printing", "Printing");
		keywords.put("pocket", "Pocket");
		keywords.put("desktop", "Desktop");
		keywords.put("rpn", "RPN");
		keywords.put("algebraic", "Algebraic");
		keywords.put("mechanical", "Mechanical");
		keywords.put("electromechanical", "Electromechanical");
		return keywords;
	}

	@Transactional
	public int deriveLabelsFromCalculators() {
		log.info("Starting label derivation from calculator data...");
		
		int labelsCreated = 0;
		int labelsAssigned = 0;

		// Create all curated labels if they don't exist
		for (Map.Entry<String, String> entry : LABEL_KEYWORDS.entrySet()) {
			String labelName = entry.getValue();
			
			Optional<Label> existingLabel = labelRepository.findByName(labelName);
			Label label;
			if (existingLabel.isEmpty()) {
				label = labelService.createCuratedLabel(labelName, 
					"Automatically derived from calculator data");
				labelsCreated++;
				log.info("Created label: {}", labelName);
			} else {
				label = existingLabel.get();
				// Ensure it's marked as curated
				if (!label.getIsCurated()) {
					label.setIsCurated(true);
					label = labelRepository.save(label);
				}
			}
		}

		// Process all calculators (eagerly fetch manufacturers to avoid N+1 query problem)
		List<Calculator> allCalculators = calculatorRepository.findAllWithManufacturer();
		log.info("Processing {} calculators for label assignment...", allCalculators.size());

		for (Calculator calculator : allCalculators) {
			try {
				Set<String> matchedLabels = new HashSet<>();
				String searchText = buildSearchText(calculator);

				// Check each keyword
				for (Map.Entry<String, String> entry : LABEL_KEYWORDS.entrySet()) {
					if (containsKeyword(searchText, entry.getKey())) {
						matchedLabels.add(entry.getValue());
					}
				}

				// Assign matched labels to calculator
				for (String labelName : matchedLabels) {
					Optional<Label> labelOpt = labelRepository.findByName(labelName);
					if (labelOpt.isPresent()) {
						Label label = labelOpt.get();
						
						// Check if already assigned
						Optional<CalculatorLabel> existing = calculatorLabelRepository
							.findByCalculatorIdAndLabelId(calculator.getId(), label.getId());
						
						if (existing.isEmpty()) {
							CalculatorLabel calculatorLabel = new CalculatorLabel();
							calculatorLabel.setCalculator(calculator);
							calculatorLabel.setLabel(label);
							calculatorLabelRepository.save(calculatorLabel);
							labelsAssigned++;
						}
					}
				}
			} catch (Exception e) {
				log.error("Error processing calculator ID {}: {}", calculator.getId(), e.getMessage(), e);
				// Continue with next calculator instead of failing completely
			}
		}

		log.info("Label derivation completed. Created {} labels, assigned {} label-calculator relationships.",
			labelsCreated, labelsAssigned);

		return labelsAssigned;
	}

	private String buildSearchText(Calculator calculator) {
		StringBuilder sb = new StringBuilder();
		
		try {
			if (calculator.getModel() != null) {
				sb.append(calculator.getModel().toLowerCase()).append(" ");
			}
			// Manufacturer should be eagerly loaded, but add null check for safety
			if (calculator.getManufacturer() != null) {
				String manufacturerName = calculator.getManufacturer().getName();
				if (manufacturerName != null) {
					sb.append(manufacturerName.toLowerCase()).append(" ");
				}
			}
			if (calculator.getRawRowText() != null) {
				sb.append(calculator.getRawRowText().toLowerCase()).append(" ");
			}
		} catch (Exception e) {
			log.warn("Error building search text for calculator ID {}: {}", calculator.getId(), e.getMessage());
		}
		
		return sb.toString();
	}

	private boolean containsKeyword(String text, String keyword) {
		if (text == null || keyword == null) {
			return false;
		}
		// Case-insensitive search, with word boundaries
		String lowerText = text.toLowerCase();
		String lowerKeyword = keyword.toLowerCase();
		
		// Check for exact word match or as part of a word
		return lowerText.contains(lowerKeyword);
	}
}

