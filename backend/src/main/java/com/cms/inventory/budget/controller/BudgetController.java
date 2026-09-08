package com.cms.inventory.budget.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.budget.dto.BudgetRequest;
import com.cms.inventory.budget.dto.BudgetResponse;
import com.cms.inventory.budget.service.BudgetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/budget/budgets")
public class BudgetController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_BUDGET_VIEW', 'INVENTORY_BUDGET_MANAGE')";

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_BUDGET_MANAGE')")
    public ResponseEntity<BudgetResponse> create(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.create(request));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<BudgetResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 25, sort = "periodStartDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(budgetService.findPage(locationId, activeOnly, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<BudgetResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_BUDGET_MANAGE')")
    public ResponseEntity<BudgetResponse> update(@PathVariable Long id, @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.update(id, request));
    }
}
