package com.cms.inventory.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>, JpaSpecificationExecutor<ProductVariant> {

    List<ProductVariant> findByProductIdOrderByVariantNameAsc(Long productId);

    boolean existsByVariantCodeIgnoreCase(String variantCode);
    boolean existsByVariantCodeIgnoreCaseAndIdNot(String variantCode, Long id);

    boolean existsByBarcodeIgnoreCase(String barcode);
    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);

    /** Used by the barcode-scan lookup workflow — see ProductVariantService.findByBarcode. */
    Optional<ProductVariant> findByBarcodeIgnoreCase(String barcode);
}
