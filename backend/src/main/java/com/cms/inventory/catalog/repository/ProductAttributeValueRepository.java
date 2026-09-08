package com.cms.inventory.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.ProductAttributeValue;

/**
 * {@code ProductAttributeValue} rows are otherwise managed entirely as a {@code Product} child
 * collection (see the 2026-09-07 "Product slice" decision-log entry) — this repository exists
 * only so {@code CategoryAttributeService} can check "is this attribute still in use by any
 * product" before allowing it to be deleted.
 */
@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {

    boolean existsByAttributeId(Long attributeId);
}
