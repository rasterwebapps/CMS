package com.cms.inventory.procurement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.VendorProductMapping;

@Repository
public interface VendorProductMappingRepository
        extends JpaRepository<VendorProductMapping, Long>, JpaSpecificationExecutor<VendorProductMapping> {

    boolean existsBySupplierIdAndProductIdAndIsActiveTrue(Long supplierId, Long productId);

    boolean existsBySupplierIdAndProductIdAndIsActiveTrueAndIdNot(Long supplierId, Long productId, Long id);
}
