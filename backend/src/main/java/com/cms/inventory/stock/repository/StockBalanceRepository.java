package com.cms.inventory.stock.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.inventory.stock.model.StockBalance;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, Long>, JpaSpecificationExecutor<StockBalance> {

    Optional<StockBalance> findByProductIdAndLocationIdAndBatchId(Long productId, Long locationId, Long batchId);

    Optional<StockBalance> findByProductIdAndLocationIdAndBatchIsNull(Long productId, Long locationId);

    /**
     * Every product currently holding any balance (batched or unbatched) at a location, each
     * summed across its batches — used to auto-populate a FULL_LOCATION {@code CycleCount}'s
     * sheet and to compute a line's {@code systemQtySnapshot}. See {@code ProductQtyProjection}.
     */
    @Query("""
        SELECT b.product.id AS productId, SUM(b.qtyOnHand) AS qty
        FROM StockBalance b
        WHERE b.location.id = :locationId
        GROUP BY b.product.id
        """)
    List<ProductQtyProjection> sumQtyByProductForLocation(@Param("locationId") Long locationId);

    /** Same total as above, for one product only — used when a product is added to a count
     *  sheet ad-hoc. Returns {@code null} (not zero) if the product has no balance row at all. */
    @Query("""
        SELECT SUM(b.qtyOnHand)
        FROM StockBalance b
        WHERE b.product.id = :productId AND b.location.id = :locationId
        """)
    BigDecimal sumQtyForProductAndLocation(@Param("productId") Long productId, @Param("locationId") Long locationId);

    /**
     * Atomic upsert against the {@code (product_id, location_id, batch_id)} unique constraint
     * (NULLS NOT DISTINCT) — adds the given deltas to whatever is already there, or creates the
     * row starting from these deltas. Never call this outside {@code StockMovementService}: it's
     * the only thing allowed to write this table, always paired with a {@code StockLedger} insert
     * in the same transaction.
     */
    @Modifying
    @Query(value = """
        INSERT INTO stock_balances (product_id, location_id, batch_id, qty_on_hand, value_on_hand, last_updated)
        VALUES (:productId, :locationId, :batchId, :qtyDelta, :valueDelta, now())
        ON CONFLICT (product_id, location_id, batch_id) DO UPDATE SET
          qty_on_hand = stock_balances.qty_on_hand + EXCLUDED.qty_on_hand,
          value_on_hand = stock_balances.value_on_hand + EXCLUDED.value_on_hand,
          last_updated = now()
        """, nativeQuery = true)
    void upsertBalance(@Param("productId") Long productId, @Param("locationId") Long locationId,
                        @Param("batchId") Long batchId, @Param("qtyDelta") BigDecimal qtyDelta, @Param("valueDelta") BigDecimal valueDelta);
}
