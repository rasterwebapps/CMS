package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.RotationGroup;

public interface RotationGroupRepository extends JpaRepository<RotationGroup, Long> {

    List<RotationGroup> findByTermInstanceIdAndIsActiveTrue(Long termInstanceId);
}
