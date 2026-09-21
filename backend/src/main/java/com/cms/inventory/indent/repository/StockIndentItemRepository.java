package com.cms.inventory.indent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.indent.model.StockIndentItem;
import com.cms.inventory.indent.model.enums.StockIndentItemStatus;

@Repository
public interface StockIndentItemRepository extends JpaRepository<StockIndentItem, Long> {

    List<StockIndentItem> findByStockIndentIdOrderByIdAsc(Long stockIndentId);

    boolean existsByStockIndentIdAndProductIdAndVariantId(Long stockIndentId, Long productId, Long variantId);

    boolean existsByStockIndentIdAndProductIdAndVariantIsNull(Long stockIndentId, Long productId);

    boolean existsByStockIndentIdAndStatus(Long stockIndentId, StockIndentItemStatus status);
}
