package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AcademicYearResponse;
import com.cms.dto.SemesterResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Semester;
import com.cms.model.enums.SemesterStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.SemesterRepository;

@Service
@Transactional(readOnly = true)
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final AcademicYearRepository academicYearRepository;

    public SemesterService(SemesterRepository semesterRepository,
                           AcademicYearRepository academicYearRepository) {
        this.semesterRepository = semesterRepository;
        this.academicYearRepository = academicYearRepository;
    }

    public List<SemesterResponse> findAll() {
        return semesterRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public SemesterResponse findById(Long id) {
        Semester semester = semesterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + id));
        return toResponse(semester);
    }

    public List<SemesterResponse> findByAcademicYearId(Long academicYearId) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("Academic year not found with id: " + academicYearId);
        }
        return semesterRepository.findByAcademicYearIdOrderBySemesterNumber(academicYearId).stream()
            .map(this::toResponse)
            .toList();
    }

    private SemesterResponse toResponse(Semester semester) {
        AcademicYear academicYear = semester.getAcademicYear();
        AcademicYearResponse academicYearResponse = new AcademicYearResponse(
            academicYear.getId(),
            academicYear.getName(),
            academicYear.getStartDate(),
            academicYear.getEndDate(),
            academicYear.getIsCurrent(),
            academicYear.getCreatedAt(),
            academicYear.getUpdatedAt()
        );

        SemesterStatus status = semester.getStatus() != null
            ? semester.getStatus()
            : Semester.deriveStatus(semester.getStartDate(), semester.getEndDate());

        return new SemesterResponse(
            semester.getId(),
            semester.getName(),
            academicYearResponse,
            semester.getStartDate(),
            semester.getEndDate(),
            semester.getSemesterNumber(),
            status,
            semester.getCreatedAt(),
            semester.getUpdatedAt()
        );
    }
}
