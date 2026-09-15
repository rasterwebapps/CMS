package com.cms.inventory.stock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.stock.model.InventoryBin;

@Repository
public interface InventoryBinRepository extends JpaRepository<InventoryBin, Long>, JpaSpecificationExecutor<InventoryBin> {

    boolean existsByNameIgnoreCaseAndRackId(String name, Long rackId);
    boolean existsByNameIgnoreCaseAndRackIdAndIdNot(String name, Long rackId, Long id);

    boolean existsByCodeIgnoreCaseAndRackId(String code, Long rackId);
    boolean existsByCodeIgnoreCaseAndRackIdAndIdNot(String code, Long rackId, Long id);

    List<InventoryBin> findByRackIdOrderByNameAsc(Long rackId);

    List<InventoryBin> findByRackIdAndIsActiveTrueOrderByNameAsc(Long rackId);

    List<InventoryBin> findByRackLocationIdAndIsActiveTrueOrderByNameAsc(Long locationId);

    List<InventoryBin> findByIsActiveTrueOrderByNameAsc();
}
