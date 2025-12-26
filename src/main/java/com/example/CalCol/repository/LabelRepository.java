package com.example.CalCol.repository;

import com.example.CalCol.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {

	Optional<Label> findByName(String name);

	List<Label> findByIsCuratedTrue();

	@Query("SELECT l FROM Label l WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%'))")
	List<Label> searchByName(@Param("search") String search);
}

