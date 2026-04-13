package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Admission;
import com.cms.model.enums.AdmissionStatus;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    Optional<Admission> findByStudentId(Long studentId);

    List<Admission> findByStatus(AdmissionStatus status);

    List<Admission> findByAcademicYearFromAndAcademicYearTo(Integer academicYearFrom, Integer academicYearTo);

    boolean existsByStudentId(Long studentId);
}
