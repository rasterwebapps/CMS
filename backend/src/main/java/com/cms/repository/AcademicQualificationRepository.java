package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.AcademicQualification;
import com.cms.model.enums.QualificationType;

public interface AcademicQualificationRepository extends JpaRepository<AcademicQualification, Long> {

    List<AcademicQualification> findByAdmissionId(Long admissionId);

    List<AcademicQualification> findByAdmissionIdAndQualificationType(Long admissionId, QualificationType qualificationType);

    boolean existsByAdmissionIdAndQualificationType(Long admissionId, QualificationType qualificationType);

    void deleteByAdmissionId(Long admissionId);
}
