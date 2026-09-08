package com.cms.inventory.reporting.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.budget.dto.BudgetResponse;
import com.cms.inventory.budget.service.BudgetService;
import com.cms.inventory.reporting.dto.BudgetVsActualLocationRow;
import com.cms.inventory.reporting.dto.BudgetVsActualReportResponse;

/**
 * Phase 8's sixth and final Reporting slice — allocated vs. consumed spend rolled up by
 * location, across every currently-active {@code Budget}. Reuses {@code
 * BudgetService.findPage}'s already-computed {@code consumedAmount}/{@code remainingAmount}/
 * {@code overAllocated} for each budget (the same live Purchase-Order-spend calculation the
 * Budget list itself shows) rather than re-deriving it. <strong>Caveat, deliberately accepted
 * rather than solved here:</strong> a location can carry more than one active budget for
 * different, possibly non-overlapping periods (e.g. a Q1 and a Q2 budget both still marked
 * active) — this report sums across all of them per location regardless of period, the same
 * simplification the already-shipped Inventory Dashboard's own "over-allocated budgets" count
 * already makes. A period-aware version (e.g. "this quarter only") needs a real decision about
 * what period a deployment's budgeting calendar uses, which is genuinely out of this session's
 * reach — see the "Budget vs. Actual Report slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class BudgetVsActualReportService {

    private final BudgetService budgetService;

    public BudgetVsActualReportService(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    public BudgetVsActualReportResponse get() {
        List<BudgetResponse> budgets = budgetService.findPage(null, true, Pageable.ofSize(1000)).getContent();

        Map<Long, BudgetVsActualLocationRow> rowsByLocation = new LinkedHashMap<>();
        for (BudgetResponse budget : budgets) {
            BudgetVsActualLocationRow existing = rowsByLocation.get(budget.locationId());
            if (existing == null) {
                rowsByLocation.put(budget.locationId(), new BudgetVsActualLocationRow(
                    budget.locationId(), budget.locationVirtualName(), 1,
                    budget.allocatedAmount(), budget.consumedAmount(), budget.remainingAmount(),
                    budget.overAllocated()));
            } else {
                BigDecimal allocated = existing.allocatedAmount().add(budget.allocatedAmount());
                BigDecimal consumed = existing.consumedAmount().add(budget.consumedAmount());
                rowsByLocation.put(budget.locationId(), new BudgetVsActualLocationRow(
                    budget.locationId(), budget.locationVirtualName(), existing.budgetCount() + 1,
                    allocated, consumed, allocated.subtract(consumed), consumed.compareTo(allocated) > 0));
            }
        }

        List<BudgetVsActualLocationRow> locations = new ArrayList<>(rowsByLocation.values());
        locations.sort((a, b) -> a.locationName().compareToIgnoreCase(b.locationName()));

        long grandTotalBudgetCount = locations.stream().mapToLong(BudgetVsActualLocationRow::budgetCount).sum();
        BigDecimal grandTotalAllocated = sum(locations, BudgetVsActualLocationRow::allocatedAmount);
        BigDecimal grandTotalConsumed = sum(locations, BudgetVsActualLocationRow::consumedAmount);
        BigDecimal grandTotalRemaining = grandTotalAllocated.subtract(grandTotalConsumed);

        return new BudgetVsActualReportResponse(
            locations, grandTotalBudgetCount, grandTotalAllocated, grandTotalConsumed, grandTotalRemaining, Instant.now());
    }

    private static BigDecimal sum(List<BudgetVsActualLocationRow> rows, Function<BudgetVsActualLocationRow, BigDecimal> extractor) {
        return rows.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
