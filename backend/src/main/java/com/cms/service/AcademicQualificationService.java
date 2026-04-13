package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AcademicQualificationRequest;
import com.cms.dto.AcademicQualificationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicQualification;
import com.cms.model.Admission;
import com.cms.repository.AcademicQualificationRepository;
import com.cms.repository.AdmissionRepository;

@Service
@Transactional(readOnly = true)
public class AcademicQualificationService {

    private final AcademicQualificationRepository academicQualificationRepository;
    private final AdmissionRepository admissionRepository;

    public AcademicQualificationService(AcademicQualificationRepository academicQualificationRepository,
                                        AdmissionRepository admissionRepository) {
        this.academicQualificationRepository = academicQualificationRepository;
        this.admissionRepository = admissionRepository;
    }

    @Transactional
    public AcademicQualificationResponse addQualification(Long admissionId, AcademicQualificationRequest request) {
        Admission admission = admissionRepository.findById(admissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + admissionId));
        AcademicQualification qualification = new AcademicQualification(
            admission,
            request.qualificationType(),
            request.schoolName(),
            request.majorSubject(),
            request.totalMarks(),
            request.percentage(),
            request.monthAndYearOfPassing(),
            request.universityOrBoard()
        );
        AcademicQualification saved = academicQualificationRepository.save(qualification);
        return toResponse(saved);
    }

    public List<AcademicQualificationResponse> findByAdmissionId(Long admissionId) {
        return academicQualificationRepository.findByAdmissionId(admissionId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void delete(Long id) {
        if (!academicQualificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Academic qualification not found with id: " + id);
        }
        academicQualificationRepository.deleteById(id);
    }

    private AcademicQualificationResponse toResponse(AcademicQualification qualification) {
        return new AcademicQualificationResponse(
            qualification.getId(),
            qualification.getAdmission().getId(),
            qualification.getQualificationType(),
            qualification.getSchoolName(),
            qualification.getMajorSubject(),
            qualification.getTotalMarks(),
            qualification.getPercentage(),
            qualification.getMonthAndYearOfPassing(),
            qualification.getUniversityOrBoard(),
            qualification.getCreatedAt(),
            qualification.getUpdatedAt()
        );
    }
}
