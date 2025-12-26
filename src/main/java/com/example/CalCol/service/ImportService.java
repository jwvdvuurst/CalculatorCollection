package com.example.CalCol.service;

import com.example.CalCol.entity.*;
import com.example.CalCol.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

	private final CalculatorRepository calculatorRepository;
	private final ManufacturerRepository manufacturerRepository;
	private final UserCalculatorCollectionRepository userCollectionRepository;
	private final CalculatorLabelRepository calculatorLabelRepository;
	private final LabelRepository labelRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Transactional
	public int importUserCollection(String jsonData, String username) throws Exception {
		JsonNode root = objectMapper.readTree(jsonData);
		JsonNode collectionNode = root.get("collection");

		if (collectionNode == null || !collectionNode.isArray()) {
			throw new IllegalArgumentException("Invalid collection format");
		}

		int imported = 0;
		for (JsonNode itemNode : collectionNode) {
			Long calculatorId = itemNode.get("calculatorId").asLong();
			
			// Check if calculator exists
			Optional<Calculator> calcOpt = calculatorRepository.findById(calculatorId);
			if (calcOpt.isEmpty()) {
				log.warn("Calculator with ID {} not found, skipping", calculatorId);
				continue;
			}

			// Add to user's collection if not already present
			if (!userCollectionRepository.existsByUsernameAndCalculatorId(username, calculatorId)) {
				UserCalculatorCollection collection = new UserCalculatorCollection();
				collection.setUsername(username);
				collection.setCalculator(calcOpt.get());
				userCollectionRepository.save(collection);
				imported++;
			}

			// Import labels if present
			JsonNode labelsNode = itemNode.get("labels");
			if (labelsNode != null && labelsNode.isArray()) {
				for (JsonNode labelNode : labelsNode) {
					String labelName = labelNode.asText();
					Optional<Label> labelOpt = labelRepository.findByName(labelName);
					if (labelOpt.isPresent()) {
						// Check if already assigned
						Optional<CalculatorLabel> existing = calculatorLabelRepository
							.findByCalculatorIdAndLabelId(calculatorId, labelOpt.get().getId());
						if (existing.isEmpty()) {
							CalculatorLabel cl = new CalculatorLabel();
							cl.setCalculator(calcOpt.get());
							cl.setLabel(labelOpt.get());
							calculatorLabelRepository.save(cl);
						}
					}
				}
			}
		}

		return imported;
	}

	@Transactional
	public ImportResult importAllData(String jsonData) throws Exception {
		JsonNode root = objectMapper.readTree(jsonData);
		ImportResult result = new ImportResult();

		// Import manufacturers
		Map<Long, Long> manufacturerIdMap = new HashMap<>();
		JsonNode manufacturersNode = root.get("manufacturers");
		if (manufacturersNode != null && manufacturersNode.isArray()) {
			for (JsonNode mfgNode : manufacturersNode) {
				String name = mfgNode.get("name").asText();
				Optional<Manufacturer> existing = manufacturerRepository.findByName(name);
				if (existing.isEmpty()) {
					Manufacturer mfg = new Manufacturer();
					mfg.setName(name);
					mfg = manufacturerRepository.save(mfg);
					manufacturerIdMap.put(mfgNode.get("id").asLong(), mfg.getId());
					result.manufacturersCreated++;
				} else {
					manufacturerIdMap.put(mfgNode.get("id").asLong(), existing.get().getId());
				}
			}
		}

		// Import calculators
		JsonNode calculatorsNode = root.get("calculators");
		if (calculatorsNode != null && calculatorsNode.isArray()) {
			for (JsonNode calcNode : calculatorsNode) {
				String model = calcNode.get("model").asText();
				Long manufacturerId = manufacturerIdMap.get(calcNode.get("manufacturerId").asLong());
				
				if (manufacturerId == null) {
					log.warn("Manufacturer not found for calculator: {}", model);
					continue;
				}

				Optional<Manufacturer> mfgOpt = manufacturerRepository.findById(manufacturerId);
				if (mfgOpt.isEmpty()) {
					log.warn("Manufacturer with ID {} not found", manufacturerId);
					continue;
				}

				// Check if calculator already exists (by model and manufacturer)
				boolean exists = calculatorRepository.findAll().stream()
					.anyMatch(c -> c.getModel().equals(model) && 
						c.getManufacturer().getId().equals(manufacturerId));

				if (!exists) {
					Calculator calc = new Calculator();
					calc.setModel(model);
					calc.setManufacturer(mfgOpt.get());
					if (calcNode.has("soldFrom")) calc.setSoldFrom(calcNode.get("soldFrom").asInt());
					if (calcNode.has("soldTo")) calc.setSoldTo(calcNode.get("soldTo").asInt());
					if (calcNode.has("sourceUrl")) calc.setSourceUrl(calcNode.get("sourceUrl").asText());
					if (calcNode.has("rawRowText")) calc.setRawRowText(calcNode.get("rawRowText").asText());
					calculatorRepository.save(calc);
					result.calculatorsCreated++;
				}
			}
		}

		// Import labels
		Map<Long, Long> labelIdMap = new HashMap<>();
		JsonNode labelsNode = root.get("labels");
		if (labelsNode != null && labelsNode.isArray()) {
			for (JsonNode labelNode : labelsNode) {
				String name = labelNode.get("name").asText();
				Optional<Label> existing = labelRepository.findByName(name);
				if (existing.isEmpty()) {
					Label label = new Label();
					label.setName(name);
					label.setIsCurated(labelNode.get("isCurated").asBoolean());
					if (labelNode.has("description")) {
						label.setDescription(labelNode.get("description").asText());
					}
					label = labelRepository.save(label);
					labelIdMap.put(labelNode.get("id").asLong(), label.getId());
					result.labelsCreated++;
				} else {
					labelIdMap.put(labelNode.get("id").asLong(), existing.get().getId());
				}
			}
		}

		return result;
	}

	public static class ImportResult {
		public int manufacturersCreated = 0;
		public int calculatorsCreated = 0;
		public int labelsCreated = 0;
	}
}

