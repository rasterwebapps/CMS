package com.cms.inventory.receiving.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cms.inventory.receiving.model.SupplierReturnLine;
import com.cms.inventory.receiving.model.enums.SupplierReturnStatus;

@Repository
public interface SupplierReturnLineRepository extends JpaRepository<SupplierReturnLine, Long> {

    List<SupplierReturnLine> findBySupplierReturnIdOrderByIdAsc(Long supplierReturnId);

    /** Quantity already returned and posted (COMPLETED only) for a given receipt line. */
    @Query("SELECT COALESCE(SUM(l.returnedQty), 0) FROM SupplierReturnLine l "
        + "WHERE l.goodsReceiptLine.id = :goodsReceiptLineId AND l.supplierReturn.status = :status")
    BigDecimal sumReturnedQtyForReceiptLine(Long goodsReceiptLineId, SupplierReturnStatus status);

    /** Quantity already entered against a receipt line within this one still-DRAFT return (same "own-draft-only" simplification GRN uses). */
    @Query("SELECT COALESCE(SUM(l.returnedQty), 0) FROM SupplierReturnLine l "
        + "WHERE l.supplierReturn.id = :supplierReturnId AND l.goodsReceiptLine.id = :goodsReceiptLineId")
    BigDecimal sumReturnedQtyInReturnForLine(Long supplierReturnId, Long goodsReceiptLineId);
}
