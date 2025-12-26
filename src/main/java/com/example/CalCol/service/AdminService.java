package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.Manufacturer;
import com.example.CalCol.repository.CalculatorImageRepository;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.ManufacturerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final CalculatorRepository calculatorRepository;
	private final ManufacturerRepository manufacturerRepository;
	private final CalculatorImageRepository imageRepository;
	private final FileStorageService fileStorageService;

	public Page<Calculator> getAllCalculators(Pageable pageable) {
		return calculatorRepository.findAll(pageable);
	}

	public Optional<Calculator> getCalculatorById(Long id) {
		return calculatorRepository.findById(id);
	}

	@Transactional
	public Calculator createCalculator(Calculator calculator) {
		return calculatorRepository.save(calculator);
	}

	@Transactional
	public Calculator updateCalculator(Long id, Calculator calculatorData) {
		Optional<Calculator> calculatorOpt = calculatorRepository.findById(id);
		if (calculatorOpt.isEmpty()) {
			throw new IllegalArgumentException("Calculator not found");
		}

		Calculator calculator = calculatorOpt.get();
		calculator.setModel(calculatorData.getModel());
		calculator.setSoldFrom(calculatorData.getSoldFrom());
		calculator.setSoldTo(calculatorData.getSoldTo());
		calculator.setSourceUrl(calculatorData.getSourceUrl());
		calculator.setRawRowText(calculatorData.getRawRowText());

		if (calculatorData.getManufacturer() != null && calculatorData.getManufacturer().getId() != null) {
			Optional<Manufacturer> manufacturerOpt = manufacturerRepository.findById(calculatorData.getManufacturer().getId());
			manufacturerOpt.ifPresent(calculator::setManufacturer);
		}

		return calculatorRepository.save(calculator);
	}

	@Transactional
	public boolean deleteCalculator(Long id) {
		Optional<Calculator> calculatorOpt = calculatorRepository.findById(id);
		if (calculatorOpt.isEmpty()) {
			return false;
		}

		Calculator calculator = calculatorOpt.get();
		
		// Delete associated images
		imageRepository.findByCalculatorId(calculator.getId(), Pageable.unpaged())
			.getContent()
			.forEach(image -> fileStorageService.deleteFile(image.getImagePath()));

		calculatorRepository.delete(calculator);
		return true;
	}

	public Page<Manufacturer> getAllManufacturers(Pageable pageable) {
		return manufacturerRepository.findAll(pageable);
	}

	public Optional<Manufacturer> getManufacturerById(Long id) {
		return manufacturerRepository.findById(id);
	}

	@Transactional
	public Manufacturer createManufacturer(Manufacturer manufacturer) {
		return manufacturerRepository.save(manufacturer);
	}
}

