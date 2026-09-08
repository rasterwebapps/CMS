package com.cms.inventory.stock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.stock.model.InventoryLocation;

@Repository
public interface InventoryLocationRepository extends JpaRepository<InventoryLocation, Long>, JpaSpecificationExecutor<InventoryLocation> {

    boolean existsByVirtualNameIgnoreCase(String virtualName);
    boolean existsByVirtualNameIgnoreCaseAndIdNot(String virtualName, Long id);

    List<InventoryLocation> findByIsActiveTrueOrderByVirtualNameAsc();

    List<InventoryLocation> findAllByOrderByVirtualNameAsc();
}
