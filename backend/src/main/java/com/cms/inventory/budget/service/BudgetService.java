package com.cms.inventory.budget.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.budget.dto.BudgetRequest;
import com.cms.inventory.budget.dto.BudgetResponse;
import com.cms.inventory.budget.model.Budget;
import com.cms.inventory.budget.repository.BudgetRepository;
import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;
import com.cms.inventory.procurement.repository.PurchaseOrderItemRepository;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;

/**
 * Owns Budget allocations — Phase 6's ("Budgets & Approvals") first slice. {@code consumedAmount}
 * is computed live from {@link PurchaseOrderItemRepository#sumCommittedSpendForLocationAndDateRange}
 * (never stored on the {@link Budget} row itself) and surfaced purely as informational
 * allocated-vs-consumed — nothing in this slice blocks a Purchase Requisition/Order submit from
 * exceeding it. Real hard enforcement is a deliberate future escalation with its own review, per
 * the plan. See the "Budget allocation slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final InventoryLocationRepository locationRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    public BudgetService(BudgetRepository budgetRepository,
                          InventoryLocationRepository locationRepository,
                          PurchaseOrderItemRepository purchaseOrderItemRepository) {
        this.budgetRepository = budgetRepository;
        this.locationRepository = locationRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
    }

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        Budget budget = new Budget();
        applyRequest(budget, request);
        return toResponse(budgetRepository.save(budget));
    }

    public Page<BudgetResponse> findPage(Long locationId, Boolean activeOnly, Pageable pageable) {
        Specification<Budget> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (Boolean.TRUE.equals(activeOnly)) predicate = cb.and(predicate, cb.isTrue(root.get("isActive")));
            return predicate;
        };
        return budgetRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public BudgetResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public BudgetResponse update(Long id, BudgetRequest request) {
        Budget budget = findOrThrow(id);
        applyRequest(budget, request);
        return toResponse(budgetRepository.save(budget));
    }

    private void applyRequest(Budget budget, BudgetRequest request) {
        if (request.periodEndDate().isBefore(request.periodStartDate())) {
            throw new IllegalArgumentException("Period end date cannot be before the start date");
        }
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));

        budget.setLocation(location);
        budget.setPeriodStartDate(request.periodStartDate());
        budget.setPeriodEndDate(request.periodEndDate());
        budget.setAllocatedAmount(request.allocatedAmount());
        budget.setNotes(trim(request.notes()));
        if (request.isActive() != null) budget.setIsActive(request.isActive());
    }

    private Budget findOrThrow(Long id) {
        return budgetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private BudgetResponse toResponse(Budget budget) {
        InventoryLocation location = budget.getLocation();
        BigDecimal consumed = purchaseOrderItemRepository.sumCommittedSpendForLocationAndDateRange(
            location.getId(), budget.getPeriodStartDate(), budget.getPeriodEndDate(), PurchaseOrderStatus.PENDING);
        BigDecimal remaining = budget.getAllocatedAmount().subtract(consumed);
        return new BudgetResponse(
            budget.getId(), location.getId(), location.getVirtualName(),
            budget.getPeriodStartDate(), budget.getPeriodEndDate(),
            budget.getAllocatedAmount(), consumed, remaining, remaining.signum() < 0,
            budget.getNotes(), budget.getIsActive(), budget.getCreatedAt(), budget.getUpdatedAt());
    }
}
