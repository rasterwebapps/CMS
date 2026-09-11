package com.cms.inventory.procurement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.inventory.procurement.model.InventoryCurrencySetting;

@Repository
public interface InventoryCurrencySettingRepository extends JpaRepository<InventoryCurrencySetting, Long> {
}
