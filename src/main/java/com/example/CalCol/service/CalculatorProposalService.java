package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.CalculatorProposal;
import com.example.CalCol.entity.Manufacturer;
import com.example.CalCol.repository.CalculatorProposalRepository;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.ManufacturerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalculatorProposalService {

	private final CalculatorProposalRepository proposalRepository;
	private final CalculatorRepository calculatorRepository;
	private final ManufacturerRepository manufacturerRepository;

	public Page<CalculatorProposal> getPendingProposals(Pageable pageable) {
		return proposalRepository.findByIsApprovedFalse(pageable);
	}

	public Page<CalculatorProposal> getUserProposals(String username, Pageable pageable) {
		return proposalRepository.findByProposedBy(username, pageable);
	}

	@Transactional
	public CalculatorProposal createProposal(String model, String manufacturerName, 
			Integer soldFrom, Integer soldTo, String sourceUrl, String rawRowText, 
			String notes, String proposedBy) {
		CalculatorProposal proposal = new CalculatorProposal();
		proposal.setModel(model);
		proposal.setManufacturerName(manufacturerName);
		proposal.setSoldFrom(soldFrom);
		proposal.setSoldTo(soldTo);
		proposal.setSourceUrl(sourceUrl);
		proposal.setRawRowText(rawRowText);
		proposal.setNotes(notes);
		proposal.setProposedBy(proposedBy);
		return proposalRepository.save(proposal);
	}

	@Transactional
	public boolean approveProposal(Long proposalId, String approvedBy) {
		Optional<CalculatorProposal> proposalOpt = proposalRepository.findById(proposalId);
		if (proposalOpt.isEmpty()) {
			return false;
		}

		CalculatorProposal proposal = proposalOpt.get();
		
		// Get or create manufacturer
		Manufacturer manufacturer = manufacturerRepository.findByName(proposal.getManufacturerName())
			.orElseGet(() -> {
				Manufacturer mfg = new Manufacturer();
				mfg.setName(proposal.getManufacturerName());
				return manufacturerRepository.save(mfg);
			});

		// Create calculator
		Calculator calculator = new Calculator();
		calculator.setModel(proposal.getModel());
		calculator.setManufacturer(manufacturer);
		calculator.setSoldFrom(proposal.getSoldFrom());
		calculator.setSoldTo(proposal.getSoldTo());
		calculator.setSourceUrl(proposal.getSourceUrl());
		calculator.setRawRowText(proposal.getRawRowText());
		calculatorRepository.save(calculator);

		// Mark proposal as approved
		proposal.setIsApproved(true);
		proposal.setApprovedBy(approvedBy);
		proposal.setApprovedAt(LocalDateTime.now());
		proposalRepository.save(proposal);

		return true;
	}

	@Transactional
	public boolean rejectProposal(Long proposalId) {
		Optional<CalculatorProposal> proposalOpt = proposalRepository.findById(proposalId);
		if (proposalOpt.isEmpty()) {
			return false;
		}
		proposalRepository.delete(proposalOpt.get());
		return true;
	}
}

