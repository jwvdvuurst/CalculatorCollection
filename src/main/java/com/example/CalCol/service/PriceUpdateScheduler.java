package com.example.CalCol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task for updating calculator prices periodically
 */
@Component
@Slf4j
public class PriceUpdateScheduler {

	private final PriceService priceService;

	@Value("${app.price.update.enabled:true}")
	private boolean priceUpdateEnabled;

	@Value("${app.price.update.schedule.enabled:true}")
	private boolean scheduleEnabled;

	public PriceUpdateScheduler(PriceService priceService) {
		this.priceService = priceService;
	}

	/**
	 * Update prices daily at 2 AM
	 * Cron format: second, minute, hour, day of month, month, day of week
	 */
	@Scheduled(cron = "${app.price.update.cron:0 0 2 * * ?}")
	public void updatePricesDaily() {
		if (!priceUpdateEnabled || !scheduleEnabled) {
			log.debug("Price update schedule is disabled");
			return;
		}

		log.info("Starting scheduled price update");
		try {
			int updated = priceService.updateAllPrices();
			log.info("Scheduled price update completed: {} calculators updated", updated);
		} catch (Exception e) {
			log.error("Error during scheduled price update: {}", e.getMessage(), e);
		}
	}

	/**
	 * Update prices weekly on Sunday at 3 AM
	 * This is a fallback in case daily updates are too frequent
	 */
	@Scheduled(cron = "${app.price.update.weekly.cron:0 0 3 * * SUN}")
	public void updatePricesWeekly() {
		if (!priceUpdateEnabled || !scheduleEnabled) {
			log.debug("Price update schedule is disabled");
			return;
		}

		// Only run if daily updates are disabled
		if (scheduleEnabled) {
			log.info("Starting weekly price update");
			try {
				int updated = priceService.updateAllPrices();
				log.info("Weekly price update completed: {} calculators updated", updated);
			} catch (Exception e) {
				log.error("Error during weekly price update: {}", e.getMessage(), e);
			}
		}
	}
}
