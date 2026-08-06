package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.RotationMember;

public interface RotationMemberRepository extends JpaRepository<RotationMember, Long> {

    List<RotationMember> findByRotationGroupIdOrderByMemberOrderAsc(Long rotationGroupId);
}
