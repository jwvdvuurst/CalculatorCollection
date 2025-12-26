package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.Manufacturer;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.ManufacturerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializationService implements CommandLineRunner {

	private final ManufacturerRepository manufacturerRepository;
	private final CalculatorRepository calculatorRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	@Transactional
	public void run(String... args) {
		if (manufacturerRepository.count() > 0) {
			log.info("Database already contains data. Skipping initialization.");
			return;
		}

		log.info("Starting data initialization from calculators.json...");
		File jsonFile = new File("calculators.json");

		if (!jsonFile.exists()) {
			log.warn("calculators.json file not found. Skipping data initialization.");
			return;
		}

		try {
			JsonNode rootNode = objectMapper.readTree(jsonFile);
			JsonNode manufacturersNode = rootNode.get("manufacturers");

			if (manufacturersNode == null || !manufacturersNode.isArray()) {
				log.error("Invalid JSON structure: 'manufacturers' array not found.");
				return;
			}

			Map<String, Manufacturer> manufacturerCache = new HashMap<>();
			int totalManufacturers = manufacturersNode.size();
			int processedCount = 0;

			for (JsonNode manufacturerNode : manufacturersNode) {
				String manufacturerField = manufacturerNode.get("manufacturer").asText();
				JsonNode modelsNode = manufacturerNode.get("models");

				if (modelsNode == null || !modelsNode.isArray()) {
					continue;
				}

				// Split manufacturer string on colon to extract actual manufacturer name
				String actualManufacturerName;
				String modelSuffix = null;
				if (manufacturerField.contains(":")) {
					int colonIndex = manufacturerField.indexOf(":");
					actualManufacturerName = manufacturerField.substring(0, colonIndex).trim();
					modelSuffix = manufacturerField.substring(colonIndex + 1).trim();
				} else {
					actualManufacturerName = manufacturerField.trim();
				}

				Manufacturer manufacturer = manufacturerCache.computeIfAbsent(actualManufacturerName, name -> {
					Manufacturer m = new Manufacturer();
					m.setName(name);
					return manufacturerRepository.save(m);
				});

				for (JsonNode modelNode : modelsNode) {
					Calculator calculator = new Calculator();
					
					// Get model name from JSON
					String modelName = getTextValue(modelNode, "model");
					
					// If model name is empty/null or looks like just a year (starts with ~), 
					// use the suffix from manufacturer field instead
					if (modelName == null || modelName.trim().isEmpty() || 
						(modelName.trim().startsWith("~") && modelSuffix != null)) {
						modelName = modelSuffix;
					}
					// If model name exists and is meaningful, use it as-is
					// (modelSuffix is ignored in this case to avoid duplication)
					
					calculator.setModel(modelName);
					calculator.setSoldFrom(getIntValue(modelNode, "sold_from"));
					calculator.setSoldTo(getIntValue(modelNode, "sold_to"));
					calculator.setSourceUrl(getTextValue(modelNode, "source_url"));
					calculator.setRawRowText(getTextValue(modelNode, "raw_row_text"));
					calculator.setManufacturer(manufacturer);

					calculatorRepository.save(calculator);
				}

				processedCount++;
				if (processedCount % 100 == 0) {
					log.info("Processed {}/{} manufacturers...", processedCount, totalManufacturers);
				}
			}

			log.info("Data initialization completed. Loaded {} manufacturers and {} calculators.",
					manufacturerRepository.count(), calculatorRepository.count());

		} catch (IOException e) {
			log.error("Error reading calculators.json file: {}", e.getMessage(), e);
		} catch (Exception e) {
			log.error("Error during data initialization: {}", e.getMessage(), e);
		}
	}

	private String getTextValue(JsonNode node, String fieldName) {
		JsonNode fieldNode = node.get(fieldName);
		if (fieldNode == null || fieldNode.isNull()) {
			return null;
		}
		return fieldNode.asText();
	}

	private Integer getIntValue(JsonNode node, String fieldName) {
		JsonNode fieldNode = node.get(fieldName);
		if (fieldNode == null || fieldNode.isNull()) {
			return null;
		}
		return fieldNode.asInt();
	}
}

