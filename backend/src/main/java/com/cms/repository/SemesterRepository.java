package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Semester;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findByAcademicYearId(Long academicYearId);

    List<Semester> findByAcademicYearIdOrderBySemesterNumber(Long academicYearId);

    boolean existsByNameAndAcademicYearIdAndIdNot(String name, Long academicYearId, Long id);
}
