package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.CalculatorProposal;
import com.example.CalCol.entity.Manufacturer;
import com.example.CalCol.repository.CalculatorProposalRepository;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.ManufacturerRepository;
import com.example.CalCol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalculatorProposalService {

	private final CalculatorProposalRepository proposalRepository;
	private final CalculatorRepository calculatorRepository;
	private final ManufacturerRepository manufacturerRepository;
	private final UserRepository userRepository;
	
	@Autowired(required = false)
	private EmailService emailService;

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

		// Send email notification if email service is available
		if (emailService != null && proposal.getProposedBy() != null) {
			userRepository.findByUsername(proposal.getProposedBy()).ifPresent(user -> {
				if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
					try {
						emailService.sendProposalApprovedEmail(
							user.getEmail(),
							calculator.getModel(),
							manufacturer.getName(),
							calculator.getId()
						);
						log.info("Proposal approval email sent to: {}", user.getEmail());
					} catch (Exception e) {
						log.error("Failed to send proposal approval email to {}: {}", user.getEmail(), e.getMessage(), e);
					}
				}
			});
		}

		return true;
	}

	@Transactional
	public boolean rejectProposal(Long proposalId) {
		Optional<CalculatorProposal> proposalOpt = proposalRepository.findById(proposalId);
		if (proposalOpt.isEmpty()) {
			return false;
		}
		
		CalculatorProposal proposal = proposalOpt.get();
		String model = proposal.getModel();
		String manufacturer = proposal.getManufacturerName();
		String proposedBy = proposal.getProposedBy();
		
		// Send email notification if email service is available
		if (emailService != null && proposedBy != null) {
			userRepository.findByUsername(proposedBy).ifPresent(user -> {
				if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
					try {
						emailService.sendProposalRejectedEmail(
							user.getEmail(),
							model,
							manufacturer,
							"Proposal was rejected by an administrator."
						);
						log.info("Proposal rejection email sent to: {}", user.getEmail());
					} catch (Exception e) {
						log.error("Failed to send proposal rejection email to {}: {}", user.getEmail(), e.getMessage(), e);
					}
				}
			});
		}
		
		proposalRepository.delete(proposal);
		return true;
	}
}

