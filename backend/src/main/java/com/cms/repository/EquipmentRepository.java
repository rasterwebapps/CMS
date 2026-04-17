package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Equipment;
import com.cms.model.enums.EquipmentCategory;
import com.cms.model.enums.EquipmentStatus;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByLabId(Long labId);

    List<Equipment> findByCategory(EquipmentCategory category);

    List<Equipment> findByStatus(EquipmentStatus status);

    List<Equipment> findByLabIdAndStatus(Long labId, EquipmentStatus status);

    Optional<Equipment> findByAssetCode(String assetCode);

    boolean existsByAssetCodeAndIdNot(String assetCode, Long id);

    List<Equipment> findByLabIdAndCategory(Long labId, EquipmentCategory category);
}
