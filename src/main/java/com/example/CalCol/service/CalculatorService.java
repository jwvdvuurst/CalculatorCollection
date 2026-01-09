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
	
	public Page<Manufacturer> searchManufacturersWithSort(String search, String sort, Pageable pageable) {
		boolean hasSearch = search != null && !search.trim().isEmpty();
		String searchTerm = hasSearch ? search.trim() : "";
		
		if ("name_asc".equals(sort)) {
			Pageable sortedPageable = org.springframework.data.domain.PageRequest.of(
				pageable.getPageNumber(), 
				pageable.getPageSize(),
				org.springframework.data.domain.Sort.by("name").ascending()
			);
			return hasSearch 
				? manufacturerRepository.findByNameContainingIgnoreCase(searchTerm, sortedPageable)
				: manufacturerRepository.findAll(sortedPageable);
		} else if ("name_desc".equals(sort)) {
			Pageable sortedPageable = org.springframework.data.domain.PageRequest.of(
				pageable.getPageNumber(), 
				pageable.getPageSize(),
				org.springframework.data.domain.Sort.by("name").descending()
			);
			return hasSearch 
				? manufacturerRepository.findByNameContainingIgnoreCase(searchTerm, sortedPageable)
				: manufacturerRepository.findAll(sortedPageable);
		} else if ("count_desc".equals(sort)) {
			return hasSearch
				? manufacturerRepository.findByNameContainingIgnoreCaseOrderByCalculatorCountDesc(searchTerm, pageable)
				: manufacturerRepository.findAllOrderByCalculatorCountDesc(pageable);
		} else if ("count_asc".equals(sort)) {
			return hasSearch
				? manufacturerRepository.findByNameContainingIgnoreCaseOrderByCalculatorCountAsc(searchTerm, pageable)
				: manufacturerRepository.findAllOrderByCalculatorCountAsc(pageable);
		} else {
			// Default: sort by ID (ascending)
			Pageable sortedPageable = org.springframework.data.domain.PageRequest.of(
				pageable.getPageNumber(), 
				pageable.getPageSize(),
				org.springframework.data.domain.Sort.by("id").ascending()
			);
			return hasSearch
				? manufacturerRepository.findByNameContainingIgnoreCase(searchTerm, sortedPageable)
				: manufacturerRepository.findAll(sortedPageable);
		}
	}
	
	@Transactional
	public boolean mergeManufacturers(Long targetManufacturerId, Long sourceManufacturerId, String newName) {
		Optional<Manufacturer> targetOpt = manufacturerRepository.findById(targetManufacturerId);
		Optional<Manufacturer> sourceOpt = manufacturerRepository.findById(sourceManufacturerId);
		
		if (targetOpt.isEmpty() || sourceOpt.isEmpty()) {
			return false;
		}
		
		Manufacturer target = targetOpt.get();
		
		// Update target name if provided
		if (newName != null && !newName.trim().isEmpty()) {
			target.setName(newName.trim());
			manufacturerRepository.save(target);
		}
		
		// Use a direct SQL update to change manufacturer_id for all calculators
		// This bypasses JPA relationship management and avoids orphanRemoval issues
		// The update happens directly in the database, so calculators are moved without
		// triggering the cascade delete behavior
		int updatedCount = calculatorRepository.updateManufacturerForCalculators(sourceManufacturerId, targetManufacturerId);
		
		// Flush to ensure the update is persisted
		calculatorRepository.flush();
		
		// Reload the source manufacturer from database to get the updated state
		// After the SQL update, the calculators should no longer reference this manufacturer
		sourceOpt = manufacturerRepository.findById(sourceManufacturerId);
		if (sourceOpt.isEmpty()) {
			return false; // Should not happen, but check anyway
		}
		
		Manufacturer source = sourceOpt.get();
		
		// Clear the list to ensure it's empty (the SQL update already moved them in DB,
		// but the entity might still have them in memory)
		source.getCalculators().clear();
		manufacturerRepository.save(source);
		
		// Now safe to delete the source manufacturer
		manufacturerRepository.delete(source);
		
		return true;
	}
	
	@Transactional
	public boolean updateManufacturer(Long id, String newName) {
		Optional<Manufacturer> manufacturerOpt = manufacturerRepository.findById(id);
		if (manufacturerOpt.isEmpty()) {
			return false;
		}
		
		Manufacturer manufacturer = manufacturerOpt.get();
		if (newName != null && !newName.trim().isEmpty()) {
			manufacturer.setName(newName.trim());
			manufacturerRepository.save(manufacturer);
		}
		
		return true;
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
		return addToCollection(username, calculatorId, null);
	}

	@Transactional
	public boolean addToCollection(String username, Long calculatorId, String notes) {
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
		if (notes != null && !notes.trim().isEmpty()) {
			collection.setNotes(notes.trim());
		}
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

	@Transactional
	public boolean updateCollectionNotes(String username, Long calculatorId, String notes) {
		Optional<UserCalculatorCollection> collectionOpt = 
			userCollectionRepository.findByUsernameAndCalculatorId(username, calculatorId);
		
		if (collectionOpt.isEmpty()) {
			return false;
		}

		UserCalculatorCollection collection = collectionOpt.get();
		collection.setNotes(notes != null ? notes.trim() : null);
		userCollectionRepository.save(collection);
		return true;
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

	@Transactional
	public Calculator saveCalculator(Calculator calculator) {
		return calculatorRepository.save(calculator);
	}
}

