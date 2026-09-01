package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.EscortRotationAssignment;

public interface EscortRotationAssignmentRepository extends JpaRepository<EscortRotationAssignment, Long> {

    List<EscortRotationAssignment> findByBatchIdOrderByRotationMember_MemberOrderAsc(Long batchId);

    List<EscortRotationAssignment> findByFaculty_Id(Long facultyId);
}
