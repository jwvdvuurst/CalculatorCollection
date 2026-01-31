package com.example.CalCol.repository;

import com.example.CalCol.entity.Calculator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CalculatorRepository extends JpaRepository<Calculator, Long> {

	@Query("SELECT c FROM Calculator c WHERE LOWER(c.model) LIKE LOWER(CONCAT('%', :search, '%'))")
	Page<Calculator> findByModelContainingIgnoreCase(@Param("search") String search, Pageable pageable);

	@Query("SELECT c FROM Calculator c WHERE c.manufacturer.id = :manufacturerId")
	Page<Calculator> findByManufacturerId(@Param("manufacturerId") Long manufacturerId, Pageable pageable);

	@Query("SELECT c FROM Calculator c WHERE LOWER(c.model) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
			"LOWER(c.manufacturer.name) LIKE LOWER(CONCAT('%', :search, '%'))")
	Page<Calculator> searchByModelOrManufacturer(@Param("search") String search, Pageable pageable);

	@Query("SELECT c FROM Calculator c WHERE c.currentPrice IS NOT NULL AND " +
			"c.currentPrice >= :minPrice AND c.currentPrice <= :maxPrice")
	Page<Calculator> findByPriceRange(@Param("minPrice") java.math.BigDecimal minPrice, 
									  @Param("maxPrice") java.math.BigDecimal maxPrice, 
									  Pageable pageable);

	@Query("SELECT c FROM Calculator c WHERE c.manufacturer.id = :manufacturerId AND " +
			"c.currentPrice IS NOT NULL AND c.currentPrice >= :minPrice AND c.currentPrice <= :maxPrice")
	Page<Calculator> findByManufacturerIdAndPriceRange(@Param("manufacturerId") Long manufacturerId,
													   @Param("minPrice") java.math.BigDecimal minPrice,
													   @Param("maxPrice") java.math.BigDecimal maxPrice,
													   Pageable pageable);

	@Query("SELECT c FROM Calculator c WHERE (LOWER(c.model) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
			"LOWER(c.manufacturer.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
			"c.currentPrice IS NOT NULL AND c.currentPrice >= :minPrice AND c.currentPrice <= :maxPrice")
	Page<Calculator> searchByModelOrManufacturerAndPriceRange(@Param("search") String search,
															  @Param("minPrice") java.math.BigDecimal minPrice,
															  @Param("maxPrice") java.math.BigDecimal maxPrice,
															  Pageable pageable);

	@Query("SELECT c FROM Calculator c JOIN FETCH c.manufacturer")
	java.util.List<Calculator> findAllWithManufacturer();
	
	@Modifying
	@Query("UPDATE Calculator c SET c.manufacturer.id = :targetManufacturerId WHERE c.manufacturer.id = :sourceManufacturerId")
	int updateManufacturerForCalculators(@Param("sourceManufacturerId") Long sourceManufacturerId, 
										 @Param("targetManufacturerId") Long targetManufacturerId);
}

