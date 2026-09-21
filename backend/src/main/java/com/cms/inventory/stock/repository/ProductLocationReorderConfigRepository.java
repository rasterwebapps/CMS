package com.cms.inventory.stock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cms.inventory.stock.model.ProductLocationReorderConfig;
import com.cms.inventory.stock.model.ReorderConfigShortageProjection;

@Repository
public interface ProductLocationReorderConfigRepository
        extends JpaRepository<ProductLocationReorderConfig, Long>, JpaSpecificationExecutor<ProductLocationReorderConfig> {

    boolean existsByProductIdAndLocationIdAndIsActiveTrue(Long productId, Long locationId);

    boolean existsByProductIdAndLocationIdAndIsActiveTrueAndIdNot(Long productId, Long locationId, Long id);

    /**
     * Every active, auto-indent-enabled (product, location) config currently at or below its
     * reorder level — the raw candidate set {@code AutoIndentService} nets against open Stock
     * Indent quantity. {@code LEFT JOIN} against {@code stock_balances} (unlike the Wanted List's
     * own shortage query) so a location that has never received this product yet still counts as
     * a real shortage — a deliberately configured reorder policy already establishes the stocking
     * relationship, unlike the Wanted List's cross-every-active-product default.
     */
    @Query(value = """
        SELECT c.id AS configId, c.product_id AS productId, c.location_id AS locationId,
               COALESCE(SUM(b.qty_on_hand), 0) AS qtyOnHand,
               c.reorder_level AS reorderLevel, c.reorder_qty AS reorderQty, c.max_stock_qty AS maxStockQty,
               l.default_supplying_location_id AS defaultSupplyingLocationId
        FROM product_location_reorder_configs c
        JOIN inventory_locations l ON l.id = c.location_id
        LEFT JOIN stock_balances b ON b.product_id = c.product_id AND b.location_id = c.location_id
        WHERE c.is_active = true AND c.auto_indent_enabled = true AND l.is_active = true
        GROUP BY c.id, c.product_id, c.location_id, c.reorder_level, c.reorder_qty, c.max_stock_qty, l.default_supplying_location_id
        HAVING COALESCE(SUM(b.qty_on_hand), 0) < c.reorder_level
        """, nativeQuery = true)
    List<ReorderConfigShortageProjection> findShortageCandidates();
}
