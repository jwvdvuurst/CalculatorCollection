package com.example.CalCol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing API quotas and rate limits
 */
@Service
@Slf4j
public class QuotaService {

	// Rate limiting: track last request time per service
	private final Map<String, LocalDateTime> lastRequestTime = new ConcurrentHashMap<>();
	
	// Monthly quota: track usage count per service per month
	private final Map<String, AtomicInteger> monthlyUsage = new ConcurrentHashMap<>();
	private final Map<String, LocalDateTime> quotaResetTime = new ConcurrentHashMap<>();
	
	// Rate limit configuration (queries per second)
	@Value("${app.quota.brave.rate-limit:1}")
	private int braveRateLimit;
	
	@Value("${app.quota.google.rate-limit:10}")
	private int googleRateLimit;
	
	@Value("${app.quota.bing.rate-limit:10}")
	private int bingRateLimit;
	
	@Value("${app.quota.ai.rate-limit:5}")
	private int aiRateLimit;
	
	// Monthly quota configuration (queries per month)
	@Value("${app.quota.brave.monthly-limit:2000}")
	private int braveMonthlyLimit;
	
	@Value("${app.quota.google.monthly-limit:10000}")
	private int googleMonthlyLimit;
	
	@Value("${app.quota.bing.monthly-limit:10000}")
	private int bingMonthlyLimit;
	
	@Value("${app.quota.ai.monthly-limit:5000}")
	private int aiMonthlyLimit;

	/**
	 * Check if a request can be made for the given service
	 * @param serviceName Name of the service (e.g., "brave", "google", "bing", "ai")
	 * @return true if request can be made, false if quota/rate limit exceeded
	 */
	public boolean canMakeRequest(String serviceName) {
		// Check rate limit
		if (!checkRateLimit(serviceName)) {
			log.warn("Rate limit exceeded for service: {}", serviceName);
			return false;
		}
		
		// Check monthly quota
		if (!checkMonthlyQuota(serviceName)) {
			log.warn("Monthly quota exceeded for service: {}", serviceName);
			return false;
		}
		
		return true;
	}

	/**
	 * Record a successful request for quota tracking
	 * @param serviceName Name of the service
	 */
	public void recordRequest(String serviceName) {
		updateLastRequestTime(serviceName);
		incrementMonthlyUsage(serviceName);
	}

	/**
	 * Get remaining monthly quota for a service
	 * @param serviceName Name of the service
	 * @return Remaining queries for the current month
	 */
	public int getRemainingQuota(String serviceName) {
		int limit = getMonthlyLimit(serviceName);
		int used = getMonthlyUsage(serviceName);
		return Math.max(0, limit - used);
	}

	/**
	 * Get current monthly usage for a service
	 * @param serviceName Name of the service
	 * @return Number of queries used this month
	 */
	public int getMonthlyUsage(String serviceName) {
		ensureQuotaTracking(serviceName);
		return monthlyUsage.get(serviceName).get();
	}

	/**
	 * Get rate limit for a service (queries per second)
	 * @param serviceName Name of the service
	 * @return Rate limit in queries per second
	 */
	public int getRateLimit(String serviceName) {
		return switch (serviceName.toLowerCase()) {
			case "brave" -> braveRateLimit;
			case "google" -> googleRateLimit;
			case "bing" -> bingRateLimit;
			case "ai" -> aiRateLimit;
			default -> 1;
		};
	}

	/**
	 * Get monthly limit for a service
	 * @param serviceName Name of the service
	 * @return Monthly query limit
	 */
	public int getMonthlyLimit(String serviceName) {
		return switch (serviceName.toLowerCase()) {
			case "brave" -> braveMonthlyLimit;
			case "google" -> googleMonthlyLimit;
			case "bing" -> bingMonthlyLimit;
			case "ai" -> aiMonthlyLimit;
			default -> 1000;
		};
	}

