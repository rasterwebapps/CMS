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

    /** Whether {@code productId} has any active variant — the trigger for "variant becomes
     *  required" across Stock Movement/PO/Transfer/Issue Request. See {@code
     *  StockMovementService.requireVariantIfProductHasAny}. */
    boolean existsByProductIdAndIsActiveTrue(Long productId);

    /** Confirms a given variant actually belongs to the given product before it's accepted on any
     *  movement/line — a variant id from a different product must never silently pass through. */
    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

    boolean existsByVariantCodeIgnoreCase(String variantCode);
    boolean existsByVariantCodeIgnoreCaseAndIdNot(String variantCode, Long id);

    boolean existsByBarcodeIgnoreCase(String barcode);
    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);

    /** Used by the barcode-scan lookup workflow — see ProductVariantService.findByBarcode. */
    Optional<ProductVariant> findByBarcodeIgnoreCase(String barcode);
}
