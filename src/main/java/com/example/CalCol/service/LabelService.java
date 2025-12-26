package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.CalculatorLabel;
import com.example.CalCol.entity.Label;
import com.example.CalCol.repository.CalculatorLabelRepository;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelService {

	private final LabelRepository labelRepository;
	private final CalculatorLabelRepository calculatorLabelRepository;
	private final CalculatorRepository calculatorRepository;

	public List<Label> getAllCuratedLabels() {
		return labelRepository.findByIsCuratedTrue();
	}

	public List<Label> searchLabels(String search) {
		if (search == null || search.trim().isEmpty()) {
			return labelRepository.findAll();
		}
		return labelRepository.searchByName(search.trim());
	}

	public List<Label> getCalculatorLabels(Long calculatorId) {
		return calculatorLabelRepository.findLabelsByCalculatorId(calculatorId);
	}

	@Transactional
	public Label createOrGetLabel(String labelName, boolean isCurated) {
		Optional<Label> existing = labelRepository.findByName(labelName.trim());
		if (existing.isPresent()) {
			return existing.get();
		}

		Label label = new Label();
		label.setName(labelName.trim());
		label.setIsCurated(isCurated);
		return labelRepository.save(label);
	}

	@Transactional
	public boolean addLabelToCalculator(Long calculatorId, Long labelId) {
		Optional<Calculator> calculatorOpt = calculatorRepository.findById(calculatorId);
		Optional<Label> labelOpt = labelRepository.findById(labelId);

		if (calculatorOpt.isEmpty() || labelOpt.isEmpty()) {
			return false;
		}

		// Check if already exists
		Optional<CalculatorLabel> existing = calculatorLabelRepository
			.findByCalculatorIdAndLabelId(calculatorId, labelId);
		if (existing.isPresent()) {
			return false; // Already labeled
		}

		CalculatorLabel calculatorLabel = new CalculatorLabel();
		calculatorLabel.setCalculator(calculatorOpt.get());
		calculatorLabel.setLabel(labelOpt.get());
		calculatorLabelRepository.save(calculatorLabel);
		return true;
	}

	@Transactional
	public boolean removeLabelFromCalculator(Long calculatorId, Long labelId) {
		Optional<CalculatorLabel> calculatorLabelOpt = calculatorLabelRepository
			.findByCalculatorIdAndLabelId(calculatorId, labelId);

		if (calculatorLabelOpt.isEmpty()) {
			return false;
		}

		calculatorLabelRepository.delete(calculatorLabelOpt.get());
		return true;
	}

	@Transactional
	public Label createCuratedLabel(String name, String description) {
		Label label = createOrGetLabel(name, true);
		if (description != null && !description.trim().isEmpty()) {
			label.setDescription(description.trim());
			label = labelRepository.save(label);
		}
		return label;
	}

	public List<Label> getAllLabels() {
		return labelRepository.findAll();
	}

	public Optional<Label> getLabelById(Long id) {
		return labelRepository.findById(id);
	}

	@Transactional
	public Label updateLabel(Long id, String name, String description, Boolean isCurated) {
		Optional<Label> labelOpt = labelRepository.findById(id);
		if (labelOpt.isEmpty()) {
			throw new IllegalArgumentException("Label not found");
		}

		Label label = labelOpt.get();
		label.setName(name.trim());
		if (description != null) {
			label.setDescription(description.trim());
		}
		if (isCurated != null) {
			label.setIsCurated(isCurated);
		}
		return labelRepository.save(label);
	}

	@Transactional
	public boolean deleteLabel(Long id) {
		Optional<Label> labelOpt = labelRepository.findById(id);
		if (labelOpt.isEmpty()) {
			return false;
		}

		// Delete all calculator-label associations first
		calculatorLabelRepository.findAll().stream()
			.filter(cl -> cl.getLabel().getId().equals(id))
			.collect(Collectors.toList())
			.forEach(calculatorLabelRepository::delete);

		labelRepository.delete(labelOpt.get());
		return true;
	}
}

