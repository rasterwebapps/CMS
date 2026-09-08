package com.cms.inventory.stock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.stock.model.CycleCountLine;
import com.cms.inventory.stock.model.enums.CycleCountLineStatus;

@Repository
public interface CycleCountLineRepository extends JpaRepository<CycleCountLine, Long> {

    List<CycleCountLine> findByCycleCountIdOrderByIdAsc(Long cycleCountId);

    boolean existsByCycleCountIdAndProductId(Long cycleCountId, Long productId);

    boolean existsByCycleCountIdAndStatus(Long cycleCountId, CycleCountLineStatus status);
}
