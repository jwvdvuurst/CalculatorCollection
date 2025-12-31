package com.example.CalCol.repository;

import com.example.CalCol.entity.CalculatorLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalculatorLinkRepository extends JpaRepository<CalculatorLink, Long> {

	List<CalculatorLink> findByCalculatorId(Long calculatorId);
	
	boolean existsByCalculatorIdAndUrl(Long calculatorId, String url);
}

