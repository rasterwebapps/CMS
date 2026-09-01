package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.ClinicalShiftTheoryBlock;

public interface ClinicalShiftTheoryBlockRepository extends JpaRepository<ClinicalShiftTheoryBlock, Long> {

    List<ClinicalShiftTheoryBlock> findByShiftGroupIdOrderBySequenceOrderAsc(Long shiftGroupId);

    void deleteByShiftGroupId(Long shiftGroupId);
}
