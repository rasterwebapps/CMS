package com.cms.inventory.procurement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

    List<Supplier> findByIsActiveTrueOrderBySupplierNameAsc();
    List<Supplier> findAllByOrderBySupplierNameAsc();

    boolean existsBySupplierCodeIgnoreCase(String supplierCode);
    boolean existsBySupplierCodeIgnoreCaseAndIdNot(String supplierCode, Long id);
}
