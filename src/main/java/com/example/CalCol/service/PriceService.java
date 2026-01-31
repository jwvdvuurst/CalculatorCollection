package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.repository.CalculatorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for fetching and updating calculator prices from marketplaces
 */
@Service
@Slf4j
public class PriceService {

	private final CalculatorRepository calculatorRepository;
	private final WebSearchService webSearchService;
	
	private volatile LocalDateTime pauseUntil = null;
	private static final long PAUSE_DURATION_MINUTES = 60;

	@Value("${app.price.update.enabled:true}")
	private boolean priceUpdateEnabled;

	@Value("${app.price.default-currency:EUR}")
	private String defaultCurrency;

	@Value("${app.price.max-age-days:30}")
	private int maxAgeDays;

	public PriceService(CalculatorRepository calculatorRepository, 
						WebSearchService webSearchService) {
		this.calculatorRepository = calculatorRepository;
		this.webSearchService = webSearchService;
	}

	/**
	 * Update price for a specific calculator
	 * @throws RateLimitException if HTTP 429 (Too Many Requests) is encountered
	 */
	@Transactional
	public boolean updatePriceForCalculator(Long calculatorId) throws RateLimitException {
		if (!priceUpdateEnabled) {
			log.debug("Price updates are disabled");
			return false;
		}

		java.util.Optional<Calculator> calculatorOpt = calculatorRepository.findById(calculatorId);
		if (calculatorOpt.isPresent()) {
			return updatePriceForCalculator(calculatorOpt.get());
		}
		return false;
	}

