package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.CalculatorImage;
import com.example.CalCol.entity.CalculatorLink;
import com.example.CalCol.repository.CalculatorImageRepository;
import com.example.CalCol.repository.CalculatorLinkRepository;
import com.example.CalCol.repository.CalculatorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ResourceOwnershipServiceTest {

	@Mock
	private CalculatorLinkRepository linkRepository;

	@Mock
	private CalculatorRepository calculatorRepository;

	@Mock
	private CalculatorImageRepository imageRepository;

	@Mock
	private FileStorageService fileStorageService;

	@InjectMocks
	private LinkService linkService;

	@InjectMocks
	private ImageService imageService;

	@Test
	void updateLink_rejectsAnotherUsersLink() {
		CalculatorLink link = link(5L, "owner");
		given(linkRepository.findById(11L)).willReturn(Optional.of(link));

		assertThatThrownBy(() -> linkService.updateLink(5L, 11L, "https://example.test", "title", null, "other", false))
			.isInstanceOf(AccessDeniedException.class);

		then(linkRepository).should(never()).save(link);
	}

	@Test
	void deleteImage_rejectsAnotherUsersPendingImage() {
		CalculatorImage image = image(5L, "owner", false);
		given(imageRepository.findById(12L)).willReturn(Optional.of(image));

		boolean deleted = imageService.deleteImage(5L, 12L, "other", false);

		assertThat(deleted).isFalse();
		then(imageRepository).should(never()).delete(image);
		then(fileStorageService).shouldHaveNoInteractions();
	}

	private CalculatorLink link(Long calculatorId, String addedBy) {
		CalculatorLink link = new CalculatorLink();
		link.setCalculator(calculator(calculatorId));
		link.setAddedBy(addedBy);
		return link;
	}

	private CalculatorImage image(Long calculatorId, String uploadedBy, boolean approved) {
		CalculatorImage image = new CalculatorImage();
		image.setCalculator(calculator(calculatorId));
		image.setUploadedBy(uploadedBy);
		image.setIsApproved(approved);
		return image;
	}

	private Calculator calculator(Long id) {
		Calculator calculator = new Calculator();
		calculator.setId(id);
		return calculator;
	}
}