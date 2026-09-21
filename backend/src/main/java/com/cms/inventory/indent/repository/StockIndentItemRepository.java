package com.cms.inventory.indent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cms.inventory.indent.model.IndentOpenQtyProjection;
import com.cms.inventory.indent.model.StockIndentItem;
import com.cms.inventory.indent.model.enums.StockIndentItemStatus;

@Repository
public interface StockIndentItemRepository extends JpaRepository<StockIndentItem, Long> {

    List<StockIndentItem> findByStockIndentIdOrderByIdAsc(Long stockIndentId);

    boolean existsByStockIndentIdAndProductIdAndVariantId(Long stockIndentId, Long productId, Long variantId);

    boolean existsByStockIndentIdAndProductIdAndVariantIsNull(Long stockIndentId, Long productId);

    boolean existsByStockIndentIdAndStatus(Long stockIndentId, StockIndentItemStatus status);

    boolean existsByStockIndentIdAndStatusIn(Long stockIndentId, List<StockIndentItemStatus> statuses);

    /**
     * Quantity already "in the pipeline" per (product, requesting location) — an item counts as
     * open once its indent has been submitted and the line hasn't reached a terminal state yet.
     * {@code PENDING} (awaiting the department head) and {@code APPROVED} (awaiting the store's
     * own fulfillment decision, Phase D) both count — neither has posted a stock movement yet, so
     * the requesting location's on-hand balance doesn't reflect them. Only {@code FULFILLED}
     * (and the other terminal outcomes) are excluded, since a {@code FULFILLED} line's movement is
     * already reflected in current stock — counting it again would double-count the same stock.
     * Used by {@code AutoIndentService} so it never re-flags a shortfall that's already open.
     */
    @Query(value = """
        SELECT i.product_id AS productId, r.requesting_location_id AS locationId, SUM(i.requested_qty) AS qty
        FROM stock_indent_items i
        JOIN stock_indents r ON r.id = i.stock_indent_id
        WHERE i.status IN ('PENDING', 'APPROVED') AND r.status = 'SUBMITTED'
        GROUP BY i.product_id, r.requesting_location_id
        """, nativeQuery = true)
    List<IndentOpenQtyProjection> findOpenQtyByProductAndRequestingLocation();
}
