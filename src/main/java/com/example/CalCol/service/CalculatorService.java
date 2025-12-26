package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.CalculatorImage;
import com.example.CalCol.entity.CalculatorLink;
import com.example.CalCol.entity.Manufacturer;
import com.example.CalCol.entity.UserCalculatorCollection;
import com.example.CalCol.repository.CalculatorImageRepository;
import com.example.CalCol.repository.CalculatorLinkRepository;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.ManufacturerRepository;
import com.example.CalCol.repository.UserCalculatorCollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalculatorService {

	private final CalculatorRepository calculatorRepository;
	private final ManufacturerRepository manufacturerRepository;
	private final UserCalculatorCollectionRepository userCollectionRepository;
	private final CalculatorImageRepository calculatorImageRepository;
	private final CalculatorLinkRepository calculatorLinkRepository;

	public Page<Calculator> getAllCalculators(Pageable pageable) {
		return calculatorRepository.findAll(pageable);
	}

	public Page<Calculator> searchCalculators(String search, Pageable pageable) {
		if (search == null || search.trim().isEmpty()) {
			return getAllCalculators(pageable);
		}
		return calculatorRepository.searchByModelOrManufacturer(search.trim(), pageable);
	}

	public Page<Calculator> getCalculatorsByManufacturer(Long manufacturerId, Pageable pageable) {
		return calculatorRepository.findByManufacturerId(manufacturerId, pageable);
	}

	public Optional<Calculator> getCalculatorById(Long id) {
		return calculatorRepository.findById(id);
	}

	public Page<Manufacturer> getAllManufacturers(Pageable pageable) {
		return manufacturerRepository.findAll(pageable);
	}

	public Page<Manufacturer> searchManufacturers(String search, Pageable pageable) {
		if (search == null || search.trim().isEmpty()) {
			return getAllManufacturers(pageable);
		}
		return manufacturerRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
	}

	public Optional<Manufacturer> getManufacturerById(Long id) {
		return manufacturerRepository.findById(id);
	}

	public Page<UserCalculatorCollection> getUserCollection(String username, Pageable pageable) {
		return userCollectionRepository.findByUsernameOrderByAddedAtDesc(username, pageable);
	}

	public long getUserCollectionCount(String username) {
		return userCollectionRepository.countByUsername(username);
	}

	@Transactional
	public boolean addToCollection(String username, Long calculatorId) {
		if (userCollectionRepository.existsByUsernameAndCalculatorId(username, calculatorId)) {
			return false; // Already in collection
		}

		Optional<Calculator> calculatorOpt = calculatorRepository.findById(calculatorId);
		if (calculatorOpt.isEmpty()) {
			return false; // Calculator not found
		}

		UserCalculatorCollection collection = new UserCalculatorCollection();
		collection.setUsername(username);
		collection.setCalculator(calculatorOpt.get());
		userCollectionRepository.save(collection);
		return true;
	}

	@Transactional
	public boolean removeFromCollection(String username, Long calculatorId) {
		Optional<UserCalculatorCollection> collectionOpt = 
			userCollectionRepository.findByUsernameAndCalculatorId(username, calculatorId);
		
		if (collectionOpt.isEmpty()) {
			return false;
		}

		userCollectionRepository.delete(collectionOpt.get());
		return true;
	}

	public boolean isInCollection(String username, Long calculatorId) {
		return userCollectionRepository.existsByUsernameAndCalculatorId(username, calculatorId);
	}

	public java.util.List<CalculatorImage> getApprovedImages(Long calculatorId) {
		return calculatorImageRepository.findByCalculatorIdAndIsApprovedTrue(calculatorId);
	}

	public java.util.List<CalculatorImage> getImagesForUser(Long calculatorId, String username) {
		return calculatorImageRepository.findApprovedOrUserImages(calculatorId, username);
	}

	public java.util.List<CalculatorLink> getCalculatorLinks(Long calculatorId) {
		return calculatorLinkRepository.findByCalculatorId(calculatorId);
	}
}

