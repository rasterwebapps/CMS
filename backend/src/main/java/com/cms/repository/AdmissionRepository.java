package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Admission;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    Optional<Admission> findByStudentId(Long studentId);


    List<Admission> findByJoiningAcademicYearId(Long joiningAcademicYearId);

    boolean existsByStudentId(Long studentId);
}
