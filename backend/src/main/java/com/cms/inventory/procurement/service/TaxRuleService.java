package com.cms.inventory.procurement.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.procurement.dto.TaxRuleRequest;
import com.cms.inventory.procurement.dto.TaxRuleResponse;
import com.cms.inventory.procurement.model.TaxRule;
import com.cms.inventory.procurement.repository.TaxRuleRepository;

@Service
@Transactional(readOnly = true)
public class TaxRuleService {

    private final TaxRuleRepository taxRuleRepository;

    public TaxRuleService(TaxRuleRepository taxRuleRepository) {
        this.taxRuleRepository = taxRuleRepository;
    }

    @Transactional
    public TaxRuleResponse create(TaxRuleRequest request) {
        String name = requireTrimmed(request.name(), "Name is required");
        if (taxRuleRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A tax rule named '" + name + "' already exists");
        }

        TaxRule taxRule = new TaxRule();
        taxRule.setName(name);
        taxRule.setRatePercent(request.ratePercent());
        if (request.isActive() != null) taxRule.setIsActive(request.isActive());
        return toResponse(taxRuleRepository.save(taxRule));
    }

    public List<TaxRuleResponse> findAll(boolean activeOnly) {
        List<TaxRule> rules = activeOnly ? taxRuleRepository.findByIsActiveTrueOrderByNameAsc() : taxRuleRepository.findAllByOrderByNameAsc();
        return rules.stream().map(this::toResponse).toList();
    }

    public Page<TaxRuleResponse> findPage(String search, Pageable pageable) {
        Specification<TaxRule> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%");
        };
        return taxRuleRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public TaxRuleResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public TaxRuleResponse update(Long id, TaxRuleRequest request) {
        TaxRule taxRule = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Name is required");
        if (taxRuleRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("A tax rule named '" + name + "' already exists");
        }

        taxRule.setName(name);
        taxRule.setRatePercent(request.ratePercent());
        if (request.isActive() != null) taxRule.setIsActive(request.isActive());
        return toResponse(taxRuleRepository.save(taxRule));
    }

    @Transactional
    public void delete(Long id) {
        if (!taxRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tax rule not found with id: " + id);
        }
        taxRuleRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        TaxRule taxRule = findOrThrow(id);
        taxRule.setIsActive(Boolean.TRUE.equals(request.isActive()));
        TaxRule saved = taxRuleRepository.save(taxRule);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        return excludeId != null
            ? taxRuleRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId)
            : taxRuleRepository.existsByNameIgnoreCase(trimmed);
    }

    TaxRule findOrThrow(Long id) {
        return taxRuleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tax rule not found with id: " + id));
    }

    private TaxRuleResponse toResponse(TaxRule t) {
        return new TaxRuleResponse(t.getId(), t.getName(), t.getRatePercent(), t.getIsActive(), t.getCreatedAt(), t.getUpdatedAt());
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
