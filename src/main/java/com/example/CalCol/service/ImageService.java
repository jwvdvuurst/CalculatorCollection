package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.CalculatorImage;
import com.example.CalCol.repository.CalculatorImageRepository;
import com.example.CalCol.repository.CalculatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageService {

	private final CalculatorImageRepository imageRepository;
	private final CalculatorRepository calculatorRepository;
	private final FileStorageService fileStorageService;

	@Transactional
	public CalculatorImage uploadImage(Long calculatorId, MultipartFile file, String username, boolean proposeForRepository) throws IOException {
		Optional<Calculator> calculatorOpt = calculatorRepository.findById(calculatorId);
		if (calculatorOpt.isEmpty()) {
			throw new IllegalArgumentException("Calculator not found");
		}

		String filename = fileStorageService.storeFile(file);

		CalculatorImage image = new CalculatorImage();
		image.setCalculator(calculatorOpt.get());
		image.setImagePath(filename);
		image.setUploadedBy(username);
		image.setIsProposal(proposeForRepository);
		image.setIsApproved(!proposeForRepository); // If not a proposal, auto-approve for user's own collection

		return imageRepository.save(image);
	}

	public Page<CalculatorImage> getPendingProposals(Pageable pageable) {
		return imageRepository.findByIsProposalTrueAndIsApprovedFalse(pageable);
	}

	@Transactional
	public boolean approveImage(Long imageId, String approvedBy) {
		Optional<CalculatorImage> imageOpt = imageRepository.findById(imageId);
		if (imageOpt.isEmpty()) {
			return false;
		}

		CalculatorImage image = imageOpt.get();
		image.setIsApproved(true);
		image.setIsProposal(false);
		image.setApprovedBy(approvedBy);
		image.setApprovedAt(LocalDateTime.now());

		imageRepository.save(image);
		return true;
	}

	@Transactional
	public boolean rejectImage(Long imageId) {
		Optional<CalculatorImage> imageOpt = imageRepository.findById(imageId);
		if (imageOpt.isEmpty()) {
			return false;
		}

		CalculatorImage image = imageOpt.get();
		// Delete the file
		fileStorageService.deleteFile(image.getImagePath());
		// Delete the record
		imageRepository.delete(image);
		return true;
	}

	@Transactional
	public boolean deleteImage(Long imageId, String username) {
		Optional<CalculatorImage> imageOpt = imageRepository.findById(imageId);
		if (imageOpt.isEmpty()) {
			return false;
		}

		CalculatorImage image = imageOpt.get();
		// Only allow deletion if user owns it or it's not approved
		if (!image.getUploadedBy().equals(username) && image.getIsApproved()) {
			return false;
		}

		fileStorageService.deleteFile(image.getImagePath());
		imageRepository.delete(image);
		return true;
	}

	public Optional<CalculatorImage> getImageById(Long imageId) {
		return imageRepository.findById(imageId);
	}
}

