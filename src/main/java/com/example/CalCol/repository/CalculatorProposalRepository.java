package com.example.CalCol.repository;

import com.example.CalCol.entity.CalculatorProposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalculatorProposalRepository extends JpaRepository<CalculatorProposal, Long> {

	Page<CalculatorProposal> findByIsApprovedFalse(Pageable pageable);

	Page<CalculatorProposal> findByProposedBy(String proposedBy, Pageable pageable);
}

