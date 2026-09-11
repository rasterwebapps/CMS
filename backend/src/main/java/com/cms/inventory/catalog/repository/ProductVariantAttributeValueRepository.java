package com.cms.inventory.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.ProductVariantAttributeValue;

@Repository
public interface ProductVariantAttributeValueRepository extends JpaRepository<ProductVariantAttributeValue, Long> {

    /** Used by CategoryAttributeService's delete guard, alongside ProductAttributeValueRepository's
     *  own check — a category attribute in use by any variant must not be deletable either. */
    boolean existsByAttributeId(Long attributeId);
}
