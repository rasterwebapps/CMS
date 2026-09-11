package com.cms.inventory.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.UomConversionTemplate;

@Repository
public interface UomConversionTemplateRepository
        extends JpaRepository<UomConversionTemplate, Long>, JpaSpecificationExecutor<UomConversionTemplate> {

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<UomConversionTemplate> findByIsActiveTrueOrderByNameAsc();
    List<UomConversionTemplate> findAllByOrderByNameAsc();

    /** The picker in a product's Unit Hierarchy section only offers templates whose base unit
     *  matches that product's own — a template built on a different base unit can't apply. */
    List<UomConversionTemplate> findByBaseUomIdAndIsActiveTrueOrderByNameAsc(Long baseUomId);
}
