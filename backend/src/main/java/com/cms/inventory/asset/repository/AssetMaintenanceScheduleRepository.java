package com.cms.inventory.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.asset.model.AssetMaintenanceSchedule;

@Repository
public interface AssetMaintenanceScheduleRepository
        extends JpaRepository<AssetMaintenanceSchedule, Long>, JpaSpecificationExecutor<AssetMaintenanceSchedule> {
}
