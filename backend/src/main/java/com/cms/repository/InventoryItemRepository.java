package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cms.model.InventoryItem;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByLabId(Long labId);

    Optional<InventoryItem> findByItemCode(String itemCode);

    @Query("SELECT i FROM InventoryItem i WHERE i.quantity < i.minimumQuantity")
    List<InventoryItem> findLowStockItems();

    @Query("SELECT i FROM InventoryItem i WHERE i.lab.id = :labId AND i.quantity < i.minimumQuantity")
    List<InventoryItem> findLowStockItemsByLabId(Long labId);
}
