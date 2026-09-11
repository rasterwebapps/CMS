package com.cms.inventory.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.procurement.dto.CurrencyExchangeRateRequest;
import com.cms.inventory.procurement.dto.CurrencyExchangeRateResponse;
import com.cms.inventory.procurement.model.CurrencyExchangeRate;
import com.cms.inventory.procurement.repository.CurrencyExchangeRateRepository;

/** Covers the "Multi-currency FX" Phase 3 item — CRUD on the rate table plus
 *  {@code resolveToBaseCurrency}'s three outcomes (resolved, same-as-base, unresolvable). */
@ExtendWith(MockitoExtension.class)
class CurrencyExchangeRateServiceTest {

    @Mock private CurrencyExchangeRateRepository rateRepository;
    @Mock private InventoryCurrencySettingsService currencySettingsService;

    private CurrencyExchangeRateService service;

    @BeforeEach
    void setUp() {
        service = new CurrencyExchangeRateService(rateRepository, currencySettingsService);
        lenient().when(rateRepository.save(any(CurrencyExchangeRate.class))).thenAnswer(inv -> {
            CurrencyExchangeRate r = inv.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
    }

    private CurrencyExchangeRateRequest request(String code, String rate, LocalDate date) {
        return new CurrencyExchangeRateRequest(code, new BigDecimal(rate), date, null);
    }

    @Test
    void createsRate() {
        when(rateRepository.existsByCurrencyCodeIgnoreCaseAndEffectiveDate("USD", LocalDate.of(2026, 9, 1))).thenReturn(false);

        CurrencyExchangeRateResponse response = service.create(request("usd", "83.50", LocalDate.of(2026, 9, 1)));

        assertThat(response.currencyCode()).isEqualTo("USD");
        assertThat(response.rateToBase()).isEqualByComparingTo("83.50");
    }

    @Test
    void rejectsDuplicateCurrencyAndDatePair() {
        when(rateRepository.existsByCurrencyCodeIgnoreCaseAndEffectiveDate("USD", LocalDate.of(2026, 9, 1))).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("USD", "83.50", LocalDate.of(2026, 9, 1))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void throwsWhenUpdatingUnknownRate() {
        when(rateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("USD", "83.50", LocalDate.of(2026, 9, 1))))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteThrowsWhenRateNotFound() {
        when(rateRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── resolveToBaseCurrency ────────────────────────────────────────────────

    @Test
    void resolvesNullWhenBaseCurrencyNotConfigured() {
        when(currencySettingsService.findBaseCurrencyCodeOrNull()).thenReturn(null);

        assertThat(service.resolveToBaseCurrency("USD", new BigDecimal("100"))).isNull();
    }

    @Test
    void resolvesSameAmountWhenCurrencyAlreadyIsBaseCurrency() {
        when(currencySettingsService.findBaseCurrencyCodeOrNull()).thenReturn("INR");

        CurrencyExchangeRateService.Resolved resolved = service.resolveToBaseCurrency("INR", new BigDecimal("100"));

        assertThat(resolved).isNotNull();
        assertThat(resolved.baseCurrencyCode()).isEqualTo("INR");
        assertThat(resolved.amount()).isEqualByComparingTo("100");
    }

    @Test
    void resolvesConvertedAmountUsingMostRecentRateOnOrBeforeToday() {
        when(currencySettingsService.findBaseCurrencyCodeOrNull()).thenReturn("INR");
        CurrencyExchangeRate rate = new CurrencyExchangeRate();
        rate.setCurrencyCode("USD");
        rate.setRateToBase(new BigDecimal("83.5"));
        when(rateRepository.findFirstByCurrencyCodeIgnoreCaseAndIsActiveTrueAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            "USD", LocalDate.now())).thenReturn(Optional.of(rate));

        CurrencyExchangeRateService.Resolved resolved = service.resolveToBaseCurrency("USD", new BigDecimal("10"));

        assertThat(resolved).isNotNull();
        assertThat(resolved.baseCurrencyCode()).isEqualTo("INR");
        assertThat(resolved.amount()).isEqualByComparingTo("835.00");
    }

    @Test
    void resolvesNullWhenNoRateOnFileForCurrency() {
        when(currencySettingsService.findBaseCurrencyCodeOrNull()).thenReturn("INR");
        when(rateRepository.findFirstByCurrencyCodeIgnoreCaseAndIsActiveTrueAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            "USD", LocalDate.now())).thenReturn(Optional.empty());

        assertThat(service.resolveToBaseCurrency("USD", new BigDecimal("10"))).isNull();
    }

    @Test
    void pairExistsDelegatesToRepository() {
        when(rateRepository.existsByCurrencyCodeIgnoreCaseAndEffectiveDate("USD", LocalDate.of(2026, 9, 1))).thenReturn(true);

        assertThat(service.pairExists("USD", LocalDate.of(2026, 9, 1), null)).isTrue();
    }
}
