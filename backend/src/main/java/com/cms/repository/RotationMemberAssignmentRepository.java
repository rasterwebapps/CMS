package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.RotationMemberAssignment;

public interface RotationMemberAssignmentRepository extends JpaRepository<RotationMemberAssignment, Long> {

    List<RotationMemberAssignment> findByRotationSlotIdOrderByRotationMember_MemberOrderAsc(Long rotationSlotId);

    Optional<RotationMemberAssignment> findByRotationMemberIdAndRotationSlotId(Long rotationMemberId, Long rotationSlotId);

    long countByBatchId(Long batchId);
}
