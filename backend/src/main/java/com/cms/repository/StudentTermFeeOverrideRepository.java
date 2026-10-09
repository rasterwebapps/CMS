package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.StudentTermFeeOverride;

public interface StudentTermFeeOverrideRepository extends JpaRepository<StudentTermFeeOverride, Long> {

    List<StudentTermFeeOverride> findByStudentId(Long studentId);

    Optional<StudentTermFeeOverride> findByStudentIdAndSemesterNumber(Long studentId, Integer semesterNumber);
}
