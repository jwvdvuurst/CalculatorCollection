package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.SharedCollection;
import com.example.CalCol.entity.SharedCollectionCalculator;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.SharedCollectionCalculatorRepository;
import com.example.CalCol.repository.SharedCollectionRepository;
import com.example.CalCol.repository.UserCalculatorCollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareService {

	private final SharedCollectionRepository sharedCollectionRepository;
	private final SharedCollectionCalculatorRepository sharedCalculatorRepository;
	private final UserCalculatorCollectionRepository userCollectionRepository;
	private final CalculatorRepository calculatorRepository;

	@Transactional
	public SharedCollection createShare(String username, List<Long> calculatorIds, String title, 
			String description, Integer daysValid, boolean isPublic) {
		// Verify all calculators are in user's collection
		for (Long calculatorId : calculatorIds) {
			if (!userCollectionRepository.existsByUsernameAndCalculatorId(username, calculatorId)) {
				throw new IllegalArgumentException("Calculator " + calculatorId + " is not in your collection");
			}
		}

		SharedCollection share = new SharedCollection();
		share.setSharedBy(username);
		share.setTitle(title);
		share.setDescription(description);
		share.setIsPublic(isPublic);
		share.setShareToken(UUID.randomUUID().toString());
		
		if (daysValid != null && daysValid > 0) {
			share.setExpiresAt(LocalDateTime.now().plusDays(daysValid));
		}

		share = sharedCollectionRepository.save(share);

		// Add calculators to share
		for (Long calculatorId : calculatorIds) {
			Optional<Calculator> calcOpt = calculatorRepository.findById(calculatorId);
			if (calcOpt.isPresent()) {
				SharedCollectionCalculator scc = new SharedCollectionCalculator();
				scc.setSharedCollection(share);
				scc.setCalculator(calcOpt.get());
				sharedCalculatorRepository.save(scc);
			}
		}

		return share;
	}

	public Optional<SharedCollection> getSharedCollection(String token) {
		Optional<SharedCollection> shareOpt = sharedCollectionRepository.findByShareToken(token);
		if (shareOpt.isEmpty()) {
			return Optional.empty();
		}

		SharedCollection share = shareOpt.get();
		// Check if expired
		if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
			return Optional.empty();
		}

		return Optional.of(share);
	}

	public List<Calculator> getSharedCalculators(String token) {
		return sharedCalculatorRepository.findCalculatorsByShareToken(token);
	}

	public List<SharedCollection> getUserShares(String username) {
		return sharedCollectionRepository.findActiveBySharedBy(username);
	}

	@Transactional
	public boolean deleteShare(String token, String username) {
		Optional<SharedCollection> shareOpt = sharedCollectionRepository.findByShareToken(token);
		if (shareOpt.isEmpty() || !shareOpt.get().getSharedBy().equals(username)) {
			return false;
		}

		SharedCollection share = shareOpt.get();
		// Delete associated calculators
		sharedCalculatorRepository.findBySharedCollectionId(share.getId())
			.forEach(sharedCalculatorRepository::delete);
		
		sharedCollectionRepository.delete(share);
		return true;
	}
}

