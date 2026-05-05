package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cms.model.PpeItem;
import com.cms.model.enums.PpeCondition;

public interface PpeItemRepository extends JpaRepository<PpeItem, Long> {

    List<PpeItem> findByLabId(Long labId);

    List<PpeItem> findByCondition(PpeCondition condition);

    @Query("SELECT p FROM PpeItem p WHERE p.availableQuantity < p.minimumRequired")
    List<PpeItem> findLowStockItems();
}

