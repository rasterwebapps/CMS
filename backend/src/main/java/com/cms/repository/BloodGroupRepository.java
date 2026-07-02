package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cms.model.BloodGroupMaster;

public interface BloodGroupRepository extends JpaRepository<BloodGroupMaster, Long>, JpaSpecificationExecutor<BloodGroupMaster> {
    List<BloodGroupMaster> findByIsActiveTrueOrderByNameAsc();
    List<BloodGroupMaster> findAllByOrderByNameAsc();
    boolean existsByCode(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    Optional<BloodGroupMaster> findByCode(String code);
}

