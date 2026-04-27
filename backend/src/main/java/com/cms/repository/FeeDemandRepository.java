package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.FeeDemand;
import com.cms.model.enums.DemandStatus;

public interface FeeDemandRepository extends JpaRepository<FeeDemand, Long> {

    List<FeeDemand> findByTermInstanceId(Long termInstanceId);

    Optional<FeeDemand> findByStudentTermEnrollmentId(Long enrollmentId);

    List<FeeDemand> findByTermInstanceIdAndStatus(Long termInstanceId, DemandStatus status);

    List<FeeDemand> findByTermInstanceIdAndStatusNot(Long termInstanceId, DemandStatus status);

    List<FeeDemand> findByAcademicYearId(Long academicYearId);

    List<FeeDemand> findByStudentTermEnrollmentStudentId(Long studentId);
}
