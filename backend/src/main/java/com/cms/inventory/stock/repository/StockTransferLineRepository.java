package com.cms.inventory.stock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.stock.model.StockTransferLine;

@Repository
public interface StockTransferLineRepository extends JpaRepository<StockTransferLine, Long> {

    List<StockTransferLine> findByStockTransferIdOrderByIdAsc(Long stockTransferId);

    boolean existsByStockTransferIdAndProductIdAndVariantId(Long stockTransferId, Long productId, Long variantId);

    boolean existsByStockTransferIdAndProductIdAndVariantIsNull(Long stockTransferId, Long productId);
}
