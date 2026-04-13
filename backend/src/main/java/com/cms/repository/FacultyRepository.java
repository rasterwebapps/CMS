package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Faculty;
import com.cms.model.enums.FacultyStatus;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    Optional<Faculty> findByEmployeeCode(String employeeCode);

    Optional<Faculty> findByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    List<Faculty> findByDepartmentId(Long departmentId);

    List<Faculty> findByStatus(FacultyStatus status);

    List<Faculty> findByDepartmentIdAndStatus(Long departmentId, FacultyStatus status);
}
