package com.example.CalCol.repository;

import com.example.CalCol.entity.SharedCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedCollectionRepository extends JpaRepository<SharedCollection, Long> {

	Optional<SharedCollection> findByShareToken(String shareToken);

	@Query("SELECT sc FROM SharedCollection sc WHERE sc.sharedBy = :username AND (sc.expiresAt IS NULL OR sc.expiresAt > CURRENT_TIMESTAMP)")
	java.util.List<SharedCollection> findActiveBySharedBy(@Param("username") String username);
}

