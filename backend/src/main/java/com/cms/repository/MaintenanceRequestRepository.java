package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.MaintenanceRequest;
import com.cms.model.enums.MaintenancePriority;
import com.cms.model.enums.MaintenanceStatus;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    List<MaintenanceRequest> findByEquipmentId(Long equipmentId);

    List<MaintenanceRequest> findByStatus(MaintenanceStatus status);

    List<MaintenanceRequest> findByPriority(MaintenancePriority priority);

    List<MaintenanceRequest> findByAssignedToId(Long assignedToId);

    List<MaintenanceRequest> findByRequestedById(Long requestedById);

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.status IN :statuses")
    List<MaintenanceRequest> findByStatusIn(@Param("statuses") List<MaintenanceStatus> statuses);

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.equipment.lab.id = :labId")
    List<MaintenanceRequest> findByLabId(@Param("labId") Long labId);

    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.status <> 'COMPLETED' AND mr.status <> 'CANCELLED'")
    List<MaintenanceRequest> findPendingRequests();
}