	private boolean checkRateLimit(String serviceName) {
		LocalDateTime lastRequest = lastRequestTime.get(serviceName);
		if (lastRequest == null) {
			return true; // No previous request, allow
		}
		
		int rateLimit = getRateLimit(serviceName);
		// Use milliseconds for more precise timing
		long millisSinceLastRequest = ChronoUnit.MILLIS.between(lastRequest, LocalDateTime.now());
		
		// Need to wait at least 1000/rateLimit milliseconds between requests
		// For rate limit of 1 per second, need 1000ms between requests
		// For rate limit of 10 per second, need 100ms between requests
		long minMillisBetweenRequests = 1000L / rateLimit;
		
		boolean allowed = millisSinceLastRequest >= minMillisBetweenRequests;
		if (!allowed) {
			log.debug("Rate limit check for {}: {}ms since last request, need {}ms. Rate limit: {}/sec", 
				serviceName, millisSinceLastRequest, minMillisBetweenRequests, rateLimit);
		}
		return allowed;
	}

	private boolean checkMonthlyQuota(String serviceName) {
		ensureQuotaTracking(serviceName);
		
		int limit = getMonthlyLimit(serviceName);
		int used = monthlyUsage.get(serviceName).get();
		
		return used < limit;
	}

	private void updateLastRequestTime(String serviceName) {
		lastRequestTime.put(serviceName, LocalDateTime.now());
	}

	private void incrementMonthlyUsage(String serviceName) {
		ensureQuotaTracking(serviceName);
		monthlyUsage.get(serviceName).incrementAndGet();
		log.debug("Incremented monthly usage for {}: {}/{}", 
			serviceName, monthlyUsage.get(serviceName).get(), getMonthlyLimit(serviceName));
	}

	private void ensureQuotaTracking(String serviceName) {
		// Initialize if not exists or if month has changed
		LocalDateTime resetTime = quotaResetTime.get(serviceName);
		LocalDateTime now = LocalDateTime.now();
		
		if (resetTime == null || 
			now.getYear() != resetTime.getYear() || 
			now.getMonth() != resetTime.getMonth()) {
			// New month, reset quota
			monthlyUsage.put(serviceName, new AtomicInteger(0));
			quotaResetTime.put(serviceName, now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
			log.info("Reset monthly quota for service: {} (new limit: {})", serviceName, getMonthlyLimit(serviceName));
		}
	}

	/**
	 * Get quota status for all services
	 * @return Map of service name to quota status
	 */
	public Map<String, QuotaStatus> getAllQuotaStatus() {
		Map<String, QuotaStatus> status = new ConcurrentHashMap<>();
		
		String[] services = {"brave", "google", "bing", "ai"};
		for (String service : services) {
			ensureQuotaTracking(service);
			QuotaStatus quotaStatus = new QuotaStatus();
			quotaStatus.setServiceName(service);
			quotaStatus.setUsed(getMonthlyUsage(service));
			quotaStatus.setLimit(getMonthlyLimit(service));
			quotaStatus.setRemaining(getRemainingQuota(service));
			quotaStatus.setRateLimit(getRateLimit(service));
			quotaStatus.setCanMakeRequest(canMakeRequest(service));
			
			LocalDateTime resetTime = quotaResetTime.get(service);
			if (resetTime != null) {
				// Calculate next reset time (first day of next month)
				LocalDateTime nextReset = resetTime.plusMonths(1);
				quotaStatus.setNextReset(nextReset);
			}
			
			status.put(service, quotaStatus);
		}
		
		return status;
	}

	/**
	 * Quota status information
	 */
	public static class QuotaStatus {
		private String serviceName;
		private int used;
		private int limit;
		private int remaining;
		private int rateLimit;
		private boolean canMakeRequest;
		private LocalDateTime nextReset;

		public String getServiceName() {
			return serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		public int getUsed() {
			return used;
		}

		public void setUsed(int used) {
			this.used = used;
		}

		public int getLimit() {
			return limit;
		}

		public void setLimit(int limit) {
			this.limit = limit;
		}

		public int getRemaining() {
			return remaining;
		}

		public void setRemaining(int remaining) {
			this.remaining = remaining;
		}

		public int getRateLimit() {
			return rateLimit;
		}

		public void setRateLimit(int rateLimit) {
			this.rateLimit = rateLimit;
		}

		public boolean isCanMakeRequest() {
			return canMakeRequest;
		}

		public void setCanMakeRequest(boolean canMakeRequest) {
			this.canMakeRequest = canMakeRequest;
		}

		public LocalDateTime getNextReset() {
			return nextReset;
		}

		public void setNextReset(LocalDateTime nextReset) {
			this.nextReset = nextReset;
		}
	}
}

