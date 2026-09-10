package com.cms.inventory.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.TaxSubType;
import com.cms.inventory.procurement.model.enums.JurisdictionMode;

@Repository
public interface TaxSubTypeRepository extends JpaRepository<TaxSubType, Long>, JpaSpecificationExecutor<TaxSubType> {

    List<TaxSubType> findByTaxRule_IdOrderByJurisdictionModeAscComponentNameAsc(Long taxRuleId);

    List<TaxSubType> findByTaxRule_IdAndJurisdictionModeAndIsActiveTrueOrderByComponentNameAsc(
        Long taxRuleId, JurisdictionMode jurisdictionMode);

    /** Every active row sharing (taxRuleId, jurisdictionMode) — used to validate the sum-to-100 invariant. */
    List<TaxSubType> findByTaxRule_IdAndJurisdictionMode(Long taxRuleId, JurisdictionMode jurisdictionMode);

    boolean existsByTaxRule_IdAndJurisdictionModeAndComponentNameIgnoreCase(
        Long taxRuleId, JurisdictionMode jurisdictionMode, String componentName);

    boolean existsByTaxRule_IdAndJurisdictionModeAndComponentNameIgnoreCaseAndIdNot(
        Long taxRuleId, JurisdictionMode jurisdictionMode, String componentName, Long id);
}
