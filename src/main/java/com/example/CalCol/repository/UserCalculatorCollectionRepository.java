package com.example.CalCol.repository;

import com.example.CalCol.entity.UserCalculatorCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCalculatorCollectionRepository extends JpaRepository<UserCalculatorCollection, Long> {

	Page<UserCalculatorCollection> findByUsernameOrderByAddedAtDesc(String username, Pageable pageable);

	Optional<UserCalculatorCollection> findByUsernameAndCalculatorId(String username, Long calculatorId);

	@Query("SELECT COUNT(uc) FROM UserCalculatorCollection uc WHERE uc.username = :username")
	long countByUsername(@Param("username") String username);

	boolean existsByUsernameAndCalculatorId(String username, Long calculatorId);
}

