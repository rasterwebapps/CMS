package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.model.RoomAllocation;
import com.cms.model.enums.RoomAllocationStatus;

@Repository
public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, Long>, JpaSpecificationExecutor<RoomAllocation> {

    long countByHostelRoomIdAndStatus(Long hostelRoomId, RoomAllocationStatus status);

    boolean existsByStudentIdAndStatus(Long studentId, RoomAllocationStatus status);

    List<RoomAllocation> findByHostelRoomIdAndStatus(Long hostelRoomId, RoomAllocationStatus status);

    List<RoomAllocation> findByStudentIdOrderByStartDateDesc(Long studentId);
}
