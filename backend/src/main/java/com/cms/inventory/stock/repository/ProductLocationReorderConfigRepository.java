package com.cms.inventory.stock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.stock.model.ProductLocationReorderConfig;

@Repository
public interface ProductLocationReorderConfigRepository
        extends JpaRepository<ProductLocationReorderConfig, Long>, JpaSpecificationExecutor<ProductLocationReorderConfig> {

    boolean existsByProductIdAndLocationIdAndIsActiveTrue(Long productId, Long locationId);

    boolean existsByProductIdAndLocationIdAndIsActiveTrueAndIdNot(Long productId, Long locationId, Long id);

    /** Candidate set for the (future Phase C) auto-indent detection job. */
    List<ProductLocationReorderConfig> findByIsActiveTrueAndAutoIndentEnabledTrue();
}
