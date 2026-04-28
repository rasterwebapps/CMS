package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.FeeStructureYearAmount;

public interface FeeStructureYearAmountRepository extends JpaRepository<FeeStructureYearAmount, Long> {

    List<FeeStructureYearAmount> findByFeeStructureIdOrderByYearNumber(Long feeStructureId);

    List<FeeStructureYearAmount> findByFeeStructureIdAndYearNumber(Long feeStructureId, Integer yearNumber);

    void deleteByFeeStructureId(Long feeStructureId);
}
