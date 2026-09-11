package com.cms.inventory.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.inventory.stock.model.StockBatch;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    // Written as an explicit JPQL query rather than a derived-name method — Spring Data's query
    // derivation parses the "Or" inside the batchOrSerialNo property name as the boolean OR
    // keyword (splitting it into "batch" OR "serialNo", neither of which is a real property),
    // which fails at startup with "No property 'batch' found for type 'StockBatch'". The variant
    // match is written null-safe (rather than a derived "AndVariantId"/"AndVariantIsNull" split)
    // so one query serves both a variant-bearing and a plain product the same way findBalance's
    // batch-id lookup does.
    @Query("SELECT b FROM StockBatch b WHERE b.product.id = :productId "
        + "AND ((:variantId IS NULL AND b.variant IS NULL) OR b.variant.id = :variantId) "
        + "AND LOWER(b.batchOrSerialNo) = LOWER(:batchOrSerialNo)")
    Optional<StockBatch> findByProductAndVariantAndBatchOrSerialNo(@Param("productId") Long productId,
        @Param("variantId") Long variantId, @Param("batchOrSerialNo") String batchOrSerialNo);
}
