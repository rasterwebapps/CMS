package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.StaffReferrer;

public interface StaffReferrerRepository extends JpaRepository<StaffReferrer, Long> {

    List<StaffReferrer> findByIsActiveTrue();

    List<StaffReferrer> findByNameContainingIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
