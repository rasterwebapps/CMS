package com.cms.inventory.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.TaxType;

@Repository
public interface TaxTypeRepository extends JpaRepository<TaxType, Long>, JpaSpecificationExecutor<TaxType> {

    List<TaxType> findByIsActiveTrueOrderByNameAsc();
    List<TaxType> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
