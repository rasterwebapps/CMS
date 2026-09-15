package com.cms.inventory.stock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.stock.model.InventoryRack;

@Repository
public interface InventoryRackRepository extends JpaRepository<InventoryRack, Long>, JpaSpecificationExecutor<InventoryRack> {

    boolean existsByNameIgnoreCaseAndLocationId(String name, Long locationId);
    boolean existsByNameIgnoreCaseAndLocationIdAndIdNot(String name, Long locationId, Long id);

    boolean existsByCodeIgnoreCaseAndLocationId(String code, Long locationId);
    boolean existsByCodeIgnoreCaseAndLocationIdAndIdNot(String code, Long locationId, Long id);

    List<InventoryRack> findByLocationIdOrderByNameAsc(Long locationId);

    List<InventoryRack> findByLocationIdAndIsActiveTrueOrderByNameAsc(Long locationId);

    List<InventoryRack> findByIsActiveTrueOrderByNameAsc();
}
