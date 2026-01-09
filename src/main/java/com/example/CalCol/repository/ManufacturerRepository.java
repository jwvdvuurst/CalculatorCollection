package com.example.CalCol.repository;

import com.example.CalCol.entity.Manufacturer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManufacturerRepository extends JpaRepository<Manufacturer, Long> {

	Optional<Manufacturer> findByName(String name);

	@Query("SELECT m FROM Manufacturer m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))")
	Page<Manufacturer> findByNameContainingIgnoreCase(@Param("search") String search, Pageable pageable);
	
	@Query("SELECT m FROM Manufacturer m LEFT JOIN m.calculators c GROUP BY m.id, m.name ORDER BY COUNT(c) DESC")
	Page<Manufacturer> findAllOrderByCalculatorCountDesc(Pageable pageable);
	
	@Query("SELECT m FROM Manufacturer m LEFT JOIN m.calculators c GROUP BY m.id, m.name ORDER BY COUNT(c) ASC")
	Page<Manufacturer> findAllOrderByCalculatorCountAsc(Pageable pageable);
	
	@Query("SELECT m FROM Manufacturer m LEFT JOIN m.calculators c WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) GROUP BY m.id, m.name ORDER BY COUNT(c) DESC")
	Page<Manufacturer> findByNameContainingIgnoreCaseOrderByCalculatorCountDesc(@Param("search") String search, Pageable pageable);
	
	@Query("SELECT m FROM Manufacturer m LEFT JOIN m.calculators c WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) GROUP BY m.id, m.name ORDER BY COUNT(c) ASC")
	Page<Manufacturer> findByNameContainingIgnoreCaseOrderByCalculatorCountAsc(@Param("search") String search, Pageable pageable);
}

