package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    List<Organization> findAllByOrderByNameAsc();
    List<Organization> findByIsActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
