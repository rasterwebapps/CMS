package com.cms.inventory.issue.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.issue.model.StockIssueRequestItem;
import com.cms.inventory.issue.model.enums.StockIssueRequestItemStatus;

@Repository
public interface StockIssueRequestItemRepository extends JpaRepository<StockIssueRequestItem, Long> {

    List<StockIssueRequestItem> findByStockIssueRequestIdOrderByIdAsc(Long stockIssueRequestId);

    boolean existsByStockIssueRequestIdAndProductId(Long stockIssueRequestId, Long productId);

    boolean existsByStockIssueRequestIdAndStatus(Long stockIssueRequestId, StockIssueRequestItemStatus status);
}
