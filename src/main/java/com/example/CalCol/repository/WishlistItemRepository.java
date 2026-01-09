package com.example.CalCol.repository;

import com.example.CalCol.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
	
	Page<WishlistItem> findByUsernameOrderByAddedAtDesc(String username, Pageable pageable);
	
	long countByUsername(String username);
	
	boolean existsByUsernameAndCalculatorId(String username, Long calculatorId);
	
	Optional<WishlistItem> findByUsernameAndCalculatorId(String username, Long calculatorId);
}






