package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Block;

public interface BlockRepository extends JpaRepository<Block, Long> {
    List<Block> findByBranchIdOrderByOrderIndexAsc(Long branchId);
    List<Block> findByBranchIdAndIsActiveTrueOrderByOrderIndexAsc(Long branchId);

    boolean existsByBranchIdAndNameIgnoreCase(Long branchId, String name);
    boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(Long branchId, String name, Long id);

    boolean existsByBranchIdAndCodeIgnoreCase(Long branchId, String code);
    boolean existsByBranchIdAndCodeIgnoreCaseAndIdNot(Long branchId, String code, Long id);
}
