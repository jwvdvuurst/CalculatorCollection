package com.example.CalCol.repository;

import com.example.CalCol.entity.CalculatorImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalculatorImageRepository extends JpaRepository<CalculatorImage, Long> {

	List<CalculatorImage> findByCalculatorIdAndIsApprovedTrue(Long calculatorId);

	Page<CalculatorImage> findByIsProposalTrueAndIsApprovedFalse(Pageable pageable);

	Page<CalculatorImage> findByCalculatorId(Long calculatorId, Pageable pageable);

	@Query("SELECT ci FROM CalculatorImage ci WHERE ci.calculator.id = :calculatorId AND (ci.isApproved = true OR ci.uploadedBy = :username)")
	List<CalculatorImage> findApprovedOrUserImages(@Param("calculatorId") Long calculatorId, @Param("username") String username);
}

