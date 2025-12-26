package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.CalculatorLink;
import com.example.CalCol.repository.CalculatorLinkRepository;
import com.example.CalCol.repository.CalculatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LinkService {

	private final CalculatorLinkRepository linkRepository;
	private final CalculatorRepository calculatorRepository;

	public List<CalculatorLink> getCalculatorLinks(Long calculatorId) {
		return linkRepository.findByCalculatorId(calculatorId);
	}

	@Transactional
	public CalculatorLink addLink(Long calculatorId, String url, String title, String description, String addedBy) {
		Optional<Calculator> calculatorOpt = calculatorRepository.findById(calculatorId);
		if (calculatorOpt.isEmpty()) {
			throw new IllegalArgumentException("Calculator not found");
		}

		CalculatorLink link = new CalculatorLink();
		link.setCalculator(calculatorOpt.get());
		link.setUrl(url);
		link.setTitle(title);
		link.setDescription(description);
		link.setAddedBy(addedBy);

		return linkRepository.save(link);
	}

	@Transactional
	public boolean deleteLink(Long linkId, String username) {
		Optional<CalculatorLink> linkOpt = linkRepository.findById(linkId);
		if (linkOpt.isEmpty()) {
			return false;
		}

		CalculatorLink link = linkOpt.get();
		// Only allow deletion by the user who added it
		if (!link.getAddedBy().equals(username)) {
			return false;
		}

		linkRepository.delete(link);
		return true;
	}
}

