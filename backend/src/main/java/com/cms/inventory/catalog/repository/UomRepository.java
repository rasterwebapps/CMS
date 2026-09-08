package com.cms.inventory.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.Uom;

@Repository
public interface UomRepository extends JpaRepository<Uom, Long>, JpaSpecificationExecutor<Uom> {

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Uom> findByIsActiveTrueOrderByNameAsc();

    List<Uom> findAllByOrderByNameAsc();
}
