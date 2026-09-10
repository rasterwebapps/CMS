package com.cms.inventory.procurement.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.procurement.dto.TaxSubTypeRequest;
import com.cms.inventory.procurement.dto.TaxSubTypeResponse;
import com.cms.inventory.procurement.model.TaxRule;
import com.cms.inventory.procurement.model.TaxSubType;
import com.cms.inventory.procurement.model.enums.JurisdictionMode;
import com.cms.inventory.procurement.repository.TaxRuleRepository;
import com.cms.inventory.procurement.repository.TaxSubTypeRepository;

/**
 * Manages a {@link TaxRule}'s (= a "Tax" row's) component split for each {@link JurisdictionMode}
 * — e.g. "GST 12%" fans out to IGST 100% under INTERSTATE, or CGST 50 / SGST 50 under INTRASTATE.
 * Individual rows are allowed to be saved incrementally (a component set doesn't have to already
 * sum to 100 the moment the first row is added) — the sum is only hard-enforced to equal exactly
 * 100 at the point a component set is actually used to compute tax, in
 * {@link #requireCompleteSplit}, called from {@code PurchaseOrderService.addLine} via {@code
 * JurisdictionService}. What IS enforced immediately here is that a set never exceeds 100, and
 * that component names don't repeat within the same (tax, jurisdiction mode) group. See the
 * "GAP-02 pickup" decision-log entry. Rides on {@code INVENTORY_TAX_RULE_VIEW}/{@code
 * _MANAGE} rather than its own permission pair, per the Security Lead round's decision — a
 * TaxSubType is edited as part of its parent Tax, not a standalone operation.
 */
@Service
@Transactional(readOnly = true)
public class TaxSubTypeService {

    private final TaxSubTypeRepository taxSubTypeRepository;
    private final TaxRuleRepository taxRuleRepository;

    public TaxSubTypeService(TaxSubTypeRepository taxSubTypeRepository, TaxRuleRepository taxRuleRepository) {
        this.taxSubTypeRepository = taxSubTypeRepository;
        this.taxRuleRepository = taxRuleRepository;
    }

    @Transactional
    public TaxSubTypeResponse create(TaxSubTypeRequest request) {
        TaxRule taxRule = requireTaxRule(request.taxRuleId());
        JurisdictionMode mode = parseMode(request.jurisdictionMode());
        String componentName = requireTrimmed(request.componentName(), "Component name is required");

        if (taxSubTypeRepository.existsByTaxRule_IdAndJurisdictionModeAndComponentNameIgnoreCase(taxRule.getId(), mode, componentName)) {
            throw new IllegalArgumentException("'" + componentName + "' already exists for this tax under " + mode.name());
        }
        requireWithinCap(taxRule.getId(), mode, request.splitPercent(), null);

        TaxSubType subType = new TaxSubType();
        subType.setTaxRule(taxRule);
        subType.setJurisdictionMode(mode);
        subType.setComponentName(componentName);
        subType.setSplitPercent(request.splitPercent());
        if (request.isActive() != null) subType.setIsActive(request.isActive());
        return toResponse(taxSubTypeRepository.save(subType));
    }

    public List<TaxSubTypeResponse> findByTaxRule(Long taxRuleId) {
        return taxSubTypeRepository.findByTaxRule_IdOrderByJurisdictionModeAscComponentNameAsc(taxRuleId)
            .stream().map(this::toResponse).toList();
    }

    public TaxSubTypeResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public TaxSubTypeResponse update(Long id, TaxSubTypeRequest request) {
        TaxSubType subType = findOrThrow(id);
        TaxRule taxRule = requireTaxRule(request.taxRuleId());
        JurisdictionMode mode = parseMode(request.jurisdictionMode());
        String componentName = requireTrimmed(request.componentName(), "Component name is required");

        if (taxSubTypeRepository.existsByTaxRule_IdAndJurisdictionModeAndComponentNameIgnoreCaseAndIdNot(
                taxRule.getId(), mode, componentName, id)) {
            throw new IllegalArgumentException("'" + componentName + "' already exists for this tax under " + mode.name());
        }
        requireWithinCap(taxRule.getId(), mode, request.splitPercent(), id);

        subType.setTaxRule(taxRule);
        subType.setJurisdictionMode(mode);
        subType.setComponentName(componentName);
        subType.setSplitPercent(request.splitPercent());
        if (request.isActive() != null) subType.setIsActive(request.isActive());
        return toResponse(taxSubTypeRepository.save(subType));
    }

    @Transactional
    public void delete(Long id) {
        if (!taxSubTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tax sub-type not found with id: " + id);
        }
        taxSubTypeRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        TaxSubType subType = findOrThrow(id);
        subType.setIsActive(Boolean.TRUE.equals(request.isActive()));
        TaxSubType saved = taxSubTypeRepository.save(subType);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    /**
     * The active components for (taxRuleId, mode), guaranteed to sum to exactly 100 — throws if
     * the split isn't configured or is incomplete/over. Called from {@code JurisdictionService}
     * right before a PO line's tax is computed, so a genuinely incomplete config blocks the line
     * with a clear message instead of silently under/over-charging tax.
     */
    public List<TaxSubType> requireCompleteSplit(Long taxRuleId, JurisdictionMode mode) {
        List<TaxSubType> components = taxSubTypeRepository
            .findByTaxRule_IdAndJurisdictionModeAndIsActiveTrueOrderByComponentNameAsc(taxRuleId, mode);
        if (components.isEmpty()) {
            throw new IllegalStateException(
                "This tax has no " + mode.name().toLowerCase(Locale.ROOT) + " tax components configured yet");
        }
        BigDecimal sum = components.stream().map(TaxSubType::getSplitPercent).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalStateException(
                "This tax's " + mode.name().toLowerCase(Locale.ROOT) + " components sum to " + sum
                    + "%, not 100% — finish configuring them before using this tax");
        }
        return components;
    }

    private void requireWithinCap(Long taxRuleId, JurisdictionMode mode, BigDecimal newPercent, Long excludeId) {
        BigDecimal existingSum = taxSubTypeRepository.findByTaxRule_IdAndJurisdictionMode(taxRuleId, mode).stream()
            .filter(s -> excludeId == null || !s.getId().equals(excludeId))
            .map(TaxSubType::getSplitPercent)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (existingSum.add(newPercent).compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                "This would push " + mode.name() + "'s components to " + existingSum.add(newPercent)
                    + "% — the total for a jurisdiction mode cannot exceed 100%");
        }
    }

    private TaxRule requireTaxRule(Long id) {
        return taxRuleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tax not found with id: " + id));
    }

    private JurisdictionMode parseMode(String value) {
        try {
            return JurisdictionMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid jurisdiction mode '" + value + "'");
        }
    }

    TaxSubType findOrThrow(Long id) {
        return taxSubTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tax sub-type not found with id: " + id));
    }

    private TaxSubTypeResponse toResponse(TaxSubType s) {
        TaxRule taxRule = s.getTaxRule();
        return new TaxSubTypeResponse(
            s.getId(), taxRule.getId(), taxRule.getName(), s.getJurisdictionMode().name(),
            s.getComponentName(), s.getSplitPercent(), s.getIsActive(), s.getCreatedAt(), s.getUpdatedAt());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) throw new IllegalArgumentException(message);
        return t;
    }
}
