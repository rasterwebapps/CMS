package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.LabInChargeAssignment;

public interface LabInChargeAssignmentRepository extends JpaRepository<LabInChargeAssignment, Long> {

    List<LabInChargeAssignment> findByLabId(Long labId);

    boolean existsByLabIdAndAssigneeId(Long labId, Long assigneeId);

    void deleteByLabId(Long labId);
}
