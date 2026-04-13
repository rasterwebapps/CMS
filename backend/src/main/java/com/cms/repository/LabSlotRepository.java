package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.LabSlot;

public interface LabSlotRepository extends JpaRepository<LabSlot, Long> {

    List<LabSlot> findByIsActiveTrue();

    List<LabSlot> findAllByOrderBySlotOrderAsc();

    List<LabSlot> findByIsActiveTrueOrderBySlotOrderAsc();
}
