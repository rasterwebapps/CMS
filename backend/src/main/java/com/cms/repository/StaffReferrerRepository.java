package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cms.model.StaffReferrer;

public interface StaffReferrerRepository extends JpaRepository<StaffReferrer, Long>, JpaSpecificationExecutor<StaffReferrer> {

    List<StaffReferrer> findByIsActiveTrue();

    List<StaffReferrer> findByNameContainingIgnoreCase(String name);

    boolean existsByInstitutionIdAndNameIgnoreCase(Long institutionId, String name);

    boolean existsByInstitutionIdAndNameIgnoreCaseAndIdNot(Long institutionId, String name, Long id);

    boolean existsByInstitutionIdAndEmployeeCodeIgnoreCase(Long institutionId, String employeeCode);

    boolean existsByInstitutionIdAndEmployeeCodeIgnoreCaseAndIdNot(Long institutionId, String employeeCode, Long id);
}
