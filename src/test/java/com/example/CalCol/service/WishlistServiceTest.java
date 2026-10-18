package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.WishlistItem;
import com.example.CalCol.repository.CalculatorLabelRepository;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.UserCalculatorCollectionRepository;
import com.example.CalCol.repository.WishlistItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

	@Mock
	private WishlistItemRepository wishlistRepository;

	@Mock
	private CalculatorRepository calculatorRepository;

	@Mock
	private CalculatorLabelRepository calculatorLabelRepository;

	@Mock
	private UserCalculatorCollectionRepository userCollectionRepository;

	@InjectMocks
	private WishlistService wishlistService;

	@Test
	void moveToCollection_keepsWishlistUntouchedWhenCalculatorIsAlreadyCollected() {
		WishlistItem wishlistItem = wishlistItem(5L, "notes to preserve");
		given(wishlistRepository.findByUsernameAndCalculatorId("owner", 5L)).willReturn(Optional.of(wishlistItem));
		given(userCollectionRepository.existsByUsernameAndCalculatorId("owner", 5L)).willReturn(true);

		WishlistService.MoveToCollectionResult result = wishlistService.moveToCollection("owner", 5L);

		assertThat(result).isEqualTo(WishlistService.MoveToCollectionResult.ALREADY_IN_COLLECTION);
		then(wishlistRepository).should(never()).delete(wishlistItem);
		then(userCollectionRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void moveToCollection_copiesNotesBeforeRemovingWishlistItem() {
		WishlistItem wishlistItem = wishlistItem(5L, "notes to preserve");
		given(wishlistRepository.findByUsernameAndCalculatorId("owner", 5L)).willReturn(Optional.of(wishlistItem));
		given(userCollectionRepository.existsByUsernameAndCalculatorId("owner", 5L)).willReturn(false);

		WishlistService.MoveToCollectionResult result = wishlistService.moveToCollection("owner", 5L);

		ArgumentCaptor<com.example.CalCol.entity.UserCalculatorCollection> captor = ArgumentCaptor.forClass(com.example.CalCol.entity.UserCalculatorCollection.class);
		then(userCollectionRepository).should().save(captor.capture());
		then(wishlistRepository).should().delete(wishlistItem);
		assertThat(result).isEqualTo(WishlistService.MoveToCollectionResult.MOVED);
		assertThat(captor.getValue().getNotes()).isEqualTo("notes to preserve");
	}

	private WishlistItem wishlistItem(Long calculatorId, String notes) {
		Calculator calculator = new Calculator();
		calculator.setId(calculatorId);
		WishlistItem wishlistItem = new WishlistItem();
		wishlistItem.setCalculator(calculator);
		wishlistItem.setNotes(notes);
		return wishlistItem;
	}
}