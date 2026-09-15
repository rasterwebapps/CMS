package com.cms.inventory.stock.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.inventory.stock.model.StockBinAllocation;

@Repository
public interface StockBinAllocationRepository extends JpaRepository<StockBinAllocation, Long> {

    List<StockBinAllocation> findByStockBalanceId(Long stockBalanceId);

    Optional<StockBinAllocation> findByStockBalanceIdAndBinId(Long stockBalanceId, Long binId);

    /**
     * Atomic upsert against the {@code (stock_balance_id, bin_id)} unique constraint — adds the
     * given delta to whatever is already there, or creates the row starting from it. Never call
     * this outside {@code StockMovementService}: it's the only thing allowed to write this table,
     * always paired with the same movement's {@code StockBalance} upsert in the same transaction.
     */
    @Modifying
    @Query(value = """
        INSERT INTO stock_bin_allocations (stock_balance_id, bin_id, qty, last_updated)
        VALUES (:stockBalanceId, :binId, :qtyDelta, now())
        ON CONFLICT (stock_balance_id, bin_id) DO UPDATE SET
          qty = stock_bin_allocations.qty + EXCLUDED.qty,
          last_updated = now()
        """, nativeQuery = true)
    void upsertAllocation(@Param("stockBalanceId") Long stockBalanceId, @Param("binId") Long binId, @Param("qtyDelta") BigDecimal qtyDelta);
}
