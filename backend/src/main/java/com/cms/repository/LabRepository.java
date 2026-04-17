package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Lab;

public interface LabRepository extends JpaRepository<Lab, Long> {

    List<Lab> findByDepartmentId(Long departmentId);

    boolean existsByNameAndDepartmentId(String name, Long departmentId);

    boolean existsByNameAndDepartmentIdAndIdNot(String name, Long departmentId, Long id);
}
