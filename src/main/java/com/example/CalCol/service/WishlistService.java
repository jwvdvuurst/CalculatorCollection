package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.Label;
import com.example.CalCol.entity.WishlistItem;
import com.example.CalCol.repository.CalculatorLabelRepository;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WishlistService {

	private final WishlistItemRepository wishlistRepository;
	private final CalculatorRepository calculatorRepository;
	private final CalculatorLabelRepository calculatorLabelRepository;

	public Page<WishlistItem> getUserWishlist(String username, Pageable pageable) {
		return wishlistRepository.findByUsernameOrderByAddedAtDesc(username, pageable);
	}

	public long getUserWishlistCount(String username) {
		return wishlistRepository.countByUsername(username);
	}

	@Transactional
	public boolean addToWishlist(String username, Long calculatorId, String notes) {
		if (wishlistRepository.existsByUsernameAndCalculatorId(username, calculatorId)) {
			return false; // Already in wishlist
		}

		Optional<Calculator> calculatorOpt = calculatorRepository.findById(calculatorId);
		if (calculatorOpt.isEmpty()) {
			return false; // Calculator not found
		}

		Calculator calculator = calculatorOpt.get();
		WishlistItem wishlistItem = new WishlistItem();
		wishlistItem.setUsername(username);
		wishlistItem.setCalculator(calculator);
		if (notes != null && !notes.trim().isEmpty()) {
			wishlistItem.setNotes(notes.trim());
		}
		
		// Auto-generate default search queries from calculator info
		String defaultQuery = generateDefaultSearchQuery(calculator);
		if (defaultQuery != null && !defaultQuery.trim().isEmpty()) {
			wishlistItem.setMarktplaatsQuery(defaultQuery);
			wishlistItem.setEbayQuery(defaultQuery);
			wishlistItem.setEtsyQuery(defaultQuery);
		}
		
		wishlistRepository.save(wishlistItem);
		return true;
	}
	
	/**
	 * Generate a default search query from calculator manufacturer and model
	 * Format: "[vintage] [manufacturer] [model] [type] calculator"
	 * "vintage" is included if soldFrom is null or <= 2000 (same logic as enrichment service)
	 * Type is "electronic", "mechanical", or "electromechanical" based on calculator labels
	 */
	private String generateDefaultSearchQuery(Calculator calculator) {
		if (calculator == null) {
			return null;
		}
		
		// Get calculator labels to check for Mechanical/Electromechanical
		List<Label> labels = calculatorLabelRepository.findLabelsByCalculatorId(calculator.getId());
		String calculatorType = "electronic"; // default
		
		// Check if calculator has Mechanical or Electromechanical label
		for (Label label : labels) {
			String labelName = label.getName();
			if ("Mechanical".equalsIgnoreCase(labelName)) {
				calculatorType = "mechanical";
				break; // Prefer Mechanical over Electromechanical if both exist
			} else if ("Electromechanical".equalsIgnoreCase(labelName)) {
				calculatorType = "electromechanical";
				// Don't break, in case Mechanical is also present (which would override)
			}
		}
		
		StringBuilder query = new StringBuilder();
		
		// Add "vintage" keyword if applicable (same logic as EnrichmentService)
		// Include "vintage" if soldFrom is null or <= 2000
		if (calculator.getSoldFrom() == null || calculator.getSoldFrom() <= 2000) {
			query.append("vintage ");
		}
		
		// Add manufacturer
		if (calculator.getManufacturer() != null && calculator.getManufacturer().getName() != null) {
			query.append(calculator.getManufacturer().getName().trim());
		}
		
		// Add model
		if (calculator.getModel() != null && !calculator.getModel().trim().isEmpty()) {
			if (query.length() > 0 && !query.toString().endsWith(" ")) {
				query.append(" ");
			}
			query.append(calculator.getModel().trim());
		}
		
		// Add calculator type (electronic, mechanical, or electromechanical)
		if (query.length() > 0) {
			query.append(" ").append(calculatorType).append(" calculator");
		}
		
		return query.length() > 0 ? query.toString().trim() : null;
	}

	@Transactional
	public boolean removeFromWishlist(String username, Long calculatorId) {
		Optional<WishlistItem> wishlistOpt = 
			wishlistRepository.findByUsernameAndCalculatorId(username, calculatorId);
		
		if (wishlistOpt.isEmpty()) {
			return false;
		}

		wishlistRepository.delete(wishlistOpt.get());
		return true;
	}

	public boolean isInWishlist(String username, Long calculatorId) {
		return wishlistRepository.existsByUsernameAndCalculatorId(username, calculatorId);
	}

	@Transactional
	public boolean updateWishlistNotes(String username, Long calculatorId, String notes) {
		Optional<WishlistItem> wishlistOpt = 
			wishlistRepository.findByUsernameAndCalculatorId(username, calculatorId);
		
		if (wishlistOpt.isEmpty()) {
			return false;
		}

		WishlistItem item = wishlistOpt.get();
		item.setNotes(notes != null ? notes.trim() : null);
		wishlistRepository.save(item);
		return true;
	}

	public Optional<WishlistItem> getWishlistItem(String username, Long calculatorId) {
		return wishlistRepository.findByUsernameAndCalculatorId(username, calculatorId);
	}
	
	@Transactional
	public boolean updateWishlistSearchQueries(String username, Long calculatorId, 
			String marktplaatsQuery, String ebayQuery, String etsyQuery) {
		Optional<WishlistItem> wishlistOpt = 
			wishlistRepository.findByUsernameAndCalculatorId(username, calculatorId);
		
		if (wishlistOpt.isEmpty()) {
			return false;
		}

		WishlistItem item = wishlistOpt.get();
		item.setMarktplaatsQuery(marktplaatsQuery != null && !marktplaatsQuery.trim().isEmpty() 
			? marktplaatsQuery.trim() : null);
		item.setEbayQuery(ebayQuery != null && !ebayQuery.trim().isEmpty() 
			? ebayQuery.trim() : null);
		item.setEtsyQuery(etsyQuery != null && !etsyQuery.trim().isEmpty() 
			? etsyQuery.trim() : null);
		wishlistRepository.save(item);
		return true;
	}
	
	@Transactional
	public boolean resetWishlistSearchQueriesToDefault(String username, Long calculatorId) {
		Optional<WishlistItem> wishlistOpt = 
			wishlistRepository.findByUsernameAndCalculatorId(username, calculatorId);
		
		if (wishlistOpt.isEmpty()) {
			return false;
		}

		WishlistItem item = wishlistOpt.get();
		Calculator calculator = item.getCalculator();
		
		// Regenerate default search query from calculator info
		String defaultQuery = generateDefaultSearchQuery(calculator);
		if (defaultQuery != null && !defaultQuery.trim().isEmpty()) {
			item.setMarktplaatsQuery(defaultQuery);
			item.setEbayQuery(defaultQuery);
			item.setEtsyQuery(defaultQuery);
		} else {
			// Clear queries if no default can be generated
			item.setMarktplaatsQuery(null);
			item.setEbayQuery(null);
			item.setEtsyQuery(null);
		}
		
		wishlistRepository.save(item);
		return true;
	}
	
	/**
	 * Generate search URL for Marktplaats.nl
	 */
	public String generateMarktplaatsUrl(String query) {
		if (query == null || query.trim().isEmpty()) {
			return null;
		}
		try {
			String encodedQuery = java.net.URLEncoder.encode(query.trim(), java.nio.charset.StandardCharsets.UTF_8);
			return "https://www.marktplaats.nl/q/" + encodedQuery + "/";
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Generate search URL for eBay
	 */
	public String generateEbayUrl(String query) {
		if (query == null || query.trim().isEmpty()) {
			return null;
		}
		try {
			String encodedQuery = java.net.URLEncoder.encode(query.trim(), java.nio.charset.StandardCharsets.UTF_8);
			return "https://www.ebay.nl/sch/i.html?_nkw=" + encodedQuery;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Generate search URL for Etsy
	 */
	public String generateEtsyUrl(String query) {
		if (query == null || query.trim().isEmpty()) {
			return null;
		}
		try {
			String encodedQuery = java.net.URLEncoder.encode(query.trim(), java.nio.charset.StandardCharsets.UTF_8);
			return "https://www.etsy.com/nl/search?q=" + encodedQuery;
		} catch (Exception e) {
			return null;
		}
	}
}

