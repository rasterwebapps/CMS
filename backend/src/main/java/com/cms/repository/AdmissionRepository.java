package com.cms.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Admission;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    Optional<Admission> findByStudentId(Long studentId);

    Optional<Admission> findByEnquiryId(Long enquiryId);

    List<Admission> findByJoiningAcademicYearId(Long joiningAcademicYearId);

    List<Admission> findByStudentIdIn(Collection<Long> studentIds);

    boolean existsByStudentId(Long studentId);
}
