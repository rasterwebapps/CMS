package com.cms.inventory.procurement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.procurement.dto.CurrencyExchangeRateRequest;
import com.cms.inventory.procurement.dto.CurrencyExchangeRateResponse;
import com.cms.inventory.procurement.model.CurrencyExchangeRate;
import com.cms.inventory.procurement.repository.CurrencyExchangeRateRepository;

/**
 * The manually-maintained {@code currency_exchange_rates} reference table, and the resolution
 * logic ({@link #resolveToBaseCurrency}) {@code VendorProductMappingService} uses to show a
 * mapping's price in base-currency terms. See the 2026-09-11 "Multi-currency FX" decision-log
 * entry.
 */
@Service
@Transactional(readOnly = true)
public class CurrencyExchangeRateService {

    private final CurrencyExchangeRateRepository rateRepository;
    private final InventoryCurrencySettingsService currencySettingsService;

    public CurrencyExchangeRateService(CurrencyExchangeRateRepository rateRepository,
                                        InventoryCurrencySettingsService currencySettingsService) {
        this.rateRepository = rateRepository;
        this.currencySettingsService = currencySettingsService;
    }

    @Transactional
    public CurrencyExchangeRateResponse create(CurrencyExchangeRateRequest request) {
        CurrencyExchangeRate rate = new CurrencyExchangeRate();
        applyRequest(rate, request, null);
        return toResponse(rateRepository.save(rate));
    }

    public List<CurrencyExchangeRateResponse> findAll() {
        return rateRepository.findAllByOrderByCurrencyCodeAscEffectiveDateDesc().stream().map(this::toResponse).toList();
    }

    public Page<CurrencyExchangeRateResponse> findPage(String search, Pageable pageable) {
        Specification<CurrencyExchangeRate> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("currencyCode")), "%" + search.trim().toLowerCase() + "%");
        };
        return rateRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public CurrencyExchangeRateResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public CurrencyExchangeRateResponse update(Long id, CurrencyExchangeRateRequest request) {
        CurrencyExchangeRate rate = findOrThrow(id);
        applyRequest(rate, request, id);
        return toResponse(rateRepository.save(rate));
    }

    @Transactional
    public void delete(Long id) {
        if (!rateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Currency exchange rate not found with id: " + id);
        }
        rateRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        CurrencyExchangeRate rate = findOrThrow(id);
        rate.setIsActive(Boolean.TRUE.equals(request.isActive()));
        CurrencyExchangeRate saved = rateRepository.save(rate);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    /**
     * {@code amount} in {@code currencyCode} converted to the institution's base currency, as of
     * today — {@code null} whenever conversion can't be resolved (no base currency configured
     * yet, or no exchange rate on file for that currency as of today), never an exception: a
     * missing rate shouldn't block whatever screen is showing the mapping. Returns {@code amount}
     * itself unchanged when {@code currencyCode} already *is* the base currency (rate 1:1, no
     * lookup needed).
     */
    public Resolved resolveToBaseCurrency(String currencyCode, BigDecimal amount) {
        if (currencyCode == null || amount == null) return null;
        String baseCurrencyCode = currencySettingsService.findBaseCurrencyCodeOrNull();
        if (baseCurrencyCode == null) return null;
        if (currencyCode.equalsIgnoreCase(baseCurrencyCode)) {
            return new Resolved(baseCurrencyCode, amount);
        }
        return rateRepository.findFirstByCurrencyCodeIgnoreCaseAndIsActiveTrueAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                currencyCode, LocalDate.now())
            .map(rate -> new Resolved(baseCurrencyCode, amount.multiply(rate.getRateToBase()).setScale(2, RoundingMode.HALF_UP)))
            .orElse(null);
    }

    /** @param baseCurrencyCode the institution's configured base currency. @param amount the
     *  converted amount in that currency. */
    public record Resolved(String baseCurrencyCode, BigDecimal amount) {}

    public boolean pairExists(String currencyCode, LocalDate effectiveDate, Long excludeId) {
        if (currencyCode == null || effectiveDate == null) return false;
        return excludeId != null
            ? rateRepository.existsByCurrencyCodeIgnoreCaseAndEffectiveDateAndIdNot(currencyCode, effectiveDate, excludeId)
            : rateRepository.existsByCurrencyCodeIgnoreCaseAndEffectiveDate(currencyCode, effectiveDate);
    }

    private void applyRequest(CurrencyExchangeRate rate, CurrencyExchangeRateRequest request, Long excludeId) {
        String currencyCode = requireTrimmed(request.currencyCode(), "Currency code is required").toUpperCase();
        boolean taken = excludeId != null
            ? rateRepository.existsByCurrencyCodeIgnoreCaseAndEffectiveDateAndIdNot(currencyCode, request.effectiveDate(), excludeId)
            : rateRepository.existsByCurrencyCodeIgnoreCaseAndEffectiveDate(currencyCode, request.effectiveDate());
        if (taken) {
            throw new IllegalArgumentException(
                "A rate for '" + currencyCode + "' effective " + request.effectiveDate() + " already exists");
        }
        rate.setCurrencyCode(currencyCode);
        rate.setRateToBase(request.rateToBase());
        rate.setEffectiveDate(request.effectiveDate());
        if (request.isActive() != null) rate.setIsActive(request.isActive());
    }

    private CurrencyExchangeRate findOrThrow(Long id) {
        return rateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Currency exchange rate not found with id: " + id));
    }

    private CurrencyExchangeRateResponse toResponse(CurrencyExchangeRate r) {
        return new CurrencyExchangeRateResponse(r.getId(), r.getCurrencyCode(), r.getRateToBase(),
            r.getEffectiveDate(), r.getIsActive(), r.getCreatedAt(), r.getUpdatedAt());
    }

    private static String requireTrimmed(String s, String message) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(message);
        return t;
    }
}
