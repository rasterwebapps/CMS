package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.BloodGroupMaster;

public interface BloodGroupRepository extends JpaRepository<BloodGroupMaster, Long> {
    List<BloodGroupMaster> findByIsActiveTrueOrderByNameAsc();
    List<BloodGroupMaster> findAllByOrderByNameAsc();
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    boolean existsByNameAndIdNot(String name, Long id);
    Optional<BloodGroupMaster> findByCode(String code);
}

