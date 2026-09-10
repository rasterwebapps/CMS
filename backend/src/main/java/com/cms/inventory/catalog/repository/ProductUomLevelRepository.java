package com.cms.inventory.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.ProductUomLevel;

@Repository
public interface ProductUomLevelRepository extends JpaRepository<ProductUomLevel, Long> {

    List<ProductUomLevel> findByChainVersionIdOrderByLevelRankAsc(Long chainVersionId);
}
