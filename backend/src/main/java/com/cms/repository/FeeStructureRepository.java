package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.FeeStructure;
import com.cms.model.enums.FeeType;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    List<FeeStructure> findByFeeStructureGroupId(Long groupId);

    List<FeeStructure> findByFeeStructureGroupIdAndIsActiveTrue(Long groupId);

    void deleteByFeeStructureGroupId(Long groupId);

    boolean existsByFeeStructureGroupId(Long groupId);

    boolean existsByFeeTypeAndFeeStructureGroupIdAndIdNot(FeeType feeType, Long groupId, Long id);
}
