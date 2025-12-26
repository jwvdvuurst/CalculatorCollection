package com.example.CalCol.repository;

import com.example.CalCol.entity.CalculatorLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalculatorLabelRepository extends JpaRepository<CalculatorLabel, Long> {

	List<CalculatorLabel> findByCalculatorId(Long calculatorId);

	@Query("SELECT cl FROM CalculatorLabel cl WHERE cl.calculator.id = :calculatorId AND cl.label.id = :labelId")
	Optional<CalculatorLabel> findByCalculatorIdAndLabelId(@Param("calculatorId") Long calculatorId, @Param("labelId") Long labelId);

	@Query("SELECT cl.label FROM CalculatorLabel cl WHERE cl.calculator.id = :calculatorId")
	List<com.example.CalCol.entity.Label> findLabelsByCalculatorId(@Param("calculatorId") Long calculatorId);
}

