package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.Label;
import com.example.CalCol.entity.UserCalculatorCollection;
import com.example.CalCol.repository.CalculatorLabelRepository;
import com.example.CalCol.repository.CalculatorLinkRepository;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.LabelRepository;
import com.example.CalCol.repository.ManufacturerRepository;
import com.example.CalCol.repository.UserCalculatorCollectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

	private final CalculatorRepository calculatorRepository;
	private final ManufacturerRepository manufacturerRepository;
	private final UserCalculatorCollectionRepository userCollectionRepository;
	private final CalculatorLinkRepository linkRepository;
	private final CalculatorLabelRepository calculatorLabelRepository;
	private final LabelRepository labelRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public String exportAllDataAsJson() throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		
		// Export manufacturers
		ArrayNode manufacturers = root.putArray("manufacturers");
		manufacturerRepository.findAll().forEach(mfg -> {
			ObjectNode mfgNode = manufacturers.addObject();
			mfgNode.put("id", mfg.getId());
			mfgNode.put("name", mfg.getName());
		});

		// Export calculators
		ArrayNode calculators = root.putArray("calculators");
		calculatorRepository.findAll().forEach(calc -> {
			ObjectNode calcNode = calculators.addObject();
			calcNode.put("id", calc.getId());
			calcNode.put("model", calc.getModel());
			calcNode.put("manufacturerId", calc.getManufacturer().getId());
			if (calc.getSoldFrom() != null) calcNode.put("soldFrom", calc.getSoldFrom());
			if (calc.getSoldTo() != null) calcNode.put("soldTo", calc.getSoldTo());
			if (calc.getSourceUrl() != null) calcNode.put("sourceUrl", calc.getSourceUrl());
			if (calc.getRawRowText() != null) calcNode.put("rawRowText", calc.getRawRowText());
		});

		// Export labels
		ArrayNode labels = root.putArray("labels");
		labelRepository.findAll().forEach(label -> {
			ObjectNode labelNode = labels.addObject();
			labelNode.put("id", label.getId());
			labelNode.put("name", label.getName());
			labelNode.put("isCurated", label.getIsCurated());
			if (label.getDescription() != null) labelNode.put("description", label.getDescription());
		});

		// Export calculator labels
		ArrayNode calculatorLabels = root.putArray("calculatorLabels");
		calculatorLabelRepository.findAll().forEach(cl -> {
			ObjectNode clNode = calculatorLabels.addObject();
			clNode.put("calculatorId", cl.getCalculator().getId());
			clNode.put("labelId", cl.getLabel().getId());
		});

		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
	}

	public String exportUserCollectionAsJson(String username) throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("username", username);
		root.put("exportDate", java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

		ArrayNode collection = root.putArray("collection");
		userCollectionRepository.findByUsernameOrderByAddedAtDesc(username, 
			org.springframework.data.domain.Pageable.unpaged())
			.getContent()
			.forEach(item -> {
				ObjectNode itemNode = collection.addObject();
				Calculator calc = item.getCalculator();
				itemNode.put("calculatorId", calc.getId());
				itemNode.put("model", calc.getModel());
				itemNode.put("manufacturer", calc.getManufacturer().getName());
				if (calc.getSoldFrom() != null) itemNode.put("soldFrom", calc.getSoldFrom());
				if (calc.getSoldTo() != null) itemNode.put("soldTo", calc.getSoldTo());
				itemNode.put("addedAt", item.getAddedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

				// Include labels
				ArrayNode labels = itemNode.putArray("labels");
				calculatorLabelRepository.findLabelsByCalculatorId(calc.getId())
					.forEach(label -> labels.add(label.getName()));

				// Include links
				ArrayNode links = itemNode.putArray("links");
				linkRepository.findByCalculatorId(calc.getId())
					.forEach(link -> {
						ObjectNode linkNode = links.addObject();
						linkNode.put("url", link.getUrl());
						linkNode.put("title", link.getTitle());
						if (link.getDescription() != null) linkNode.put("description", link.getDescription());
					});
			});

		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
	}

	public String exportUserCollectionAsCsv(String username) {
		StringBuilder csv = new StringBuilder();
		csv.append("Calculator ID,Model,Manufacturer,Sold From,Sold To,Added At,Labels,Links\n");

		userCollectionRepository.findByUsernameOrderByAddedAtDesc(username, 
			org.springframework.data.domain.Pageable.unpaged())
			.getContent()
			.forEach(item -> {
				Calculator calc = item.getCalculator();
				csv.append(calc.getId()).append(",");
				csv.append(escapeCsv(calc.getModel())).append(",");
				csv.append(escapeCsv(calc.getManufacturer().getName())).append(",");
				csv.append(calc.getSoldFrom() != null ? calc.getSoldFrom() : "").append(",");
				csv.append(calc.getSoldTo() != null ? calc.getSoldTo() : "").append(",");
				csv.append(item.getAddedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
				
				// Labels
				String labels = calculatorLabelRepository.findLabelsByCalculatorId(calc.getId())
					.stream()
					.map(Label::getName)
					.collect(Collectors.joining("; "));
				csv.append(escapeCsv(labels)).append(",");
				
				// Links
				String links = linkRepository.findByCalculatorId(calc.getId())
					.stream()
					.map(l -> l.getTitle() + " (" + l.getUrl() + ")")
					.collect(Collectors.joining("; "));
				csv.append(escapeCsv(links)).append("\n");
			});

		return csv.toString();
	}

	private String escapeCsv(String value) {
		if (value == null) return "";
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}
}

