package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.RotationSlot;

public interface RotationSlotRepository extends JpaRepository<RotationSlot, Long> {

    Optional<RotationSlot> findByClassScheduleId(Long classScheduleId);

    List<RotationSlot> findByRotationGroupIdOrderBySlotOrderAsc(Long rotationGroupId);

    boolean existsByClassScheduleId(Long classScheduleId);
}
