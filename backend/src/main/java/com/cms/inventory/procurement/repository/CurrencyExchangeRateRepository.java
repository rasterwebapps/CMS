package com.cms.inventory.procurement.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.CurrencyExchangeRate;

@Repository
public interface CurrencyExchangeRateRepository
        extends JpaRepository<CurrencyExchangeRate, Long>, JpaSpecificationExecutor<CurrencyExchangeRate> {

    boolean existsByCurrencyCodeIgnoreCaseAndEffectiveDate(String currencyCode, LocalDate effectiveDate);
    boolean existsByCurrencyCodeIgnoreCaseAndEffectiveDateAndIdNot(String currencyCode, LocalDate effectiveDate, Long id);

    /** The rate in effect for {@code currencyCode} as of {@code asOf} — the most recent active
     *  row whose effective date isn't in the future. Used to resolve a VendorProductMapping's
     *  price into base-currency terms; see {@code CurrencyExchangeRateService.resolveRate}. */
    Optional<CurrencyExchangeRate> findFirstByCurrencyCodeIgnoreCaseAndIsActiveTrueAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
        String currencyCode, LocalDate asOf);

    List<CurrencyExchangeRate> findAllByOrderByCurrencyCodeAscEffectiveDateDesc();
}
