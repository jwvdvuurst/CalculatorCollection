package com.example.CalCol.repository;

import com.example.CalCol.entity.SharedCollectionCalculator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SharedCollectionCalculatorRepository extends JpaRepository<SharedCollectionCalculator, Long> {

	List<SharedCollectionCalculator> findBySharedCollectionId(Long sharedCollectionId);

	@Query("SELECT scc.calculator FROM SharedCollectionCalculator scc WHERE scc.sharedCollection.shareToken = :token")
	List<com.example.CalCol.entity.Calculator> findCalculatorsByShareToken(@Param("token") String token);
}

