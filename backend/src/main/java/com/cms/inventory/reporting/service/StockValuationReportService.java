package com.cms.inventory.reporting.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.reporting.dto.StockValuationCategoryRow;
import com.cms.inventory.reporting.dto.StockValuationReportResponse;
import com.cms.inventory.stock.model.StockValuationByCategoryProjection;
import com.cms.inventory.stock.repository.StockBalanceRepository;

/**
 * Phase 8's second Reporting slice — the classic ERP "inventory valuation summary": total
 * on-hand value grouped by category, with an optional location filter, computed live from the
 * already-materialized {@code StockBalance} table (same "computed live, never stored" posture as
 * the Dashboard). Deliberately does not sum raw quantity across a category — products in the
 * same category can carry different UOMs, so a summed quantity figure would be meaningless; only
 * a distinct product count and total value (always safely summable) are shown per category. See
 * the "Stock Valuation Report slice" decision-log entry for why this was judged buildable without
 * further product input, revising the earlier, more conservative call in the Phase 8 breakdown.
 */
@Service
@Transactional(readOnly = true)
public class StockValuationReportService {

    private final StockBalanceRepository stockBalanceRepository;

    public StockValuationReportService(StockBalanceRepository stockBalanceRepository) {
        this.stockBalanceRepository = stockBalanceRepository;
    }

    public StockValuationReportResponse get(Long locationId) {
        List<StockValuationByCategoryProjection> rows = stockBalanceRepository.sumValuationByCategory(locationId);

        List<StockValuationCategoryRow> categories = rows.stream()
            .map(r -> new StockValuationCategoryRow(r.getCategoryId(), r.getCategoryName(), r.getProductCount(), r.getTotalValue()))
            .toList();

        long grandTotalProductCount = categories.stream().mapToLong(StockValuationCategoryRow::productCount).sum();
        BigDecimal grandTotalValue = categories.stream()
            .map(StockValuationCategoryRow::totalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StockValuationReportResponse(categories, grandTotalProductCount, grandTotalValue, Instant.now());
    }
}
