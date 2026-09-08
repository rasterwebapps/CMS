package com.cms.inventory.receiving.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cms.inventory.receiving.model.GoodsReceiptLine;

@Repository
public interface GoodsReceiptLineRepository extends JpaRepository<GoodsReceiptLine, Long> {

    List<GoodsReceiptLine> findByGoodsReceiptIdOrderByIdAsc(Long goodsReceiptId);

    /**
     * Sum of this DRAFT receipt's own lines already entered for a PO line — used alongside the
     * PO line's own confirmed {@code receivedQty} to block over-receipt while still drafting.
     * Deliberately does not look at *other* still-open DRAFT receipts against the same PO line —
     * a known, documented simplification (see the decision-log entry); the real guard that
     * always holds is the fresh re-check performed at confirm time.
     */
    @Query("SELECT COALESCE(SUM(l.receivedQty), 0) FROM GoodsReceiptLine l "
        + "WHERE l.goodsReceipt.id = :goodsReceiptId AND l.purchaseOrderItem.id = :purchaseOrderItemId")
    BigDecimal sumReceivedQtyInReceiptForItem(Long goodsReceiptId, Long purchaseOrderItemId);
}
