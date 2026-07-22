package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Floor;

public interface FloorRepository extends JpaRepository<Floor, Long> {
    List<Floor> findByBlockIdOrderByFloorNumberAsc(Long blockId);
    List<Floor> findByBlockIdAndIsActiveTrueOrderByFloorNumberAsc(Long blockId);

    boolean existsByBlockIdAndNameIgnoreCase(Long blockId, String name);
    boolean existsByBlockIdAndNameIgnoreCaseAndIdNot(Long blockId, String name, Long id);

    boolean existsByBlockIdAndFloorNumber(Long blockId, Integer floorNumber);
    boolean existsByBlockIdAndFloorNumberAndIdNot(Long blockId, Integer floorNumber, Long id);
}
