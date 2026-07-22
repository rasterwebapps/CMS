package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Branch;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByOrganizationIdOrderByNameAsc(Long organizationId);
    List<Branch> findByOrganizationIdAndIsActiveTrueOrderByNameAsc(Long organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);
    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(Long organizationId, String name, Long id);

    boolean existsByOrganizationIdAndCodeIgnoreCase(Long organizationId, String code);
    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(Long organizationId, String code, Long id);
}
