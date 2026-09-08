package com.cms.inventory.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.TaxRule;

@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRule, Long>, JpaSpecificationExecutor<TaxRule> {

    List<TaxRule> findByIsActiveTrueOrderByNameAsc();
    List<TaxRule> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