	/**
	 * Update price for a calculator by searching marketplaces
	 * @throws RateLimitException if HTTP 429 (Too Many Requests) is encountered
	 */
	@Transactional
	public boolean updatePriceForCalculator(Calculator calculator) throws RateLimitException {
		if (!priceUpdateEnabled) {
			log.debug("Price updates are disabled");
			return false;
		}

		try {
			// Reload calculator within transaction to ensure manufacturer is loaded
			Calculator loadedCalculator = calculatorRepository.findById(calculator.getId())
				.orElse(null);
			if (loadedCalculator == null) {
				log.warn("Calculator not found: {}", calculator.getId());
				return false;
			}
			
			// Access manufacturer to ensure it's loaded (triggers lazy loading within transaction)
			String manufacturer = loadedCalculator.getManufacturer().getName();
			String model = loadedCalculator.getModel();
			
			// Build search query for marketplaces
			String searchQuery = buildPriceSearchQuery(manufacturer, model, loadedCalculator.getSoldFrom());
			
			log.info("Searching for price: {}", searchQuery);
			
			// Search multiple marketplaces
			List<PriceResult> prices = new ArrayList<>();
			
			// Search eBay (via web search)
			prices.addAll(searchEbayPrices(searchQuery));
			
			// Search Marktplaats (via web search)
			prices.addAll(searchMarktplaatsPrices(searchQuery));
			
			// Search general marketplaces
			prices.addAll(searchGeneralMarketplacePrices(searchQuery));
			
			if (prices.isEmpty()) {
				log.debug("No prices found for calculator ID: {}", loadedCalculator.getId());
				return false;
			}
			
			// Calculate average price (excluding outliers)
			BigDecimal averagePrice = calculateAveragePrice(prices);
			
			if (averagePrice != null && averagePrice.compareTo(BigDecimal.ZERO) > 0) {
				loadedCalculator.setCurrentPrice(averagePrice);
				loadedCalculator.setPriceCurrency(defaultCurrency);
				loadedCalculator.setPriceLastUpdated(LocalDateTime.now());
				calculatorRepository.save(loadedCalculator);
				
				log.info("Updated price for calculator ID {}: {} {}", 
					loadedCalculator.getId(), averagePrice, defaultCurrency);
				return true;
			}
			
			return false;
		} catch (RateLimitException e) {
			// Re-throw rate limit exceptions to be handled by updateAllPrices
			throw e;
		} catch (Exception e) {
			log.error("Error updating price for calculator ID {}: {}", 
				calculator.getId(), e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Update prices for all calculators (batch operation)
	 */
	public int updateAllPrices() {
		if (!priceUpdateEnabled) {
			log.debug("Price updates are disabled");
			return 0;
		}
		
		// Check if we're paused due to rate limiting
		if (pauseUntil != null && LocalDateTime.now().isBefore(pauseUntil)) {
			long minutesRemaining = java.time.temporal.ChronoUnit.MINUTES.between(
				LocalDateTime.now(), pauseUntil);
			log.info("Price update is paused due to rate limiting. Resuming in {} minutes.", minutesRemaining);
			return 0;
		}
		
		// Clear pause if time has passed
		if (pauseUntil != null && LocalDateTime.now().isAfter(pauseUntil)) {
			log.info("Rate limit pause period has ended. Resuming price updates.");
			pauseUntil = null;
		}

		log.info("Starting batch price update for all calculators");
		
		List<Calculator> calculators = calculatorRepository.findAll();
		int updated = 0;
		int total = calculators.size();
		
		for (Calculator calculator : calculators) {
			try {
				// Check if we're paused (in case pause was set during processing)
				if (pauseUntil != null && LocalDateTime.now().isBefore(pauseUntil)) {
					long minutesRemaining = java.time.temporal.ChronoUnit.MINUTES.between(
						LocalDateTime.now(), pauseUntil);
					log.info("Price update paused due to rate limiting. Resuming in {} minutes.", minutesRemaining);
					break;
				}
				
				// Process each calculator in its own transaction to avoid lazy loading issues
				updated += updateCalculatorPriceInTransaction(calculator.getId());
				
				// Rate limiting: wait a bit between requests to avoid overwhelming APIs
				Thread.sleep(2000); // 2 seconds between calculators
			} catch (RateLimitException e) {
				// Handle rate limit: pause for 60 minutes
				pauseUntil = LocalDateTime.now().plusMinutes(PAUSE_DURATION_MINUTES);
				log.warn("Received HTTP 429 (Too Many Requests). Pausing price updates for {} minutes. Reason: {}", 
					PAUSE_DURATION_MINUTES, e.getMessage());
				log.warn("Price updates will resume at: {}", pauseUntil);
				break; // Stop processing and wait
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.error("Price update interrupted");
				break;
			} catch (Exception e) {
				log.error("Error updating price for calculator ID {}: {}", 
					calculator.getId(), e.getMessage());
			}
		}
		
		log.info("Batch price update completed: {}/{} calculators updated", updated, total);
		return updated;
	}

	/**
	 * Update price for a single calculator within a transaction
	 * This ensures the manufacturer is loaded within the transaction context
	 * @throws RateLimitException if HTTP 429 is encountered
	 */
	@Transactional
	private int updateCalculatorPriceInTransaction(Long calculatorId) throws RateLimitException {
		// Reload calculator within transaction to ensure manufacturer is loaded
		java.util.Optional<Calculator> calculatorOpt = calculatorRepository.findById(calculatorId);
		if (calculatorOpt.isEmpty()) {
			return 0;
		}
		
		Calculator calculator = calculatorOpt.get();
		
		// Skip if price was updated recently (within maxAgeDays)
		if (calculator.getPriceLastUpdated() != null) {
			long daysSinceUpdate = java.time.temporal.ChronoUnit.DAYS.between(
				calculator.getPriceLastUpdated(), LocalDateTime.now());
			if (daysSinceUpdate < maxAgeDays) {
				log.debug("Skipping calculator ID {} - price updated {} days ago", 
					calculator.getId(), daysSinceUpdate);
				return 0;
			}
		}
		
		// Now update the price (this will reload within its own transaction)
		// This may throw RateLimitException which should propagate to updateAllPrices
		if (updatePriceForCalculator(calculator)) {
			return 1;
		}
		return 0;
	}

	/**
	 * Build search query for price searches
	 */
	private String buildPriceSearchQuery(String manufacturer, String model, Integer soldFrom) {
		StringBuilder query = new StringBuilder();
		
		// Add "vintage" if calculator is old
		if (soldFrom != null && soldFrom <= 2000) {
			query.append("vintage ");
		}
		
		query.append(manufacturer).append(" ");
		query.append(model).append(" ");
		query.append("calculator");
		
		return query.toString().trim();
	}

	/**
	 * Search eBay for prices
	 */
	private List<PriceResult> searchEbayPrices(String query) throws RateLimitException {
		List<PriceResult> prices = new ArrayList<>();
		
		try {
			// Search for eBay listings
			String ebayQuery = query + " site:ebay.nl OR site:ebay.com";
			List<WebSearchService.SearchResult> results = webSearchService.searchGoogle(ebayQuery, 10);
			
			for (WebSearchService.SearchResult result : results) {
				BigDecimal price = extractPriceFromText(result.getTitle() + " " + result.getSnippet());
				if (price != null) {
					prices.add(new PriceResult(price, "eBay", result.getUrl()));
				}
			}
		} catch (RateLimitException e) {
			// Re-throw rate limit exceptions
			throw e;
		} catch (Exception e) {
			log.error("Error searching eBay prices: {}", e.getMessage());
		}
		
		return prices;
	}

	/**
	 * Search Marktplaats for prices
	 */
	private List<PriceResult> searchMarktplaatsPrices(String query) throws RateLimitException {
		List<PriceResult> prices = new ArrayList<>();
		
		try {
			// Search for Marktplaats listings
			String marktplaatsQuery = query + " site:marktplaats.nl";
			List<WebSearchService.SearchResult> results = webSearchService.searchGoogle(marktplaatsQuery, 10);
			
			for (WebSearchService.SearchResult result : results) {
				BigDecimal price = extractPriceFromText(result.getTitle() + " " + result.getSnippet());
				if (price != null) {
					prices.add(new PriceResult(price, "Marktplaats", result.getUrl()));
				}
			}
		} catch (RateLimitException e) {
			// Re-throw rate limit exceptions
			throw e;
		} catch (Exception e) {
			log.error("Error searching Marktplaats prices: {}", e.getMessage());
		}
		
		return prices;
	}

	/**
	 * Search general marketplaces for prices
	 */
	private List<PriceResult> searchGeneralMarketplacePrices(String query) throws RateLimitException {
		List<PriceResult> prices = new ArrayList<>();
		
		try {
			// Search for general marketplace listings
			String marketplaceQuery = query + " kopen OR buy OR verkoop OR sale";
			List<WebSearchService.SearchResult> results = webSearchService.searchGoogle(marketplaceQuery, 10);
			
			for (WebSearchService.SearchResult result : results) {
				BigDecimal price = extractPriceFromText(result.getTitle() + " " + result.getSnippet());
				if (price != null) {
					prices.add(new PriceResult(price, "General", result.getUrl()));
				}
			}
		} catch (RateLimitException e) {
			// Re-throw rate limit exceptions
			throw e;
		} catch (Exception e) {
			log.error("Error searching general marketplace prices: {}", e.getMessage());
		}
		
		return prices;
	}

	/**
	 * Extract price from text using regex patterns
	 */
	private BigDecimal extractPriceFromText(String text) {
		if (text == null || text.isEmpty()) {
			return null;
		}
		
		// Pattern for EUR prices: €50, €50.00, 50 euro, 50 EUR, etc.
		Pattern eurPattern = Pattern.compile("€\\s*([0-9]+(?:[.,][0-9]{2})?)|([0-9]+(?:[.,][0-9]{2})?)\\s*(?:euro|EUR|€)", Pattern.CASE_INSENSITIVE);
		
		// Pattern for USD prices: $50, $50.00, 50 dollar, 50 USD, etc.
		Pattern usdPattern = Pattern.compile("\\$\\s*([0-9]+(?:[.,][0-9]{2})?)|([0-9]+(?:[.,][0-9]{2})?)\\s*(?:dollar|USD|\\$)", Pattern.CASE_INSENSITIVE);
		
		// Try EUR first (default currency)
		Matcher eurMatcher = eurPattern.matcher(text);
		if (eurMatcher.find()) {
			String priceStr = eurMatcher.group(1) != null ? eurMatcher.group(1) : eurMatcher.group(2);
			if (priceStr != null) {
				try {
					// Replace comma with dot for decimal
					priceStr = priceStr.replace(',', '.');
					BigDecimal price = new BigDecimal(priceStr);
					// Filter out unrealistic prices (too low or too high)
					if (price.compareTo(BigDecimal.valueOf(1)) >= 0 && 
						price.compareTo(BigDecimal.valueOf(10000)) <= 0) {
						return price;
					}
				} catch (NumberFormatException e) {
					log.debug("Could not parse price: {}", priceStr);
				}
			}
		}
		
		// Try USD
		Matcher usdMatcher = usdPattern.matcher(text);
		if (usdMatcher.find()) {
			String priceStr = usdMatcher.group(1) != null ? usdMatcher.group(1) : usdMatcher.group(2);
			if (priceStr != null) {
				try {
					priceStr = priceStr.replace(',', '.');
					BigDecimal price = new BigDecimal(priceStr);
					// Convert USD to EUR (approximate rate: 1 USD = 0.92 EUR)
					BigDecimal priceInEur = price.multiply(BigDecimal.valueOf(0.92));
					if (priceInEur.compareTo(BigDecimal.valueOf(1)) >= 0 && 
						priceInEur.compareTo(BigDecimal.valueOf(10000)) <= 0) {
						return priceInEur;
					}
				} catch (NumberFormatException e) {
					log.debug("Could not parse price: {}", priceStr);
				}
			}
		}
		
		return null;
	}

	/**
	 * Calculate average price from multiple price results, excluding outliers
	 */
	private BigDecimal calculateAveragePrice(List<PriceResult> prices) {
		if (prices.isEmpty()) {
			return null;
		}
		
		if (prices.size() == 1) {
			return prices.get(0).getPrice();
		}
		
		// Sort prices
		prices.sort((a, b) -> a.getPrice().compareTo(b.getPrice()));
		
		// Remove outliers (prices that are more than 2 standard deviations from mean)
		List<BigDecimal> validPrices = new ArrayList<>();
		for (PriceResult result : prices) {
			validPrices.add(result.getPrice());
		}
		
		// Calculate mean
		BigDecimal sum = validPrices.stream()
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal mean = sum.divide(BigDecimal.valueOf(validPrices.size()), 2, RoundingMode.HALF_UP);
		
		// Calculate standard deviation
		BigDecimal variance = validPrices.stream()
			.map(price -> price.subtract(mean).pow(2))
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.divide(BigDecimal.valueOf(validPrices.size()), 2, RoundingMode.HALF_UP);
		BigDecimal stdDev = new BigDecimal(Math.sqrt(variance.doubleValue()));
		
		// Filter out outliers (more than 2 standard deviations from mean)
		List<BigDecimal> filteredPrices = validPrices.stream()
			.filter(price -> {
				BigDecimal diff = price.subtract(mean).abs();
				return diff.compareTo(stdDev.multiply(BigDecimal.valueOf(2))) <= 0;
			})
			.toList();
		
		if (filteredPrices.isEmpty()) {
			// If all prices are outliers, use median
			int mid = validPrices.size() / 2;
			return validPrices.get(mid);
		}
		
		// Calculate average of filtered prices
		BigDecimal filteredSum = filteredPrices.stream()
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return filteredSum.divide(BigDecimal.valueOf(filteredPrices.size()), 2, RoundingMode.HALF_UP);
	}

	/**
	 * Inner class to hold price search results
	 */
	private static class PriceResult {
		private final BigDecimal price;
		private final String source;
		private final String url;

		public PriceResult(BigDecimal price, String source, String url) {
			this.price = price;
			this.source = source;
			this.url = url;
		}

		public BigDecimal getPrice() {
			return price;
		}

		public String getSource() {
			return source;
		}

		public String getUrl() {
			return url;
		}
	}
}
